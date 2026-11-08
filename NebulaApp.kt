package com.nebula.app

import android.app.Application
import com.nebula.app.di.AppContainer

class NebulaApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
