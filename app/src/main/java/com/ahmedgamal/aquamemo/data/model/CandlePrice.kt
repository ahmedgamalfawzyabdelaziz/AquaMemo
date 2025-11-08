package com.ahmedgamal.aquamemo.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "candle_prices")
data class CandlePrice(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val candleNumber: Int, // 1-7
    val price: Double,
    val currency: String = "USD" // "USD", "EUR", "EGP", "SAR", etc.
) {
    companion object {
        // الأسعار الافتراضية
        fun getDefaultPrices(): List<CandlePrice> = listOf(
            CandlePrice(candleNumber = 1, price = 50.0),
            CandlePrice(candleNumber = 2, price = 75.0),
            CandlePrice(candleNumber = 3, price = 75.0),
            CandlePrice(candleNumber = 4, price = 120.0),
            CandlePrice(candleNumber = 5, price = 100.0),
            CandlePrice(candleNumber = 6, price = 120.0),
            CandlePrice(candleNumber = 7, price = 120.0)
        )
    }
}