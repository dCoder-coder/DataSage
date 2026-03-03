package com.retailiq.datasage.data.api

import com.retailiq.datasage.data.model.supplier.CreatePoRequest
import com.retailiq.datasage.data.model.supplier.CreateSupplierRequest
import com.retailiq.datasage.data.model.supplier.GoodsReceiptRequest
import com.retailiq.datasage.data.model.supplier.PurchaseOrderDto
import com.retailiq.datasage.data.model.supplier.SupplierDto
import com.retailiq.datasage.data.model.supplier.SupplierProfileDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SupplierApiService {
    
    // --- Suppliers ---

    @GET("api/v1/suppliers")
    suspend fun getSuppliers(): ApiResponse<List<SupplierDto>>

    @GET("api/v1/suppliers/{id}")
    suspend fun getSupplierProfile(@Path("id") id: String): ApiResponse<SupplierProfileDto>

    @POST("api/v1/suppliers")
    suspend fun createSupplier(@Body request: CreateSupplierRequest): ApiResponse<Map<String, String>>

    // --- Purchase Orders ---

    @GET("api/v1/purchase-orders")
    suspend fun getPurchaseOrders(
        @Query("supplier_id") supplierId: String? = null,
        @Query("status") status: String? = null
    ): ApiResponse<List<PurchaseOrderDto>>

    @GET("api/v1/purchase-orders/{id}")
    suspend fun getPurchaseOrder(@Path("id") id: String): ApiResponse<PurchaseOrderDto>

    @POST("api/v1/purchase-orders")
    suspend fun createPurchaseOrder(@Body request: CreatePoRequest): ApiResponse<Map<String, String>>
    
    @POST("api/v1/purchase-orders/{id}/send")
    suspend fun sendPurchaseOrder(@Path("id") id: String): ApiResponse<Map<String, String>>

    @POST("api/v1/purchase-orders/{id}/receive")
    suspend fun receiveGoods(
        @Path("id") id: String,
        @Body request: GoodsReceiptRequest
    ): ApiResponse<Map<String, String>>
}
