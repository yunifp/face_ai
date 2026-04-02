package com.example.biometrikapp.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    // State untuk mengontrol animasi berjalan
    var startAnimation by remember { mutableStateOf(false) }

    // Animasi Fade-In (Muncul perlahan)
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000), label = "alpha"
    )

    // Animasi Scale (Membesar perlahan dari kecil)
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.5f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing), label = "scale"
    )

    // Mengatur waktu tampilan (Delay)
    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(2000) // Tahan splash screen selama 2 detik
        onSplashFinished() // Pindah ke layar berikutnya
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF152A53)), // Warna biru utama aplikasi
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .alpha(alphaAnim)
                .scale(scaleAnim)
        ) {
            // Logo (Membesar menyesuaikan header)
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Logo",
                    tint = Color.White,
                    modifier = Modifier.size(72.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Teks VoteNow persis seperti di Header tapi lebih besar
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = Color.White)) { append("Vote") }
                    withStyle(style = SpanStyle(color = Color(0xFF2EA8FF))) { append("Now") }
                },
                fontWeight = FontWeight.ExtraBold,
                fontSize = 48.sp,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle pemanis
            Text(
                text = "Biometric Security System",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}