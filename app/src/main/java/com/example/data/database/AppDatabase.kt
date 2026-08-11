package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.*

@Database(
    entities = [
        User::class,
        Customer::class,
        Product::class,
        SaleInvoice::class,
        SaleItem::class,
        Installment::class,
        Repair::class,
        GoldPriceHistory::class,
        AuditLog::class
    ],
    version = 9,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract val shopDao: ShopDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Safe migration placeholder for schema updates
            }
        }

        private val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // 1. users table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `users_new` (
                        `id` INTEGER NOT NULL PRIMARY KEY,
                        `pinHash` TEXT NOT NULL,
                        `fingerprintEnabled` INTEGER NOT NULL,
                        `dailyGoldPrice` TEXT NOT NULL,
                        `taxPercent` TEXT NOT NULL,
                        `minStockAlert` INTEGER NOT NULL,
                        `selectedPrinterName` TEXT,
                        `selectedPrinterAddress` TEXT
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `users_new` (`id`, `pinHash`, `fingerprintEnabled`, `dailyGoldPrice`, `taxPercent`, `minStockAlert`, `selectedPrinterName`, `selectedPrinterAddress`)
                    SELECT `id`, `pinHash`, `fingerprintEnabled`, CAST(`dailyGoldPrice` AS TEXT), CAST(`taxPercent` AS TEXT), `minStockAlert`, `selectedPrinterName`, `selectedPrinterAddress` FROM `users`
                """.trimIndent())
                db.execSQL("DROP TABLE `users`")
                db.execSQL("ALTER TABLE `users_new` RENAME TO `users`")

                // 2. products table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `products_new` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `name` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `weightGram` TEXT NOT NULL,
                        `karat` INTEGER NOT NULL,
                        `wagePrice` TEXT NOT NULL,
                        `wageType` TEXT NOT NULL,
                        `stock` INTEGER NOT NULL,
                        `minStock` INTEGER NOT NULL,
                        `imagePath` TEXT,
                        `imagePath2` TEXT,
                        `imagePath3` TEXT,
                        `imagePath4` TEXT,
                        `imagePath5` TEXT,
                        `purchasePrice` TEXT NOT NULL,
                        `customBarcode` TEXT NOT NULL,
                        `isDeleted` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `products_new` (`id`, `name`, `category`, `weightGram`, `karat`, `wagePrice`, `wageType`, `stock`, `minStock`, `imagePath`, `imagePath2`, `imagePath3`, `imagePath4`, `imagePath5`, `purchasePrice`, `customBarcode`, `isDeleted`, `createdAt`)
                    SELECT `id`, `name`, `category`, CAST(`weightGram` AS TEXT), `karat`, CAST(`wagePrice` AS TEXT), `wageType`, `stock`, `minStock`, `imagePath`, `imagePath2`, `imagePath3`, `imagePath4`, `imagePath5`, CAST(`purchasePrice` AS TEXT), `customBarcode`, `isDeleted`, `createdAt` FROM `products`
                """.trimIndent())
                db.execSQL("DROP TABLE `products`")
                db.execSQL("ALTER TABLE `products_new` RENAME TO `products`")

                // 3. sale_invoices table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sale_invoices_new` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `customerId` INTEGER NOT NULL,
                        `date` INTEGER NOT NULL,
                        `totalAmount` TEXT NOT NULL,
                        `discount` TEXT NOT NULL,
                        `tax` TEXT NOT NULL,
                        `paidAmount` TEXT NOT NULL,
                        `paymentType` TEXT NOT NULL,
                        `installmentsCount` INTEGER NOT NULL,
                        `prepayment` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `sale_invoices_new` (`id`, `customerId`, `date`, `totalAmount`, `discount`, `tax`, `paidAmount`, `paymentType`, `installmentsCount`, `prepayment`, `createdAt`)
                    SELECT `id`, `customerId`, `date`, CAST(`totalAmount` AS TEXT), CAST(`discount` AS TEXT), CAST(`tax` AS TEXT), CAST(`paidAmount` AS TEXT), `paymentType`, `installmentsCount`, CAST(`prepayment` AS TEXT), `createdAt` FROM `sale_invoices`
                """.trimIndent())
                db.execSQL("DROP TABLE `sale_invoices`")
                db.execSQL("ALTER TABLE `sale_invoices_new` RENAME TO `sale_invoices`")

                // 4. sale_items table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sale_items_new` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `invoiceId` INTEGER NOT NULL,
                        `productId` INTEGER NOT NULL,
                        `quantity` INTEGER NOT NULL,
                        `unitPrice` TEXT NOT NULL,
                        `total` TEXT NOT NULL,
                        `customWeight` TEXT,
                        `customName` TEXT
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `sale_items_new` (`id`, `invoiceId`, `productId`, `quantity`, `unitPrice`, `total`, `customWeight`, `customName`)
                    SELECT `id`, `invoiceId`, `productId`, `quantity`, CAST(`unitPrice` AS TEXT), CAST(`total` AS TEXT), CASE WHEN `customWeight` IS NULL THEN NULL ELSE CAST(`customWeight` AS TEXT) END, `customName` FROM `sale_items`
                """.trimIndent())
                db.execSQL("DROP TABLE `sale_items`")
                db.execSQL("ALTER TABLE `sale_items_new` RENAME TO `sale_items`")

                // 5. installments table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `installments_new` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `invoiceId` INTEGER NOT NULL,
                        `dueDate` INTEGER NOT NULL,
                        `amount` TEXT NOT NULL,
                        `paid` INTEGER NOT NULL,
                        `paymentDate` INTEGER
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `installments_new` (`id`, `invoiceId`, `dueDate`, `amount`, `paid`, `paymentDate`)
                    SELECT `id`, `invoiceId`, `dueDate`, CAST(`amount` AS TEXT), `paid`, `paymentDate` FROM `installments`
                """.trimIndent())
                db.execSQL("DROP TABLE `installments`")
                db.execSQL("ALTER TABLE `installments_new` RENAME TO `installments`")

                // 6. repairs table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `repairs_new` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `customerId` INTEGER NOT NULL,
                        `description` TEXT NOT NULL,
                        `imagePath` TEXT,
                        `estimatedCost` TEXT NOT NULL,
                        `upfrontPayment` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `deliveredAt` INTEGER
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `repairs_new` (`id`, `customerId`, `description`, `imagePath`, `estimatedCost`, `upfrontPayment`, `status`, `createdAt`, `deliveredAt`)
                    SELECT `id`, `customerId`, `description`, `imagePath`, CAST(`estimatedCost` AS TEXT), CAST(`upfrontPayment` AS TEXT), `status`, `createdAt`, `deliveredAt` FROM `repairs`
                """.trimIndent())
                db.execSQL("DROP TABLE `repairs`")
                db.execSQL("ALTER TABLE `repairs_new` RENAME TO `repairs`")

                // 7. gold_price_history table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `gold_price_history_new` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `date` INTEGER NOT NULL,
                        `pricePerGram` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `gold_price_history_new` (`id`, `date`, `pricePerGram`)
                    SELECT `id`, `date`, CAST(`pricePerGram` AS TEXT) FROM `gold_price_history`
                """.trimIndent())
                db.execSQL("DROP TABLE `gold_price_history`")
                db.execSQL("ALTER TABLE `gold_price_history_new` RENAME TO `gold_price_history`")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gildar_gold_shop.db"
                )
                .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
