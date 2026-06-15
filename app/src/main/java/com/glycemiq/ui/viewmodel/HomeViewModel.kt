package com.glycemiq.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glycemiq.data.repository.DataSyncManager
import com.glycemiq.data.repository.GlucoseRepository
import com.glycemiq.data.repository.MedicationRepository
import com.glycemiq.data.repository.RecommendationEngine
import com.glycemiq.domain.model.GlucoseRecordUi
import com.glycemiq.domain.model.MedicationUi
import com.glycemiq.domain.model.Recommendation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val recentRecords: List<GlucoseRecordUi> = emptyList(),
    val medications: List<MedicationUi> = emptyList(),
    val recommendation: Recommendation? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    glucoseRepository: GlucoseRepository,
    medicationRepository: MedicationRepository,
    private val recommendationEngine: RecommendationEngine,
    dataSyncManager: DataSyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dataSyncManager.syncAll()
        }
        viewModelScope.launch {
            combine(
                glucoseRepository.getAllRecords(),
                medicationRepository.getRecommendableMedications()
            ) { records, medications ->
                val recommendation = recommendationEngine.getRecommendation(
                    records.firstOrNull(),
                    medications
                )
                HomeUiState(
                    recentRecords = records.take(5),
                    medications = medications,
                    recommendation = recommendation,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
