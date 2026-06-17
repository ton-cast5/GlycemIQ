package com.glycemiq.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glycemiq.data.repository.GlucoseRepository
import com.glycemiq.domain.model.ChartDataPoint
import com.glycemiq.domain.model.GlucoseRecordUi
import com.glycemiq.util.DateTimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

enum class ChartType(val label: String) {
    INDIVIDUAL("Por registro"),
    DAILY("Promedio diario"),
    WEEKLY("Promedio semanal")
}

data class ChartsUiState(
    val chartType: ChartType = ChartType.INDIVIDUAL,
    val dataPoints: List<ChartDataPoint> = emptyList(),
    val recordCount: Int = 0,
    val isLoading: Boolean = true,
    val isEmpty: Boolean = true
)

@HiltViewModel
class ChartsViewModel @Inject constructor(
    private val glucoseRepository: GlucoseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChartsUiState())
    val uiState: StateFlow<ChartsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            glucoseRepository.getAllRecords().collect { records ->
                val filtered = filterLastDays(records, 60)
                updateChartData(_uiState.value.chartType, filtered)
            }
        }
    }

    fun setChartType(type: ChartType) {
        val records = filterLastDays(glucoseRepository.getRecordsSnapshot(), 60)
        updateChartData(type, records)
    }

    private fun filterLastDays(records: List<GlucoseRecordUi>, days: Int): List<GlucoseRecordUi> {
        val startTime = Instant.now()
            .atZone(DateTimeUtils.MEXICO_ZONE)
            .minus(days.toLong(), ChronoUnit.DAYS)
            .toInstant()
            .toEpochMilli()
        return records
            .filter { it.timestamp >= startTime }
            .sortedBy { it.timestamp }
    }

    private fun updateChartData(type: ChartType, records: List<GlucoseRecordUi>) {
        val points = when (type) {
            ChartType.INDIVIDUAL -> glucoseRepository.toIndividualPoints(records)
            ChartType.DAILY -> glucoseRepository.calculateDailyAverages(records)
            ChartType.WEEKLY -> glucoseRepository.calculateWeeklyAverages(records)
        }
        _uiState.value = ChartsUiState(
            chartType = type,
            dataPoints = points,
            recordCount = records.size,
            isLoading = false,
            isEmpty = records.isEmpty()
        )
    }
}
