package com.glycemiq

import android.app.Application
import com.glycemiq.data.sync.RealtimeDataSync
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GlycemIQApplication : Application() {

    @Inject
    lateinit var realtimeDataSync: RealtimeDataSync

    override fun onCreate() {
        super.onCreate()
        realtimeDataSync.start()
    }
}
