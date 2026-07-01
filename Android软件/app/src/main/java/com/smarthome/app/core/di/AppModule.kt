package com.smarthome.app.core.di

import android.content.Context
import com.smarthome.app.core.datastore.AppSettingsDataStore
import com.smarthome.app.core.datastore.TokenDataStore
import com.smarthome.app.core.network.AuthInterceptor
import com.smarthome.app.core.network.WebSocketManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val appModule = module {
    // Moshi
    single {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    // OkHttpClient with AuthInterceptor
    single {
        val authInterceptor = AuthInterceptor(androidContext())
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // DataStores
    single { TokenDataStore(androidContext()) }
    single { AppSettingsDataStore(androidContext()) }

    // WebSocket
    single { WebSocketManager(get()) }
}
