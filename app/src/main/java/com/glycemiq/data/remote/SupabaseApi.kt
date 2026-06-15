package com.glycemiq.data.remote

import com.glycemiq.BuildConfig
import com.glycemiq.data.remote.dto.GlucoseRecordDto
import com.glycemiq.data.remote.dto.GlucoseRecordInsertDto
import com.glycemiq.data.remote.dto.MedicationDto
import com.glycemiq.data.remote.dto.MedicationInsertDto
import com.glycemiq.data.remote.dto.toUi
import com.glycemiq.domain.model.GlucoseRecordUi
import com.glycemiq.domain.model.MedicationUi
import com.glycemiq.util.DeviceIdProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseApi @Inject constructor(
    private val client: HttpClient,
    private val deviceIdProvider: DeviceIdProvider
) {
    private val baseUrl = "${BuildConfig.SUPABASE_URL}/rest/v1"
    private val apiKey = BuildConfig.SUPABASE_KEY

    private val deviceId get() = deviceIdProvider.getDeviceId()

    // --- Glucose ---

    suspend fun fetchGlucoseRecords(): List<GlucoseRecordUi> =
        client.get("$baseUrl/glucose_records") {
            supabaseHeaders()
            parameter("device_id", "eq.$deviceId")
            parameter("order", "timestamp.desc")
            parameter("select", "*")
        }.body<List<GlucoseRecordDto>>().map { it.toUi() }

    suspend fun fetchGlucoseRecordsSince(startTime: Long): List<GlucoseRecordUi> =
        client.get("$baseUrl/glucose_records") {
            supabaseHeaders()
            parameter("device_id", "eq.$deviceId")
            parameter("timestamp", "gte.$startTime")
            parameter("order", "timestamp.asc")
            parameter("select", "*")
        }.body<List<GlucoseRecordDto>>().map { it.toUi() }

    suspend fun insertGlucoseRecord(
        value: Int,
        context: String,
        timestamp: Long
    ): GlucoseRecordUi {
        val result = client.post("$baseUrl/glucose_records") {
            supabaseHeaders(returnRepresentation = true)
            setBody(
                GlucoseRecordInsertDto(
                    deviceId = deviceId,
                    value = value,
                    context = context,
                    timestamp = timestamp
                )
            )
        }.body<List<GlucoseRecordDto>>()
        return result.first().toUi()
    }

    suspend fun deleteGlucoseRecord(id: String) {
        client.delete("$baseUrl/glucose_records") {
            supabaseHeaders()
            parameter("id", "eq.$id")
        }
    }

    // --- Medications ---

    suspend fun fetchMedications(): List<MedicationUi> =
        client.get("$baseUrl/medications") {
            supabaseHeaders()
            parameter("device_id", "eq.$deviceId")
            parameter("order", "scheduled_hour,scheduled_minute")
            parameter("select", "*")
        }.body<List<MedicationDto>>().map { it.toUi() }

    suspend fun insertMedication(dto: MedicationInsertDto): MedicationUi {
        val result = client.post("$baseUrl/medications") {
            supabaseHeaders(returnRepresentation = true)
            setBody(dto.copy(deviceId = deviceId))
        }.body<List<MedicationDto>>()
        return result.first().toUi()
    }

    suspend fun updateMedication(medication: MedicationUi) {
        client.patch("$baseUrl/medications") {
            supabaseHeaders()
            parameter("id", "eq.${medication.id}")
            setBody(
                MedicationDto(
                    id = medication.id,
                    deviceId = deviceId,
                    name = medication.name,
                    dose = medication.dose,
                    scheduledHour = medication.scheduledHour,
                    scheduledMinute = medication.scheduledMinute,
                    intervalHours = medication.intervalHours,
                    recommendForHighGlucose = medication.recommendForHighGlucose,
                    isActive = medication.isActive
                )
            )
        }
    }

    suspend fun deleteMedication(id: String) {
        client.delete("$baseUrl/medications") {
            supabaseHeaders()
            parameter("id", "eq.$id")
        }
    }

    suspend fun getMedicationById(id: String): MedicationUi? {
        val result = client.get("$baseUrl/medications") {
            supabaseHeaders()
            parameter("id", "eq.$id")
            parameter("select", "*")
        }.body<List<MedicationDto>>()
        return result.firstOrNull()?.toUi()
    }

    private fun io.ktor.client.request.HttpRequestBuilder.supabaseHeaders(returnRepresentation: Boolean = false) {
        header("apikey", apiKey)
        header("Authorization", "Bearer $apiKey")
        contentType(ContentType.Application.Json)
        if (returnRepresentation) {
            header("Prefer", "return=representation")
        }
    }
}
