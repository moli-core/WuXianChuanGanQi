package com.smarthome.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.tokenDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

class TokenDataStore(private val context: Context) {

    private object Keys {
        val TOKEN = stringPreferencesKey("jwt_token")
        val USER_ID = intPreferencesKey("user_id")
        val USERNAME = stringPreferencesKey("username")
        val NICKNAME = stringPreferencesKey("nickname")
        val ROLE = stringPreferencesKey("role")
    }

    val token: Flow<String?> = context.tokenDataStore.data.map { it[Keys.TOKEN] }

    val userId: Flow<Int?> = context.tokenDataStore.data.map { it[Keys.USER_ID] }

    val username: Flow<String?> = context.tokenDataStore.data.map { it[Keys.USERNAME] }

    val nickname: Flow<String?> = context.tokenDataStore.data.map { it[Keys.NICKNAME] }

    val role: Flow<String?> = context.tokenDataStore.data.map { it[Keys.ROLE] }

    val isLoggedIn: Flow<Boolean> = token.map { !it.isNullOrEmpty() }

    suspend fun getToken(): String? {
        return context.tokenDataStore.data.first()[Keys.TOKEN]
    }

    suspend fun saveAuthData(
        token: String,
        userId: Int,
        username: String,
        nickname: String,
        role: String
    ) {
        context.tokenDataStore.edit { prefs ->
            prefs[Keys.TOKEN] = token
            prefs[Keys.USER_ID] = userId
            prefs[Keys.USERNAME] = username
            prefs[Keys.NICKNAME] = nickname
            prefs[Keys.ROLE] = role
        }
    }

    suspend fun clearAuth() {
        context.tokenDataStore.edit { it.clear() }
    }

    suspend fun getNickname(): String? {
        return context.tokenDataStore.data.first()[Keys.NICKNAME]
    }
}
