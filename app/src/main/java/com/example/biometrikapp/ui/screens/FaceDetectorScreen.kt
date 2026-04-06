package com.example.biometrikapp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.biometrikapp.api.RetrofitClient
import com.example.biometrikapp.ui.BiometricViewModel
import com.example.biometrikapp.ui.ScanState
import com.example.biometrikapp.utils.FaceAnalyzer
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceDetectorScreen(
    viewModel: BiometricViewModel = viewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scanState = viewModel.scanState

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_FRONT) }
    var faceStatusMessage by remember { mutableStateOf("Posisikan wajah Anda\ndi dalam area") }
    var isFaceInsideFrame by remember { mutableStateOf(false) }

    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // --- LOGIKA ANALYZER YANG MENDUKUNG KAMERA DEPAN/BELAKANG & ROTASI DINAMIS ---
    val analyzer = remember {
        FaceAnalyzer { bitmap, rotation, isValid, message ->
            isFaceInsideFrame = isValid
            faceStatusMessage = message

            // Hanya proses gambar jika dalam state Scanning
            if (isValid && scanState is ScanState.Scanning) {
                // Rotation dikirim ke ViewModel untuk postRotate sesuai orientasi sensor (Front/Back)
                viewModel.processFaceImage(context, bitmap, rotation)
            }
        }
    }

    LaunchedEffect(lensFacing, scanState) {
        if (scanState is ScanState.Scanning) {
            analyzer.resetCooldown()
            faceStatusMessage = "Posisikan wajah Anda\ndi dalam area"
            isFaceInsideFrame = false
        }
    }

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(lensFacing, hasCameraPermission) {
        if (hasCameraPermission) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    // Mengikuti standar industri untuk deteksi wajah real-time
                    .build().also {
                        it.setAnalyzer(Executors.newSingleThreadExecutor(), analyzer)
                    }

                // Menggunakan lensFacing dinamis (bisa diganti Front/Back)
                val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            // 1. Layer Preview Kamera
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

            // 2. Layer Overlay KYC (Oval Area)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val ovalWidth = canvasWidth * 0.70f
                val ovalHeight = ovalWidth * 1.35f
                val topLeft = Offset((canvasWidth - ovalWidth) / 2, (canvasHeight - ovalHeight) / 2.2f)

                drawRect(color = Color.Black.copy(alpha = 0.65f))

                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = topLeft,
                    size = Size(ovalWidth, ovalHeight),
                    cornerRadius = CornerRadius(ovalWidth / 2, ovalHeight / 2),
                    blendMode = BlendMode.Clear
                )

                val strokeColor = when {
                    scanState is ScanState.Loading -> Color(0xFF2EA8FF)
                    scanState is ScanState.Success -> Color(0xFF4CAF50)
                    scanState is ScanState.Error -> Color(0xFFE53935)
                    isFaceInsideFrame -> Color(0xFF4CAF50)
                    else -> Color.White.copy(alpha = 0.5f)
                }

                drawRoundRect(
                    color = strokeColor,
                    topLeft = topLeft,
                    size = Size(ovalWidth, ovalHeight),
                    cornerRadius = CornerRadius(ovalWidth / 2, ovalHeight / 2),
                    style = Stroke(width = if (scanState is ScanState.Loading || scanState is ScanState.Success) 10f else 4f)
                )
            }

            // 3. UI Header & Instruksi
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.background(Color.Black.copy(0.4f), CircleShape)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                    Text("Liveness Check", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT)
                                CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
                        },
                        modifier = Modifier.background(Color.Black.copy(0.4f), CircleShape)
                    ) {
                        Icon(Icons.Default.Cameraswitch, "Switch Camera", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = if (scanState is ScanState.Loading) "Memproses wajah..." else faceStatusMessage,
                    color = if (isFaceInsideFrame || scanState is ScanState.Loading) Color(0xFF4CAF50) else Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                if (scanState is ScanState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(50.dp).padding(bottom = 20.dp), color = Color(0xFF2EA8FF), strokeWidth = 4.dp)
                }
            }

            // 4. Result Bottom Sheet
            AnimatedVisibility(
                visible = scanState is ScanState.Success || scanState is ScanState.Error,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Card(
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.width(40.dp).height(4.dp).background(Color(0xFFE0E0E0), CircleShape))
                        Spacer(modifier = Modifier.height(24.dp))

                        if (scanState is ScanState.Success) {
                            val user = scanState.user

                            Box(contentAlignment = Alignment.Center) {
                                if (!user.foto_profil.isNullOrEmpty()) {
                                    // PENGAMANAN URL: Mencegah terjadinya double slash (//) antara BASE_URL dan nama file
                                    val safeBaseUrl = RetrofitClient.BASE_URL.trimEnd('/')
                                    val safePath = user.foto_profil.replace("\\", "/").trimStart('/')
                                    val imageUrl = "$safeBaseUrl/$safePath"

                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = "Foto Profil",
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(CircleShape)
                                            .border(3.dp, Color(0xFF4CAF50), CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE0E0E0))
                                            .border(3.dp, Color(0xFF4CAF50), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(50.dp))
                                    }
                                }

                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Sukses",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .offset(x = 4.dp, y = 4.dp)
                                        .size(28.dp)
                                        .background(Color.White, CircleShape)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Verifikasi Berhasil", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF152A53))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Akses sistem diizinkan untuk:", fontSize = 14.sp, color = Color.Gray)

                            // Menyesuaikan dengan properti baru (nama_penduduk)
                            val namaTampil = user.nama_penduduk ?: user.nama_lengkap ?: "Tanpa Nama"
                            Text(namaTampil, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)

                            Spacer(modifier = Modifier.height(24.dp))

                            Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFF8F9FA), RoundedCornerShape(12.dp)).padding(16.dp)) {
                                KycDetailRow("NIK", user.nik)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE0E0E0))
                                KycDetailRow("Gender", user.jenis_kelamin ?: "-")
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE0E0E0))
                                KycDetailRow("TTL", "${user.tempat_lahir ?: "-"}, ${user.tanggal_lahir ?: "-"}")
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            Button(
                                onClick = { viewModel.resetScan() },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(100),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF152A53))
                            ) {
                                Text("LANJUTKAN", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                        } else if (scanState is ScanState.Error) {
                            Surface(color = Color(0xFFFFEBEE), shape = CircleShape) {
                                Icon(Icons.Default.Close, null, tint = Color(0xFFE53935), modifier = Modifier.size(72.dp).padding(16.dp))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Verifikasi Gagal", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF152A53))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(scanState.message, textAlign = TextAlign.Center, color = Color.Gray, fontSize = 14.sp)

                            Spacer(modifier = Modifier.height(32.dp))

                            Button(
                                onClick = { viewModel.resetScan() },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(100),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF152A53))
                            ) {
                                Text("COBA LAGI", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KycDetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Text(text = value, color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
    }
}