package com.beradeep.aiyo.di

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AiyoApp : Application() {
    companion object {
        lateinit var instance: AiyoApp
            private set

        fun init(app: Application) {
            instance = app as AiyoApp
        }
    }
}