package com.glycemiq.data.repository

import com.glycemiq.data.local.dao.GlucoseDao
import com.glycemiq.data.local.dao.MedicationDao
import com.glycemiq.data.local.entity.GlucoseRecord
import com.glycemiq.data.local.entity.Medication
import com.glycemiq.domain.model.ChartDataPoint
import com.glycemiq.domain.model.GlucoseContext
import com.glycemiq.domain.model.GlucoseLevel
import com.glycemiq.domain.model.GlucoseRecordUi
import com.glycemiq.domain.model.MedicationUi
import com.glycemiq.domain.model.Recommendation
import com.glycemiq.util.DateTimeUtils
import com.glycemiq.util.toUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlucoseRepository @Inject constructor(
    private val glucoseDao: GlucoseDao
) {
    fun getAllRecords(): Flow<List<GlucoseRecordUi>> =
        glucoseDao.getAllRecords().map { records -> records.map { it.toUi() } }

    fun getRecentRecords(limit: Int = 5): Flow<List<GlucoseRecordUi>> =
        glucoseDao.getRecentRecords(limit).map { records -> records.map { it.toUi() } }

    suspend fun addRecord(value: Int, context: GlucoseContext, timestamp: Long = DateTimeUtils.nowMillis()): Long {
        require(value in 20..600) { "El nivel de glucosa debe estar entre 20 y 600 mg/dL" }
        return glucoseDao.insert(
            GlucoseRecord(
                value = value,
                context = context.name,
                timestamp = timestamp
            )
        )
    }

    suspend fun deleteRecord(id: Long) {
        glucoseDao.getById(id)?.let { glucoseDao.delete(it) }
    }

    fun getRecordsForCharts(days: Int = 30): Flow<List<GlucoseRecordUi>> {
        val startTime = Instant.now()
            .atZone(DateTimeUtils.MEXICO_ZONE)
            .minus(days.toLong(), ChronoUnit.DAYS)
            .toInstant()
            .toEpochMilli()
        return glucoseDao.getRecordsSince(startTime).map { records -> records.map { it.toUi() } }
    }

    fun calculateDailyAverages(records: List<GlucoseRecordUi>): List<ChartDataPoint> {
        return records
            .groupBy { DateTimeUtils.formatDate(it.timestamp) }
            .map { (date, dayRecords) ->
                val avg = dayRecords.map { it.value }.average().toFloat()
                ChartDataPoint(
                    label = date,
                    value = avg,
                    level = GlucoseLevel.classify(avg.toInt())
                )
            }
            .sortedBy { it.label }
    }

    fun calculateWeeklyAverages(records: List<GlucoseRecordUi>): List<ChartDataPoint> {
        return records
            .groupBy { record ->
                val zoned = Instant.ofEpochMilli(record.timestamp).atZone(DateTimeUtils.MEXICO_ZONE)
                val weekStart = zoned.toLocalDate().with(java.time.DayOfWeek.MONDAY)
                DateTimeUtils.formatDate(weekStart.atStartOfDay(DateTimeUtils.MEXICO_ZONE).toInstant().toEpochMilli())
            }
            .map { (week, weekRecords) ->
                val avg = weekRecords.map { it.value }.average().toFloat()
                ChartDataPoint(
                    label = "Sem. $week",
                    value = avg,
                    level = GlucoseLevel.classify(avg.toInt())
                )
            }
            .sortedBy { it.label }
    }

    fun toIndividualPoints(records: List<GlucoseRecordUi>): List<ChartDataPoint> =
        records.map { record ->
            ChartDataPoint(
                label = DateTimeUtils.formatDateTime(record.timestamp),
                value = record.value.toFloat(),
                level = record.level
            )
        }
}

@Singleton
class MedicationRepository @Inject constructor(
    private val medicationDao: MedicationDao
) {
    fun getAllMedications(): Flow<List<MedicationUi>> =
        medicationDao.getAllMedications().map { meds -> meds.map { it.toUi() } }

    fun getActiveMedications(): Flow<List<MedicationUi>> =
        medicationDao.getActiveMedications().map { meds -> meds.map { it.toUi() } }

    suspend fun addMedication(name: String, dose: String, hour: Int, minute: Int): Long {
        require(name.isNotBlank()) { "El nombre del medicamento es obligatorio" }
        require(dose.isNotBlank()) { "La dosis es obligatoria" }
        require(hour in 0..23 && minute in 0..59) { "Hora inválida" }
        return medicationDao.insert(
            Medication(
                name = name.trim(),
                dose = dose.trim(),
                scheduledHour = hour,
                scheduledMinute = minute
            )
        )
    }

    suspend fun updateMedication(medication: MedicationUi) {
        medicationDao.update(
            Medication(
                id = medication.id,
                name = medication.name,
                dose = medication.dose,
                scheduledHour = medication.scheduledHour,
                scheduledMinute = medication.scheduledMinute,
                isActive = medication.isActive
            )
        )
    }

    suspend fun deleteMedication(id: Long) {
        medicationDao.getById(id)?.let { medicationDao.delete(it) }
    }

    suspend fun getMedicationById(id: Long): MedicationUi? =
        medicationDao.getById(id)?.toUi()
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
                val activeMeds = medications.filter { it.isActive }.map { it.name }
                if (activeMeds.isEmpty()) {
                    Recommendation(
                        message = "Tu glucosa está elevada (${latestGlucose.value} mg/dL). " +
                            "Consulta a tu médico y considera registrar tus medicamentos en la app."
                    )
                } else {
                    Recommendation(
                        message = "Tu glucosa está elevada (${latestGlucose.value} mg/dL). " +
                            "Según tus registros, podrías considerar los siguientes medicamentos:",
                        medications = activeMeds
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
