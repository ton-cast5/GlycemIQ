package com.glycemiq.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.glycemiq.data.repository.MedicationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MedicationAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var alarmScheduler: MedicationAlarmScheduler

    @Inject
    lateinit var medicationRepository: MedicationRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        val medicationId = intent.getStringExtra(EXTRA_MEDICATION_ID) ?: return
        val medicationName = intent.getStringExtra(EXTRA_MEDICATION_NAME) ?: return

        scope.launch {
            try {
                notificationHelper.showMedicationReminder(medicationId, medicationName)
                val medication = medicationRepository.getMedicationById(medicationId)
                if (medication != null && medication.isActive) {
                    alarmScheduler.scheduleNextInterval(medication)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_MEDICATION_ID = "medication_id"
        const val EXTRA_MEDICATION_NAME = "medication_name"
        const val EXTRA_INTERVAL_HOURS = "interval_hours"
    }
}
