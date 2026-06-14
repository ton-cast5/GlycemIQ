package com.glycemiq.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.glycemiq.data.local.entity.GlucoseRecord
import com.glycemiq.data.local.entity.Medication
import kotlinx.coroutines.flow.Flow

@Dao
interface GlucoseDao {
    @Query("SELECT * FROM glucose_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<GlucoseRecord>>

    @Query("SELECT * FROM glucose_records WHERE timestamp >= :startTime ORDER BY timestamp ASC")
    fun getRecordsSince(startTime: Long): Flow<List<GlucoseRecord>>

    @Query("SELECT * FROM glucose_records ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentRecords(limit: Int): Flow<List<GlucoseRecord>>

    @Insert
    suspend fun insert(record: GlucoseRecord): Long

    @Delete
    suspend fun delete(record: GlucoseRecord)

    @Query("SELECT * FROM glucose_records WHERE id = :id")
    suspend fun getById(id: Long): GlucoseRecord?
}

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications ORDER BY scheduledHour, scheduledMinute")
    fun getAllMedications(): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE isActive = 1 ORDER BY name")
    fun getActiveMedications(): Flow<List<Medication>>

    @Insert
    suspend fun insert(medication: Medication): Long

    @Update
    suspend fun update(medication: Medication)

    @Delete
    suspend fun delete(medication: Medication)

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getById(id: Long): Medication?
}
