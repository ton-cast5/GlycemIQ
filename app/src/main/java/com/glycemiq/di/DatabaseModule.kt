package com.glycemiq.di

import android.content.Context
import androidx.room.Room
import com.glycemiq.data.local.GlycemIQDatabase
import com.glycemiq.data.local.dao.GlucoseDao
import com.glycemiq.data.local.dao.MedicationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GlycemIQDatabase =
        Room.databaseBuilder(
            context,
            GlycemIQDatabase::class.java,
            "glycemiq_database"
        ).build()

    @Provides
    fun provideGlucoseDao(database: GlycemIQDatabase): GlucoseDao = database.glucoseDao()

    @Provides
    fun provideMedicationDao(database: GlycemIQDatabase): MedicationDao = database.medicationDao()
}
