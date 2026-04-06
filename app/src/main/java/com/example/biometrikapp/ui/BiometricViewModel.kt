package com.example.biometrikapp.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.biometrikapp.api.RetrofitClient
import com.example.biometrikapp.api.UserData
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

sealed class ScanState {
    object Idle : ScanState()
    object Scanning : ScanState()
    object Loading : ScanState()
    data class Success(val user: UserData) : ScanState()
    data class Error(val message: String) : ScanState()
}

class BiometricViewModel : ViewModel() {
    var scanState by mutableStateOf<ScanState>(ScanState.Scanning)
        private set

    private val TAG = "BiometricVM"

    fun resetScan() {
        scanState = ScanState.Scanning
    }

    // TERIMA ROTATION DEGREES DINAMIS DARI SENSOR
    fun processFaceImage(context: Context, bitmap: Bitmap, rotationDegrees: Int, expectedNik: String? = null) {
        if (scanState is ScanState.Loading || scanState is ScanState.Success) return
        scanState = ScanState.Loading

        viewModelScope.launch {
            try {
                // Rotasi Dinamis (Anti Miring di Tablet)
                val matrix = Matrix()
                matrix.postRotate(rotationDegrees.toFloat())
                val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

                val file = File(context.cacheDir, "face_capture.jpg")
                val os = FileOutputStream(file)
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, os)
                os.flush()
                os.close()

                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

                val response = RetrofitClient.instance.recognizeFace(body)

                if (response.success && response.data != null) {
                    val detectedUser = response.data

                    if (expectedNik != null) {
                        if (detectedUser.nik == expectedNik) {
                            scanState = ScanState.Success(detectedUser)
                        } else {
                            scanState = ScanState.Error("Wajah terdeteksi sebagai ${detectedUser.nama_lengkap}. Gunakan wajah yang sesuai NIK Anda!")
                        }
                    } else {
                        scanState = ScanState.Success(detectedUser)
                    }

                } else {
                    scanState = ScanState.Error(response.message)
                }
            } catch (e: retrofit2.HttpException) {
                val err = e.response()?.errorBody()?.string()
                Log.e(TAG, "HTTP Error: $err")
                scanState = ScanState.Error("Wajah tidak jelas atau posisi miring.")
            } catch (e: Exception) {
                Log.e(TAG, "Exception: ${e.message}")
                scanState = ScanState.Error("Koneksi gagal atau wajah tidak terdaftar di sistem.")
            }
        }
    }
}