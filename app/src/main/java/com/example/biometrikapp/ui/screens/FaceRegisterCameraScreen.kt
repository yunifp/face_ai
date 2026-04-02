package com.example.biometrikapp.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
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
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FaceRegisterCameraScreen(
    onAllCaptured: (List<Bitmap>) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // Total frame yang akan diambil selama 5 detik (2 frame/detik = 10 frame)
    val totalFrames = 10
    var capturedCount by remember { mutableIntStateOf(0) }

    // State Animasi & Proses
    var isRecording by remember { mutableStateOf(false) }
    var showFlash by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }

    val imageCapture = remember { ImageCapture.Builder().build() }

    // --- ANIMASI LIVENESS (WARNA-WARNI UNTUK PANTULAN WAJAH) ---
    val infiniteTransition = rememberInfiniteTransition(label = "liveness")
    val livenessColor by infiniteTransition.animateColor(
        initialValue = Color(0xFFFF4B4B), // Merah
        targetValue = Color(0xFFFF4B4B),
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2500 // Kecepatan siklus warna
                Color(0xFFFF4B4B) at 0
                Color(0xFF4CAF50) at 500
                Color(0xFF2EA8FF) at 1000
                Color(0xFFFFC107) at 1500
                Color(0xFFE040FB) at 2000
                Color(0xFFFF4B4B) at 2500
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "livenessColor"
    )

    // Instruksi Dinamis
    val instruction = when {
        showSuccess -> "Perekaman Selesai!"
        isRecording -> "Putar kepala Anda perlahan...\n($capturedCount / $totalFrames)"
        else -> "Tekan Mulai dan putar kepala\nke kiri dan kanan selama 5 detik"
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // 1. Layer Kamera
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageCapture)
                    } catch (e: Exception) {
                        Log.e("CameraX", "Kamera gagal di-bind", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Layer Overlay Gelap dengan Lubang Oval & Efek Warna-Warni
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val ovalWidth = canvasWidth * 0.72f
            val ovalHeight = ovalWidth * 1.35f
            val topLeft = Offset((canvasWidth - ovalWidth) / 2, (canvasHeight - ovalHeight) / 2.5f)

            drawRect(color = Color.Black.copy(alpha = 0.5f))
            drawRect(color = livenessColor.copy(alpha = 0.35f))

            drawRoundRect(
                color = Color.Transparent,
                topLeft = topLeft,
                size = Size(ovalWidth, ovalHeight),
                cornerRadius = CornerRadius(ovalWidth / 2, ovalHeight / 2),
                blendMode = BlendMode.Clear
            )

            val strokeColor = if (showSuccess) Color.Green else if (isRecording) Color(0xFF2EA8FF) else livenessColor
            drawRoundRect(
                color = strokeColor,
                topLeft = topLeft,
                size = Size(ovalWidth, ovalHeight),
                cornerRadius = CornerRadius(ovalWidth / 2, ovalHeight / 2),
                style = Stroke(width = if (showSuccess || isRecording) 12f else 6f)
            )
        }

        // 3. Layer Efek Flash Kamera Putih Tipis
        AnimatedVisibility(
            visible = showFlash,
            enter = fadeIn(tween(50)),
            exit = fadeOut(tween(200))
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.5f)))
        }

        // 4. Layer Animasi Sukses (Checkmark di tengah)
        AnimatedVisibility(
            visible = showSuccess,
            enter = scaleIn(tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(200)),
            exit = scaleOut(tween(200)) + fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color(0xFF4CAF50).copy(alpha = 0.9f), CircleShape)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = "Sukses", tint = Color.White, modifier = Modifier.size(64.dp))
            }
        }

        // 5. Layer UI Controls (Teks & Tombol)
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            Text("PEREKAMAN WAJAH 3D", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)

            // Teks instruksi
            Surface(
                color = Color.Black.copy(0.6f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Crossfade(targetState = instruction, animationSpec = tween(300), label = "instruction") { text ->
                    Text(
                        text = text,
                        color = if (showSuccess) Color.Green else if (isRecording) Color(0xFF2EA8FF) else Color.White,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // --- PROGRESS BAR PEREKAMAN ---
            if (isRecording || showSuccess) {
                LinearProgressIndicator(
                    progress = { capturedCount / totalFrames.toFloat() },
                    modifier = Modifier.fillMaxWidth(0.8f).height(10.dp).clip(RoundedCornerShape(50)),
                    color = Color(0xFF4CAF50),
                    trackColor = Color.White.copy(alpha = 0.3f),
                )
                Spacer(modifier = Modifier.height(30.dp))
            }

            // Tombol Mulai Rekam
            FloatingActionButton(
                onClick = {
                    if (isRecording || showSuccess) return@FloatingActionButton
                    isRecording = true
                    capturedCount = 0

                    scope.launch(Dispatchers.Main) {
                        val collectedBitmaps = mutableListOf<Bitmap>()

                        // Looping otomatis sebanyak 10 kali (sekitar 5 detik)
                        for (i in 1..totalFrames) {
                            // 1. Panggil fungsi memotret ke Memori (Sangat Cepat)
                            val bitmap = captureFrameInMemory(imageCapture, context)

                            if (bitmap != null) {
                                collectedBitmaps.add(bitmap)
                                capturedCount = i

                                // Efek kedip halus sebagai penanda frame terambil
                                showFlash = true
                                delay(30)
                                showFlash = false
                            }

                            // 2. Jeda waktu sebelum mengambil frame berikutnya (~470ms)
                            // Total waktu per putaran menjadi ~500ms (2 Frame per detik)
                            delay(470)
                        }

                        // Selesai merekam
                        isRecording = false
                        showSuccess = true
                        delay(1200) // Tahan layar sukses sejenak

                        onAllCaptured(collectedBitmaps.toList())
                    }
                },
                containerColor = if (isRecording) Color.Gray else Color.White,
                shape = CircleShape,
                modifier = Modifier.size(76.dp).padding(bottom = 8.dp)
            ) {
                if (isRecording) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Videocam, contentDescription = "Mulai Rekam", modifier = Modifier.size(36.dp), tint = Color.Black)
                }
            }

            TextButton(onClick = onCancel, enabled = !isRecording) {
                Text("Batal", color = if (isRecording) Color.Gray else Color.White, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// --- FUNGSI AJAIB: MEMOTRET LANGSUNG KE MEMORI & RESIZE ---
suspend fun captureFrameInMemory(imageCapture: ImageCapture, context: Context): Bitmap? = suspendCancellableCoroutine { cont ->
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                try {
                    // 1. Ubah Proxy menjadi Bitmap (Otomatis menyesuaikan rotasi kamera)
                    val originalBitmap = imageProxy.toBitmap()

                    // 2. KECILKAN UKURAN (Resize) AGAR HP TIDAK CRASH (OOM)
                    // Kita patok lebarnya maksimal 480 pixel, tingginya menyesuaikan proporsi
                    val targetWidth = 480f
                    val scaleFactor = targetWidth / originalBitmap.width
                    val targetHeight = (originalBitmap.height * scaleFactor).toInt()

                    val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth.toInt(), targetHeight, true)

                    // 3. Tutup Proxy agar memori kamera terbebas
                    imageProxy.close()

                    // 4. Lanjutkan proses coroutine dengan mengirimkan Bitmap yang sudah ringan
                    cont.resume(resizedBitmap)
                } catch (e: Exception) {
                    Log.e("FaceRecord", "Gagal memproses frame", e)
                    imageProxy.close()
                    cont.resume(null)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("FaceRecord", "Gagal menjepret kamera", exception)
                cont.resume(null)
            }
        }
    )
}