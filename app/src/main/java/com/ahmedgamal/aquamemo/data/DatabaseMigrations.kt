// DatabaseMigrations.kt
package com.ahmedgamal.aquamemo.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ahmedgamal.aquamemo.data.model.CandlePrice

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // إنشاء جدول سجل التغييرات الجديد
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS filter_change_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                filterType TEXT NOT NULL,
                candleNumber INTEGER NOT NULL,
                changeDate INTEGER NOT NULL,
                notes TEXT NOT NULL DEFAULT ''
            )
        """)
    }
}
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // إنشاء جدول الأسعار الجديد
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS candle_prices (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                candleNumber INTEGER NOT NULL,
                price REAL NOT NULL,
                currency TEXT NOT NULL DEFAULT 'USD'
            )
        """)
        val defaultPrices = CandlePrice.getDefaultPrices()
        defaultPrices.forEach { price ->
            db.execSQL("""
                INSERT INTO candle_prices (candleNumber, price, currency) 
                VALUES (${price.candleNumber}, ${price.price}, '${price.currency}')
            """)
        }
    }
}
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // إنشاء جدول سجل الإشعارات الجديد
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `notification_history` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `type` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `message` TEXT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                `isRead` INTEGER NOT NULL DEFAULT 0,
                `iconType` TEXT NOT NULL
            )
            """.trimIndent()
        )
    }
}
// ✅ الترحيل من الإصدار 5 إلى 6 (إضافة جدول TDS)
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // إنشاء جدول tds_readings الجديد
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `tds_readings` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `value` INTEGER NOT NULL, 
                `date` INTEGER NOT NULL, 
                `notes` TEXT NOT NULL
            )
            """.trimIndent()
        )
    }
}