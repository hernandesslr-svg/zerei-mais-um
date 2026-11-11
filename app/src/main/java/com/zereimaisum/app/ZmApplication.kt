package com.zereimaisum.app

import android.app.Application
import com.zereimaisum.app.di.AppContainer

class ZmApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
