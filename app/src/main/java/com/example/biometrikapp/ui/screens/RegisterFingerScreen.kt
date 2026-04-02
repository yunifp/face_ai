package com.example.biometrikapp.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.biometrikapp.api.FingerprintRequest
import com.example.biometrikapp.api.RetrofitClient
import com.example.biometrikapp.utils.toBase64String
import com.example.biometrikapp.utils.toPart
import com.zkteco.android.biometric.module.fingerprintreader.ZKFingerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.graphics.SolidColor

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
    val scope = rememberCoroutineScope()
    val api = RetrofitClient.instance
    val context = LocalContext.current

    var currentStep by remember { mutableIntStateOf(0) }
    var registeredUserId by remember { mutableStateOf<Int?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Biodata Form
    var nik by remember { mutableStateOf("") }
    var nama by remember { mutableStateOf("") }
    var jk by remember { mutableStateOf("Laki-laki") }
    var tempat by remember { mutableStateOf("") }
    var tgl by remember { mutableStateOf("") }
    var alamat by remember { mutableStateOf("") }

    // State Upload Foto
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUri = uri
    }

    // State DatePicker
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

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

    suspend fun saveToServer(finalTemp: ByteArray) {
        if (registeredUserId == null) return
        try {
            isLoading = true
            val base64Template = finalTemp.toBase64String()
            val response = api.saveFingerprint(
                userId = registeredUserId!!,
                request = FingerprintRequest(selectedFingerId, selectedFingerName, base64Template)
            )

            withContext(Dispatchers.Main) {
                if (response.success) {
                    Toast.makeText(context, "Sidik Jari $selectedFingerName Tersimpan!", Toast.LENGTH_SHORT).show()
                    currentStep = 1
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

    // Dialog Pemilih Tanggal
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        tgl = formatter.format(Date(millis))
                    }
                    showDatePicker = false
                }) { Text("Pilih", color = Color(0xFF152A53), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal", color = Color.Gray) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 8.dp, color = Color(0xFF152A53)) {
                TopAppBar(
                    title = { Text("Registrasi Biometrik", fontWeight = FontWeight.Bold, color = Color.White) },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFFF8F9FA), Color(0xFFE9ECEF)))) // Gradient senada dengan Home
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                        Text("Langkah 1: Biodata & Foto", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF152A53), letterSpacing = 0.5.sp)
                        Spacer(Modifier.height(24.dp))

                        // Box Foto Profil Senada
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .shadow(8.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable { galleryLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (imageUri != null) {
                                AsyncImage(model = imageUri, contentDescription = "Foto Profil", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                                    Spacer(Modifier.height(4.dp))
                                    Text("Upload Foto", textAlign = TextAlign.Center, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(8.dp),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(20.dp).animateContentSize()) {
                                OutlinedTextField(
                                    value = nik, onValueChange = { if (it.length <= 16) nik = it },
                                    label = { Text("NIK (16 Digit)") }, modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Default.Badge, null, tint = Color(0xFF1976D2)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = nama, onValueChange = { nama = it },
                                    label = { Text("Nama Lengkap") }, modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Default.Person, null, tint = Color(0xFF1976D2)) },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(Modifier.height(16.dp))

                                // PERBAIKAN RADIO BUTTON: Dibuat layout column rapi ke bawah dan dibungkus row
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text("Jenis Kelamin", fontSize = 14.sp, color = Color.DarkGray, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { jk = "Laki-laki" }) {
                                            RadioButton(selected = jk == "Laki-laki", onClick = { jk = "Laki-laki" }, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF1E88E5)))
                                            Text("Laki-laki", fontSize = 14.sp)
                                        }
                                        Spacer(modifier = Modifier.width(24.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { jk = "Perempuan" }) {
                                            RadioButton(selected = jk == "Perempuan", onClick = { jk = "Perempuan" }, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF1E88E5)))
                                            Text("Perempuan", fontSize = 14.sp)
                                        }
                                    }
                                }

                                Spacer(Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = tempat, onValueChange = { tempat = it },
                                    label = { Text("Tempat Lahir") }, modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = Color(0xFF1976D2)) },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(Modifier.height(16.dp))

                                // PERBAIKAN TANGGAL: Menggunakan Box penangkap klik untuk memunculkan DatePicker
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = tgl, onValueChange = { },
                                        label = { Text("Tgl Lahir (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth(),
                                        leadingIcon = { Icon(Icons.Default.DateRange, null, tint = Color(0xFF1976D2)) },
                                        readOnly = true, // Readonly agar keyboard tidak muncul
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    // Surface transparan menutupi textfield agar bisa di-klik memanggil kalender
                                    Surface(
                                        modifier = Modifier.matchParentSize().clickable { showDatePicker = true },
                                        color = Color.Transparent
                                    ) {}
                                }

                                Spacer(Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = alamat, onValueChange = { alamat = it },
                                    label = { Text("Alamat Lengkap") }, modifier = Modifier.fillMaxWidth(), minLines = 3,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                        // Tombol Bergaya Senada
                        Button(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    try {
                                        var fotoPart: MultipartBody.Part? = null
                                        if (imageUri != null) {
                                            val resolver = context.contentResolver
                                            val file = File(context.cacheDir, "temp_profile.jpg")
                                            val inputStream = resolver.openInputStream(imageUri!!)
                                            val outputStream = FileOutputStream(file)
                                            inputStream?.copyTo(outputStream)
                                            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                                            fotoPart = MultipartBody.Part.createFormData("foto_profil", file.name, requestFile)
                                        }

                                        val res = api.registerUserProfile(nik.toPart(), nama.toPart(), jk.toPart(), tempat.toPart(), tgl.toPart(), alamat.toPart(), fotoPart)
                                        if (res.success) {
                                            registeredUserId = res.data?.id
                                            currentStep = 1
                                        } else {
                                            Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) { Toast.makeText(context, "Koneksi ke Server Gagal", Toast.LENGTH_SHORT).show() }
                                    finally { isLoading = false }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(55.dp),
                            contentPadding = PaddingValues(0.dp), // Reset padding agar gradient full
                            shape = RoundedCornerShape(24.dp),
                            enabled = !isLoading && nik.length == 16 && nama.isNotEmpty() && tempat.isNotEmpty() && tgl.isNotEmpty() && alamat.isNotEmpty()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (!isLoading && nik.length == 16 && nama.isNotEmpty() && tempat.isNotEmpty() && tgl.isNotEmpty() && alamat.isNotEmpty())
                                            Brush.horizontalGradient(listOf(Color(0xFF43A047), Color(0xFF2E7D32))) // Hijau senada
                                        else SolidColor(Color.LightGray)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                else Text("Simpan Profil & Lanjut", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }

                    1 -> {
                        Text("Langkah 2: Pilih Jari", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color(0xFF152A53))
                        Text("ID Sistem: $registeredUserId", color = Color.Gray, fontSize = 12.sp)
                        Spacer(Modifier.height(16.dp))

                        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.height(420.dp)) {
                            items(fingers) { (id, name) ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
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
                                Toast.makeText(context, "Pendaftaran Selesai. Memuat ulang aplikasi...", Toast.LENGTH_LONG).show()
                                onRegisterComplete()

                                val packageManager = context.packageManager
                                val intent = packageManager.getLaunchIntentForPackage(context.packageName)
                                val componentName = intent?.component
                                val restartIntent = Intent.makeRestartActivityTask(componentName)

                                context.startActivity(restartIntent)
                                Runtime.getRuntime().exit(0)
                            },
                            modifier = Modifier.fillMaxWidth().height(55.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF1E88E5), Color(0xFF1565C0)))), // Biru senada
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Selesai Pendaftaran User", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
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
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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
                            modifier = Modifier.fillMaxWidth().height(55.dp),
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