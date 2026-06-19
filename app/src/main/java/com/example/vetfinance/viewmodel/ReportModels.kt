package com.example.vetfinance.viewmodel

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.example.vetfinance.R

enum class ReportPeriodType(@StringRes val displayResId: Int) {
    DAY(R.string.period_day),
    WEEK(R.string.period_week),
    MONTH(R.string.period_month)
}

@Immutable
data class HistoricalPeriod(
    val id: String,
    val displayName: String,
    val startDate: Long,
    val endDate: Long
)

enum class TopProductsPeriod(@StringRes val displayResId: Int) {
    WEEK(R.string.topproducts_period_week),
    MONTH(R.string.topproducts_period_month),
    YEAR(R.string.topproducts_period_year)
}

enum class TopProductsMetric {
    QUANTITY,
    REVENUE
}
