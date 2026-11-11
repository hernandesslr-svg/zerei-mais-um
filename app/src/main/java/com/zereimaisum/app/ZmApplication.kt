package com.zereimaisum.app

import android.app.Application
import com.zereimaisum.app.di.AppContainer
import com.zereimaisum.app.notifications.BillReminders

class ZmApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        BillReminders.createChannel(this)
        BillReminders.schedule(this)
    }
}
