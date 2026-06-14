package com.glycemiq.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.glycemiq.data.local.dao.GlucoseDao
import com.glycemiq.data.local.dao.MedicationDao
import com.glycemiq.data.local.entity.GlucoseRecord
import com.glycemiq.data.local.entity.Medication

@Database(
    entities = [GlucoseRecord::class, Medication::class],
    version = 1,
    exportSchema = false
)
abstract class GlycemIQDatabase : RoomDatabase() {
    abstract fun glucoseDao(): GlucoseDao
    abstract fun medicationDao(): MedicationDao
}
