package com.example.biometrikapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.biometrikapp.api.UserData

// --- Definisi Warna Kustom (Selaras dengan ScannerScreen) ---
private val PrimaryBlue = Color(0xFF4361EE)
private val BackgroundGray = Color(0xFFF4F6F9)
private val SuccessGreen = Color(0xFF2ECC71)
private val ErrorRed = Color(0xFFE74C3C)

private const val BASE_IMAGE_URL = "https://teaching-reindeer-square.ngrok-free.app/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FingerprintResultScreen(
    user: UserData?,
    onScanAgain: () -> Unit,
    onDashboard: () -> Unit
) {
    val isRecognized = user != null

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (isRecognized) "HASIL IDENTIFIKASI" else "TIDAK DIKENALI",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isRecognized) PrimaryBlue else ErrorRed,
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

            // --- KONTEN UTAMA ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                if (isRecognized) {
                    SuccessResultContent(user!!)
                } else {
                    ErrorResultContent()
                }
            }

            // --- FOOTER / TOMBOL AKSI ---
            ActionButtons(
                isRecognized = isRecognized,
                onScanAgain = onScanAgain,
                onDashboard = onDashboard
            )
        }
    }
}

// --- Komponen Terpisah Agar Lebih Bersih ---

@Composable
private fun SuccessResultContent(user: UserData) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Foto Profil dengan Border & Shadow selaras dengan ScannerPreview
        Surface(
            modifier = Modifier.size(140.dp),
            shape = CircleShape,
            color = Color.White,
            border = BorderStroke(4.dp, PrimaryBlue.copy(alpha = 0.3f)),
            shadowElevation = 8.dp
        ) {
            if (!user.foto_profil.isNullOrEmpty()) {
                AsyncImage(
                    model = "$BASE_IMAGE_URL${user.foto_profil.removePrefix("/")}",
                    contentDescription = "Foto Profil",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Color.LightGray
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = user.nama_lengkap,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PrimaryBlue,
            textAlign = TextAlign.Center
        )
        Text(
            text = "NIK: ${user.nik}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )

        Spacer(Modifier.height(28.dp))

        // Card Detail (Selaras dengan ConnectionStatusCard)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                DetailResultRow("Tempat Lahir", user.tempat_lahir)
                DetailResultRow("Tanggal Lahir", user.tanggal_lahir)
                DetailResultRow("Jenis Kelamin", user.jenis_kelamin)
                DetailResultRow("Alamat", user.alamat, showDivider = false) // Elemen terakhir tanpa garis
            }
        }
    }
}

@Composable
private fun ErrorResultContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Icon(
            imageVector = Icons.Default.Cancel,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = ErrorRed
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "MAAF, JARI TIDAK DIKENALI",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = ErrorRed,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Pastikan jari bersih dan menempel sempurna pada alat scanner, atau pastikan sidik jari Anda sudah terdaftar di database.",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun ActionButtons(
    isRecognized: Boolean,
    onScanAgain: () -> Unit,
    onDashboard: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onDashboard,
            modifier = Modifier
                .weight(1f)
                .height(55.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, PrimaryBlue)
        ) {
            Text("DASHBOARD", fontWeight = FontWeight.Bold, color = PrimaryBlue)
        }

        Button(
            onClick = onScanAgain,
            modifier = Modifier
                .weight(1f)
                .height(55.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecognized) SuccessGreen else PrimaryBlue
            )
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("SCAN LAGI", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DetailResultRow(label: String, value: String?, showDivider: Boolean = true) {
    Column(modifier = Modifier.padding(bottom = if (showDivider) 12.dp else 0.dp)) {
        Text(
            text = label.uppercase(),
            fontSize = 11.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value ?: "-",
            fontSize = 16.sp,
            color = Color.DarkGray,
            fontWeight = FontWeight.SemiBold
        )
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(top = 8.dp),
                color = BackgroundGray
            )
        }
    }
}