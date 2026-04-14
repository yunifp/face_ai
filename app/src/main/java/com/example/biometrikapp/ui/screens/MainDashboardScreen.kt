package com.example.biometrikapp.ui.screens

import android.annotation.SuppressLint
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biometrikapp.api.RetrofitClient
import kotlinx.coroutines.launch

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    onStartFaceScan: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onStartFingerprintScan: () -> Unit,
    onNavigateToFingerprintRegister: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val screenWidth = configuration.screenWidthDp.dp

    val transitionState = remember { MutableTransitionState(false).apply { targetState = true } }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // --- STATE UNTUK NOTIFIKASI SNACKBAR ---
    val snackbarHostState = remember { SnackbarHostState() }

    // --- SETUP KONEKSI & REFRESH ---
    var isServerOnline by remember { mutableStateOf<Boolean?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    // --- STATE UNTUK SYNC DATA ---
    var isSyncing by remember { mutableStateOf(false) }

    fun checkServerConnection() {
        scope.launch {
            isRefreshing = true
            isServerOnline = null
            try {
                val response = RetrofitClient.instance.checkConnection()
                isServerOnline = response.success
            } catch (_: Exception) {
                isServerOnline = false
            } finally {
                isRefreshing = false
            }
        }
    }

    // --- FUNGSI TRIGGER SYNC ---
    fun triggerSyncData() {
        if (isSyncing) return
        scope.launch {
            isSyncing = true
            try {
                val response = RetrofitClient.instance.triggerSync()
                if (response.success) {
                    snackbarHostState.showSnackbar("✅ Sync Berhasil!")
                } else {
                    snackbarHostState.showSnackbar("❌ Sync Gagal: ${response.message}")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("⚠️ Kesalahan koneksi saat Sync")
            } finally {
                isSyncing = false
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                        IconButton(onClick = { /* Help */ }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = null, tint = Color.White)
                                Text("Help", fontSize = 9.sp, color = Color.White)
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFFF8F9FA), Color(0xFFE9ECEF))))
                .padding(paddingValues)
        ) {
            val maxWidth = maxWidth
            // Penyesuaian konten agar tidak terlalu lebar di Tablet
            val contentModifier = if (maxWidth > 600.dp) {
                Modifier.widthIn(max = 700.dp).align(Alignment.TopCenter)
            } else {
                Modifier.fillMaxSize()
            }

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
                            modifier = contentModifier
                                .fillMaxSize()
                                .padding(horizontal = if (maxWidth > 600.dp) 40.dp else 24.dp)
                                .verticalScroll(scrollState),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(if (isLandscape) 16.dp else 24.dp))

                            // --- SECTION 1: FACE RECOGNITION ---
                            SectionHeader(
                                title = "Face Recognition",
                                icon = Icons.Outlined.Face,
                                iconTint = Color(0xFF1976D2),
                                bgColors = listOf(Color(0xFFEAF2FD), Color(0xFFD4E3FA)),
                                pulseScale = pulseScale
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                                horizontalArrangement = Arrangement.spacedBy(if (maxWidth > 600.dp) 24.dp else 16.dp)
                            ) {
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

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                                horizontalArrangement = Arrangement.spacedBy(if (maxWidth > 600.dp) 24.dp else 16.dp)
                            ) {
                                AnimatedActionCard(
                                    modifier = Modifier.weight(1f),
                                    title = "REGISTER NEW\nFINGER",
                                    icon = Icons.Default.Fingerprint,
                                    badgeIcon = Icons.Default.Add,
                                    containerColors = listOf(Color(0xFF43A047), Color(0xFF2E7D32)),
                                    contentColor = Color.White,
                                    isPrimary = true,
                                    badgeTint = Color(0xFF1B5E20),
                                    onClick = onNavigateToFingerprintRegister
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
                                    onClick = onStartFingerprintScan
                                )
                            }

                            // --- SECTION 3: SYNC DATABASE CARD ---
                            val syncInteractionSource = remember { MutableInteractionSource() }
                            val isSyncPressed by syncInteractionSource.collectIsPressedAsState()
                            val syncScale by animateFloatAsState(if (isSyncPressed) 0.96f else 1f, tween(150), label = "")
                            val syncElevation by animateFloatAsState(if (isSyncPressed) 2f else 8.dp.value, tween(150), label = "")

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp)
                                    .graphicsLayer { scaleX = syncScale; scaleY = syncScale }
                                    .shadow(syncElevation.dp, RoundedCornerShape(20.dp), spotColor = Color(0xFF152A53))
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable(
                                        interactionSource = syncInteractionSource,
                                        indication = null,
                                        enabled = !isSyncing,
                                        onClick = { triggerSyncData() }
                                    ),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Brush.horizontalGradient(listOf(Color(0xFF152A53), Color(0xFF1E3A70))))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isSyncing) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                modifier = Modifier.size(24.dp),
                                                strokeWidth = 2.5.dp
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text(
                                                text = "Menyinkronkan Data...",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                letterSpacing = 0.5.sp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Sync,
                                                contentDescription = "Sync",
                                                tint = Color.White,
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text(
                                                text = "SYNC DATABASE",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                letterSpacing = 1.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // SPACING EXTRA AGAR LAYOUT TIDAK KEPOTONG DI BAWAH (KHUSUSNYA TABLET)
                            Spacer(modifier = Modifier.height(48.dp))
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
                                .padding(vertical = if (isLandscape) 8.dp else 16.dp),
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

                            Box(modifier = Modifier.size(8.dp).background(statusColor, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(style = SpanStyle(color = Color.Gray)) { append("Status: ") }
                                    withStyle(style = SpanStyle(color = statusColor, fontWeight = FontWeight.Bold)) { append(statusText) }
                                },
                                fontSize = 10.sp
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
            modifier = Modifier.background(Brush.horizontalGradient(bgColors)).padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp).scale(pulseScale))
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF152A53), letterSpacing = 0.5.sp)
        }
    }
}

@Composable
fun AnimatedActionCard(
    modifier: Modifier = Modifier, title: String, icon: ImageVector, badgeIcon: ImageVector,
    containerColors: List<Color>, contentColor: Color, isPrimary: Boolean, badgeTint: Color, onClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.94f else 1f, tween(150), label = "")
    val elevation by animateFloatAsState(if (isPressed) 2f else 8.dp.value, tween(150), label = "")

    Card(
        modifier = modifier
            // Rasio aspek disesuaikan jika landscape agar tidak terlalu tinggi
            .aspectRatio(if (isLandscape) 1.2f else 0.85f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(elevation.dp, RoundedCornerShape(24.dp), spotColor = if (isPrimary) containerColors.first() else Color.Gray)
            .clip(RoundedCornerShape(24.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(24.dp), border = if (!isPrimary) BorderStroke(1.5.dp, Color(0xFFE0E0E0)) else null
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(containerColors))) {
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isPrimary) Color.White else contentColor,
                        modifier = Modifier.size(if (isLandscape) 48.dp else 56.dp)
                    )
                    Icon(
                        imageVector = badgeIcon, contentDescription = null, tint = badgeTint,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(20.dp)
                            .background(Color.White, CircleShape)
                            .padding(if (isPrimary) 2.dp else 1.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = title,
                    color = contentColor,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}