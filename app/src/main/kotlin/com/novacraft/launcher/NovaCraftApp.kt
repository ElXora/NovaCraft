package com.novacraft.launcher

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * NovaCraftApp
 *
 * Application entry point. Initializes Hilt dependency injection,
 * Timber logging, and WorkManager with Hilt integration.
 */
@HiltAndroidApp
class NovaCraftApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.d("NovaCraft Launcher started - Java Minecraft. Anywhere.")
    }
}
