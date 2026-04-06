package com.example.biometrikapp.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.biometrikapp.api.RetrofitClient
import com.example.biometrikapp.api.UserData
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(onRegisterComplete: () -> Unit, onNavigateToLogin: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var usersList by remember { mutableStateOf<List<UserData>>(emptyList()) }
    var selectedUser by remember { mutableStateOf<UserData?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var expandedDropdown by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var isCameraMode by remember { mutableStateOf(false) }

    // MENAMPUNG 3 FOTO POSE
    var capturedBitmaps by remember { mutableStateOf<List<Bitmap>?>(null) }

    // Fetch data users saat screen dimuat
    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.instance.getAllUsers()
            if (response.success && response.data != null) {
                usersList = response.data
            }
        } catch (e: Exception) {
            Log.e("Register", "Failed to fetch users", e)
        }
    }

    val customTextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color(0xFF152A53), unfocusedBorderColor = Color(0xFFE0E0E0),
        focusedLabelColor = Color(0xFF152A53), unfocusedLabelColor = Color.Gray,
        cursorColor = Color(0xFF152A53), focusedContainerColor = Color.White, unfocusedContainerColor = Color(0xFFFDFDFD)
    )

    if (isCameraMode) {
        FaceRegisterCameraScreen(
            onAllCaptured = { bitmaps ->
                capturedBitmaps = bitmaps
                isCameraMode = false
            },
            onCancel = { isCameraMode = false }
        )
    } else {
        Scaffold(
            topBar = {
                Surface(shadowElevation = 8.dp, color = Color(0xFF152A53)) {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2EA8FF), modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(style = SpanStyle(color = Color.White)) { append("Vote") }
                                        withStyle(style = SpanStyle(color = Color(0xFF2EA8FF))) { append("Now") }
                                    }, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, letterSpacing = 1.sp
                                )
                            }
                        },
                        navigationIcon = { IconButton(onClick = onNavigateToLogin) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) } },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    )
                }
            }
        ) { padding ->
            BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFF8F9FA), Color(0xFFE9ECEF)))).padding(padding)) {
                val screenWidth = maxWidth
                val contentWidthModifier = if (screenWidth > 700.dp) Modifier.widthIn(max = 750.dp).align(Alignment.TopCenter) else Modifier.fillMaxSize()

                Column(modifier = contentWidthModifier.verticalScroll(scrollState).padding(bottom = 32.dp)) {

                    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
                        Text("Registrasi Wajah", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFF152A53))
                        Text("Cari data penduduk lalu rekam biometrik wajah", fontSize = 14.sp, color = Color.Gray)
                    }

                    // --- CARD PENCARIAN NIK ---
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = if (screenWidth > 600.dp) 40.dp else 20.dp),
                        shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            ExposedDropdownMenuBox(
                                expanded = expandedDropdown,
                                onExpandedChange = { expandedDropdown = !expandedDropdown }
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = {
                                        searchQuery = it
                                        expandedDropdown = true
                                        if (selectedUser != null && it != selectedUser?.nik) {
                                            selectedUser = null
                                        }
                                    },
                                    label = { Text("Ketik atau Pilih NIK Penduduk") },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF152A53)) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                                    colors = customTextFieldColors,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )

                                val filteredUsers = usersList.filter { it.nik.contains(searchQuery, ignoreCase = true) }
                                if (filteredUsers.isNotEmpty() && expandedDropdown) {
                                    ExposedDropdownMenu(
                                        expanded = expandedDropdown,
                                        onDismissRequest = { expandedDropdown = false },
                                        modifier = Modifier.background(Color.White).fillMaxWidth()
                                    ) {
                                        filteredUsers.take(7).forEach { user ->
                                            DropdownMenuItem(
                                                text = { Text("${user.nik} - ${user.nama_penduduk ?: "Tanpa Nama"}") },
                                                onClick = {
                                                    searchQuery = user.nik
                                                    selectedUser = user
                                                    expandedDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- CARD DETAIL PENDUDUK (READ-ONLY) ---
                    if (selectedUser != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = if (screenWidth > 600.dp) 40.dp else 20.dp),
                            shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Text("Detail Penduduk", fontWeight = FontWeight.Bold, color = Color(0xFF152A53), fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(verticalAlignment = Alignment.Top) {
                                    // Foto Profil
                                    Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFE0E0E0)), contentAlignment = Alignment.Center) {
                                        if (!selectedUser!!.foto_profil.isNullOrEmpty()) {
                                            // Memastikan URL valid untuk foto
                                            val imageUrl = "${RetrofitClient.BASE_URL}/${selectedUser!!.foto_profil!!.replace("\\", "/")}"
                                            AsyncImage(
                                                model = imageUrl,
                                                contentDescription = "Foto Profil",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    // Info Penduduk
                                    Column {
                                        Text(selectedUser!!.nama_penduduk ?: "-", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                        Text("NIK: ${selectedUser!!.nik}", color = Color.Gray, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("${selectedUser!!.tempat_lahir ?: "-"}, ${selectedUser!!.tanggal_lahir ?: "-"}", fontSize = 13.sp)
                                        Text("Jenis Kelamin: ${selectedUser!!.jenis_kelamin ?: "-"}", fontSize = 13.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Divider(color = Color(0xFFE0E0E0))
                                Spacer(modifier = Modifier.height(16.dp))

                                Text("Alamat Lengkap", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("${selectedUser!!.alamat ?: "-"}", fontSize = 14.sp)
                                Text("RT/RW: ${selectedUser!!.rt ?: "-"}/${selectedUser!!.rw ?: "-"}", fontSize = 14.sp, color = Color.DarkGray)
                                Text("Desa: ${selectedUser!!.nama_desa ?: "-"}, Kec: ${selectedUser!!.nama_kec ?: "-"}", fontSize = 14.sp, color = Color.DarkGray)
                                Text("Kab/Kota: ${selectedUser!!.nama_kab ?: "-"}, Prov: ${selectedUser!!.nama_pro ?: "-"}", fontSize = 14.sp, color = Color.DarkGray)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // --- AREA BIOMETRIK WAJAH ---
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = if (screenWidth > 600.dp) 40.dp else 20.dp),
                        shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
                    ) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Autentikasi Wajah (3 Pose)", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF152A53))
                            Text("Sistem otomatis mengambil pose Depan, Kiri, Kanan", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(20.dp))

                            if (capturedBitmaps.isNullOrEmpty()) {
                                Box(
                                    modifier = Modifier.size(140.dp).drawBehind {
                                        drawCircle(color = Color(0xFF152A53).copy(0.2f), radius = size.minDimension / 2, style = Stroke(width = 6f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 15f), 0f)))
                                    }, contentAlignment = Alignment.Center
                                ) { Icon(Icons.Outlined.Face, null, tint = Color(0xFF152A53).copy(0.4f), modifier = Modifier.size(60.dp)) }
                            } else {
                                val itemSize = if (screenWidth > 600.dp) 110.dp else 85.dp
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                    capturedBitmaps!!.forEach { bmp ->
                                        Box(modifier = Modifier.padding(6.dp)) {
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = null,
                                                modifier = Modifier.size(itemSize).clip(CircleShape).border(2.dp, Color(0xFF4CAF50), CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.align(Alignment.BottomEnd).background(Color.White, CircleShape).size(22.dp))
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    if(selectedUser == null) {
                                        Toast.makeText(context, "Pilih NIK Penduduk terlebih dahulu!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        isCameraMode = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp).border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(100)),
                                shape = RoundedCornerShape(100),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0F2F5), contentColor = Color(0xFF152A53))
                            ) {
                                Icon(Icons.Default.Face, null)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(if (capturedBitmaps.isNullOrEmpty()) "MULAI SCAN WAJAH" else "ULANGI SCAN WAJAH", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // --- TOMBOL SIMPAN HANYA MENGIRIM ID DAN FOTO WAJAH ---
                    Button(
                        onClick = {
                            if (selectedUser == null) { Toast.makeText(context, "Pilih penduduk terlebih dahulu!", Toast.LENGTH_SHORT).show(); return@Button }
                            if (capturedBitmaps == null || capturedBitmaps!!.size < 3) { Toast.makeText(context, "Harap selesaikan 3 pose wajah!", Toast.LENGTH_SHORT).show(); return@Button }

                            isLoading = true
                            scope.launch {
                                try {
                                    val faceParts = capturedBitmaps!!.mapIndexed { i, bmp ->
                                        val file = File(context.cacheDir, "face_kyc_$i.jpg")
                                        val os = FileOutputStream(file)
                                        bmp.compress(Bitmap.CompressFormat.JPEG, 85, os)
                                        os.close()
                                        MultipartBody.Part.createFormData("face_images", file.name, file.asRequestBody("image/jpeg".toMediaTypeOrNull()))
                                    }

                                    val response = RetrofitClient.instance.registerFaceImages(selectedUser!!.id, faceParts)

                                    if (response.success) {
                                        Toast.makeText(context, "Biometrik Wajah Berhasil Disimpan!", Toast.LENGTH_LONG).show()
                                        onRegisterComplete()
                                    } else {
                                        Toast.makeText(context, response.message, Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally { isLoading = false }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = if (screenWidth > 600.dp) 60.dp else 24.dp).height(58.dp),
                        shape = RoundedCornerShape(100), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF152A53)), enabled = !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        else Text("SIMPAN BIOMETRIK WAJAH", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    }
                    Spacer(modifier = Modifier.height(50.dp))
                }
            }
        }
    }
}