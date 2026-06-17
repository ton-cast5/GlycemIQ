package com.glycemiq.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glycemiq.data.repository.GlucoseRepository
import com.glycemiq.pdf.PdfReportGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportUiState(
    val isGenerating: Boolean = false,
    val recordCount: Int = 0,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val glucoseRepository: GlucoseRepository,
    private val pdfReportGenerator: PdfReportGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            glucoseRepository.getAllRecords().collect { records ->
                _uiState.value = _uiState.value.copy(recordCount = records.size)
            }
        }
    }

    fun generateReport(onFileReady: (java.io.File) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGenerating = true,
                errorMessage = null,
                successMessage = null
            )
            try {
                glucoseRepository.refresh()
                val records = glucoseRepository.getRecordsSnapshot()
                if (records.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        errorMessage = "No hay registros para generar el reporte"
                    )
                    return@launch
                }
                val file = pdfReportGenerator.generateReport(records)
                onFileReady(file)
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    successMessage = "Reporte generado correctamente"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    errorMessage = e.message ?: "Error al generar el reporte"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
