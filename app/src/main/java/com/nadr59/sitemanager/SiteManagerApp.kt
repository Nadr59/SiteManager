// app/src/main/java/com/nadr59/sitemanager/SiteManagerApp.kt

package com.nadr59.sitemanager

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class SiteManagerApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // تهيئة Timber للـ logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
