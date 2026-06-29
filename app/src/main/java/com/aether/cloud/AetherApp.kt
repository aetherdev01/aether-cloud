package com.aether.cloud

import android.app.Application
import com.google.firebase.FirebaseApp

class AetherApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
