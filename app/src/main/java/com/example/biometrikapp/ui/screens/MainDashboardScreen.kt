package com.example.biometrikapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biometrikapp.api.RetrofitClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    onStartFaceScan: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onStartFingerprintScan: () -> Unit,
    onNavigateToFingerprintRegister: () -> Unit, // Parameter untuk pendaftaran jari baru
) {
    val transitionState = remember { MutableTransitionState(false).apply { targetState = true } }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // --- SETUP KONEKSI & REFRESH ---
    var isServerOnline by remember { mutableStateOf<Boolean?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    fun checkServerConnection() {
        scope.launch {
            isRefreshing = true
            isServerOnline = null
            try {
                val response = RetrofitClient.instance.checkConnection()
                // Karena checkConnection mengembalikan StandardResponse<Any>
                isServerOnline = response.success
            } catch (_: Exception) {
                isServerOnline = false
            } finally {
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        checkServerConnection()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = ""
    )

    Scaffold(
        topBar = {
            Surface(shadowElevation = 8.dp, color = Color(0xFF152A53)) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(style = SpanStyle(color = Color.White)) { append("Vote") }
                                    withStyle(style = SpanStyle(color = Color(0xFF2EA8FF))) { append("Now") }
                                },
                                fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, letterSpacing = 1.sp
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    actions = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(end = 16.dp).clip(RoundedCornerShape(8.dp)).clickable { }.padding(4.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Help", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFFF8F9FA), Color(0xFFE9ECEF))))
                .padding(paddingValues)
        ) {
            AnimatedVisibility(
                visibleState = transitionState,
                enter = fadeIn(tween(800)) + slideInVertically(initialOffsetY = { 100 }, animationSpec = tween(800, easing = FastOutSlowInEasing))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {

                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { checkServerConnection() },
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp)
                                .verticalScroll(scrollState),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(24.dp))

                            // --- SECTION 1: FACE RECOGNITION (PROFIL & WAJAH) ---
                            SectionHeader(
                                title = "Face Recognition",
                                icon = Icons.Outlined.Face,
                                iconTint = Color(0xFF1976D2),
                                bgColors = listOf(Color(0xFFEAF2FD), Color(0xFFD4E3FA)),
                                pulseScale = pulseScale
                            )

                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                AnimatedActionCard(
                                    modifier = Modifier.weight(1f),
                                    title = "USER\nREGISTRATION",
                                    icon = Icons.Default.Face,
                                    badgeIcon = Icons.Default.Add,
                                    containerColors = listOf(Color(0xFF1E88E5), Color(0xFF1565C0)),
                                    contentColor = Color.White,
                                    isPrimary = true,
                                    badgeTint = Color(0xFF1976D2),
                                    onClick = onNavigateToRegister
                                )
                                AnimatedActionCard(
                                    modifier = Modifier.weight(1f),
                                    title = "FACE\nVERIFICATION",
                                    icon = Icons.Outlined.Face,
                                    badgeIcon = Icons.Default.CheckCircle,
                                    containerColors = listOf(Color.White, Color(0xFFF8F9FA)),
                                    contentColor = Color(0xFF1976D2),
                                    isPrimary = false,
                                    badgeTint = Color(0xFF43A047),
                                    onClick = onStartFaceScan
                                )
                            }

                            // --- SECTION 2: FINGERPRINT ---
                            SectionHeader(
                                title = "Fingerprint",
                                icon = Icons.Default.Fingerprint,
                                iconTint = Color(0xFF388E3C),
                                bgColors = listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9)),
                                pulseScale = pulseScale
                            )

                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                AnimatedActionCard(
                                    modifier = Modifier.weight(1f),
                                    title = "REGISTER NEW\nFINGER",
                                    icon = Icons.Default.Fingerprint,
                                    badgeIcon = Icons.Default.Add,
                                    containerColors = listOf(Color(0xFF43A047), Color(0xFF2E7D32)),
                                    contentColor = Color.White,
                                    isPrimary = true,
                                    badgeTint = Color(0xFF1B5E20),
                                    onClick = onNavigateToFingerprintRegister // Terhubung ke RegisterFingerScreen
                                )
                                AnimatedActionCard(
                                    modifier = Modifier.weight(1f),
                                    title = "FINGERPRINT\nVERIFICATION",
                                    icon = Icons.Default.Fingerprint,
                                    badgeIcon = Icons.Default.CheckCircle,
                                    containerColors = listOf(Color.White, Color(0xFFF8F9FA)),
                                    contentColor = Color(0xFF388E3C),
                                    isPrimary = false,
                                    badgeTint = Color(0xFF43A047),
                                    onClick = onStartFingerprintScan // Terhubung ke FingerprintScannerScreen
                                )
                            }
                        }
                    }

                    // ================= CONNECTION STATUS =================
                    Surface(
                        color = Color.White,
                        shadowElevation = 16.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val statusColor = when (isServerOnline) {
                                true -> Color(0xFF4CAF50)
                                false -> Color(0xFFE53935)
                                null -> Color.Gray
                            }

                            val statusText = when (isServerOnline) {
                                true -> "ONLINE / SERVER READY"
                                false -> "OFFLINE / SERVER DOWN"
                                null -> "CHECKING CONNECTION..."
                            }

                            Box(modifier = Modifier.size(10.dp).background(statusColor, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(style = SpanStyle(color = Color.Gray)) { append("Status: ") }
                                    withStyle(style = SpanStyle(color = statusColor, fontWeight = FontWeight.Bold)) { append(statusText) }
                                },
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector, iconTint: Color, bgColors: List<Color>, pulseScale: Float) {
    Surface(
        color = Color.White, shape = RoundedCornerShape(50), shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier.background(Brush.horizontalGradient(bgColors)).padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.scale(pulseScale))
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF152A53), letterSpacing = 0.5.sp)
        }
    }
}

@Composable
fun AnimatedActionCard(
    modifier: Modifier = Modifier, title: String, icon: ImageVector, badgeIcon: ImageVector,
    containerColors: List<Color>, contentColor: Color, isPrimary: Boolean, badgeTint: Color, onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.92f else 1f, tween(150), label = "")
    val elevation by animateFloatAsState(if (isPressed) 2f else 12f, tween(150), label = "")

    Card(
        modifier = modifier
            .aspectRatio(0.80f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(elevation.dp, RoundedCornerShape(24.dp), spotColor = if (isPrimary) containerColors.first() else Color.Gray)
            .clip(RoundedCornerShape(24.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(24.dp), border = if (!isPrimary) BorderStroke(1.5.dp, Color(0xFFE0E0E0)) else null
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(containerColors))) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box {
                    Icon(imageVector = icon, contentDescription = null, tint = if (isPrimary) Color.White else contentColor, modifier = Modifier.size(68.dp))
                    Icon(
                        imageVector = badgeIcon, contentDescription = null, tint = badgeTint,
                        modifier = Modifier.align(Alignment.BottomEnd).size(24.dp).background(Color.White, CircleShape).padding(if (isPrimary) 3.dp else 1.dp)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(text = title, color = contentColor, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, fontSize = 14.sp, letterSpacing = 1.sp, lineHeight = 20.sp)
            }
        }
    }
}