package com.example.biometrikapp.utils

import android.graphics.Bitmap
import android.util.Base64
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody

// Konversi Base64 NO_WRAP agar aman dikirim ke server Python
fun ByteArray.toBase64String(): String {
    return Base64.encodeToString(this, Base64.NO_WRAP)
}

fun String.toPart(): RequestBody = RequestBody.create("text/plain".toMediaTypeOrNull(), this)

fun rawByteArrayToBitmap(data: ByteArray, width: Int, height: Int): Bitmap {
    val pixels = IntArray(width * height)
    for (i in 0 until width * height) {
        val gray = data[i].toInt() and 0xff
        pixels[i] = 0xff shl 24 or (gray shl 16) or (gray shl 8) or gray
    }
    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
}