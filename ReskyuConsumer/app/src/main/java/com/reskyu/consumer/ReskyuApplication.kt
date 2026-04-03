package com.reskyu.consumer

import android.app.Application

/**
 * ReskyuApplication
 *
 * The entry point of the app. Used to initialize global singletons like
 * Firebase, Crashlytics, and other SDKs before any Activity starts.
 *
 * Remember to register this in AndroidManifest.xml:
 *   android:name=".ReskyuApplication"
 */
class ReskyuApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // TODO: Initialize Firebase
        // FirebaseApp.initializeApp(this)

        // TODO: Initialize Crashlytics (optional)
        // FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
    }
}
