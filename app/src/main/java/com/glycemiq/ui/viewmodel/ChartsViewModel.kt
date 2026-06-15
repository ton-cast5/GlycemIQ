package com.glycemiq.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glycemiq.data.repository.DataSyncManager
import com.glycemiq.data.repository.GlucoseRepository
import com.glycemiq.domain.model.ChartDataPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ChartType(val label: String) {
    INDIVIDUAL("Por registro"),
    DAILY("Promedio diario"),
    WEEKLY("Promedio semanal")
}

data class ChartsUiState(
    val chartType: ChartType = ChartType.INDIVIDUAL,
    val dataPoints: List<ChartDataPoint> = emptyList(),
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false
)

@HiltViewModel
class ChartsViewModel @Inject constructor(
    private val glucoseRepository: GlucoseRepository,
    dataSyncManager: DataSyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChartsUiState())
    val uiState: StateFlow<ChartsUiState> = _uiState.asStateFlow()

    private var cachedRecords = emptyList<com.glycemiq.domain.model.GlucoseRecordUi>()

    init {
        viewModelScope.launch {
            dataSyncManager.syncAll()
            loadData()
        }
    }

    private suspend fun loadData() {
        cachedRecords = glucoseRepository.getRecordsForCharts(60)
        updateChartData(_uiState.value.chartType)
    }

    fun setChartType(type: ChartType) {
        _uiState.value = _uiState.value.copy(chartType = type)
        updateChartData(type)
    }

    private fun updateChartData(type: ChartType) {
        val points = when (type) {
            ChartType.INDIVIDUAL -> glucoseRepository.toIndividualPoints(cachedRecords)
            ChartType.DAILY -> glucoseRepository.calculateDailyAverages(cachedRecords)
            ChartType.WEEKLY -> glucoseRepository.calculateWeeklyAverages(cachedRecords)
        }
        _uiState.value = ChartsUiState(
            chartType = type,
            dataPoints = points,
            isLoading = false,
            isEmpty = points.isEmpty()
        )
    }
}
