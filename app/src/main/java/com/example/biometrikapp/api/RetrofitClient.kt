package com.example.biometrikapp.api

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// --- DATA MODELS ---
data class StandardResponse<T>(val success: Boolean, val message: String, val data: T?)

data class UserData(
    val id: Int,
    val nik: String,
    val nama_lengkap: String,
    val jenis_kelamin: String,
    val tempat_lahir: String,
    val tanggal_lahir: String,
    val alamat: String,
    val foto_profil: String?
)

data class FingerprintRequest(
    val finger_id: Int,
    val finger_name: String,
    val template_data: String
)

// Model baru untuk menerima gabungan template dan user
data class FingerprintTemplateData(
    val finger_id: Int,
    val finger_name: String,
    val template_data: String,
    val user: UserData
)

data class LoginRequest(val nik: String)

// --- INTERFACE API ---
interface ApiService {
    @POST("users/login")
    suspend fun login(@Body request: LoginRequest): StandardResponse<UserData>

    // DIPERBAIKI: Hapus garis miring (/) di akhir agar tidak error 404/307 di FastAPI
    @GET("check-connection")
    suspend fun checkConnection(): StandardResponse<Any>

    @GET("users/{user_id}")
    suspend fun getUser(@Path("user_id") userId: Int): StandardResponse<UserData>

    @GET("users/check-nik/{nik}")
    suspend fun checkNik(@Path("nik") nik: String): StandardResponse<UserData>

    @Multipart
    @POST("users/recognize-face/")
    suspend fun recognizeFace(@Part file: MultipartBody.Part): StandardResponse<UserData>

    @Multipart
    @POST("users/")
    suspend fun registerUser(
        @Part("nik") nik: RequestBody, @Part("nama_lengkap") namaLengkap: RequestBody,
        @Part("jenis_kelamin") jenisKelamin: RequestBody, @Part("tempat_lahir") tempatLahir: RequestBody,
        @Part("tanggal_lahir") tanggalLahir: RequestBody, @Part("alamat") alamat: RequestBody,
        @Part face_images: List<MultipartBody.Part>, @Part foto_profil: MultipartBody.Part? = null
    ): StandardResponse<UserData>

    // SUDAH SINKRON DENGAN PREFIX "/finger"
    @Multipart
    @POST("finger/register-profile")
    suspend fun registerUserProfile(
        @Part("nik") nik: RequestBody,
        @Part("nama_lengkap") nama: RequestBody,
        @Part("jenis_kelamin") jk: RequestBody,
        @Part("tempat_lahir") tempat: RequestBody,
        @Part("tanggal_lahir") tgl: RequestBody,
        @Part("alamat") alamat: RequestBody,
        @Part foto: MultipartBody.Part?
    ): StandardResponse<UserData>

    // SUDAH SINKRON DENGAN PREFIX "/finger"
    @POST("finger/{user_id}/fingerprint")
    suspend fun saveFingerprint(
        @Path("user_id") userId: Int,
        @Body request: FingerprintRequest
    ): StandardResponse<Any>

    // SUDAH SINKRON DENGAN PREFIX "/finger"
    @GET("finger/all-fingerprints")
    suspend fun getAllFingerprints(): StandardResponse<List<FingerprintTemplateData>>
}

// --- CLIENT SETUP ---
object RetrofitClient {
    private const val BASE_URL = "http://103.30.86.85:5800/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}