package com.glycemiq.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glycemiq.data.local.entity.Medication
import com.glycemiq.data.repository.MedicationRepository
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
    val hour: String = "08",
    val minute: String = "00",
    val editingId: Long? = null,
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
    private val alarmScheduler: MedicationAlarmScheduler
) : ViewModel() {

    private val _formState = MutableStateFlow(MedicationFormState())
    val formState: StateFlow<MedicationFormState> = _formState.asStateFlow()

    private val _listState = MutableStateFlow(MedicationListState())
    val listState: StateFlow<MedicationListState> = _listState.asStateFlow()

    init {
        loadMedications()
    }

    private fun loadMedications() {
        viewModelScope.launch {
            medicationRepository.getAllMedications()
                .catch { e ->
                    _listState.value = MedicationListState(
                        isLoading = false,
                        errorMessage = e.message
                    )
                }
                .collect { medications ->
                    _listState.value = MedicationListState(
                        medications = medications,
                        isLoading = false
                    )
                }
        }
    }

    fun updateName(name: String) {
        _formState.value = _formState.value.copy(name = name, errorMessage = null)
    }

    fun updateDose(dose: String) {
        _formState.value = _formState.value.copy(dose = dose, errorMessage = null)
    }

    fun updateHour(hour: String) {
        val filtered = hour.filter { it.isDigit() }.take(2)
        _formState.value = _formState.value.copy(hour = filtered)
    }

    fun updateMinute(minute: String) {
        val filtered = minute.filter { it.isDigit() }.take(2)
        _formState.value = _formState.value.copy(minute = filtered)
    }

    fun startEditing(medication: MedicationUi) {
        _formState.value = MedicationFormState(
            name = medication.name,
            dose = medication.dose,
            hour = String.format("%02d", medication.scheduledHour),
            minute = String.format("%02d", medication.scheduledMinute),
            editingId = medication.id
        )
    }

    fun resetForm() {
        _formState.value = MedicationFormState()
    }

    fun saveMedication() {
        val current = _formState.value
        val hour = current.hour.toIntOrNull()
        val minute = current.minute.toIntOrNull()

        if (current.name.isBlank()) {
            _formState.value = current.copy(errorMessage = "El nombre es obligatorio")
            return
        }
        if (current.dose.isBlank()) {
            _formState.value = current.copy(errorMessage = "La dosis es obligatoria")
            return
        }
        if (hour == null || hour !in 0..23) {
            _formState.value = current.copy(errorMessage = "Hora inválida (0-23)")
            return
        }
        if (minute == null || minute !in 0..59) {
            _formState.value = current.copy(errorMessage = "Minutos inválidos (0-59)")
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
                            scheduledHour = hour,
                            scheduledMinute = minute
                        )
                        medicationRepository.updateMedication(updated)
                        alarmScheduler.cancelAlarm(updated.id)
                        scheduleAlarm(updated)
                    }
                } else {
                    val id = medicationRepository.addMedication(
                        current.name.trim(),
                        current.dose.trim(),
                        hour,
                        minute
                    )
                    scheduleAlarm(
                        MedicationUi(
                            id = id,
                            name = current.name.trim(),
                            dose = current.dose.trim(),
                            scheduledHour = hour,
                            scheduledMinute = minute
                        )
                    )
                }
                _formState.value = MedicationFormState(
                    successMessage = if (current.editingId != null) {
                        "Medicamento actualizado"
                    } else {
                        "Medicamento registrado"
                    }
                )
            } catch (e: Exception) {
                _formState.value = current.copy(
                    isSaving = false,
                    errorMessage = e.message ?: "Error al guardar"
                )
            }
        }
    }

    fun deleteMedication(id: Long) {
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
                scheduleAlarm(updated)
            } else {
                alarmScheduler.cancelAlarm(updated.id)
            }
        }
    }

    private fun scheduleAlarm(medication: MedicationUi) {
        alarmScheduler.scheduleAlarm(
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

    fun clearMessages() {
        _formState.value = _formState.value.copy(errorMessage = null, successMessage = null)
    }
}
