package com.glycemiq.data.remote.dto

import com.glycemiq.domain.model.GlucoseContext
import com.glycemiq.domain.model.GlucoseLevel
import com.glycemiq.domain.model.GlucoseRecordUi
import com.glycemiq.domain.model.MedicationUi
import com.glycemiq.util.DateTimeUtils
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GlucoseRecordDto(
    val id: String? = null,
    @SerialName("device_id") val deviceId: String,
    val value: Int,
    val context: String,
    @SerialName("recorded_at") val recordedAt: String
)

@Serializable
data class MedicationDto(
    val id: String? = null,
    @SerialName("device_id") val deviceId: String,
    val name: String,
    val dose: String,
    @SerialName("scheduled_hour") val scheduledHour: Int,
    @SerialName("scheduled_minute") val scheduledMinute: Int,
    @SerialName("interval_hours") val intervalHours: Int = 24,
    @SerialName("recommend_for_high_glucose") val recommendForHighGlucose: Boolean = false,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class GlucoseRecordInsertDto(
    @SerialName("device_id") val deviceId: String,
    val value: Int,
    val context: String,
    @SerialName("recorded_at") val recordedAt: String
)

@Serializable
data class MedicationInsertDto(
    @SerialName("device_id") val deviceId: String,
    val name: String,
    val dose: String,
    @SerialName("scheduled_hour") val scheduledHour: Int,
    @SerialName("scheduled_minute") val scheduledMinute: Int,
    @SerialName("interval_hours") val intervalHours: Int,
    @SerialName("recommend_for_high_glucose") val recommendForHighGlucose: Boolean,
    @SerialName("is_active") val isActive: Boolean = true
)

fun GlucoseRecordDto.toUi(): GlucoseRecordUi = GlucoseRecordUi(
    id = id.orEmpty(),
    value = value,
    context = GlucoseContext.fromName(context),
    timestamp = DateTimeUtils.parseRecordedAtToMillis(recordedAt),
    level = GlucoseLevel.classify(value)
)

fun MedicationDto.toUi(): MedicationUi = MedicationUi(
    id = id.orEmpty(),
    name = name,
    dose = dose,
    scheduledHour = scheduledHour,
    scheduledMinute = scheduledMinute,
    intervalHours = intervalHours,
    recommendForHighGlucose = recommendForHighGlucose,
    isActive = isActive
)
