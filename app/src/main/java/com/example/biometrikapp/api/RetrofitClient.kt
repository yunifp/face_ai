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
    val id: Long,
    val nik: String,
    val nama_lengkap: String?,
    val nama_penduduk: String?,
    val jenis_kelamin: String?,
    val tempat_lahir: String?,
    val tanggal_lahir: String?,
    val alamat: String?,
    val nama_pro: String?,
    val nama_kab: String?,
    val nama_kec: String?,
    val nama_desa: String?,
    val rt: String?,
    val rw: String?,
    val foto_profil: String?
)

data class FingerprintRequest(
    val finger_id: Int,
    val finger_name: String,
    val template_data: String
)

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

    @GET("check-connection")
    suspend fun checkConnection(): StandardResponse<Any>

    @GET("users/")
    suspend fun getAllUsers(): StandardResponse<List<UserData>>

    @GET("users/{user_id}")
    suspend fun getUser(@Path("user_id") userId: Long): StandardResponse<UserData>

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

    // --- ENDPOINT BARU UNTUK WAJAH SAJA ---
    @Multipart
    @POST("users/{user_id}/faces")
    suspend fun registerFaceImages(
        @Path("user_id") userId: Long,
        @Part face_images: List<MultipartBody.Part>
    ): StandardResponse<UserData>

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

    @POST("finger/{user_id}/fingerprint")
    suspend fun saveFingerprint(
        @Path("user_id") userId: Long,
        @Body request: FingerprintRequest
    ): StandardResponse<Any>

    @GET("finger/all-fingerprints")
    suspend fun getAllFingerprints(): StandardResponse<List<FingerprintTemplateData>>

    @POST("sync/trigger")
    suspend fun triggerSync(): StandardResponse<Any>
}

// --- CLIENT SETUP ---
object RetrofitClient {
    const val BASE_URL = "https://votenow.hitungsuara.id/biometrik/"

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