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

    @Test
    fun `closed business day blocks createInvoice inside transaction`() = runTest {
        val prodId = shopDao.insertProduct(Product(name = "گوشواره", category = "طلا", weightGram = BigDecimal.ONE, wagePrice = BigDecimal.ZERO, wageType = "FIXED", stock = 5)).toInt()
        val invoice = SaleInvoice(customerId = customerId, totalAmount = 1000.0, discount = 0.0, tax = 0.0, paidAmount = 1000.0, paymentType = "CASH")
        val items = listOf(SaleItem(invoiceId = 0, productId = prodId, quantity = 1, unitPrice = 1000.0, total = 1000.0))

        // Close today
        val preview = repository.getTodaySummaryPreview()
        assertTrue(repository.closeDay(preview, BigDecimal.ZERO, BigDecimal.ZERO, "بستن روز").isSuccess)

        try {
            repository.createInvoice(invoice, items, emptyList())
            fail("Expected IllegalStateException due to closed business day lock")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("بسته شده است") == true)
        }
        // Verify product stock did not change
        val prod = shopDao.getProductById(prodId)
        assertEquals(5, prod?.stock)
    }

    @Test
    fun `closed business day blocks deleteInvoice inside transaction`() = runTest {
        val prodId = shopDao.insertProduct(Product(name = "انگشتر مردانه", category = "طلا", weightGram = BigDecimal.ONE, wagePrice = BigDecimal.ZERO, wageType = "FIXED", stock = 5)).toInt()
        val invoice = SaleInvoice(customerId = customerId, totalAmount = 2000.0, discount = 0.0, tax = 0.0, paidAmount = 2000.0, paymentType = "CASH")
        val items = listOf(SaleItem(invoiceId = 0, productId = prodId, quantity = 1, unitPrice = 2000.0, total = 2000.0))

        val invId = repository.createInvoice(invoice, items, emptyList()).toInt()
        val savedInvoice = shopDao.getAllInvoicesSync().first { it.id == invId }

        // Close day
        val preview = repository.getTodaySummaryPreview()
        assertTrue(repository.closeDay(preview, BigDecimal.ZERO, BigDecimal.ZERO, "بستن").isSuccess)

        // Attempt delete on closed day
        try {
            repository.deleteInvoice(savedInvoice)
            fail("Expected IllegalStateException due to closed business day lock")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("بسته شده است") == true)
        }

        // Verify invoice was NOT cancelled and stock remains 4
        val afterProd = shopDao.getProductById(prodId)
        assertEquals(4, afterProd?.stock)
        val afterInv = shopDao.getAllInvoicesSync().first { it.id == invId }
        assertEquals("CASH", afterInv.paymentType)
    }

    @Test
    fun `closed business day blocks payInstallment inside transaction`() = runTest {
        val prodId = shopDao.insertProduct(Product(name = "سکه", category = "سکه", weightGram = BigDecimal.ONE, wagePrice = BigDecimal.ZERO, wageType = "FIXED", stock = 2)).toInt()
        val invoice = SaleInvoice(customerId = customerId, totalAmount = 10000.0, discount = 0.0, tax = 0.0, paidAmount = 5000.0, paymentType = "INSTALLMENT", installmentsCount = 1)
        val items = listOf(SaleItem(invoiceId = 0, productId = prodId, quantity = 1, unitPrice = 10000.0, total = 10000.0))
        val installments = listOf(com.example.data.model.Installment(invoiceId = 0, dueDate = System.currentTimeMillis() + 86400000, amount = 5000.0, paid = false))

        val invId = repository.createInvoice(invoice, items, installments).toInt()
        val inst = shopDao.getAllInstallmentsSync().first { it.invoiceId == invId }

        // Close day
        val preview = repository.getTodaySummaryPreview()
        assertTrue(repository.closeDay(preview, BigDecimal.ZERO, BigDecimal.ZERO, "بستن").isSuccess)

        // Attempt pay installment
        try {
            repository.payInstallment(inst.id, true)
            fail("Expected IllegalStateException due to closed business day lock")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("بسته شده است") == true)
        }

        val afterInst = shopDao.getAllInstallmentsSync().first { it.id == inst.id }
        assertFalse(afterInst.paid)
    }

    @Test
    fun `closed business day blocks product insertions and stock adjustments`() = runTest {
        val prodId = shopDao.insertProduct(Product(name = "زنجیر", category = "طلا", weightGram = BigDecimal.ONE, wagePrice = BigDecimal.ZERO, wageType = "FIXED", stock = 3)).toInt()

        // Close day
        val preview = repository.getTodaySummaryPreview()
        assertTrue(repository.closeDay(preview, BigDecimal.ZERO, BigDecimal.ZERO, "بستن").isSuccess)

        // 1. Insert product
        try {
            repository.insertProduct(Product(name = "دستبند چرم", category = "طلا", weightGram = BigDecimal.ONE, wagePrice = BigDecimal.ZERO, wageType = "FIXED", stock = 1))
            fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("بسته شده است") == true)
        }

        // 2. Update stock directly
        try {
            repository.updateProductStock(prodId, 10)
            fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("بسته شده است") == true)
        }

        val savedProd = shopDao.getProductById(prodId)
        assertEquals(3, savedProd?.stock)

        // 3. Delete product
        try {
            repository.deleteProduct(savedProd!!)
            fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("بسته شده است") == true)
        }
    }

    @Test
    fun `closed business day blocks start and apply of stock take`() = runTest {
        shopDao.insertProduct(Product(name = "النگو پهن", category = "طلا", weightGram = BigDecimal.ONE, wagePrice = BigDecimal.ZERO, wageType = "FIXED", stock = 2))

        // Close day
        val preview = repository.getTodaySummaryPreview()
        assertTrue(repository.closeDay(preview, BigDecimal.ZERO, BigDecimal.ZERO, "بستن").isSuccess)

        // Starting stock take on closed day must fail
        val startRes = repository.startStockTakeSession()
        assertTrue(startRes.isFailure)
        assertTrue(startRes.exceptionOrNull()?.message?.contains("بسته شده است") == true)
    }

    @Test
    fun `canonical barcode assignment format G-xxxxxx when customBarcode is blank`() = runTest {
        val prodId = shopDao.insertProduct(Product(name = "آویز بدون بارکد", category = "طلا", weightGram = BigDecimal.ONE, wagePrice = BigDecimal.ZERO, wageType = "FIXED", customBarcode = "  ", stock = 1)).toInt()

        val session = repository.startStockTakeSession().getOrThrow()
        val items = shopDao.getStockTakeItemsForSessionSync(session.id)
        val item = items.first { it.productId == prodId }

        val expectedBarcode = "G-${prodId.toString().padStart(6, '0')}"
        assertEquals(expectedBarcode, item.productBarcode)

        // Resolving by canonical barcode succeeds
        val scanRes = repository.scanBarcodeForStockTake(session.id, expectedBarcode)
        assertTrue(scanRes.isSuccess)
        val scanSuccess = scanRes.getOrThrow() as com.example.data.repository.StockTakeScanResult.Success
        assertEquals(1, scanSuccess.item.countedStock)
        assertTrue(scanSuccess.item.isCounted)
    }

    @Test
    fun `resolveStockTakeItemReview successfully reconciles changed item and allows finalization`() = runTest {
        val prodId = shopDao.insertProduct(Product(name = "گردنبند مروارید", category = "طلا", weightGram = BigDecimal.ONE, wagePrice = BigDecimal.ZERO, wageType = "FIXED", customBarcode = "PEARL-1", stock = 4)).toInt()

        val session = repository.startStockTakeSession().getOrThrow()
        repository.scanBarcodeForStockTake(session.id, "PEARL-1")
        repository.scanBarcodeForStockTake(session.id, "PEARL-1")
        repository.scanBarcodeForStockTake(session.id, "PEARL-1")
        repository.scanBarcodeForStockTake(session.id, "PEARL-1") // counted 4

        // Concurrent update: product stock changes to 6
        shopDao.updateProductStock(prodId, 6)

        // Prepare review: status becomes NEEDS_REVIEW, session becomes REVIEW_REQUIRED
        val reviewRes = repository.prepareReconciliationReview(session.id).getOrThrow()
        val itemBefore = reviewRes.first { it.productId == prodId }
        assertEquals("NEEDS_REVIEW", itemBefore.status)

        // Finalize must fail
        val finalizeFail = repository.applyStockTakeAdjustments(session.id)
        assertTrue(finalizeFail.isFailure)

        // Resolve item by accepting new baseline and verified count = 6
        val resolveRes = repository.resolveStockTakeItemReview(session.id, prodId, 6)
        assertTrue(resolveRes.isSuccess)
        val resolvedItem = resolveRes.getOrThrow()
        assertEquals("MATCHED", resolvedItem.status)
        assertFalse(resolvedItem.changedDuringSession)
        assertEquals(6, resolvedItem.countedStock)
        assertEquals(6, resolvedItem.expectedStockAtStart)

        val updatedSession = shopDao.getStockTakeSessionById(session.id)
        assertEquals("IN_PROGRESS", updatedSession?.status)

        // Now finalize succeeds
        val finalizeSuccess = repository.applyStockTakeAdjustments(session.id)
        assertTrue(finalizeSuccess.isSuccess)
        val applied = finalizeSuccess.getOrThrow()
        assertEquals("COMPLETED", applied.completedSession.status)
        assertEquals(6, shopDao.getProductById(prodId)?.stock)
    }

    @Test
    fun `canceling stock take session leaves inventory untouched`() = runTest {
        val prodId = shopDao.insertProduct(Product(name = "نیم ست", category = "طلا", weightGram = BigDecimal.ONE, wagePrice = BigDecimal.ZERO, wageType = "FIXED", customBarcode = "HALF-1", stock = 7)).toInt()

        val session = repository.startStockTakeSession().getOrThrow()
        repository.scanBarcodeForStockTake(session.id, "HALF-1") // count 1

        val cancelRes = repository.cancelStockTakeSession(session.id)
        assertTrue(cancelRes.isSuccess)

        val updatedSession = shopDao.getStockTakeSessionById(session.id)
        assertEquals("CANCELLED", updatedSession?.status)

        // Stock in database remains intact
        assertEquals(7, shopDao.getProductById(prodId)?.stock)
    }

    @Test
    fun `new product added during session appears in review and flags REVIEW_REQUIRED`() = runTest {
        val prod1 = shopDao.insertProduct(Product(name = "کالای اولیه", category = "طلا", weightGram = BigDecimal.ONE, wagePrice = BigDecimal.ZERO, wageType = "FIXED", customBarcode = "C1", stock = 2)).toInt()

        val session = repository.startStockTakeSession().getOrThrow()
        repository.scanBarcodeForStockTake(session.id, "C1")
        repository.scanBarcodeForStockTake(session.id, "C1")

        // Add new product during session
        val prod2 = repository.insertProduct(Product(name = "کالای جدید حین انبارگردانی", category = "طلا", weightGram = BigDecimal.ONE, wagePrice = BigDecimal.ZERO, wageType = "FIXED", customBarcode = "C2", stock = 3)).toInt()

        // Verify session was flagged
        val activeSession = shopDao.getStockTakeSessionById(session.id)
        assertEquals("REVIEW_REQUIRED", activeSession?.status)

        // Prepare review
        val reviewList = repository.prepareReconciliationReview(session.id).getOrThrow()
        val newProdItem = reviewList.firstOrNull { it.productId == prod2 }
        assertNotNull(newProdItem)
        assertEquals("NEW_PRODUCT_DURING_SESSION", newProdItem!!.status)

        // Cannot finalize until resolved
        val applyFail = repository.applyStockTakeAdjustments(session.id)
        assertTrue(applyFail.isFailure)

        // Resolve new item
        repository.resolveStockTakeItemReview(session.id, prod2, 3)

        // Finalize succeeds
        val applySuccess = repository.applyStockTakeAdjustments(session.id)
        assertTrue(applySuccess.isSuccess)
    }
}
