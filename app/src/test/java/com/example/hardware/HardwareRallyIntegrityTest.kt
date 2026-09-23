package com.example.hardware

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.AppDatabase
import com.example.data.database.ShopDao
import com.example.data.model.Product
import com.example.data.model.SaleInvoice
import com.example.data.model.SaleItem
import com.example.data.model.Customer
import com.example.data.repository.ShopRepository
import com.example.domain.util.BarcodeResolver
import com.example.hardware.core.HardwareDeviceType
import com.example.hardware.scale.WeightComparisonEngine
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HardwareRallyIntegrityTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: ShopDao
    private lateinit var repository: ShopRepository

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.shopDao
        repository = ShopRepository(dao, context, database)
        dao.insertCustomer(Customer(name = "مشتری", phone = "1", address = ""))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun duplicate_custom_barcode_is_rejected_transactionally() = runTest {
        val first = Product(
            name = "کالای اول",
            category = "انگشتر",
            weightGram = BigDecimal("2.1"),
            wagePrice = BigDecimal.ZERO,
            wageType = "FIXED",
            stock = 1,
            customBarcode = "DUP-01"
        )
        assertTrue(repository.insertProduct(first) > 0)

        val second = first.copy(id = 0, name = "کالای دوم")
        try {
            repository.insertProduct(second)
        } catch (_: IllegalStateException) {
        }

        val active = dao.getAllProductsSync().filter { !it.isDeleted }
        assertEquals(1, active.size)
        assertEquals("کالای اول", active.single().name)
    }

    @Test
    fun numeric_and_g_id_alias_collisions_are_rejected() = runTest {
        val first = Product(
            id = 1,
            name = "کالای اول",
            category = "انگشتر",
            weightGram = BigDecimal("1.0"),
            wagePrice = BigDecimal.ZERO,
            wageType = "FIXED",
            stock = 1
        )
        assertTrue(repository.insertProduct(first) > 0)

        val second = Product(
            name = "کالای دوم",
            category = "انگشتر",
            weightGram = BigDecimal("1.0"),
            wagePrice = BigDecimal.ZERO,
            wageType = "FIXED",
            stock = 1,
            customBarcode = "1"
        )

        var failed = false
        try {
            repository.insertProduct(second)
        } catch (_: IllegalStateException) {
            failed = true
        }
        assertTrue(failed)

        val active = dao.getAllProductsSync().filter { !it.isDeleted }
        assertEquals(1, active.size)
    }

    @Test
    fun camera_and_hid_share_same_ambiguous_barcode_resolution() = runTest {
        val p1 = Product(
            id = 1,
            name = "A",
            category = "انگشتر",
            weightGram = BigDecimal.ONE,
            wagePrice = BigDecimal.ZERO,
            wageType = "FIXED",
            customBarcode = "LEGACY-DUP",
            stock = 1
        )
        val p2 = p1.copy(id = 2, name = "B")
        val resolution = BarcodeResolver.resolveProductExact("LEGACY-DUP", listOf(p1, p2))
        assertTrue(resolution is com.example.domain.util.ProductResolution.Ambiguous)
    }

    @Test
    fun weight_comparison_reports_signed_difference() {
        val result = WeightComparisonEngine.compare(
            expected = BigDecimal("3.42"),
            measured = BigDecimal("3.41"),
            tolerance = BigDecimal("0.005")
        )
        assertEquals(BigDecimal("-0.01"), result.differenceGrams)
        assertEquals(false, result.withinTolerance)
    }
}
