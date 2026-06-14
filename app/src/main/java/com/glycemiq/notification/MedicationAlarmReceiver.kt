package com.glycemiq.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.glycemiq.data.local.GlycemIQDatabase
import com.glycemiq.data.local.entity.Medication
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
    lateinit var database: GlycemIQDatabase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        val medicationId = intent.getLongExtra(EXTRA_MEDICATION_ID, -1)
        val medicationName = intent.getStringExtra(EXTRA_MEDICATION_NAME) ?: return
        val hour = intent.getIntExtra(EXTRA_MEDICATION_HOUR, 0)
        val minute = intent.getIntExtra(EXTRA_MEDICATION_MINUTE, 0)

        scope.launch {
            try {
                notificationHelper.showMedicationReminder(medicationId, medicationName)

                val medication = database.medicationDao().getById(medicationId)
                if (medication != null && medication.isActive) {
                    alarmScheduler.scheduleAlarm(
                        Medication(
                            id = medicationId,
                            name = medicationName,
                            dose = medication.dose,
                            scheduledHour = hour,
                            scheduledMinute = minute,
                            isActive = true
                        )
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_MEDICATION_ID = "medication_id"
        const val EXTRA_MEDICATION_NAME = "medication_name"
        const val EXTRA_MEDICATION_HOUR = "medication_hour"
        const val EXTRA_MEDICATION_MINUTE = "medication_minute"
    }
}
