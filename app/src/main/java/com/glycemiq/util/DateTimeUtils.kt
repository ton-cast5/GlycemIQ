package com.glycemiq.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeUtils {
    val MEXICO_ZONE: ZoneId = ZoneId.of("America/Mexico_City")
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("es", "MX"))
    private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale("es", "MX"))
    private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale("es", "MX"))

    fun nowMillis(): Long = Instant.now().atZone(MEXICO_ZONE).toInstant().toEpochMilli()

    fun formatDate(timestamp: Long): String =
        Instant.ofEpochMilli(timestamp).atZone(MEXICO_ZONE).format(DATE_FORMATTER)

    fun formatTime(timestamp: Long): String =
        Instant.ofEpochMilli(timestamp).atZone(MEXICO_ZONE).format(TIME_FORMATTER)

    fun formatDateTime(timestamp: Long): String =
        Instant.ofEpochMilli(timestamp).atZone(MEXICO_ZONE).format(DATE_TIME_FORMATTER)

    fun formatHourMinute(hour: Int, minute: Int): String =
        String.format(Locale("es", "MX"), "%02d:%02d", hour, minute)
}
