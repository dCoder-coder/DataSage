package com.retailiq.datasage.di

import com.retailiq.datasage.BuildConfig
import com.retailiq.datasage.core.AuthEvent
import com.retailiq.datasage.core.AuthEventBus
import com.retailiq.datasage.core.TokenStore
import com.retailiq.datasage.data.api.AlertsApiService
import com.retailiq.datasage.data.api.AnalyticsApiService
import com.retailiq.datasage.data.api.AuthApiService
import com.retailiq.datasage.data.api.CustomerApiService
import com.retailiq.datasage.data.api.ForecastApiService
import com.retailiq.datasage.data.api.InventoryApiService
import com.retailiq.datasage.data.api.RefreshRequest
import com.retailiq.datasage.data.api.ReportsApiService
import com.retailiq.datasage.data.api.StoreApiService
import com.retailiq.datasage.data.api.TransactionApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttp(tokenStore: TokenStore, authEventBus: AuthEventBus): OkHttpClient {
        val authInterceptor = Interceptor { chain ->
            val token = tokenStore.getAccessToken()
            val request = chain.request().newBuilder().apply {
                if (!token.isNullOrBlank()) addHeader("Authorization", "Bearer $token")
            }.build()
            chain.proceed(request)
        }

        val refreshInterceptor = object : Interceptor {
            override fun intercept(chain: Interceptor.Chain): Response {
                val request = chain.request()
                val response = chain.proceed(request)
                if (response.code != 401 || request.url.encodedPath.contains("/auth/refresh")) return response
                response.close()
                val refresh = tokenStore.getRefreshToken() ?: return chain.proceed(request)
                return try {
                    val retrofit = Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL).addConverterFactory(GsonConverterFactory.create()).build()
                    val authApi = retrofit.create(AuthApiService::class.java)
                    val tokenRes = runBlocking { authApi.refresh(RefreshRequest(refresh)).data }
                    if (tokenRes != null) {
                        tokenStore.saveTokens(tokenRes.accessToken, tokenRes.refreshToken)
                        chain.proceed(request.newBuilder().header("Authorization", "Bearer ${tokenRes.accessToken}").build())
                    } else {
                        authEventBus.emit(AuthEvent.SessionExpired)
                        chain.proceed(request)
                    }
                } catch (_: Exception) {
                    tokenStore.clearTokens()
                    authEventBus.emit(AuthEvent.SessionExpired)
                    chain.proceed(request)
                }
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(refreshInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()
    }

    @Provides @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides @Singleton fun authApi(retrofit: Retrofit): AuthApiService = retrofit.create(AuthApiService::class.java)
    @Provides @Singleton fun storeApi(retrofit: Retrofit): StoreApiService = retrofit.create(StoreApiService::class.java)
    @Provides @Singleton fun txApi(retrofit: Retrofit): TransactionApiService = retrofit.create(TransactionApiService::class.java)
    @Provides @Singleton fun inventoryApi(retrofit: Retrofit): InventoryApiService = retrofit.create(InventoryApiService::class.java)
    @Provides @Singleton fun customerApi(retrofit: Retrofit): CustomerApiService = retrofit.create(CustomerApiService::class.java)
    @Provides @Singleton fun analyticsApi(retrofit: Retrofit): AnalyticsApiService = retrofit.create(AnalyticsApiService::class.java)
    @Provides @Singleton fun forecastApi(retrofit: Retrofit): ForecastApiService = retrofit.create(ForecastApiService::class.java)
    @Provides @Singleton fun alertsApi(retrofit: Retrofit): AlertsApiService = retrofit.create(AlertsApiService::class.java)
    @Provides @Singleton fun reportsApi(retrofit: Retrofit): ReportsApiService = retrofit.create(ReportsApiService::class.java)
}
