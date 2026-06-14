package com.glycemiq.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "glucose_records")
data class GlucoseRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val value: Int,
    val context: String,
    val timestamp: Long
)

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dose: String,
    val scheduledHour: Int,
    val scheduledMinute: Int,
    val isActive: Boolean = true
)
