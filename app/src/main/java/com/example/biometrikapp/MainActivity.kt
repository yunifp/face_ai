package com.example.biometrikapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.biometrikapp.api.UserData

// --- IMPORT SDK ZKTECO ANDROID ---
import com.zkteco.android.biometric.FingerprintExceptionListener
import com.zkteco.android.biometric.core.device.ParameterHelper
import com.zkteco.android.biometric.core.device.TransportType
import com.zkteco.android.biometric.core.utils.ToolUtils
import com.zkteco.android.biometric.module.fingerprintreader.FingerprintCaptureListener
import com.zkteco.android.biometric.module.fingerprintreader.FingerprintSensor
import com.zkteco.android.biometric.module.fingerprintreader.FingprintFactory
import com.zkteco.android.biometric.module.fingerprintreader.exception.FingerprintException

// --- IMPORT SCREEN & UTILS ---
import com.example.biometrikapp.ui.screens.*
import com.example.biometrikapp.ui.theme.BiometrikAppTheme
import java.util.HashMap

class MainActivity : FragmentActivity() {

    private val TAG = "MainActivity"
    private val ZKTECO_VID = 0x1b55
    private val LIVE20R_PID = 0x0120
    private val LIVE10R_PID = 0x0124
    private val PERMISSION_REQUEST_CODE = 9

    // Variabel Sensor & USB
    private var fingerprintSensor: FingerprintSensor? = null
    private var usb_pid = 0
    private var deviceIndex = 0

    // State untuk dikirim ke Jetpack Compose
    private var isSensorConnected by mutableStateOf(false)
    private var lastCapturedBitmap by mutableStateOf<Bitmap?>(null)
    private var lastCapturedTemplate by mutableStateOf<ByteArray?>(null)

    // ============================================================
    // ⚡ LOAD LIBRARIES (Tetap dipertahankan karena isu nama file) ⚡
    // ============================================================
    companion object {
        init {
            try {
                System.loadLibrary("zksensorcore")
                System.loadLibrary("zkalg12")
                System.loadLibrary("slkidcap")
                System.loadLibrary("zkfinger10")
                System.loadLibrary("libzkfp") // Library hasil rename/copy Anda
                Log.d("ZKFP", "Native Libraries loaded successfully!")
            } catch (e: UnsatisfiedLinkError) {
                Log.e("ZKFP", "Fatal: Library not found! ${e.message}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPermissions()

        setContent {
            BiometrikAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        sensor = fingerprintSensor,
                        isConnected = isSensorConnected,
                        capturedBitmap = lastCapturedBitmap,
                        capturedTemplate = lastCapturedTemplate,
                        onConnectRequest = { tryConnectDevice() },
                        onResetHardwareData = {
                            lastCapturedTemplate = null
                            lastCapturedBitmap = null
                        }
                    )
                }
            }
        }
    }

    // ============================================================
    // ⚡ LOGIKA HARDWARE (DIADAPTASI DARI CONTOH JAVA) ⚡
    // ============================================================

    private fun tryConnectDevice() {
        if (isSensorConnected) {
            showToast("Device already connected!")
            return
        }

        if (!enumSensor()) {
            showToast("Device ZKTeco not found! Check USB OTG connection.")
            return
        }

        openDevice()
    }

    private fun enumSensor(): Boolean {
        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        for (device in usbManager.deviceList.values) {
            if (device.vendorId == ZKTECO_VID &&
                (device.productId == LIVE20R_PID || device.productId == LIVE10R_PID)) {
                usb_pid = device.productId
                return true
            }
        }
        return false
    }

    private fun openDevice() {
        // Stop sensor lama jika ada
        fingerprintSensor?.let { FingprintFactory.destroy(it) }

        val deviceParams: MutableMap<String, Any> = HashMap()
        deviceParams[ParameterHelper.PARAM_KEY_VID] = ZKTECO_VID
        deviceParams[ParameterHelper.PARAM_KEY_PID] = usb_pid

        // Buat instance sensor (Android Transport USB)
        fingerprintSensor = FingprintFactory.createFingerprintSensor(this, TransportType.USB, deviceParams)

        try {
            fingerprintSensor?.open(deviceIndex)

            // Pasang listener sesuai contoh Java Anda
            fingerprintSensor?.setFingerprintCaptureListener(deviceIndex, captureListener)
            fingerprintSensor?.SetFingerprintExceptionListener(exceptionListener)

            // Mulai proses pemindaian
            fingerprintSensor?.startCapture(deviceIndex)

            isSensorConnected = true
            showToast("Fingerprint Connected!")
        } catch (e: FingerprintException) {
            Log.e(TAG, "Failed to open device: ${e.message}")
            isSensorConnected = false
            showToast("Connect Failed!")
        }
    }

