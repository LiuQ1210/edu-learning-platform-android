package com.github.learningplatform.core.di

import android.content.Context
import androidx.room.Room
import com.github.learningplatform.data.local.AppDatabase
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "learn_platform.db"
        )
            // Room 2.7+ 需要显式指定是否丢弃全部表；开发期先允许
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    @Singleton
    fun provideCacheDao(db: AppDatabase) = db.cacheDao()
}