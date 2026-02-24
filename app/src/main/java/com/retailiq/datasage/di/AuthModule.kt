package com.retailiq.datasage.di

import com.retailiq.datasage.core.AuthManager
import com.retailiq.datasage.core.TokenStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds
    @Singleton
    abstract fun bindTokenStore(authManager: AuthManager): TokenStore
}