    // LISTENER CAPTURE (Pusat pengolahan data mentah dari alat)
    private val captureListener = object : FingerprintCaptureListener {
        override fun captureOK(fpImage: ByteArray) {
            // Gunakan ToolUtils untuk merender gambar sidik jari ke Bitmap
            val width = fingerprintSensor?.imageWidth ?: 0
            val height = fingerprintSensor?.imageHeight ?: 0
            val bitmap = ToolUtils.renderCroppedGreyScaleBitmap(fpImage, width, height)

            runOnUiThread {
                lastCapturedBitmap = bitmap
            }
        }

        override fun captureError(e: FingerprintException) {
            // Capture error sering terjadi jika jari diangkat terlalu cepat, jangan matikan koneksi, cukup log saja
            Log.e(TAG, "Capture Error: ${e.message}")
        }

        override fun extractOK(fpTemplate: ByteArray) {
            runOnUiThread {
                lastCapturedTemplate = fpTemplate
            }
        }

        override fun extractError(i: Int) {
            Log.e(TAG, "Extract Error Code: $i")
        }
    }

    // LISTENER EXCEPTION (Menangani saat kabel OTG goyang atau USB error)
    private val exceptionListener = FingerprintExceptionListener {
        Log.e(TAG, "USB Exception occurred! Triggering safe device close.")
        runOnUiThread {
            isSensorConnected = false
            closeDevice() // Tutup alat secara paksa agar tidak terjadi Zombie State
            showToast("Koneksi USB Terputus. Silakan hubungkan ulang.")
        }
    }

    private fun closeDevice() {
        try {
            fingerprintSensor?.stopCapture(deviceIndex)
            fingerprintSensor?.close(deviceIndex)
            isSensorConnected = false
        } catch (e: Exception) {
            Log.e(TAG, "Error closing: ${e.message}")
        }
    }

    // ============================================================
    // ⚡ PERMISSION & UTILS ⚡
    // ============================================================

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        val listNeeded = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (listNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, listNeeded.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    private fun showToast(msg: String) {
        runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
    }

    override fun onDestroy() {
        super.onDestroy()
        closeDevice()
    }
}

@Composable
fun AppNavigation(
    sensor: FingerprintSensor?,
    isConnected: Boolean,
    capturedBitmap: Bitmap?,
    capturedTemplate: ByteArray?,
    onConnectRequest: () -> Unit,
    onResetHardwareData: () -> Unit
) {
    var currentScreen by remember { mutableStateOf("splash") }
    var selectedUser by remember { mutableStateOf<UserData?>(null) }

    var directToScan by remember { mutableStateOf(false) }

    BackHandler(enabled = currentScreen != "dashboard" && currentScreen != "splash") {
        currentScreen = "dashboard"
    }

    when (currentScreen) {
        "splash" -> SplashScreen(onSplashFinished = { currentScreen = "dashboard" })

        "dashboard" -> MainDashboardScreen(
            onStartFaceScan = { currentScreen = "face_detector" },
            onNavigateToRegister = { currentScreen = "register" },
            onStartFingerprintScan = {
                // Reset memori sisa sebelum masuk scanner
                onResetHardwareData()
                currentScreen = "fingerprint_scanner"
            },
            onNavigateToFingerprintRegister = {
                // Reset memori sisa sebelum masuk registrasi
                onResetHardwareData()
                currentScreen = "fingerprint_register"
            }
        )

        "face_detector" -> FaceDetectorScreen(onBack = { currentScreen = "dashboard" })

        "register" -> RegisterScreen(
            onRegisterComplete = { currentScreen = "dashboard" },
            onNavigateToLogin = { currentScreen = "dashboard" }
        )

        "fingerprint_register" -> RegisterFingerScreen(
            isConnected = isConnected,
            capturedBitmap = capturedBitmap,
            capturedTemplate = capturedTemplate,
            onRegisterComplete = {
                currentScreen = "dashboard"
            },
            onBack = { currentScreen = "dashboard" },
            onConnect = onConnectRequest
        )

        "fingerprint_scanner" -> FingerprintScannerScreen(
            isConnected = isConnected,
            capturedBitmap = capturedBitmap,
            capturedTemplate = capturedTemplate,
            onBack = { currentScreen = "dashboard" },
            onConnect = onConnectRequest,
            onResultFound = { user ->
                selectedUser = user // User bisa berisi Data atau null
                currentScreen = "fingerprint_result"
            }
        )

        "fingerprint_result" -> {
            FingerprintResultScreen(
                user = selectedUser,
                onDashboard = { currentScreen = "dashboard" },
                onScanAgain = {
                    onResetHardwareData()
                    directToScan = true
                    currentScreen = "fingerprint_scanner"
                }
            )
        }
    }
}