package com.get.dailymantra.common.core.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import androidx.core.content.edit
import javax.inject.Singleton

@Singleton
class EncryptedTokenStore @Inject constructor(
    @ApplicationContext context: Context,
) : TokenStore {

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "daily_mantra_secure_prefs",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    override fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    override fun saveTokens(accessToken: String, refreshToken: String) = prefs.edit {
        putString(KEY_ACCESS_TOKEN, accessToken)
        putString(KEY_REFRESH_TOKEN, refreshToken)
    }

    override fun clearTokens() = prefs.edit {
        remove(KEY_ACCESS_TOKEN)
        remove(KEY_REFRESH_TOKEN)
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "auth_access_token"
        const val KEY_REFRESH_TOKEN = "auth_refresh_token"
    }
}
