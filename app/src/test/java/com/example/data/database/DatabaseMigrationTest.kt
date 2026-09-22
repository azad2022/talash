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
            .addMigrations(AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10)
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
