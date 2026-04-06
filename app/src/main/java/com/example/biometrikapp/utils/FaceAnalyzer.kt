package com.example.biometrikapp.utils

import android.graphics.Bitmap
import android.graphics.Rect
import android.media.Image
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark

class FaceAnalyzer(
    private val onResult: (Bitmap, Int, Boolean, String) -> Unit
) : ImageAnalysis.Analyzer {

    // WAJIB AKTIF: Classification (untuk Null Check) & Landmark (untuk pastikan organ wajah ada)
    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL) // <--- TAMBAH INI
        .build()

    private val detector = FaceDetection.getClient(options)
    private var isCooldown = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage: Image? = imageProxy.image

        if (mediaImage != null && !isCooldown) {
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

            detector.process(image)
                .addOnSuccessListener { faces ->
                    if (faces.isEmpty()) {
                        onResult(imageProxy.toBitmap(), rotationDegrees, false, "Wajah tidak terdeteksi")
                    } else {
                        val face = faces.first()

                        val frameWidth = if (rotationDegrees == 90 || rotationDegrees == 270) mediaImage.height else mediaImage.width
                        val frameHeight = if (rotationDegrees == 90 || rotationDegrees == 270) mediaImage.width else mediaImage.height

                        val targetWidth = frameWidth * 0.75f
                        val targetHeight = targetWidth
                        val targetLeft = (frameWidth - targetWidth) / 2
                        val targetTop = (frameHeight - targetHeight) / 2
                        val targetRect = Rect(targetLeft.toInt(), targetTop.toInt(), (targetLeft + targetWidth).toInt(), (targetTop + targetHeight).toInt())

                        val faceRect = face.boundingBox
                        val isInside = targetRect.contains(faceRect)
                        val isTooSmall = faceRect.width() < (targetWidth * 0.35f)
                        val isCropped = faceRect.left < 5 || faceRect.top < 5 || faceRect.right > frameWidth - 5 || faceRect.bottom > frameHeight - 5

                        val rotY = face.headEulerAngleY
                        val rotZ = face.headEulerAngleZ
                        val isLookingAway = rotY > 15 || rotY < -15 || rotZ > 12 || rotZ < -12

                        // --- JURUS ANTI-PENGHALANG (TANPA HARUS SENYUM) ---

                        // 1. LANDMARK CHECK: Jika hidung/mulut ditutup tebal, landmark akan hilang (null)
                        val nose = face.getLandmark(FaceLandmark.NOSE_BASE)
                        val mouthBottom = face.getLandmark(FaceLandmark.MOUTH_BOTTOM)
                        val mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)
                        val mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)

                        val isLandmarkMissing = nose == null || mouthBottom == null || mouthLeft == null || mouthRight == null

                        // 2. CLASSIFICATION NULL CHECK:
                        // Wajah datar (biasa) akan menghasilkan angka 0.01 - 0.2 (TIDAK NULL).
                        // Jika pakai masker atau tertutup tangan penuh, AI gagal menghitung -> Hasilnya NULL.
                        val smileProb = face.smilingProbability
                        val isMouthOccluded = smileProb == null

                        val leftEye = face.leftEyeOpenProbability
                        val rightEye = face.rightEyeOpenProbability
                        val isEyesOccluded = leftEye == null || rightEye == null || leftEye < 0.2f || rightEye < 0.2f

                        when {
                            isCropped -> onResult(imageProxy.toBitmap(), rotationDegrees, false, "Wajah Terpotong! Mundur sedikit")
                            !isInside -> onResult(imageProxy.toBitmap(), rotationDegrees, false, "Posisikan wajah di dalam oval")
                            isTooSmall -> onResult(imageProxy.toBitmap(), rotationDegrees, false, "Maju lebih dekat ke kamera")
                            isLookingAway -> onResult(imageProxy.toBitmap(), rotationDegrees, false, "Tatap lurus sejajar kamera")

                            isEyesOccluded -> onResult(imageProxy.toBitmap(), rotationDegrees, false, "Buka mata / lepas kacamata hitam")

                            // TOLAK JIKA TERTUTUP BENDA (Masker/Tangan)
                            isLandmarkMissing || isMouthOccluded -> onResult(imageProxy.toBitmap(), rotationDegrees, false, "Singkirkan tangan/masker dari wajah!")

                            else -> {
                                isCooldown = true
                                val bitmap = imageProxy.toBitmap()
                                onResult(bitmap, rotationDegrees, true, "Posisi Sempurna! Memproses...")
                            }
                        }
                    }
                }
                .addOnFailureListener { Log.e("FaceAnalyzer", "Face detection failed", it) }
                .addOnCompleteListener { imageProxy.close() }
        } else {
            imageProxy.close()
        }
    }

    fun resetCooldown() { isCooldown = false }
}