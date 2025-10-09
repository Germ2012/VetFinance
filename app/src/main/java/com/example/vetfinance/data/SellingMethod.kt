package com.example.vetfinance.data



enum class SellingMethod(val stringValue: String) {
    BY_UNIT(SELLING_METHOD_BY_UNIT),
    BY_WEIGHT_OR_AMOUNT(SELLING_METHOD_BY_WEIGHT_OR_AMOUNT),
    DOSE_ONLY(SELLING_METHOD_DOSE_ONLY);

    companion object {
        fun fromString(value: String): SellingMethod? = entries.find { it.stringValue == value }
    }
}