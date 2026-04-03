package com.reskyu.merchant

import android.app.Application

/**
 * Application class registered in AndroidManifest via android:name=".MerchantApplication".
 * Firebase is auto-initialized by the google-services plugin — no manual call needed.
 */
class MerchantApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Firebase auto-initializes via google-services plugin (no FirebaseApp.initializeApp needed)
    }
}
