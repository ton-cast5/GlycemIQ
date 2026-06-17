package com.glycemiq.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glycemiq.data.repository.GlucoseRepository
import com.glycemiq.domain.model.GlucoseContext
import com.glycemiq.domain.model.GlucoseLevel
import com.glycemiq.domain.model.GlucoseRecordUi
import com.glycemiq.util.DateTimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GlucoseFormState(
    val value: String = "",
    val context: GlucoseContext = GlucoseContext.FASTING,
    val timestamp: Long = DateTimeUtils.nowMillis(),
    val previewLevel: GlucoseLevel? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

data class GlucoseListState(
    val records: List<GlucoseRecordUi> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class GlucoseViewModel @Inject constructor(
    private val glucoseRepository: GlucoseRepository
) : ViewModel() {

    private val _formState = MutableStateFlow(GlucoseFormState())
    val formState: StateFlow<GlucoseFormState> = _formState.asStateFlow()

    private val _listState = MutableStateFlow(GlucoseListState())
    val listState: StateFlow<GlucoseListState> = _listState.asStateFlow()

    init {
        loadRecords()
    }

    private fun loadRecords() {
        viewModelScope.launch {
            glucoseRepository.getAllRecords()
                .catch { e ->
                    _listState.value = GlucoseListState(isLoading = false, errorMessage = e.message)
                }
                .collect { records ->
                    _listState.value = GlucoseListState(records = records, isLoading = false)
                }
        }
    }

    fun updateValue(value: String) {
        val numericValue = value.filter { it.isDigit() }.take(3)
        val level = numericValue.toIntOrNull()?.let { GlucoseLevel.classify(it) }
        _formState.value = _formState.value.copy(
            value = numericValue,
            previewLevel = level,
            errorMessage = null,
            successMessage = null
        )
    }

    fun updateContext(context: GlucoseContext) {
        _formState.value = _formState.value.copy(context = context)
    }

    fun saveRecord() {
        val current = _formState.value
        val value = current.value.toIntOrNull()

        if (value == null) {
            _formState.value = current.copy(errorMessage = "Ingresa un nivel de glucosa válido")
            return
        }
        if (value !in 20..600) {
            _formState.value = current.copy(errorMessage = "El valor debe estar entre 20 y 600 mg/dL")
            return
        }

        viewModelScope.launch {
            _formState.value = current.copy(isSaving = true, errorMessage = null)
            try {
                glucoseRepository.addRecord(value, current.context, current.timestamp)
                _formState.value = GlucoseFormState(successMessage = "Registro guardado")
            } catch (e: Exception) {
                _formState.value = current.copy(
                    isSaving = false,
                    errorMessage = e.message ?: "Error al guardar"
                )
            }
        }
    }

    fun deleteRecord(id: String) {
        viewModelScope.launch {
            try {
                glucoseRepository.deleteRecord(id)
            } catch (e: Exception) {
                _listState.value = _listState.value.copy(errorMessage = e.message)
            }
        }
    }

    fun clearMessages() {
        _formState.value = _formState.value.copy(errorMessage = null, successMessage = null)
    }
}
