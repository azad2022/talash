package com.example.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.database.ShopDao
import com.example.data.model.Customer
import com.example.data.model.Product
import com.example.data.model.SaleInvoice
import com.example.data.model.SaleItem
import com.example.data.repository.ShopRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DailyClosingAndStockTakeIntegrityTest {

    private lateinit var database: AppDatabase
    private lateinit var shopDao: ShopDao
    private lateinit var repository: ShopRepository
    private var customerId: Int = 0

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        shopDao = database.shopDao
        repository = ShopRepository(shopDao, context, database)

        customerId = shopDao.insertCustomer(
            Customer(name = "مشتری تستی", phone = "09121111111", address = "تهران")
        ).toInt()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `active stock take session blocks daily closing`() = runTest {
        shopDao.insertProduct(Product(name = "النگو", category = "طلا", weightGram = BigDecimal.ONE, wagePrice = BigDecimal.ZERO, wageType = "FIXED", stock = 3))

        // Start stock take session
        val startResult = repository.startStockTakeSession("انبارگردانی نمونه")
        assertTrue(startResult.isSuccess)
        val activeSession = startResult.getOrThrow()
        assertEquals("IN_PROGRESS", activeSession.status)

        // Attempting to close the day while stock take is active must fail
        val preview = repository.getTodaySummaryPreview()
        val closeResult = repository.closeDay(preview, BigDecimal("1000"), BigDecimal("10"), "بستن روز")
        assertTrue(closeResult.isFailure)
        assertTrue(closeResult.exceptionOrNull()?.message?.contains("انبارگردانی فعال") == true)
    }

    @Test
    fun `closing day locks invoices and reopening unlocks with audit trail`() = runTest {
        val prodId = shopDao.insertProduct(Product(name = "پلاک طلا", category = "طلا", weightGram = BigDecimal.ONE, wagePrice = BigDecimal.ZERO, wageType = "FIXED", stock = 5)).toInt()

        // 1. Create invoice before closing
        val invoice = SaleInvoice(
            customerId = customerId,
            totalAmount = 5000000.0,
            discount = 0.0,
            tax = 0.0,
            paidAmount = 5000000.0,
            paymentType = "CASH"
        )
        val items = listOf(
            SaleItem(invoiceId = 0, productId = prodId, quantity = 1, unitPrice = 5000000.0, total = 5000000.0)
        )
        val invId = repository.createInvoice(invoice, items, emptyList())
        assertTrue(invId > 0)

        // 2. Close day
        val preview = repository.getTodaySummaryPreview()
        val closeRes = repository.closeDay(preview, BigDecimal("5000000"), BigDecimal("0"), "پایان روز")
        assertTrue(closeRes.isSuccess)
        val closing = closeRes.getOrThrow()
        assertEquals("CLOSED", closing.status)
        assertEquals(1, closing.revision)

        // 3. Attempting to create invoice on closed day must fail with business day lock
        try {
            repository.createInvoice(invoice, items, emptyList())
            fail("Expected IllegalStateException due to closed business day lock")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("بسته شده است") == true)
        }

        // 4. Reopen day with blank reason must fail
        val blankReopen = repository.reopenDay(closing.id, "   ")
        assertTrue(blankReopen.isFailure)

        // 5. Reopen day with valid reason succeeds and updates audit trail
        val validReopen = repository.reopenDay(closing.id, "نیاز به اصلاح فاکتور جامانده")
        assertTrue(validReopen.isSuccess)
        val reopenedClosing = validReopen.getOrThrow()
        assertEquals("REOPENED", reopenedClosing.status)
        assertEquals("نیاز به اصلاح فاکتور جامانده", reopenedClosing.reopenReason)
        assertNotNull(reopenedClosing.reopenedAt)

        // 6. Creating invoice is now permitted again
        val secondInvId = repository.createInvoice(invoice, items, emptyList())
        assertTrue(secondInvId > 0)
    }

    @Test
    fun `stock take differentiates zero counted from uncounted items and rejects completion if uncounted`() = runTest {
        val prod1 = shopDao.insertProduct(Product(name = "دستبند A", category = "طلا", weightGram = BigDecimal.ONE, wagePrice = BigDecimal.ZERO, wageType = "FIXED", customBarcode = "BAR-A", stock = 2)).toInt()
        val prod2 = shopDao.insertProduct(Product(name = "دستبند B", category = "طلا", weightGram = BigDecimal.ONE, wagePrice = BigDecimal.ZERO, wageType = "FIXED", customBarcode = "BAR-B", stock = 3)).toInt()

        val startRes = repository.startStockTakeSession()
        assertTrue(startRes.isSuccess)
        val session = startRes.getOrThrow()

        // Scan only prod1
        val scanRes = repository.scanBarcodeForStockTake(session.id, "BAR-A")
        assertTrue(scanRes.isSuccess)

        // Count prod1 again
        repository.scanBarcodeForStockTake(session.id, "BAR-A")

        // Attempting to finalize now must fail because prod2 is still uncounted (isCounted = false)
        val applyAttempt = repository.applyStockTakeAdjustments(session.id)
        assertTrue(applyAttempt.isFailure)
        assertTrue(applyAttempt.exceptionOrNull()?.message?.contains("شمارش نشده‌اند") == true)

        // Explicitly set prod2 count to 0 (physical stock count is zero, isCounted = true)
        val zeroUpdateRes = repository.manualUpdateStockTakeItemCount(session.id, prod2, 0)
        assertTrue(zeroUpdateRes.isSuccess)
        val updatedProd2Item = zeroUpdateRes.getOrThrow()
        assertTrue(updatedProd2Item.isCounted)
        assertEquals(0, updatedProd2Item.countedStock)
        assertEquals(-3, updatedProd2Item.difference)

        // Now finalize should succeed
        val applyFinal = repository.applyStockTakeAdjustments(session.id)
        assertTrue(applyFinal.isSuccess)
        val applyResult = applyFinal.getOrThrow()
        assertEquals("COMPLETED", applyResult.completedSession.status)

        // Check adjusted inventory in DB
        val dbProd1 = shopDao.getProductById(prod1)
        val dbProd2 = shopDao.getProductById(prod2)
        assertEquals(2, dbProd1?.stock)
        assertEquals(0, dbProd2?.stock)
    }

    @Test
    fun `concurrent product change during stock take flags REVIEW_REQUIRED and blocks finalize`() = runTest {
        val prod = shopDao.insertProduct(Product(name = "انگشتر", category = "طلا", weightGram = BigDecimal.ONE, wagePrice = BigDecimal.ZERO, wageType = "FIXED", customBarcode = "ANG-1", stock = 4)).toInt()

        val session = repository.startStockTakeSession().getOrThrow()

        // Scan item
        repository.scanBarcodeForStockTake(session.id, "ANG-1")
        repository.scanBarcodeForStockTake(session.id, "ANG-1")
        repository.scanBarcodeForStockTake(session.id, "ANG-1")
        repository.scanBarcodeForStockTake(session.id, "ANG-1") // counted 4

        // Concurrent update: product stock changes directly or through sales
        shopDao.updateProductStock(prod, 10)

        // Prepare review: should detect difference from expectedStockAtStart and set REVIEW_REQUIRED
        val reviewRes = repository.prepareReconciliationReview(session.id)
        assertTrue(reviewRes.isSuccess)
        val reviewedItems = reviewRes.getOrThrow()
        val item = reviewedItems.first { it.productId == prod }
        assertEquals("NEEDS_REVIEW", item.status)
        assertTrue(item.changedDuringSession)

        // Attempting to apply adjustments must be blocked and throw IllegalStateException
        val applyRes = repository.applyStockTakeAdjustments(session.id)
        assertTrue(applyRes.isFailure)
        assertTrue(applyRes.exceptionOrNull()?.message?.contains("REVIEW_REQUIRED") == true)

        val updatedSession = shopDao.getStockTakeSessionById(session.id)
        assertEquals("REVIEW_REQUIRED", updatedSession?.status)
    }

    @Test
    fun `starting stock take with duplicate barcodes fails with descriptive error`() = runTest {
        shopDao.insertProduct(Product(name = "کالای اول", category = "طلا", weightGram = BigDecimal.ONE, wagePrice = BigDecimal.ZERO, wageType = "FIXED", customBarcode = "BAR-SAME", stock = 1))
        shopDao.insertProduct(Product(name = "کالای دوم", category = "طلا", weightGram = BigDecimal.ONE, wagePrice = BigDecimal.ZERO, wageType = "FIXED", customBarcode = "bar-same", stock = 2))

        val result = repository.startStockTakeSession()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("بارکدهای تکراری") == true)
    }
}
