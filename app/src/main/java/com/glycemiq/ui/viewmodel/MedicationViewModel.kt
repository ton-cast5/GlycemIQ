package com.glycemiq.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glycemiq.data.repository.DataSyncManager
import com.glycemiq.data.repository.MedicationRepository
import com.glycemiq.domain.model.MedicationInterval
import com.glycemiq.domain.model.MedicationUi
import com.glycemiq.notification.MedicationAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MedicationFormState(
    val name: String = "",
    val dose: String = "",
    val hour: Int = 8,
    val minute: Int = 0,
    val interval: MedicationInterval = MedicationInterval.EVERY_24_HOURS,
    val recommendForHighGlucose: Boolean = false,
    val editingId: String? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

data class MedicationListState(
    val medications: List<MedicationUi> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class MedicationViewModel @Inject constructor(
    private val medicationRepository: MedicationRepository,
    private val alarmScheduler: MedicationAlarmScheduler,
    dataSyncManager: DataSyncManager
) : ViewModel() {

    private val _formState = MutableStateFlow(MedicationFormState())
    val formState: StateFlow<MedicationFormState> = _formState.asStateFlow()

    private val _listState = MutableStateFlow(MedicationListState())
    val listState: StateFlow<MedicationListState> = _listState.asStateFlow()

    init {
        viewModelScope.launch { dataSyncManager.syncAll() }
        loadMedications()
    }

    private fun loadMedications() {
        viewModelScope.launch {
            medicationRepository.getAllMedications()
                .catch { e ->
                    _listState.value = MedicationListState(isLoading = false, errorMessage = e.message)
                }
                .collect { medications ->
                    _listState.value = MedicationListState(medications = medications, isLoading = false)
                }
        }
    }

    fun updateName(name: String) {
        _formState.value = _formState.value.copy(name = name, errorMessage = null)
    }

    fun updateDose(dose: String) {
        _formState.value = _formState.value.copy(dose = dose, errorMessage = null)
    }

    fun updateTime(hour: Int, minute: Int) {
        _formState.value = _formState.value.copy(hour = hour, minute = minute)
    }

    fun updateInterval(interval: MedicationInterval) {
        _formState.value = _formState.value.copy(interval = interval)
    }

    fun updateRecommendForHigh(checked: Boolean) {
        _formState.value = _formState.value.copy(recommendForHighGlucose = checked)
    }

    fun startEditing(medication: MedicationUi) {
        _formState.value = MedicationFormState(
            name = medication.name,
            dose = medication.dose,
            hour = medication.scheduledHour,
            minute = medication.scheduledMinute,
            interval = MedicationInterval.fromHours(medication.intervalHours),
            recommendForHighGlucose = medication.recommendForHighGlucose,
            editingId = medication.id
        )
    }

    fun resetForm() {
        _formState.value = MedicationFormState()
    }

    fun saveMedication() {
        val current = _formState.value

        if (current.name.isBlank()) {
            _formState.value = current.copy(errorMessage = "El nombre es obligatorio")
            return
        }
        if (current.dose.isBlank()) {
            _formState.value = current.copy(errorMessage = "La dosis es obligatoria")
            return
        }

        viewModelScope.launch {
            _formState.value = current.copy(isSaving = true, errorMessage = null)
            try {
                if (current.editingId != null) {
                    val existing = medicationRepository.getMedicationById(current.editingId)
                    if (existing != null) {
                        val updated = existing.copy(
                            name = current.name.trim(),
                            dose = current.dose.trim(),
                            scheduledHour = current.hour,
                            scheduledMinute = current.minute,
                            intervalHours = current.interval.hours,
                            recommendForHighGlucose = current.recommendForHighGlucose
                        )
                        medicationRepository.updateMedication(updated)
                        alarmScheduler.cancelAlarm(updated.id)
                        if (updated.isActive) alarmScheduler.scheduleAlarm(updated)
                    }
                } else {
                    val id = medicationRepository.addMedication(
                        name = current.name.trim(),
                        dose = current.dose.trim(),
                        hour = current.hour,
                        minute = current.minute,
                        intervalHours = current.interval.hours,
                        recommendForHighGlucose = current.recommendForHighGlucose
                    )
                    val med = medicationRepository.getMedicationById(id)
                    if (med != null) alarmScheduler.scheduleAlarm(med)
                }
                _formState.value = MedicationFormState(
                    successMessage = if (current.editingId != null) "Actualizado" else "Registrado"
                )
            } catch (e: Exception) {
                _formState.value = current.copy(
                    isSaving = false,
                    errorMessage = e.message ?: "Error al guardar"
                )
            }
        }
    }

    fun deleteMedication(id: String) {
        viewModelScope.launch {
            alarmScheduler.cancelAlarm(id)
            medicationRepository.deleteMedication(id)
        }
    }

    fun toggleActive(medication: MedicationUi) {
        viewModelScope.launch {
            val updated = medication.copy(isActive = !medication.isActive)
            medicationRepository.updateMedication(updated)
            if (updated.isActive) {
                alarmScheduler.scheduleAlarm(updated)
            } else {
                alarmScheduler.cancelAlarm(updated.id)
            }
        }
    }

    fun clearMessages() {
        _formState.value = _formState.value.copy(errorMessage = null, successMessage = null)
    }
}
