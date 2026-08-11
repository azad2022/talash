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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ProductSoftDeleteTest {

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
            Customer(name = "مشتری تست", phone = "09120000000", address = "تهران")
        ).toInt()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // Test 1: ایجاد Product -> ایجاد Invoice -> حذف Product -> اطمینان از اینکه Product فیزیکی حذف نمیشود (Soft Delete)
    @Test
    fun test1_productWithInvoiceSoftDeleted() = runTest {
        val prodId = shopDao.insertProduct(
            Product(name = "دستبند طلا", category = "طلا", weightGram = 5.0, wagePrice = 100000.0, wageType = "FIXED", stock = 10)
        ).toInt()
        val prod = shopDao.getProductById(prodId)!!

        val invoice = SaleInvoice(customerId = customerId, totalAmount = 15000000.0, discount = 0.0, tax = 0.0, paidAmount = 15000000.0, paymentType = "CASH")
        val items = listOf(SaleItem(invoiceId = 0, productId = prodId, quantity = 2, unitPrice = 7500000.0, total = 15000000.0, customName = prod.name, customWeight = prod.weightGram))
        repository.createInvoice(invoice, items, emptyList())

        // Delete product via repository
        repository.deleteProduct(prod)

        // Physical record MUST still exist in database
        val deletedProd = shopDao.getProductById(prodId)
        assertNotNull("Product record must still exist in DB", deletedProd)
        assertTrue("Product isDeleted flag must be true", deletedProd!!.isDeleted)

        // Active products list must NOT contain soft-deleted product
        val activeProducts = shopDao.getAllProducts().first()
        assertTrue("Active products list should not contain soft-deleted product", activeProducts.none { it.id == prodId })
    }

    // Test 2: ایجاد Product با stock = 10 -> فروش 3 عدد -> Soft Delete Product -> حذف Invoice -> اطمینان از اینکه stock دوباره به 10 برمیگردد
    @Test
    fun test2_softDeletedProductStockRestoredOnInvoiceDeletion() = runTest {
        val prodId = shopDao.insertProduct(
            Product(name = "گردنبند ۱۸ عیار", category = "طلا", weightGram = 8.0, wagePrice = 200000.0, wageType = "FIXED", stock = 10)
        ).toInt()
        val prod = shopDao.getProductById(prodId)!!

        val invoice = SaleInvoice(customerId = customerId, totalAmount = 30000000.0, discount = 0.0, tax = 0.0, paidAmount = 30000000.0, paymentType = "CASH")
        val items = listOf(SaleItem(invoiceId = 0, productId = prodId, quantity = 3, unitPrice = 10000000.0, total = 30000000.0, customName = prod.name, customWeight = prod.weightGram))
        val invoiceId = repository.createInvoice(invoice, items, emptyList()).toInt()

        // Stock decreased to 7
        val prodAfterSale = shopDao.getProductById(prodId)!!
        assertEquals(7, prodAfterSale.stock)

        // Soft delete product
        repository.deleteProduct(prodAfterSale)

        // Delete invoice
        val savedInvoice = shopDao.getAllInvoicesSync().first { it.id == invoiceId }
        repository.deleteInvoice(savedInvoice)

        // Stock must be restored to 10
        val prodAfterInvoiceDelete = shopDao.getProductById(prodId)!!
        assertEquals(10, prodAfterInvoiceDelete.stock)
        assertTrue(prodAfterInvoiceDelete.isDeleted)
    }

    // Test 3: Product بدون سابقه فروش -> Delete -> اطمینان از اینکه حذف واقعی انجام میشود
    @Test
    fun test3_productWithoutSaleHistoryPhysicallyDeleted() = runTest {
        val prodId = shopDao.insertProduct(
            Product(name = "سکه بدون سابقه", category = "سکه", weightGram = 2.0, wagePrice = 50000.0, wageType = "FIXED", stock = 5)
        ).toInt()
        val prod = shopDao.getProductById(prodId)!!

        // Delete product with 0 sales
        repository.deleteProduct(prod)

        // Physical record MUST be null
        val deletedProd = shopDao.getProductById(prodId)
        assertNull("Product without sales history should be physically deleted", deletedProd)
    }

    // Test 4: Product حذفشده -> Backup -> Restore -> اطمینان از حفظ Product و ارتباط آن با SaleItemهای قدیمی
    @Test
    fun test4_softDeletedProductPreservedAcrossBackupAndRestore() = runTest {
        val prodId = shopDao.insertProduct(
            Product(name = "النگو عتیق", category = "طلا", weightGram = 12.0, wagePrice = 150000.0, wageType = "FIXED", stock = 4)
        ).toInt()
        val prod = shopDao.getProductById(prodId)!!

        val invoice = SaleInvoice(customerId = customerId, totalAmount = 40000000.0, discount = 0.0, tax = 0.0, paidAmount = 40000000.0, paymentType = "CASH")
        val items = listOf(SaleItem(invoiceId = 0, productId = prodId, quantity = 1, unitPrice = 40000000.0, total = 40000000.0, customName = prod.name, customWeight = prod.weightGram))
        repository.createInvoice(invoice, items, emptyList())

        // Soft delete product
        repository.deleteProduct(prod)

        // Export backup
        val jsonBackup = repository.exportBackupJson()

        // Restore backup
        val restoreResult = repository.importRestoreJson(jsonBackup)
        assertTrue(restoreResult.isSuccess)

        // Product must still exist in DB as isDeleted = true
        val restoredProd = shopDao.getProductById(prodId)
        assertNotNull(restoredProd)
        assertTrue(restoredProd!!.isDeleted)
        assertEquals("النگو عتیق", restoredProd.name)

        // Sale items must still point to prodId
        val saleItems = shopDao.getAllSaleItemsSync()
        assertEquals(1, saleItems.size)
        assertEquals(prodId, saleItems[0].productId)
    }

    // Test 5: Invoice قدیمی با Product آرشیوشده -> نمایش Invoice -> اطمینان از اینکه اطلاعات Product همچنان قابل نمایش است
    @Test
    fun test5_oldInvoiceDisplaysArchivedProductInformation() = runTest {
        val prodId = shopDao.insertProduct(
            Product(name = "مدال طلا آرشیو شده", category = "طلا", weightGram = 3.5, wagePrice = 90000.0, wageType = "FIXED", stock = 2)
        ).toInt()
        val prod = shopDao.getProductById(prodId)!!

        val invoice = SaleInvoice(customerId = customerId, totalAmount = 12000000.0, discount = 0.0, tax = 0.0, paidAmount = 12000000.0, paymentType = "CASH")
        val items = listOf(SaleItem(invoiceId = 0, productId = prodId, quantity = 1, unitPrice = 12000000.0, total = 12000000.0, customName = prod.name, customWeight = prod.weightGram))
        val invoiceId = repository.createInvoice(invoice, items, emptyList()).toInt()

        // Soft delete product
        repository.deleteProduct(prod)

        // Retrieve invoice details
        val invoiceDetails = shopDao.getInvoiceWithDetailsById(invoiceId).first()
        assertNotNull(invoiceDetails)
        assertEquals(1, invoiceDetails!!.items.size)

        val item = invoiceDetails.items[0]
        assertEquals(prodId, item.productId)
        assertEquals("مدال طلا آرشیو شده", item.customName)

        // Product reference in DB is also still fetchable
        val archivedProd = shopDao.getProductById(item.productId)
        assertNotNull(archivedProd)
        assertEquals("مدال طلا آرشیو شده", archivedProd!!.name)
    }
}
