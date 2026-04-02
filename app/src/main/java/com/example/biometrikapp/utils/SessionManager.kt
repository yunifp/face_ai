@file:Suppress("DEPRECATION")

package com.example.biometrikapp.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import androidx.core.content.edit

object SessionManager {
    private const val PREF_NAME = "secure_user_session"
    private const val KEY_EMAIL = "user_email"
    private const val KEY_PASSWORD = "user_password"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"

    private fun getSharedPrefs(context: Context) = EncryptedSharedPreferences.create(
        PREF_NAME,
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveLogin(context: Context, email: String, pass: String) {
        getSharedPrefs(context).edit {
            putString(KEY_EMAIL, email)
            putString(KEY_PASSWORD, pass)
            putBoolean(KEY_IS_LOGGED_IN, true)
        }
    }

    fun getSavedEmail(context: Context) = getSharedPrefs(context).getString(KEY_EMAIL, null)
    fun isLoggedIn(context: Context) = getSharedPrefs(context).getBoolean(KEY_IS_LOGGED_IN, false)

    fun logout(context: Context) {
        getSharedPrefs(context).edit().clear().apply()
    }
}