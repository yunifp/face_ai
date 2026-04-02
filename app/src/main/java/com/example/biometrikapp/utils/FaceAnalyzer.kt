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

class FaceAnalyzer(
    // Kita ubah callback agar mendukung status validasi posisi
    private val onResult: (Bitmap, Boolean, String) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE) // Lebih akurat untuk deteksi batas
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
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
                        onResult(imageProxy.toBitmap(), false, "Wajah tidak terdeteksi")
                    } else {
                        val face = faces.first()
                        val faceRect = face.boundingBox

                        // Ukuran Frame Kamera
                        // Perlu diingat: jika rotasi 90/270, lebar dan tinggi tertukar
                        val frameWidth = if (rotationDegrees == 90 || rotationDegrees == 270) mediaImage.height else mediaImage.width
                        val frameHeight = if (rotationDegrees == 90 || rotationDegrees == 270) mediaImage.width else mediaImage.height

                        // 1. Definisikan area kotak target (Target Box)
                        // Sesuai dengan UI kita (75% lebar, rasio 1:1)
                        val targetWidth = frameWidth * 0.75f
                        val targetHeight = targetWidth // Kotak di UI kita biasanya bujur sangkar/oval
                        val targetLeft = (frameWidth - targetWidth) / 2
                        val targetTop = (frameHeight - targetHeight) / 2
                        val targetRect = Rect(
                            targetLeft.toInt(),
                            targetTop.toInt(),
                            (targetLeft + targetWidth).toInt(),
                            (targetTop + targetHeight).toInt()
                        )

                        // 2. VALIDASI POSISI
                        // A. Cek apakah wajah berada sepenuhnya di dalam targetRect
                        val isInside = targetRect.contains(faceRect)

                        // B. Cek apakah wajah terlalu kecil (minimal mengisi 40% dari target box)
                        val isTooSmall = faceRect.width() < (targetWidth * 0.20f)

                        // C. Cek apakah wajah terpotong frame kamera (Margin 2%)
                        val isCropped = faceRect.left < 5 || faceRect.top < 5 ||
                                faceRect.right > frameWidth - 5 || faceRect.bottom > frameHeight - 5

                        when {
                            isCropped -> {
                                onResult(imageProxy.toBitmap(), false, "Wajah Terpotong! Mundur sedikit")
                            }
                            !isInside -> {
                                onResult(imageProxy.toBitmap(), false, "Posisikan wajah di dalam kotak")
                            }
                            isTooSmall -> {
                                onResult(imageProxy.toBitmap(), false, "Dekatkan wajah ke kamera")
                            }
                            else -> {
                                // JIKA LOLOS SEMUA VALIDASI
                                isCooldown = true
                                val bitmap = imageProxy.toBitmap()
                                onResult(bitmap, true, "Posisi Bagus! Diam sebentar...")
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("FaceAnalyzer", "Face detection failed", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    fun resetCooldown() {
        isCooldown = false
    }
}