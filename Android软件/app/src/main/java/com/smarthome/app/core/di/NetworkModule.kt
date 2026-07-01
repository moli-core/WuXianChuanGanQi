package com.smarthome.app.core.di

import com.smarthome.app.data.remote.api.*
import com.squareup.moshi.Moshi
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

val networkModule = module {
    // Retrofit
    single {
        val appSettingsDataStore = get<com.smarthome.app.core.datastore.AppSettingsDataStore>()
        val host = runBlocking { appSettingsDataStore.getBaseUrl() }
        val port = runBlocking { appSettingsDataStore.getBasePort() }
        val baseUrl = "http://$host:$port/"

        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(get<OkHttpClient>())
            .addConverterFactory(MoshiConverterFactory.create(get<Moshi>()))
            .build()
    }

    // API interfaces
    single { get<Retrofit>().create(AuthApi::class.java) }
    single { get<Retrofit>().create(DeviceApi::class.java) }
    single { get<Retrofit>().create(DataApi::class.java) }
    single { get<Retrofit>().create(AiApi::class.java) }
    single { get<Retrofit>().create(WeatherApi::class.java) }
    single { get<Retrofit>().create(VoiceApi::class.java) }
    single { get<Retrofit>().create(AiotDeviceApi::class.java) }
    single { get<Retrofit>().create(ReportApi::class.java) }
    single { get<Retrofit>().create(LogApi::class.java) }

    // Repositories
    single { com.smarthome.app.data.repository.AuthRepository(get(), get()) }
    single { com.smarthome.app.data.repository.DeviceRepository(get()) }
    single { com.smarthome.app.data.repository.DashboardRepository(get()) }
    single { com.smarthome.app.data.repository.AiRepository(get()) }
    single { com.smarthome.app.data.repository.WeatherRepository(get()) }
    single { com.smarthome.app.data.repository.VoiceRepository(get()) }
    single { com.smarthome.app.data.repository.AiotDeviceRepository(get()) }
    single { com.smarthome.app.data.repository.ReportRepository(get()) }
    single { com.smarthome.app.data.repository.LogRepository(get()) }
    single { com.smarthome.app.data.websocket.DeviceStateWebSocket(get(), get()) }

    // ViewModels
    factory { com.smarthome.app.ui.home.HomeViewModel(get(), get(), get()) }
    factory { com.smarthome.app.ui.auth.LoginViewModel(get()) }
    factory { com.smarthome.app.ui.auth.RegisterViewModel(get()) }
    factory { com.smarthome.app.ui.device.DeviceListViewModel(get()) }
    factory { com.smarthome.app.ui.chat.AiChatViewModel(get(), get(), get()) }
    factory { com.smarthome.app.ui.weather.WeatherViewModel(get()) }
    factory { com.smarthome.app.ui.report.ReportViewModel(get()) }
    factory { com.smarthome.app.ui.log.LogViewModel(get()) }
    factory { com.smarthome.app.ui.profile.ProfileViewModel(get(), get(), get()) }
    factory { com.smarthome.app.ui.profile.ProfileEditViewModel(get(), get()) }
    factory { com.smarthome.app.ui.voice.VoiceViewModel(get(), get()) }
}
