package com.example.biometrikapp.ui.screens

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biometrikapp.api.FingerprintTemplateData
import com.example.biometrikapp.api.RetrofitClient
import com.example.biometrikapp.api.UserData
import com.zkteco.android.biometric.module.fingerprintreader.ZKFingerService
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

// --- Definisi Warna Kustom ---
private val PrimaryBlue = Color(0xFF4361EE)
private val BackgroundGray = Color(0xFFF4F6F9)
private val SuccessGreen = Color(0xFF2ECC71)
private val ErrorRed = Color(0xFFE74C3C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FingerprintScannerScreen(
    isConnected: Boolean,
    capturedBitmap: Bitmap?,
    capturedTemplate: ByteArray?,
    onBack: () -> Unit,
    onConnect: () -> Unit,
    onResultFound: (UserData?) -> Unit
) {
    val api = RetrofitClient.instance
    val scope = rememberCoroutineScope()

    // State Management
    var isDataLoaded by remember { mutableStateOf(false) }
    var serverTemplates by remember { mutableStateOf<List<FingerprintTemplateData>>(emptyList()) }
    var isVerifying by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("SINKRONISASI DATABASE...") }
    var lastProcessedTemplate by remember { mutableStateOf<ByteArray?>(null) }

    // Efek Animasi Transisi Warna Status
    val statusColor by animateColorAsState(
        targetValue = when {
            !isConnected || statusText.contains("GAGAL", true) || statusText.contains("ERROR", true) -> ErrorRed
            isVerifying -> PrimaryBlue
            else -> Color.DarkGray
        },
        label = "statusColor"
    )

    // 1. SINKRONISASI AWAL
    LaunchedEffect(Unit) {
        if (!isConnected) onConnect()

        isDataLoaded = false
        statusText = "SINKRONISASI DATABASE..."

        try {
            val response = api.getAllFingerprints()
            if (response.success && response.data != null) {
                serverTemplates = response.data
                isDataLoaded = true
                statusText = "MENUNGGU JARI..."
            } else {
                statusText = "GAGAL UNDUH DATA"
            }
        } catch (e: Exception) {
            Log.e("Scanner", "Gagal load data: ${e.message}", e)
            statusText = "SERVER ERROR"
        }
    }

    // 2. LOGIKA MATCHING (Otomatis ketika ada sidik jari baru)
    LaunchedEffect(capturedTemplate, isDataLoaded, isConnected) {
        if (!isConnected || !isDataLoaded || capturedTemplate == null || isVerifying) return@LaunchedEffect

        // Pastikan instance byte array berbeda dengan sebelumnya
        if (capturedTemplate === lastProcessedTemplate) return@LaunchedEffect

        isVerifying = true
        statusText = "MENCOCOKKAN..."
        lastProcessedTemplate = capturedTemplate

        scope.launch(Dispatchers.Default) {
            val foundUser = findMatchingUser(capturedTemplate, serverTemplates)

            withContext(Dispatchers.Main) {
                isVerifying = false
                statusText = if (foundUser != null) "COCOK!" else "TIDAK DIKENAL"

                // Beri jeda sedikit agar user bisa melihat status sebelum pindah layar
                delay(300)
                onResultFound(foundUser)
            }
        }
    }

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("PINDAI SIDIK JARI", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryBlue,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(BackgroundGray)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // WIDGET STATUS KONEKSI
            ConnectionStatusCard(
                isConnected = isConnected,
                isDataLoaded = isDataLoaded,
                templateCount = serverTemplates.size,
                onConnect = onConnect
            )

            Spacer(Modifier.height(32.dp))

            // STATUS TEKS (Dinamis)
            Text(
                text = if (!isConnected) "ALAT TIDAK TERDETEKSI" else statusText,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = statusColor
            )

            Spacer(Modifier.height(24.dp))

            // PREVIEW SIDIK JARI (Dengan Animasi)
            ScannerPreview(
                capturedBitmap = capturedBitmap,
                isConnected = isConnected,
                isDataLoaded = isDataLoaded,
                isVerifying = isVerifying
            )

            Spacer(Modifier.weight(1f))

            // FOOTER INFO & TOMBOL
            Text(
                text = "Silakan tempelkan jari Anda pada alat untuk verifikasi identitas.",
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("KEMBALI KE DASHBOARD", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- Komponen Terpisah Agar Lebih Bersih ---

@Composable
private fun ConnectionStatusCard(
    isConnected: Boolean,
    isDataLoaded: Boolean,
    templateCount: Int,
    onConnect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isConnected) SuccessGreen else ErrorRed,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isConnected) "Alat Terhubung" else "Alat Terputus",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
                Text(
                    text = if (isDataLoaded) "Database Siap ($templateCount Data)" else "Sinkronisasi Database...",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }

            if (!isConnected) {
                TextButton(onClick = onConnect) {
                    Text("RE-CONNECT", fontWeight = FontWeight.Bold, color = PrimaryBlue)
                }
            }
        }
    }
}

@Composable
private fun ScannerPreview(
    capturedBitmap: Bitmap?,
    isConnected: Boolean,
    isDataLoaded: Boolean,
    isVerifying: Boolean
) {
    // Animasi denyut (pulse) untuk border saat verifikasi
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val borderColor = if (isVerifying) PrimaryBlue.copy(alpha = alpha) else Color.LightGray

    Surface(
        modifier = Modifier
            .size(220.dp, 280.dp)
            .clip(RoundedCornerShape(20.dp)),
        color = Color.White,
        border = BorderStroke(width = if (isVerifying) 4.dp else 2.dp, color = borderColor),
        shadowElevation = if (isVerifying) 12.dp else 4.dp
    ) {
        if (capturedBitmap != null && isConnected && isDataLoaded) {
            Image(
                bitmap = capturedBitmap.asImageBitmap(),
                contentDescription = "Fingerprint Preview",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = Color.LightGray,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = if (!isDataLoaded && isConnected) "LOADING..." else "STANDBY",
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

// --- Fungsi Ekstensi/Helper untuk Logika Bisnis ---

/**
 * Memisahkan logika berat ke fungsi suspend terpisah agar Composable tidak kotor.
 */
private suspend fun findMatchingUser(
    capturedTemplate: ByteArray,
    serverTemplates: List<FingerprintTemplateData>
): UserData? = coroutineScope {
    var foundUser: UserData? = null
    val matchFound = AtomicBoolean(false)
    val cores = Runtime.getRuntime().availableProcessors()
    val chunkSize = maxOf(1, serverTemplates.size / cores)

    val jobs = serverTemplates.chunked(chunkSize).map { chunk ->
        async(Dispatchers.Default) {
            for (dbData in chunk) {
                if (matchFound.get()) break
                try {
                    val dbBytes = Base64.decode(dbData.template_data, Base64.NO_WRAP)
                    val score = ZKFingerService.verify(capturedTemplate, dbBytes)

                    if (score >= 70) {
                        if (matchFound.compareAndSet(false, true)) {
                            foundUser = dbData.user
                        }
                        break
                    }
                } catch (e: Exception) {
                    // Skip ke data selanjutnya jika terjadi error decode/verify pada 1 data
                    continue
                }
            }
        }
    }

    jobs.awaitAll()
    return@coroutineScope foundUser
}