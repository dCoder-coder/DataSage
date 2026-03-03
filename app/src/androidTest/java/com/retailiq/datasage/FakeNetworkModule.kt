package com.retailiq.datasage

import com.retailiq.datasage.data.api.*
import com.retailiq.datasage.di.NetworkModule
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import org.mockito.Mockito.mock
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [NetworkModule::class]
)
object FakeNetworkModule {
    @Provides @Singleton fun authApi(): AuthApiService = mock(AuthApiService::class.java)
    @Provides @Singleton fun storeApi(): StoreApiService = mock(StoreApiService::class.java)
    @Provides @Singleton fun txApi(): TransactionApiService = mock(TransactionApiService::class.java)
    @Provides @Singleton fun inventoryApi(): InventoryApiService = mock(InventoryApiService::class.java)
    @Provides @Singleton fun customerApi(): CustomerApiService = mock(CustomerApiService::class.java)
    @Provides @Singleton fun analyticsApi(): AnalyticsApiService = mock(AnalyticsApiService::class.java)
    @Provides @Singleton fun forecastApi(): ForecastApiService = mock(ForecastApiService::class.java)
    @Provides @Singleton fun alertsApi(): AlertsApiService = mock(AlertsApiService::class.java)
    @Provides @Singleton fun recommendationsApi(): RecommendationsApiService = mock(RecommendationsApiService::class.java)
    @Provides @Singleton fun nlpQueryApi(): NlpQueryApiService = mock(NlpQueryApiService::class.java)
    @Provides @Singleton fun receiptsApi(): ReceiptsApiService = mock(ReceiptsApiService::class.java)
    @Provides @Singleton fun supplierApi(): SupplierApiService = mock(SupplierApiService::class.java)
    @Provides @Singleton fun staffApi(): StaffApiService = mock(StaffApiService::class.java)
    @Provides @Singleton fun offlineApi(): OfflineApiService = mock(OfflineApiService::class.java)
    @Provides @Singleton fun loyaltyApi(): LoyaltyApiService = mock(LoyaltyApiService::class.java)
    @Provides @Singleton fun creditApi(): CreditApiService = mock(CreditApiService::class.java)
    @Provides @Singleton fun gstApi(): GstApiService = mock(GstApiService::class.java)
    @Provides @Singleton fun whatsappApi(): WhatsAppApiService = mock(WhatsAppApiService::class.java)
    @Provides @Singleton fun chainApi(): ChainApiService = mock(ChainApiService::class.java)
    @Provides @Singleton fun pricingApi(): PricingApiService = mock(PricingApiService::class.java)
    @Provides @Singleton fun eventApi(): EventApiService = mock(EventApiService::class.java)
    @Provides @Singleton fun visionApi(): VisionApiService = mock(VisionApiService::class.java)
}
