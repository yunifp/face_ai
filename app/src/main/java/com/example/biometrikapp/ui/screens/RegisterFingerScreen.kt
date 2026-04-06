package com.example.biometrikapp.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import com.example.biometrikapp.api.FingerprintRequest
import com.example.biometrikapp.api.RetrofitClient
import com.example.biometrikapp.api.UserData
import com.example.biometrikapp.utils.toBase64String
import com.zkteco.android.biometric.module.fingerprintreader.ZKFingerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterFingerScreen(
    isConnected: Boolean,
    capturedBitmap: Bitmap?,
    capturedTemplate: ByteArray?,
    onRegisterComplete: () -> Unit,
    onBack: () -> Unit,
    onConnect: () -> Unit
) {
    val api = RetrofitClient.instance
    val context = LocalContext.current

    var currentStep by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }

    // State untuk Pencarian Penduduk (Sama seperti Register Wajah)
    var usersList by remember { mutableStateOf<List<UserData>>(emptyList()) }
    var selectedUser by remember { mutableStateOf<UserData?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var expandedDropdown by remember { mutableStateOf(false) }

    val fingers = listOf(
        1 to "Jempol Kiri", 2 to "Telunjuk Kiri", 3 to "Jari Tengah Kiri", 4 to "Jari Manis Kiri", 5 to "Kelingking Kiri",
        6 to "Jempol Kanan", 7 to "Telunjuk Kanan", 8 to "Jari Tengah Kanan", 9 to "Jari Manis Kanan", 10 to "Kelingking Kanan"
    )

    var selectedFingerId by remember { mutableIntStateOf(0) }
    var selectedFingerName by remember { mutableStateOf("") }

    // State Sidik Jari
    val enrollTemplates = remember { mutableStateListOf<ByteArray>() }
    var scanStatus by remember { mutableStateOf("Siap memindai...") }
    var lastCaptureTime by remember { mutableLongStateOf(0L) }

    // Fetch data users saat screen dimuat
    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.instance.getAllUsers()
            if (response.success && response.data != null) {
                usersList = response.data
            }
        } catch (e: Exception) {
            // Abaikan atau log error jika gagal fetch
        }
    }

    suspend fun saveToServer(finalTemp: ByteArray) {
        if (selectedUser == null) return
        try {
            isLoading = true
            val base64Template = finalTemp.toBase64String()
            val response = api.saveFingerprint(
                userId = selectedUser!!.id, // Menggunakan ID dari user yang dipilih
                request = FingerprintRequest(selectedFingerId, selectedFingerName, base64Template)
            )

            withContext(Dispatchers.Main) {
                if (response.success) {
                    Toast.makeText(context, "Sidik Jari $selectedFingerName Tersimpan!", Toast.LENGTH_SHORT).show()
                    currentStep = 1 // Kembali ke menu pilih jari
                    enrollTemplates.clear()
                } else {
                    scanStatus = "Gagal Simpan: ${response.message}"
                    enrollTemplates.clear()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                scanStatus = "Server Error: Koneksi Terputus"
                enrollTemplates.clear()
            }
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(capturedTemplate, capturedBitmap) {
        if (currentStep == 2 && capturedTemplate != null && !isLoading) {
            val currentTime = System.currentTimeMillis()

            // Jeda 1 detik agar alat membaca dengan stabil
            if (currentTime - lastCaptureTime > 1000) {
                lastCaptureTime = currentTime
                val currentTemp = capturedTemplate.clone()

                if (enrollTemplates.isEmpty() || !enrollTemplates.last().contentEquals(currentTemp)) {
                    enrollTemplates.add(currentTemp)

                    if (enrollTemplates.size < 3) {
                        scanStatus = "SAMPEL ${enrollTemplates.size}/3 SUKSES!\nSilakan Angkat & Tempel Jari Lagi..."
                    } else {
                        scanStatus = "MENGGABUNGKAN DATA..."
                        isLoading = true
                        delay(500)

                        val regTemp = ByteArray(2048)
                        val ret = ZKFingerService.merge(enrollTemplates[0], enrollTemplates[1], enrollTemplates[2], regTemp)

                        if (ret > 0) {
                            saveToServer(regTemp.copyOf(ret))
                        } else {
                            scanStatus = "Kualitas sidik jari buruk. Silakan ulangi dari awal."
                            enrollTemplates.clear()
                            isLoading = false
                        }
                    }
                }
            }
        }
    }

    BackHandler { if (currentStep > 0) currentStep-- else onBack() }

    val customTextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color(0xFF152A53), unfocusedBorderColor = Color(0xFFE0E0E0),
        focusedLabelColor = Color(0xFF152A53), unfocusedLabelColor = Color.Gray,
        cursorColor = Color(0xFF152A53), focusedContainerColor = Color.White, unfocusedContainerColor = Color(0xFFFDFDFD)
    )

    Scaffold(
        topBar = {
            Surface(shadowElevation = 8.dp, color = Color(0xFF152A53)) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Fingerprint, null, tint = Color(0xFF2EA8FF), modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(style = SpanStyle(color = Color.White)) { append("Vote") }
                                    withStyle(style = SpanStyle(color = Color(0xFF2EA8FF))) { append("Now") }
                                }, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, letterSpacing = 1.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { if (currentStep > 0) currentStep-- else onBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFFF8F9FA), Color(0xFFE9ECEF))))
                .padding(padding)
        ) {
            val screenWidth = maxWidth
            val contentWidthModifier = if (screenWidth > 700.dp) Modifier.widthIn(max = 750.dp).align(Alignment.TopCenter) else Modifier.fillMaxSize()

            Column(
                modifier = contentWidthModifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Notifikasi Alat Terputus
                if (!isConnected && currentStep > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp).clickable { onConnect() }, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE53935))
                            Spacer(Modifier.width(8.dp))
                            Text("ALAT TERPUTUS. KLIK UNTUK MENGHUBUNGKAN ULANG", color = Color(0xFFE53935), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                when (currentStep) {
                    0 -> {
                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp).fillMaxWidth()) {
                            Text("Pendaftaran Sidik Jari", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFF152A53))
                            Text("Cari data penduduk lalu rekam biometrik jari", fontSize = 14.sp, color = Color.Gray)
                        }
                        Spacer(Modifier.height(16.dp))

                        // --- CARD PENCARIAN NIK ---
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = if (screenWidth > 600.dp) 24.dp else 0.dp),
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
                                            expanded = true,
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
                        AnimatedVisibility(visible = selectedUser != null) {
                            if (selectedUser != null) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = if (screenWidth > 600.dp) 24.dp else 0.dp),
                                    shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(24.dp)) {
                                        Text("Detail Penduduk", fontWeight = FontWeight.Bold, color = Color(0xFF152A53), fontSize = 16.sp)
                                        Spacer(modifier = Modifier.height(16.dp))

                                        Row(verticalAlignment = Alignment.Top) {
                                            // Foto Profil
                                            Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFE0E0E0)), contentAlignment = Alignment.Center) {
                                                if (!selectedUser!!.foto_profil.isNullOrEmpty()) {
                                                    val safeBaseUrl = RetrofitClient.BASE_URL.trimEnd('/')
                                                    val safePath = selectedUser!!.foto_profil!!.replace("\\", "/").trimStart('/')
                                                    val imageUrl = "$safeBaseUrl/$safePath"

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
                                                Text("Gender: ${selectedUser!!.jenis_kelamin ?: "-"}", fontSize = 13.sp)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))
                                        HorizontalDivider(color = Color(0xFFE0E0E0))
                                        Spacer(modifier = Modifier.height(16.dp))

                                        Text("Alamat Lengkap", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text(selectedUser!!.alamat ?: "-", fontSize = 14.sp)
                                        Text("RT/RW: ${selectedUser!!.rt ?: "-"}/${selectedUser!!.rw ?: "-"}", fontSize = 14.sp, color = Color.DarkGray)
                                        Text("Desa: ${selectedUser!!.nama_desa ?: "-"}, Kec: ${selectedUser!!.nama_kec ?: "-"}", fontSize = 14.sp, color = Color.DarkGray)
                                        Text("Kab/Kota: ${selectedUser!!.nama_kab ?: "-"}, Prov: ${selectedUser!!.nama_pro ?: "-"}", fontSize = 14.sp, color = Color.DarkGray)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // Tombol Lanjut ke Pilih Jari
                        Button(
                            onClick = {
                                if (selectedUser != null) {
                                    currentStep = 1
                                } else {
                                    Toast.makeText(context, "Pilih NIK Penduduk terlebih dahulu!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = if (screenWidth > 600.dp) 24.dp else 0.dp).height(55.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF152A53))
                        ) {
                            Text("LANJUT REKAM JARI", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White)
                        }
                        Spacer(Modifier.height(24.dp))
                    }

                    1 -> {
                        Text("Pilih Jari yang Akan Direkam", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color(0xFF152A53))
                        Text("Penduduk: ${selectedUser?.nama_penduduk}", color = Color.Gray, fontSize = 14.sp)
                        Spacer(Modifier.height(16.dp))

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(if (screenWidth > 600.dp) 3 else 2),
                            modifier = Modifier.height(if (screenWidth > 600.dp) 300.dp else 450.dp)
                        ) {
                            items(fingers) { (id, name) ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    modifier = Modifier.padding(8.dp).clickable {
                                        selectedFingerId = id
                                        selectedFingerName = name
                                        enrollTemplates.clear()
                                        scanStatus = "Tempelkan jari $name sekarang"
                                        lastCaptureTime = 0L
                                        currentStep = 2
                                    }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Color(0xFF1E88E5), modifier = Modifier.size(32.dp))
                                        Spacer(Modifier.height(8.dp))
                                        Text(name, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.DarkGray)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        Button(
                            onClick = {
                                Toast.makeText(context, "Selesai Merekam Jari", Toast.LENGTH_SHORT).show()
                                onRegisterComplete()
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = if (screenWidth > 600.dp) 24.dp else 0.dp).height(55.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                        ) {
                            Text("SELESAI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    2 -> {
                        Text("Merekam: $selectedFingerName", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF152A53))
                        Spacer(modifier = Modifier.height(32.dp))

                        Surface(
                            modifier = Modifier.size(200.dp, 250.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = Color.White,
                            shadowElevation = 12.dp,
                            border = BorderStroke(3.dp, if (enrollTemplates.size == 3) Color(0xFF43A047) else Color.Transparent)
                        ) {
                            capturedBitmap?.let {
                                Image(bitmap = it.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize().padding(16.dp), contentScale = ContentScale.Fit)
                            } ?: Box(contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text("Menunggu Sidik Jari...", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(4.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = if (screenWidth > 600.dp) 24.dp else 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp).animateContentSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = scanStatus, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold,
                                    color = if (isLoading) Color(0xFF1976D2) else if (scanStatus.contains("Gagal")) Color.Red else Color.DarkGray,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                LinearProgressIndicator(
                                    progress = { enrollTemplates.size / 3f },
                                    modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                                    color = Color(0xFF1E88E5),
                                    trackColor = Color(0xFFE3F2FD)
                                )
                                Text("${enrollTemplates.size} / 3 Tekanan", fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp), color = Color.Gray, fontWeight = FontWeight.Medium)
                            }
                        }

                        Spacer(modifier = Modifier.height(40.dp))
                        OutlinedButton(
                            onClick = { enrollTemplates.clear(); currentStep = 1 },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = if (screenWidth > 600.dp) 24.dp else 0.dp).height(55.dp),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.5.dp, Color(0xFF152A53))
                        ) {
                            Text("Batal & Pilih Jari Lain", color = Color(0xFF152A53), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}