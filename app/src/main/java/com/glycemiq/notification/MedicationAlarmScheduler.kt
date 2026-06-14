package com.glycemiq.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.glycemiq.data.local.entity.Medication
import com.glycemiq.util.DateTimeUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicationAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarm(medication: Medication) {
        if (!medication.isActive) {
            cancelAlarm(medication.id)
            return
        }

        val triggerTime = nextTriggerMillis(medication.scheduledHour, medication.scheduledMinute)
        val pendingIntent = createPendingIntent(medication)

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }

    fun cancelAlarm(medicationId: Long) {
        val intent = Intent(context, MedicationAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medicationId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun rescheduleAll(medications: List<Medication>) {
        medications.forEach { scheduleAlarm(it) }
    }

    private fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val zone = DateTimeUtils.MEXICO_ZONE
        val now = ZonedDateTime.now(zone)
        var scheduled = ZonedDateTime.of(
            LocalDate.now(zone),
            LocalTime.of(hour, minute),
            zone
        )
        if (scheduled.isBefore(now) || scheduled.isEqual(now)) {
            scheduled = scheduled.plusDays(1)
        }
        return scheduled.toInstant().toEpochMilli()
    }

    private fun createPendingIntent(medication: Medication): PendingIntent {
        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            putExtra(MedicationAlarmReceiver.EXTRA_MEDICATION_ID, medication.id)
            putExtra(MedicationAlarmReceiver.EXTRA_MEDICATION_NAME, medication.name)
            putExtra(MedicationAlarmReceiver.EXTRA_MEDICATION_HOUR, medication.scheduledHour)
            putExtra(MedicationAlarmReceiver.EXTRA_MEDICATION_MINUTE, medication.scheduledMinute)
        }
        return PendingIntent.getBroadcast(
            context,
            medication.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
