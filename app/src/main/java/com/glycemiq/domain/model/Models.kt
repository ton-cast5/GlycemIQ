package com.glycemiq.domain.model

enum class GlucoseContext(val label: String, val shortLabel: String) {
    FASTING("En ayunas", "Ayunas"),
    BEFORE_MEAL("Antes de comer", "Antes"),
    AFTER_MEAL("Después de comer", "Después");

    companion object {
        fun fromName(name: String): GlucoseContext =
            entries.find { it.name == name } ?: FASTING
    }
}

enum class GlucoseLevel(val label: String) {
    LOW("Bajo"),
    NORMAL("Normal"),
    HIGH("Alto");

    companion object {
        fun classify(value: Int): GlucoseLevel = when {
            value < 70 -> LOW
            value <= 99 -> NORMAL
            else -> HIGH
        }
    }
}

enum class MedicationInterval(val hours: Int, val label: String) {
    EVERY_6_HOURS(6, "Cada 6 h"),
    EVERY_8_HOURS(8, "Cada 8 h"),
    EVERY_12_HOURS(12, "Cada 12 h"),
    EVERY_24_HOURS(24, "Cada 24 h");

    companion object {
        fun fromHours(hours: Int): MedicationInterval =
            entries.find { it.hours == hours } ?: EVERY_24_HOURS
    }
}

data class GlucoseRecordUi(
    val id: String = "",
    val value: Int,
    val context: GlucoseContext,
    val timestamp: Long,
    val level: GlucoseLevel = GlucoseLevel.classify(value)
)

data class MedicationUi(
    val id: String = "",
    val name: String,
    val dose: String,
    val scheduledHour: Int,
    val scheduledMinute: Int,
    val intervalHours: Int = 24,
    val recommendForHighGlucose: Boolean = false,
    val isActive: Boolean = true
)

data class ChartDataPoint(
    val label: String,
    val value: Float,
    val level: GlucoseLevel
)

data class Recommendation(
    val message: String,
    val medications: List<String> = emptyList()
)
