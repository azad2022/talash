package com.example.data.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DatabaseMigrationTest {

    @Test
    fun testMigrationFromV8ToV9PreservesFinancialValues() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migration_v8_v9.db"

        context.deleteDatabase(dbName)

        val helperConfig = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(8) {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    createV8Schema(db)
                }

                override fun onUpgrade(
                    db: androidx.sqlite.db.SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int
                ) {}
            })
            .build()

        val sqliteHelper = FrameworkSQLiteOpenHelperFactory().create(helperConfig)
        val v8Db = sqliteHelper.writableDatabase

        insertV8Data(v8Db)
        v8Db.close()

        val roomDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10, AppDatabase.MIGRATION_10_11)
            .allowMainThreadQueries()
            .build()

        val shopDao = roomDb.shopDao

        val user = shopDao.getUserSync()
        assertNotNull(user)
        assertEquals(0, BigDecimal("3500000.0").compareTo(user!!.dailyGoldPrice))
        assertEquals(0, BigDecimal("9.0").compareTo(user.taxPercent))

        val products = shopDao.getAllProducts().first()
        assertEquals(1, products.size)
        val prod = products[0]
        assertEquals(0, BigDecimal("5.25").compareTo(prod.weightGram))
        assertEquals(0, BigDecimal("150000.0").compareTo(prod.wagePrice))
        assertEquals(0, BigDecimal("18000000.0").compareTo(prod.purchasePrice))

        val invoices = shopDao.getInvoicesWithDetails().first()
        assertEquals(1, invoices.size)
        val inv = invoices[0].invoice
        assertEquals(0, BigDecimal("25000000.0").compareTo(inv.totalAmount))
        assertEquals(0, BigDecimal("500000.0").compareTo(inv.discount))
        assertEquals(0, BigDecimal("2205000.0").compareTo(inv.tax))

        val item = invoices[0].items[0]
        assertEquals(0, BigDecimal("23300000.0").compareTo(item.unitPrice))
        assertEquals(0, BigDecimal("23300000.0").compareTo(item.total))
        assertNotNull(item.customWeight)
        assertEquals(0, BigDecimal("5.25").compareTo(item.customWeight!!))

        val inst = invoices[0].installments[0]
        assertEquals(0, BigDecimal("5000000.0").compareTo(inst.amount))

        val repairs = shopDao.getAllRepairs().first()
        assertEquals(1, repairs.size)
        val rep = repairs[0].repair
        assertEquals(0, BigDecimal("1200000.0").compareTo(rep.estimatedCost))
        assertEquals(0, BigDecimal("200000.0").compareTo(rep.upfrontPayment))

        roomDb.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun testMigrationFromV10ToV11PreservesStockTakeAndClosingData() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migration_v10_v11.db"

        context.deleteDatabase(dbName)

        val helperConfig = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(10) {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    createV10Schema(db)
                }

                override fun onUpgrade(
                    db: androidx.sqlite.db.SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int
                ) {}
            })
            .build()

        val sqliteHelper = FrameworkSQLiteOpenHelperFactory().create(helperConfig)
        val v10Db = sqliteHelper.writableDatabase

        insertV10Data(v10Db)
        v10Db.close()

        val roomDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_10_11)
            .allowMainThreadQueries()
            .build()

        try {
            val shopDao = roomDb.shopDao

            // Check stock take items migration
            val items = shopDao.getStockTakeItemsForSessionSync(1)
            assertEquals(2, items.size)

            val item1 = items.first { it.id == 1L }
            assertEquals(3, item1.countedStock)
            assertEquals(true, item1.isCounted) // backfilled to true because countedStock > 0

            val item2 = items.first { it.id == 2L }
            assertEquals(0, item2.countedStock)
            assertEquals(false, item2.isCounted) // default 0

            // Check daily closing migration
            val closing = shopDao.getLatestDailyClosingForDateSync("2026-09-21")
            assertNotNull(closing)
            assertEquals("CLOSED", closing!!.status)
            assertEquals(null, closing.reopenReason)
            assertEquals(null, closing.reopenedAt)
        } finally {
            roomDb.close()
            context.deleteDatabase(dbName)
        }
    }

    private fun createV10Schema(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `users` (`id` INTEGER NOT NULL PRIMARY KEY, `pinHash` TEXT NOT NULL, `fingerprintEnabled` INTEGER NOT NULL, `dailyGoldPrice` TEXT NOT NULL, `taxPercent` TEXT NOT NULL, `minStockAlert` INTEGER NOT NULL, `selectedPrinterName` TEXT, `selectedPrinterAddress` TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `customers` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `name` TEXT NOT NULL, `phone` TEXT NOT NULL, `address` TEXT NOT NULL, `nationalId` TEXT NOT NULL, `avatarPath` TEXT, `about` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `products` (
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
        db.execSQL("CREATE TABLE IF NOT EXISTS `sale_invoices` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `customerId` INTEGER NOT NULL, `date` INTEGER NOT NULL, `totalAmount` TEXT NOT NULL, `discount` TEXT NOT NULL, `tax` TEXT NOT NULL, `paidAmount` TEXT NOT NULL, `paymentType` TEXT NOT NULL, `installmentsCount` INTEGER NOT NULL, `prepayment` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `sale_items` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `invoiceId` INTEGER NOT NULL, `productId` INTEGER NOT NULL, `quantity` INTEGER NOT NULL, `unitPrice` TEXT NOT NULL, `total` TEXT NOT NULL, `customWeight` TEXT, `customName` TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `installments` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `invoiceId` INTEGER NOT NULL, `dueDate` INTEGER NOT NULL, `amount` TEXT NOT NULL, `paid` INTEGER NOT NULL, `paymentDate` INTEGER)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `repairs` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `customerId` INTEGER NOT NULL, `description` TEXT NOT NULL, `imagePath` TEXT, `estimatedCost` TEXT NOT NULL, `upfrontPayment` TEXT NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `deliveredAt` INTEGER)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `gold_price_history` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `date` INTEGER NOT NULL, `pricePerGram` TEXT NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `audit_logs` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `userId` INTEGER NOT NULL DEFAULT 1, `action` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `details` TEXT NOT NULL)")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `daily_closings` (
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `businessDateKey` TEXT NOT NULL,
                `closedAt` INTEGER NOT NULL,
                `displayedPersianDate` TEXT NOT NULL,
                `displayedGregorianDate` TEXT NOT NULL,
                `invoiceCount` INTEGER NOT NULL,
                `salesTotal` TEXT NOT NULL,
                `paidTotal` TEXT NOT NULL,
                `installmentCreatedTotal` TEXT NOT NULL,
                `installmentCreatedCount` INTEGER NOT NULL,
                `installmentCollectedTotal` TEXT NOT NULL,
                `overdueInstallmentCount` INTEGER NOT NULL,
                `inventoryPieceCount` INTEGER NOT NULL,
                `inventoryWeight` TEXT NOT NULL,
                `inventoryValue` TEXT NOT NULL,
                `lowStockCount` INTEGER NOT NULL,
                `openRepairsCount` INTEGER NOT NULL,
                `readyRepairsCount` INTEGER NOT NULL,
                `goldRateAtClose` TEXT NOT NULL,
                `optionalPhysicalCash` TEXT,
                `optionalPhysicalGoldWeight` TEXT,
                `optionalNotes` TEXT,
                `status` TEXT NOT NULL,
                `revision` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `stock_take_sessions` (
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `startedAt` INTEGER NOT NULL,
                `completedAt` INTEGER,
                `status` TEXT NOT NULL,
                `notes` TEXT,
                `totalExpectedPieces` INTEGER NOT NULL,
                `totalCountedPieces` INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `stock_take_items` (
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `sessionId` INTEGER NOT NULL,
                `productId` INTEGER NOT NULL,
                `productName` TEXT NOT NULL,
                `productCategory` TEXT NOT NULL,
                `productBarcode` TEXT NOT NULL,
                `expectedStockAtStart` INTEGER NOT NULL,
                `countedStock` INTEGER NOT NULL,
                `systemStockAtFinalize` INTEGER,
                `difference` INTEGER NOT NULL,
                `changedDuringSession` INTEGER NOT NULL,
                `status` TEXT NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_take_items_sessionId` ON `stock_take_items` (`sessionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_take_items_productId` ON `stock_take_items` (`productId`)")
    }

    private fun insertV10Data(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("""
            INSERT INTO `daily_closings` (
                `id`, `businessDateKey`, `closedAt`, `displayedPersianDate`, `displayedGregorianDate`,
                `invoiceCount`, `salesTotal`, `paidTotal`, `installmentCreatedTotal`, `installmentCreatedCount`,
                `installmentCollectedTotal`, `overdueInstallmentCount`, `inventoryPieceCount`, `inventoryWeight`,
                `inventoryValue`, `lowStockCount`, `openRepairsCount`, `readyRepairsCount`, `goldRateAtClose`,
                `status`, `revision`
            ) VALUES (
                1, '2026-09-21', 1700000000000, '1405/06/31', '2026/09/21',
                2, '10000000', '10000000', '0', 0,
                '0', 0, 3, '15.5',
                '50000000', 0, 0, 0, '3500000',
                'CLOSED', 1
            )
        """.trimIndent())
        db.execSQL("INSERT INTO `stock_take_sessions` (`id`, `startedAt`, `status`, `totalExpectedPieces`, `totalCountedPieces`) VALUES (1, 1700000000000, 'IN_PROGRESS', 5, 3)")
        db.execSQL("INSERT INTO `stock_take_items` (`id`, `sessionId`, `productId`, `productName`, `productCategory`, `productBarcode`, `expectedStockAtStart`, `countedStock`, `difference`, `changedDuringSession`, `status`) VALUES (1, 1, 10, 'النگو', 'طلا', 'BAR1', 3, 3, 0, 0, 'PENDING')")
        db.execSQL("INSERT INTO `stock_take_items` (`id`, `sessionId`, `productId`, `productName`, `productCategory`, `productBarcode`, `expectedStockAtStart`, `countedStock`, `difference`, `changedDuringSession`, `status`) VALUES (2, 1, 11, 'انگشتر', 'طلا', 'BAR2', 2, 0, -2, 0, 'PENDING')")
    }

    private fun createV8Schema(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `users` (`id` INTEGER NOT NULL PRIMARY KEY, `pinHash` TEXT NOT NULL, `fingerprintEnabled` INTEGER NOT NULL, `dailyGoldPrice` REAL NOT NULL, `taxPercent` REAL NOT NULL, `minStockAlert` INTEGER NOT NULL, `selectedPrinterName` TEXT, `selectedPrinterAddress` TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `customers` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `name` TEXT NOT NULL, `phone` TEXT NOT NULL, `address` TEXT NOT NULL, `nationalId` TEXT NOT NULL, `avatarPath` TEXT, `about` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `products` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `name` TEXT NOT NULL, `category` TEXT NOT NULL, `weightGram` REAL NOT NULL, `karat` INTEGER NOT NULL, `wagePrice` REAL NOT NULL, `wageType` TEXT NOT NULL, `stock` INTEGER NOT NULL, `minStock` INTEGER NOT NULL, `imagePath` TEXT, `imagePath2` TEXT, `imagePath3` TEXT, `imagePath4` TEXT, `imagePath5` TEXT, `purchasePrice` REAL NOT NULL, `customBarcode` TEXT NOT NULL, `isDeleted` INTEGER NOT NULL DEFAULT 0, `createdAt` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `sale_invoices` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `customerId` INTEGER NOT NULL, `date` INTEGER NOT NULL, `totalAmount` REAL NOT NULL, `discount` REAL NOT NULL, `tax` REAL NOT NULL, `paidAmount` REAL NOT NULL, `paymentType` TEXT NOT NULL, `installmentsCount` INTEGER NOT NULL, `prepayment` REAL NOT NULL, `createdAt` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `sale_items` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `invoiceId` INTEGER NOT NULL, `productId` INTEGER NOT NULL, `quantity` INTEGER NOT NULL, `unitPrice` REAL NOT NULL, `total` REAL NOT NULL, `customWeight` REAL, `customName` TEXT)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `installments` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `invoiceId` INTEGER NOT NULL, `dueDate` INTEGER NOT NULL, `amount` REAL NOT NULL, `paid` INTEGER NOT NULL, `paymentDate` INTEGER)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `repairs` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `customerId` INTEGER NOT NULL, `description` TEXT NOT NULL, `imagePath` TEXT, `estimatedCost` REAL NOT NULL, `upfrontPayment` REAL NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `deliveredAt` INTEGER)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `gold_price_history` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `date` INTEGER NOT NULL, `pricePerGram` REAL NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `audit_logs` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `userId` INTEGER NOT NULL DEFAULT 1, `action` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `details` TEXT NOT NULL)")
    }

    private fun insertV8Data(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        db.execSQL("INSERT INTO `users` (`id`, `pinHash`, `fingerprintEnabled`, `dailyGoldPrice`, `taxPercent`, `minStockAlert`) VALUES (1, '', 0, 3500000.0, 9.0, 2)")
        db.execSQL("INSERT INTO `customers` (`id`, `name`, `phone`, `address`, `nationalId`, `about`, `createdAt`, `updatedAt`) VALUES (1, 'Customer 1', '0912', 'Tehran', '123', 'Note', 1000, 1000)")
        db.execSQL("INSERT INTO `products` (`id`, `name`, `category`, `weightGram`, `karat`, `wagePrice`, `wageType`, `stock`, `minStock`, `purchasePrice`, `customBarcode`, `isDeleted`, `createdAt`) VALUES (1, 'Ring', 'طلا', 5.25, 18, 150000.0, 'FIXED', 10, 1, 18000000.0, '123456', 0, 1000)")
        db.execSQL("INSERT INTO `sale_invoices` (`id`, `customerId`, `date`, `totalAmount`, `discount`, `tax`, `paidAmount`, `paymentType`, `installmentsCount`, `prepayment`, `createdAt`) VALUES (1, 1, 1000, 25000000.0, 500000.0, 2205000.0, 10000000.0, 'INSTALLMENT', 3, 10000000.0, 1000)")
        db.execSQL("INSERT INTO `sale_items` (`id`, `invoiceId`, `productId`, `quantity`, `unitPrice`, `total`, `customWeight`, `customName`) VALUES (1, 1, 1, 1, 23300000.0, 23300000.0, 5.25, 'Ring')")
        db.execSQL("INSERT INTO `installments` (`id`, `invoiceId`, `dueDate`, `amount`, `paid`) VALUES (1, 1, 2000, 5000000.0, 0)")
        db.execSQL("INSERT INTO `repairs` (`id`, `customerId`, `description`, `estimatedCost`, `upfrontPayment`, `status`, `createdAt`) VALUES (1, 1, 'Repair ring', 1200000.0, 200000.0, 'READY', 1000)")
        db.execSQL("INSERT INTO `gold_price_history` (`id`, `date`, `pricePerGram`) VALUES (1, 1000, 3500000.0)")
    }
}
