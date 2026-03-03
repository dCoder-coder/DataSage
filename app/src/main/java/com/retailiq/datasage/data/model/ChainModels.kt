package com.retailiq.datasage.data.model

data class ChainDashboardDto(
    val total_revenue_today: Double,
    val best_store: StoreKpiDto?,
    val worst_store: StoreKpiDto?,
    val total_open_alerts: Int,
    val store_revenues: List<StoreRevenueDto>,
    val transfer_suggestions: List<TransferSuggestionDto>
)

data class StoreKpiDto(
    val store_id: Int,
    val store_name: String,
    val revenue: Double,
    val gross_margin: Double,
    val transaction_count: Int,
    val alert_count: Int
)

data class StoreRevenueDto(
    val store_id: Int,
    val store_name: String,
    val revenue: Double
)

data class TransferSuggestionDto(
    val id: String,
    val from_store_id: Int,
    val from_store_name: String,
    val to_store_id: Int,
    val to_store_name: String,
    val product_id: Int,
    val product_name: String,
    val suggested_qty: Int,
    val reason: String,
    val status: String // PENDING, CONFIRMED
)

data class StoreCompareResponseDto(
    val period: String,
    val stores: List<String>,
    val rows: List<CompareRowDto>
)

data class CompareRowDto(
    val kpi: String,
    val values: List<CompareCellDto>
)

data class CompareCellDto(
    val store_id: Int,
    val value: Double,
    val relative_to_avg: String // "above", "near", "below"
)
