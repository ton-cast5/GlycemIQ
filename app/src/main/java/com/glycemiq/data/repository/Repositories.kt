package com.glycemiq.data.repository

import com.glycemiq.data.remote.SupabaseApi
import com.glycemiq.data.remote.dto.MedicationInsertDto
import com.glycemiq.domain.model.ChartDataPoint
import com.glycemiq.domain.model.GlucoseContext
import com.glycemiq.domain.model.GlucoseLevel
import com.glycemiq.domain.model.GlucoseRecordUi
import com.glycemiq.domain.model.MedicationUi
import com.glycemiq.domain.model.Recommendation
import com.glycemiq.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlucoseRepository @Inject constructor(
    private val supabaseApi: SupabaseApi
) {
    private val mutex = Mutex()
    private val _records = MutableStateFlow<List<GlucoseRecordUi>>(emptyList())

    suspend fun refresh() {
        mutex.withLock {
            _records.value = supabaseApi.fetchGlucoseRecords()
        }
    }

    fun getAllRecords(): Flow<List<GlucoseRecordUi>> = _records.asStateFlow()

    fun getRecordsSnapshot(): List<GlucoseRecordUi> = _records.value

    fun getRecentRecords(limit: Int = 5): Flow<List<GlucoseRecordUi>> =
        _records.asStateFlow().map { it.take(limit) }

    suspend fun addRecord(value: Int, context: GlucoseContext): String {
        require(value in 20..600) { "El nivel de glucosa debe estar entre 20 y 600 mg/dL" }
        val record = supabaseApi.insertGlucoseRecord(value, context.name)
        mutex.withLock {
            _records.value = listOf(record) + _records.value
        }
        return record.id
    }

    suspend fun deleteRecord(id: String) {
        supabaseApi.deleteGlucoseRecord(id)
        mutex.withLock {
            _records.value = _records.value.filter { it.id != id }
        }
    }

    fun calculateDailyAverages(records: List<GlucoseRecordUi>): List<ChartDataPoint> =
        records
            .groupBy { DateTimeUtils.formatDate(it.timestamp) }
            .map { (date, dayRecords) ->
                val avg = dayRecords.map { it.value }.average().toFloat()
                ChartDataPoint(date, avg, GlucoseLevel.classify(avg.toInt()))
            }
            .sortedBy { it.label }

    fun calculateWeeklyAverages(records: List<GlucoseRecordUi>): List<ChartDataPoint> =
        records
            .groupBy { record ->
                val zoned = Instant.ofEpochMilli(record.timestamp).atZone(DateTimeUtils.MEXICO_ZONE)
                val weekStart = zoned.toLocalDate().with(java.time.DayOfWeek.MONDAY)
                DateTimeUtils.formatDate(
                    weekStart.atStartOfDay(DateTimeUtils.MEXICO_ZONE).toInstant().toEpochMilli()
                )
            }
            .map { (week, weekRecords) ->
                val avg = weekRecords.map { it.value }.average().toFloat()
                ChartDataPoint("Sem. $week", avg, GlucoseLevel.classify(avg.toInt()))
            }
            .sortedBy { it.label }

    fun toIndividualPoints(records: List<GlucoseRecordUi>): List<ChartDataPoint> =
        records
            .sortedBy { it.timestamp }
            .map { record ->
                ChartDataPoint(
                    DateTimeUtils.formatDateTime(record.timestamp),
                    record.value.toFloat(),
                    record.level
                )
            }
}

@Singleton
class MedicationRepository @Inject constructor(
    private val supabaseApi: SupabaseApi
) {
    private val mutex = Mutex()
    private val _medications = MutableStateFlow<List<MedicationUi>>(emptyList())

    suspend fun refresh() {
        mutex.withLock {
            _medications.value = supabaseApi.fetchMedications()
        }
    }

    fun getAllMedications(): Flow<List<MedicationUi>> = _medications.asStateFlow()

    fun getActiveMedications(): Flow<List<MedicationUi>> =
        _medications.asStateFlow().map { meds -> meds.filter { it.isActive } }

    fun getRecommendableMedications(): Flow<List<MedicationUi>> =
        _medications.asStateFlow().map { meds ->
            meds.filter { it.isActive && it.recommendForHighGlucose }
        }

    suspend fun addMedication(
        name: String,
        dose: String,
        hour: Int,
        minute: Int,
        intervalHours: Int,
        recommendForHighGlucose: Boolean
    ): String {
        require(name.isNotBlank()) { "El nombre del medicamento es obligatorio" }
        require(dose.isNotBlank()) { "La dosis es obligatoria" }
        require(hour in 0..23 && minute in 0..59) { "Hora inválida" }
        val med = supabaseApi.insertMedication(
            MedicationInsertDto(
                deviceId = "",
                name = name.trim(),
                dose = dose.trim(),
                scheduledHour = hour,
                scheduledMinute = minute,
                intervalHours = intervalHours,
                recommendForHighGlucose = recommendForHighGlucose
            )
        )
        mutex.withLock {
            _medications.value = _medications.value + med
        }
        return med.id
    }

    suspend fun updateMedication(medication: MedicationUi) {
        supabaseApi.updateMedication(medication)
        mutex.withLock {
            _medications.value = _medications.value.map {
                if (it.id == medication.id) medication else it
            }
        }
    }

    suspend fun deleteMedication(id: String) {
        supabaseApi.deleteMedication(id)
        mutex.withLock {
            _medications.value = _medications.value.filter { it.id != id }
        }
    }

    suspend fun getMedicationById(id: String): MedicationUi? =
        supabaseApi.getMedicationById(id)
}

@Singleton
class RecommendationEngine @Inject constructor() {
    fun getRecommendation(
        latestGlucose: GlucoseRecordUi?,
        medications: List<MedicationUi>
    ): Recommendation? {
        if (latestGlucose == null) return null

        return when (latestGlucose.level) {
            GlucoseLevel.HIGH -> {
                val recommendedMeds = medications
                    .filter { it.isActive && it.recommendForHighGlucose }
                    .map { it.name }
                if (recommendedMeds.isEmpty()) {
                    Recommendation(
                        message = "Tu glucosa está elevada (${latestGlucose.value} mg/dL). " +
                            "Marca medicamentos como recomendados para glucosa alta."
                    )
                } else {
                    Recommendation(
                        message = "Tu glucosa está elevada (${latestGlucose.value} mg/dL). " +
                            "Medicamentos recomendados:",
                        medications = recommendedMeds
                    )
                }
            }
            GlucoseLevel.LOW -> Recommendation(
                message = "Tu glucosa está baja (${latestGlucose.value} mg/dL). " +
                    "Consume algo con carbohidratos y consulta a tu médico si persiste."
            )
            GlucoseLevel.NORMAL -> Recommendation(
                message = "Tu glucosa está en rango normal (${latestGlucose.value} mg/dL). " +
                    "¡Sigue con tus buenos hábitos!"
            )
        }
    }
}

@Singleton
class DataSyncManager @Inject constructor(
    private val glucoseRepository: GlucoseRepository,
    private val medicationRepository: MedicationRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var debounceJob: Job? = null

    suspend fun syncAll() {
        mutex.withLock {
            glucoseRepository.refresh()
            medicationRepository.refresh()
        }
    }

    fun syncAllDebounced(delayMs: Long = 350L) {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(delayMs)
            syncAll()
        }
    }
}
