package com.glycemiq.domain.model

enum class GlucoseContext(val label: String) {
    FASTING("En ayunas"),
    BEFORE_MEAL("Antes de comer"),
    AFTER_MEAL("Después de comer");

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

data class GlucoseRecordUi(
    val id: Long = 0,
    val value: Int,
    val context: GlucoseContext,
    val timestamp: Long,
    val level: GlucoseLevel = GlucoseLevel.classify(value)
)

data class MedicationUi(
    val id: Long = 0,
    val name: String,
    val dose: String,
    val scheduledHour: Int,
    val scheduledMinute: Int,
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
