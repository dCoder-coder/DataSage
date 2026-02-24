package com.retailiq.datasage.di

import android.content.Context
import androidx.room.Room
import com.retailiq.datasage.data.local.AppDatabase
import com.retailiq.datasage.data.local.PendingTransactionDao
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
    fun provideDb(@ApplicationContext context: Context): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "datasage.db"
    ).fallbackToDestructiveMigration().build()

    @Provides
    fun providePendingTxDao(db: AppDatabase): PendingTransactionDao = db.pendingTransactionDao()
}
