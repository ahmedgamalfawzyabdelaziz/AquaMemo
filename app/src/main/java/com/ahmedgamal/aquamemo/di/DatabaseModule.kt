package com.ahmedgamal.aquamemo.di

import android.content.Context
import androidx.room.Room
import com.ahmedgamal.aquamemo.data.AquaMemoDatabase
import com.ahmedgamal.aquamemo.data.MIGRATION_2_3
import com.ahmedgamal.aquamemo.data.MIGRATION_3_4
import com.ahmedgamal.aquamemo.data.MIGRATION_4_5
import com.ahmedgamal.aquamemo.data.dao.FilterDao
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
    fun provideDatabase(@ApplicationContext context: Context): AquaMemoDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AquaMemoDatabase::class.java,
            "aqua_memo_database"
        ).addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
        .build()
    }

    @Provides
    @Singleton
    fun provideFilterDao(db: AquaMemoDatabase): FilterDao {
        return db.filterDao()
    }
}