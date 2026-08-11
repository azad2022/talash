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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ShopRepositoryStockTest {

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

        // Insert dummy customer
        customerId = shopDao.insertCustomer(
            Customer(name = "علی رضایی", phone = "09123456789", address = "تهران")
        ).toInt()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `sale with quantity less than stock succeeds and updates stock correctly`() = runTest {
        val prodId = shopDao.insertProduct(
            Product(name = "انگشتر 18 عیار", category = "طلا", weightGram = 4.5, wagePrice = 100000.0, wageType = "FIXED", stock = 5)
        ).toInt()

        val invoice = SaleInvoice(
            customerId = customerId,
            totalAmount = 15000000.0,
            discount = 0.0,
            tax = 0.0,
            paidAmount = 15000000.0,
            paymentType = "CASH"
        )
        val items = listOf(
            SaleItem(invoiceId = 0, productId = prodId, quantity = 2, unitPrice = 7500000.0, total = 15000000.0)
        )

        val invoiceId = repository.createInvoice(invoice, items, emptyList())
        assertTrue(invoiceId > 0)

        // Verify remaining stock is 5 - 2 = 3
        val updatedProd = shopDao.getProductById(prodId)
        assertEquals(3, updatedProd?.stock)

        // Verify invoice and item exist
        val invoices = shopDao.getAllInvoicesSync()
        assertEquals(1, invoices.size)
        val saleItems = shopDao.getAllSaleItemsSync()
        assertEquals(1, saleItems.size)
    }

    @Test
    fun `sale with quantity exactly equal to stock succeeds and sets stock to zero`() = runTest {
        val prodId = shopDao.insertProduct(
            Product(name = "دستبند طلا", category = "طلا", weightGram = 6.0, wagePrice = 150000.0, wageType = "FIXED", stock = 2)
        ).toInt()

        val invoice = SaleInvoice(
            customerId = customerId,
            totalAmount = 20000000.0,
            discount = 0.0,
            tax = 0.0,
            paidAmount = 20000000.0,
            paymentType = "CASH"
        )
        val items = listOf(
            SaleItem(invoiceId = 0, productId = prodId, quantity = 2, unitPrice = 10000000.0, total = 20000000.0)
        )

        val invoiceId = repository.createInvoice(invoice, items, emptyList())
        assertTrue(invoiceId > 0)

        // Stock should be exactly 0
        val updatedProd = shopDao.getProductById(prodId)
        assertEquals(0, updatedProd?.stock)
    }

    @Test
    fun `sale with quantity greater than stock throws exception and makes no database changes`() = runTest {
        val prodId = shopDao.insertProduct(
            Product(name = "سکه پارسیان", category = "طلا", weightGram = 1.0, wagePrice = 50000.0, wageType = "FIXED", stock = 2)
        ).toInt()

        val invoice = SaleInvoice(
            customerId = customerId,
            totalAmount = 25000000.0,
            discount = 0.0,
            tax = 0.0,
            paidAmount = 25000000.0,
            paymentType = "CASH"
        )
        val items = listOf(
            SaleItem(invoiceId = 0, productId = prodId, quantity = 5, unitPrice = 5000000.0, total = 25000000.0)
        )

        try {
            repository.createInvoice(invoice, items, emptyList())
            fail("Should have thrown IllegalStateException due to insufficient stock")
        } catch (e: Exception) {
            assertTrue(e is IllegalStateException)
            assertTrue(e.message?.contains("موجودی کالا") == true)
        }

        // Stock must remain unchanged at 2
        val prodAfter = shopDao.getProductById(prodId)
        assertEquals(2, prodAfter?.stock)

        // No invoices or sale items should be created
        assertEquals(0, shopDao.getAllInvoicesSync().size)
        assertEquals(0, shopDao.getAllSaleItemsSync().size)
    }

    @Test
    fun `multi-item invoice where one item exceeds stock rolls back entire transaction`() = runTest {
        val prod1Id = shopDao.insertProduct(
            Product(name = "گوشواره طلا", category = "طلا", weightGram = 2.0, wagePrice = 80000.0, wageType = "FIXED", stock = 10)
        ).toInt()

        val prod2Id = shopDao.insertProduct(
            Product(name = "گردنبند برلیان", category = "طلا", weightGram = 12.0, wagePrice = 500000.0, wageType = "FIXED", stock = 1)
        ).toInt()

        val invoice = SaleInvoice(
            customerId = customerId,
            totalAmount = 50000000.0,
            discount = 0.0,
            tax = 0.0,
            paidAmount = 50000000.0,
            paymentType = "CASH"
        )
        val items = listOf(
            SaleItem(invoiceId = 0, productId = prod1Id, quantity = 2, unitPrice = 5000000.0, total = 10000000.0),
            SaleItem(invoiceId = 0, productId = prod2Id, quantity = 3, unitPrice = 20000000.0, total = 40000000.0) // requested 3, stock is 1!
        )

        try {
            repository.createInvoice(invoice, items, emptyList())
            fail("Should have thrown IllegalStateException for insufficient stock on prod2")
        } catch (e: Exception) {
            assertTrue(e is IllegalStateException)
        }

        // Both products must retain original stock
        assertEquals(10, shopDao.getProductById(prod1Id)?.stock)
        assertEquals(1, shopDao.getProductById(prod2Id)?.stock)

        // Zero invoices and items saved
        assertEquals(0, shopDao.getAllInvoicesSync().size)
        assertEquals(0, shopDao.getAllSaleItemsSync().size)
    }

    @Test
    fun `sale for non-existent product throws exception`() = runTest {
        val nonExistentProdId = 9999
        val invoice = SaleInvoice(
            customerId = customerId,
            totalAmount = 5000000.0,
            discount = 0.0,
            tax = 0.0,
            paidAmount = 5000000.0,
            paymentType = "CASH"
        )
        val items = listOf(
            SaleItem(invoiceId = 0, productId = nonExistentProdId, quantity = 1, unitPrice = 5000000.0, total = 5000000.0)
        )

        try {
            repository.createInvoice(invoice, items, emptyList())
            fail("Should have thrown IllegalStateException for non-existent product")
        } catch (e: Exception) {
            assertTrue(e is IllegalStateException)
        }

        assertEquals(0, shopDao.getAllInvoicesSync().size)
    }

    @Test
    fun `sale with zero or negative quantity throws exception`() = runTest {
        val prodId = shopDao.insertProduct(
            Product(name = "النگو طلا", category = "طلا", weightGram = 8.0, wagePrice = 120000.0, wageType = "FIXED", stock = 5)
        ).toInt()

        val invoice = SaleInvoice(
            customerId = customerId,
            totalAmount = 0.0,
            discount = 0.0,
            tax = 0.0,
            paidAmount = 0.0,
            paymentType = "CASH"
        )
        val zeroQtyItem = listOf(
            SaleItem(invoiceId = 0, productId = prodId, quantity = 0, unitPrice = 5000000.0, total = 0.0)
        )

        try {
            repository.createInvoice(invoice, zeroQtyItem, emptyList())
            fail("Should have thrown IllegalArgumentException for zero quantity")
        } catch (e: Exception) {
            assertTrue(e is IllegalArgumentException)
        }

        val negativeQtyItem = listOf(
            SaleItem(invoiceId = 0, productId = prodId, quantity = -2, unitPrice = 5000000.0, total = -10000000.0)
        )

        try {
            repository.createInvoice(invoice, negativeQtyItem, emptyList())
            fail("Should have thrown IllegalArgumentException for negative quantity")
        } catch (e: Exception) {
            assertTrue(e is IllegalArgumentException)
        }

        // Stock remains unchanged
        assertEquals(5, shopDao.getProductById(prodId)?.stock)
        assertEquals(0, shopDao.getAllInvoicesSync().size)
    }
}
