package com.glycemiq.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.glycemiq.data.repository.DataSyncManager
import com.glycemiq.data.repository.MedicationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var alarmScheduler: MedicationAlarmScheduler

    @Inject
    lateinit var medicationRepository: MedicationRepository

    @Inject
    lateinit var dataSyncManager: DataSyncManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val pendingResult = goAsync()
            scope.launch {
                try {
                    dataSyncManager.syncAll()
                    val medications = medicationRepository.getAllMedications().first()
                    alarmScheduler.rescheduleAll(medications)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
