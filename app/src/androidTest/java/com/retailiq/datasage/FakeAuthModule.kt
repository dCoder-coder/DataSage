package com.retailiq.datasage

import com.retailiq.datasage.core.TokenStore
import com.retailiq.datasage.di.AuthModule
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import org.mockito.Mockito.mock
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AuthModule::class]
)
object FakeAuthModule {
    @Provides
    @Singleton
    fun provideFakeTokenStore(): TokenStore = mock(TokenStore::class.java)
}
