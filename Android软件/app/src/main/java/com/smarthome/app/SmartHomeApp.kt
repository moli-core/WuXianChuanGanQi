package com.smarthome.app

import android.app.Application
import com.smarthome.app.core.di.appModule
import com.smarthome.app.core.di.networkModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SmartHomeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SmartHomeApp)
            modules(appModule, networkModule)
        }
    }
}
