package com.glycemiq.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.glycemiq.domain.model.MedicationUi
import com.glycemiq.util.DateTimeUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicationAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAlarm(medication: MedicationUi) {
        if (!medication.isActive) {
            cancelAlarm(medication.id)
            return
        }
        val triggerTime = nextTriggerMillis(
            medication.scheduledHour,
            medication.scheduledMinute,
            medication.intervalHours
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            createPendingIntent(medication)
        )
    }

    fun scheduleNextInterval(medication: MedicationUi) {
        if (!medication.isActive) return
        val triggerTime = Instant.now()
            .plus(medication.intervalHours.toLong(), ChronoUnit.HOURS)
            .toEpochMilli()
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            createPendingIntent(medication)
        )
    }

    fun cancelAlarm(medicationId: String) {
        val intent = Intent(context, MedicationAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medicationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun rescheduleAll(medications: List<MedicationUi>) {
        medications.filter { it.isActive }.forEach { scheduleAlarm(it) }
    }

    private fun nextTriggerMillis(hour: Int, minute: Int, intervalHours: Int): Long {
        val zone = DateTimeUtils.MEXICO_ZONE
        val now = ZonedDateTime.now(zone)
        var scheduled = ZonedDateTime.of(LocalDate.now(zone), LocalTime.of(hour, minute), zone)
        while (scheduled.isBefore(now) || scheduled.isEqual(now)) {
            scheduled = scheduled.plusHours(intervalHours.toLong())
        }
        return scheduled.toInstant().toEpochMilli()
    }

    private fun createPendingIntent(medication: MedicationUi): PendingIntent {
        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            putExtra(MedicationAlarmReceiver.EXTRA_MEDICATION_ID, medication.id)
            putExtra(MedicationAlarmReceiver.EXTRA_MEDICATION_NAME, medication.name)
            putExtra(MedicationAlarmReceiver.EXTRA_INTERVAL_HOURS, medication.intervalHours)
        }
        return PendingIntent.getBroadcast(
            context,
            medication.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
