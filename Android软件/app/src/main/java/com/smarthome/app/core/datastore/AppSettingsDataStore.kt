package com.smarthome.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

class AppSettingsDataStore(private val context: Context) {

    private object Keys {
        val BASE_URL = stringPreferencesKey("base_url")
        val BASE_PORT = intPreferencesKey("base_port")
    }

    val baseUrl: Flow<String> = context.settingsDataStore.data.map { it[Keys.BASE_URL] ?: DEFAULT_HOST }

    val basePort: Flow<Int> = context.settingsDataStore.data.map { it[Keys.BASE_PORT] ?: DEFAULT_PORT }

    suspend fun getBaseUrl(): String {
        return context.settingsDataStore.data.first()[Keys.BASE_URL] ?: DEFAULT_HOST
    }

    suspend fun getBasePort(): Int {
        return context.settingsDataStore.data.first()[Keys.BASE_PORT] ?: DEFAULT_PORT
    }

    suspend fun saveBaseUrl(host: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.BASE_URL] = host
        }
    }

    suspend fun saveBasePort(port: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.BASE_PORT] = port
        }
    }

    companion object {
        const val DEFAULT_HOST = "192.168.198.114"
        const val DEFAULT_PORT = 8080
    }
}
