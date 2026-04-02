package com.example.biometrikapp.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biometrikapp.api.RetrofitClient
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

// Helper function untuk mengubah URI dari Galeri menjadi File
fun uriToFile(context: Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val tempFile = File.createTempFile("profile_upload", ".jpg", context.cacheDir)
        val outputStream = FileOutputStream(tempFile)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(onRegisterComplete: () -> Unit, onNavigateToLogin: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var nik by remember { mutableStateOf("") }
    var nama by remember { mutableStateOf("") }
    var jenisKelamin by remember { mutableStateOf("Laki-laki") }
    var tempatLahir by remember { mutableStateOf("") }
    var tanggalLahir by remember { mutableStateOf("") }
    var alamat by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var isCameraMode by remember { mutableStateOf(false) }
    var capturedBitmaps by remember { mutableStateOf<List<Bitmap>?>(null) }
    var expandedJK by remember { mutableStateOf(false) }

    // --- STATE UNTUK FOTO PROFIL ---
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var profileBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Launcher untuk buka galeri HP
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        profileImageUri = uri
        uri?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, it)
                profileBitmap = ImageDecoder.decodeBitmap(source)
            } else {
                profileBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            }
        }
    }

    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    val customTextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color(0xFF152A53),
        unfocusedBorderColor = Color(0xFFE0E0E0),
        focusedLabelColor = Color(0xFF152A53),
        unfocusedLabelColor = Color.Gray,
        cursorColor = Color(0xFF152A53),
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { tanggalLahir = dateFormatter.format(Date(it)) }
                    showDatePicker = false
                }) { Text("OKE", color = Color(0xFF152A53)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("BATAL", color = Color.Gray) } }
        ) { DatePicker(state = datePickerState) }
    }

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
                                Box(modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
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
                        navigationIcon = { IconButton(onClick = onNavigateToLogin) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) } },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    )
                }
            }
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA)).padding(padding).verticalScroll(scrollState)) {

                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
                    Text("Pendaftaran Anggota Baru", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color(0xFF152A53))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Lengkapi data diri dan foto profil Anda", fontSize = 14.sp, color = Color.Gray)
                }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Parent Box: ukurannya 100dp,TIDAK ADA CLIP DI SINI
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clickable { galleryLauncher.launch("image/*") }
                            ) {
                                // 1. Layer Foto Profil (Ini yang di-clip melingkar)
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(Color(0xFFE0E0E0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (profileBitmap != null) {
                                        Image(
                                            bitmap = profileBitmap!!.asImageBitmap(),
                                            contentDescription = "Foto Profil",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(50.dp))
                                    }
                                }

                                // 2. Layer Ikon Add (Ditaruh di atas, bebas dari clip lingkaran)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        // Tambahkan border putih agar ikon terlihat terpisah (floating)
                                        .background(Color.White, CircleShape)
                                        .padding(2.dp)
                                        // Background biru asli
                                        .background(Color(0xFF152A53), CircleShape)
                                        .padding(5.dp)
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Pilih Foto", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))

                        // --- INPUT FORM LAINNYA ---
                        OutlinedTextField(
                            value = nik, onValueChange = { if (it.length <= 16) nik = it },
                            label = { Text("NIK KTP") }, placeholder = { Text("16 Digit NIK", color = Color.LightGray, fontSize = 14.sp) },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Icon(Icons.Default.Badge, null, tint = Color.Gray) },
                            colors = customTextFieldColors, singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = nama, onValueChange = { nama = it },
                            label = { Text("Nama Lengkap") }, placeholder = { Text("Sesuai KTP", color = Color.LightGray, fontSize = 14.sp) },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Person, null, tint = Color.Gray) },
                            colors = customTextFieldColors, singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        ExposedDropdownMenuBox(expanded = expandedJK, onExpandedChange = { expandedJK = !expandedJK }) {
                            OutlinedTextField(
                                value = jenisKelamin, onValueChange = {}, readOnly = true,
                                label = { Text("Jenis Kelamin") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedJK) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                                colors = customTextFieldColors
                            )
                            ExposedDropdownMenu(expanded = expandedJK, onDismissRequest = { expandedJK = false }) {
                                DropdownMenuItem(text = { Text("Laki-laki") }, onClick = { jenisKelamin = "Laki-laki"; expandedJK = false })
                                DropdownMenuItem(text = { Text("Perempuan") }, onClick = { jenisKelamin = "Perempuan"; expandedJK = false })
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = tempatLahir, onValueChange = { tempatLahir = it }, label = { Text("Tempat Lahir") },
                                modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                                colors = customTextFieldColors, singleLine = true
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = tanggalLahir, onValueChange = {}, label = { Text("Tgl Lahir") }, readOnly = true,
                                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = customTextFieldColors
                                )
                                Box(modifier = Modifier.matchParentSize().clickable { showDatePicker = true })
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = alamat, onValueChange = { alamat = it }, label = { Text("Alamat Lengkap") },
                            modifier = Modifier.fillMaxWidth(), minLines = 3, shape = RoundedCornerShape(12.dp),
                            colors = customTextFieldColors
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- AREA KAMERA KYC ---
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Verifikasi Biometrik", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF152A53))
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (capturedBitmaps.isNullOrEmpty()) {
                            Box(
                                modifier = Modifier.size(120.dp).drawBehind {
                                    drawCircle(color = Color.LightGray, radius = size.minDimension / 2, style = Stroke(width = 4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)))
                                }, contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Outlined.Face, "Wajah", tint = Color.Gray, modifier = Modifier.size(40.dp))
                                    Text("Ambil Foto", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                capturedBitmaps!!.forEachIndexed { index, bitmap ->
                                    Box(modifier = Modifier.padding(horizontal = 6.dp)) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "Preview Wajah ${index + 1}",
                                            modifier = Modifier.size(90.dp).clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Icon(
                                            Icons.Default.CheckCircle, "Sukses", tint = Color(0xFF4CAF50),
                                            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 4.dp, bottom = 4.dp).size(24.dp).background(Color.White, CircleShape)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { isCameraMode = true }, modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B8B8B))
                    ) {
                        Icon(Icons.Default.CameraAlt, null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (capturedBitmaps.isNullOrEmpty()) "BUKA KAMERA" else "ULANGI FOTO", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // --- BUTTON SIMPAN DATA API CALL ---
                    Button(
                        onClick = {
                            if (nik.length != 16) { Toast.makeText(context, "NIK harus 16 digit!", Toast.LENGTH_SHORT).show(); return@Button }
                            if (nama.isEmpty() || tempatLahir.isEmpty() || tanggalLahir.isEmpty() || alamat.isEmpty()) { Toast.makeText(context, "Lengkapi form!", Toast.LENGTH_SHORT).show(); return@Button }
                            if (capturedBitmaps == null || capturedBitmaps!!.size < 3) { Toast.makeText(context, "Silakan ambil 3 foto wajah KYC!", Toast.LENGTH_SHORT).show(); return@Button }

                            isLoading = true
                            scope.launch {
                                try {
                                    // 1. Setup Text Data
                                    val nikBody = nik.toRequestBody("text/plain".toMediaTypeOrNull())
                                    val namaBody = nama.toRequestBody("text/plain".toMediaTypeOrNull())
                                    val jkBody = jenisKelamin.toRequestBody("text/plain".toMediaTypeOrNull())
                                    val tempatBody = tempatLahir.toRequestBody("text/plain".toMediaTypeOrNull())
                                    val tglBody = tanggalLahir.toRequestBody("text/plain".toMediaTypeOrNull())
                                    val alamatBody = alamat.toRequestBody("text/plain".toMediaTypeOrNull())

                                    // 2. Setup KYC Face Images
                                    val faceParts = capturedBitmaps!!.mapIndexed { i, bmp ->
                                        val file = File(context.cacheDir, "face_$i.jpg")
                                        val os = FileOutputStream(file)
                                        bmp.compress(Bitmap.CompressFormat.JPEG, 80, os)
                                        os.close()
                                        MultipartBody.Part.createFormData("face_images", file.name, file.asRequestBody("image/jpeg".toMediaTypeOrNull()))
                                    }

                                    // 3. Setup Profile Photo (Jika Ada)
                                    var profilePart: MultipartBody.Part? = null
                                    if (profileImageUri != null) {
                                        val fileProfile = uriToFile(context, profileImageUri!!)
                                        if (fileProfile != null) {
                                            val reqFile = fileProfile.asRequestBody("image/*".toMediaTypeOrNull())
                                            profilePart = MultipartBody.Part.createFormData("foto_profil", fileProfile.name, reqFile)
                                        }
                                    }

                                    // 4. Panggil API gabungan
                                    val response = RetrofitClient.instance.registerUser(
                                        nikBody, namaBody, jkBody, tempatBody, tglBody, alamatBody, faceParts, profilePart
                                    )

                                    if (response.success) {
                                        Toast.makeText(context, "Registrasi Berhasil!", Toast.LENGTH_LONG).show()
                                        onRegisterComplete()
                                    } else {
                                        Toast.makeText(context, "Gagal: ${response.message}", Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF152A53)), enabled = !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        else Text("SIMPAN DATA", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}