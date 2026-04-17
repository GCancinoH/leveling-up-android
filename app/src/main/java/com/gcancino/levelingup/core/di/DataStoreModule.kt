package com.gcancino.levelingup.core.di

import android.content.Context
import com.gcancino.levelingup.data.local.datastore.DataStoreManager
import com.gcancino.levelingup.data.local.datastore.DataStoreCryptoManager
import com.gcancino.levelingup.data.local.datastore.EncryptedDraftsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideDataStoreCryptoManager(): DataStoreCryptoManager = DataStoreCryptoManager()

    @Provides
    @Singleton
    fun provideDataStoreManager(
        @ApplicationContext context: Context,
        encryptedDraftsManager: EncryptedDraftsManager,
        dataStoreCryptoManager: DataStoreCryptoManager
    ): DataStoreManager = DataStoreManager(context, encryptedDraftsManager, dataStoreCryptoManager)

    @Provides
    @Singleton
    fun provideEncryptedDraftsManager(@ApplicationContext context: Context): EncryptedDraftsManager = EncryptedDraftsManager(context)
}