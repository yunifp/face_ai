package com.example.biometrikapp.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import androidx.core.graphics.scale

// Enum untuk mengatur tahapan pose
enum class TargetPose { FRONT, LEFT, RIGHT, DONE }

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FaceRegisterCameraScreen(
    onAllCaptured: (List<Bitmap>) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // State Tahapan Perekaman
    var isStarted by remember { mutableStateOf(false) }
    var currentPose by remember { mutableStateOf(TargetPose.FRONT) }
    var isPoseCorrect by remember { mutableStateOf(false) } // Aktif jika wajah mengarah ke target yg benar
    var isCapturing by remember { mutableStateOf(false) } // Mencegah jepretan ganda
    var showFlash by remember { mutableStateOf(false) }

    // Menyimpan hasil 3 foto
    val collectedBitmaps = remember { mutableListOf<Bitmap>() }

    // CameraX UseCases
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Instruksi Dinamis
    val instruction = when {
        currentPose == TargetPose.DONE -> "Selesai! Memproses data..."
        !isStarted -> "Tekan Mulai untuk verifikasi wajah"
        currentPose == TargetPose.FRONT -> "1. Tatap LURUS ke kamera"
        currentPose == TargetPose.LEFT -> "2. Tengok sedikit ke KIRI"
        currentPose == TargetPose.RIGHT -> "3. Tengok sedikit ke KANAN"
        else -> ""
    }

    // Warna oval indikator (Hijau jika pose benar, Biru jika sedang merekam tapi pose belum pas, Putih default)
    val indicatorColor = if (currentPose == TargetPose.DONE) Color.Green
    else if (isPoseCorrect) Color.Green
    else if (isStarted) Color(0xFF2EA8FF)
    else Color.White

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // 1. Layer Kamera (CameraX)
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }

                    // Setup ML Kit Face Analyzer
                    val options = FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                        .build()
                    val detector = FaceDetection.getClient(options)

                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalyzer.setAnalyzer(cameraExecutor) { imageProxy ->
                        if (!isStarted || currentPose == TargetPose.DONE || isCapturing) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        @SuppressLint("UnsafeOptInUsageError")
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            detector.process(image)
                                .addOnSuccessListener { faces ->
                                    if (faces.isNotEmpty()) {
                                        val face = faces[0]
                                        // Euler Y mengukur rotasi kepala ke kiri/kanan.
                                        // Nilai positif = nengok kiri, Negatif = nengok kanan
                                        val rotY = face.headEulerAngleY

                                        // Cek apakah pose saat ini sesuai target
                                        val correct = when (currentPose) {
                                            TargetPose.FRONT -> rotY > -10 && rotY < 10
                                            TargetPose.LEFT -> rotY > 18  // Wajah ke kiri
                                            TargetPose.RIGHT -> rotY < -18 // Wajah ke kanan
                                            else -> false
                                        }

                                        isPoseCorrect = correct

                                        // Jika pose benar, otomatis jepret foto
                                        if (correct && !isCapturing) {
                                            isCapturing = true
                                            scope.launch {
                                                // Beri waktu 0.5 detik agar pengguna stabil di posisi tersebut
                                                delay(500)
                                                if (isPoseCorrect) { // Pastikan masih di pose yang benar
                                                    showFlash = true
                                                    val bitmap = captureFrameInMemory(imageCapture, context)
                                                    delay(50)
                                                    showFlash = false

                                                    if (bitmap != null) {
                                                        collectedBitmaps.add(bitmap)
                                                        // Lanjut ke pose berikutnya
                                                        currentPose = when (currentPose) {
                                                            TargetPose.FRONT -> TargetPose.LEFT
                                                            TargetPose.LEFT -> TargetPose.RIGHT
                                                            TargetPose.RIGHT -> TargetPose.DONE
                                                            TargetPose.DONE -> TargetPose.DONE
                                                        }

                                                        if (currentPose == TargetPose.DONE) {
                                                            delay(1000)
                                                            onAllCaptured(collectedBitmaps.toList())
                                                        }
                                                    }
                                                }
                                                isCapturing = false
                                            }
                                        }
                                    } else {
                                        isPoseCorrect = false
                                    }
                                }
                                .addOnCompleteListener { imageProxy.close() }
                        } else {
                            imageProxy.close()
                        }
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_FRONT_CAMERA,
                            preview,
                            imageCapture,
                            imageAnalyzer // Tambahkan Analyzer ke Lifecycle
                        )
                    } catch (e: Exception) {
                        Log.e("CameraX", "Kamera gagal di-bind", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Layer Overlay Gelap dengan Lubang Oval
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val ovalWidth = canvasWidth * 0.72f
            val ovalHeight = ovalWidth * 1.35f
            val topLeft = Offset((canvasWidth - ovalWidth) / 2, (canvasHeight - ovalHeight) / 2.5f)

            drawRect(color = Color.Black.copy(alpha = 0.6f))

            drawRoundRect(
                color = Color.Transparent,
                topLeft = topLeft,
                size = Size(ovalWidth, ovalHeight),
                cornerRadius = CornerRadius(ovalWidth / 2, ovalHeight / 2),
                blendMode = BlendMode.Clear
            )

            drawRoundRect(
                color = indicatorColor,
                topLeft = topLeft,
                size = Size(ovalWidth, ovalHeight),
                cornerRadius = CornerRadius(ovalWidth / 2, ovalHeight / 2),
                style = Stroke(width = if (isPoseCorrect) 14f else 8f)
            )
        }

        // 3. Efek Flash Kamera Putih
        AnimatedVisibility(visible = showFlash, enter = fadeIn(tween(50)), exit = fadeOut(tween(200))) {
            Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.8f)))
        }

        // 4. Layer UI Controls (Instruksi & Progress)
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            Text("VERIFIKASI WAJAH", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)

            // Teks instruksi
            Surface(
                color = Color.Black.copy(0.7f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    text = instruction,
                    color = indicatorColor,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Progress Bar (3 Step)
            if (isStarted) {
                Row(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val step1Color = if (collectedBitmaps.isNotEmpty()) Color.Green else Color.Gray
                    val step2Color = if (collectedBitmaps.size >= 2) Color.Green else Color.Gray
                    val step3Color = if (collectedBitmaps.size >= 3) Color.Green else Color.Gray

                    Box(modifier = Modifier.height(8.dp).weight(1f).clip(RoundedCornerShape(50)).background(step1Color))
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.height(8.dp).weight(1f).clip(RoundedCornerShape(50)).background(step2Color))
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.height(8.dp).weight(1f).clip(RoundedCornerShape(50)).background(step3Color))
                }
                Spacer(modifier = Modifier.height(30.dp))
            }

            // Tombol Mulai Rekam / Selesai
            if (currentPose == TargetPose.DONE) {
                Icon(Icons.Default.Check, "Sukses", tint = Color.Green, modifier = Modifier.size(64.dp).background(Color.White, CircleShape).padding(8.dp))
            } else if (!isStarted) {
                FloatingActionButton(
                    onClick = { isStarted = true },
                    containerColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(76.dp).padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.Face, contentDescription = "Mulai", modifier = Modifier.size(36.dp), tint = Color.Black)
                }
            } else {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onCancel, enabled = !isStarted) {
                Text("Batal", color = if (isStarted) Color.Transparent else Color.White, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Fungsi mengecilkan memori foto (Tetap sama seperti milik Anda)
suspend fun captureFrameInMemory(imageCapture: ImageCapture, context: Context): Bitmap? = suspendCancellableCoroutine { cont ->
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                try {
                    val originalBitmap = imageProxy.toBitmap()
                    val targetWidth = 480f
                    val scaleFactor = targetWidth / originalBitmap.width
                    val targetHeight = (originalBitmap.height * scaleFactor).toInt()
                    val resizedBitmap = originalBitmap.scale(targetWidth.toInt(), targetHeight)
                    imageProxy.close()
                    cont.resume(resizedBitmap)
                } catch (e: Exception) {
                    imageProxy.close()
                    cont.resume(null)
                }
            }
            override fun onError(exception: ImageCaptureException) {
                cont.resume(null)
            }
        }
    )
}