package com.glycemiq.data.sync

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.glycemiq.data.remote.SupabaseRealtimeClient
import com.glycemiq.data.repository.DataSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RealtimeDataSync @Inject constructor(
    private val realtimeClient: SupabaseRealtimeClient,
    private val dataSyncManager: DataSyncManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false

    fun start() {
        if (started) return
        started = true

        scope.launch {
            dataSyncManager.syncAll()
            realtimeClient.listen(scope) {
                dataSyncManager.syncAllDebounced()
            }
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                scope.launch { dataSyncManager.syncAllDebounced() }
            }
        })
    }
}
