package com.example.data.database

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopDao {

    // --- USER CONFIG ---
    @Query("SELECT * FROM users WHERE id = 1 LIMIT 1")
    fun getUserFlow(): Flow<User?>

    @Query("SELECT * FROM users WHERE id = 1 LIMIT 1")
    suspend fun getUserSync(): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    // --- CUSTOMERS ---
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: Int): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Query("SELECT COUNT(*) FROM sale_invoices WHERE customerId = :customerId")
    suspend fun getInvoiceCountForCustomer(customerId: Int): Int

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    // --- PRODUCTS ---
    @Query("SELECT * FROM products WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Int): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Query("UPDATE products SET stock = :newStock WHERE id = :productId")
    suspend fun updateProductStock(productId: Int, newStock: Int)

    @Query("UPDATE products SET stock = stock - :quantity WHERE id = :productId AND stock >= :quantity")
    suspend fun decreaseProductStock(productId: Int, quantity: Int): Int

    @Query("UPDATE products SET isDeleted = 1 WHERE id = :productId")
    suspend fun softDeleteProduct(productId: Int)

    @Query("UPDATE products SET isDeleted = 0 WHERE id = :productId")
    suspend fun unarchiveProduct(productId: Int)

    @Query("SELECT COUNT(*) FROM sale_items WHERE productId = :productId")
    suspend fun getSaleItemCountForProduct(productId: Int): Int

    @Delete
    suspend fun deleteProduct(product: Product)

    // --- SALE INVOICES ---
    @Query("SELECT * FROM sale_invoices ORDER BY date DESC")
    fun getAllInvoices(): Flow<List<SaleInvoice>>

    @Transaction
    @Query("SELECT * FROM sale_invoices ORDER BY date DESC")
    fun getInvoicesWithDetails(): Flow<List<InvoiceWithDetails>>

    @Transaction
    @Query("SELECT * FROM sale_invoices WHERE id = :id LIMIT 1")
    fun getInvoiceWithDetailsById(id: Int): Flow<InvoiceWithDetails?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: SaleInvoice): Long

    @Update
    suspend fun updateInvoice(invoice: SaleInvoice)

    @Delete
    suspend fun deleteInvoice(invoice: SaleInvoice)

    // --- SALE ITEMS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItem(item: SaleItem)

    @Query("SELECT * FROM sale_items WHERE invoiceId = :invoiceId")
    fun getItemsForInvoice(invoiceId: Int): Flow<List<SaleItem>>

    // --- INSTALLMENTS ---
    @Query("SELECT * FROM installments ORDER BY dueDate ASC")
    fun getAllInstallments(): Flow<List<Installment>>

    @Query("UPDATE installments SET paid = :paid, paymentDate = :paymentDate WHERE id = :installmentId")
    suspend fun updateInstallmentPayment(installmentId: Int, paid: Boolean, paymentDate: Long?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallment(installment: Installment)

    // --- REPAIRS ---
    @Transaction
    @Query("SELECT * FROM repairs ORDER BY createdAt DESC")
    fun getAllRepairs(): Flow<List<RepairWithCustomer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepair(repair: Repair): Long

    @Query("UPDATE repairs SET status = :status, deliveredAt = :deliveredAt WHERE id = :repairId")
    suspend fun updateRepairStatus(repairId: Int, status: String, deliveredAt: Long?)

    @Delete
    suspend fun deleteRepair(repair: Repair)

    // --- GOLD PRICE HISTORY ---
    @Query("SELECT * FROM gold_price_history ORDER BY date DESC")
    fun getGoldPriceHistory(): Flow<List<GoldPriceHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoldPriceHistory(history: GoldPriceHistory)

    // --- AUDIT LOGS ---
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLog)

    // --- SYNC QUERIES FOR BACKUP & RESTORE ---
    @Query("SELECT * FROM customers")
    suspend fun getAllCustomersSync(): List<Customer>

    @Query("SELECT * FROM products")
    suspend fun getAllProductsSync(): List<Product>

    @Query("SELECT * FROM sale_invoices")
    suspend fun getAllInvoicesSync(): List<SaleInvoice>

    @Query("SELECT * FROM sale_items")
    suspend fun getAllSaleItemsSync(): List<SaleItem>

    @Query("SELECT * FROM installments")
    suspend fun getAllInstallmentsSync(): List<Installment>

    @Query("SELECT * FROM repairs")
    suspend fun getAllRepairsSync(): List<Repair>

    @Query("SELECT * FROM gold_price_history")
    suspend fun getGoldHistorySync(): List<GoldPriceHistory>

    @Query("SELECT * FROM audit_logs")
    suspend fun getAllLogsSync(): List<AuditLog>

    @Query("DELETE FROM sale_items WHERE invoiceId = :invoiceId")
    suspend fun deleteSaleItemsForInvoice(invoiceId: Int)

    @Query("DELETE FROM installments WHERE invoiceId = :invoiceId")
    suspend fun deleteInstallmentsForInvoice(invoiceId: Int)

    // --- RESET ---
    @Query("DELETE FROM customers")
    suspend fun clearCustomers()

    @Query("DELETE FROM products")
    suspend fun clearProducts()

    @Query("DELETE FROM sale_invoices")
    suspend fun clearInvoices()

    @Query("DELETE FROM sale_items")
    suspend fun clearSaleItems()

    @Query("DELETE FROM installments")
    suspend fun clearInstallments()

    @Query("DELETE FROM repairs")
    suspend fun clearRepairs()

    @Query("DELETE FROM gold_price_history")
    suspend fun clearGoldHistory()

    @Query("DELETE FROM audit_logs")
    suspend fun clearLogs()

    // --- DAILY CLOSINGS ---
    @Query("SELECT * FROM daily_closings ORDER BY closedAt DESC")
    fun getAllDailyClosings(): Flow<List<DailyClosing>>

    @Query("SELECT * FROM daily_closings WHERE id = :id LIMIT 1")
    fun getDailyClosingById(id: Long): Flow<DailyClosing?>

    @Query("SELECT * FROM daily_closings WHERE businessDateKey = :businessDateKey ORDER BY revision DESC, closedAt DESC LIMIT 1")
    suspend fun getLatestDailyClosingForDateSync(businessDateKey: String): DailyClosing?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyClosing(dailyClosing: DailyClosing): Long

    @Update
    suspend fun updateDailyClosing(dailyClosing: DailyClosing)

    @Query("SELECT * FROM daily_closings")
    suspend fun getAllDailyClosingsSync(): List<DailyClosing>

    @Query("DELETE FROM daily_closings")
    suspend fun clearDailyClosings()

    // --- STOCK TAKE SESSIONS & ITEMS ---
    @Query("SELECT * FROM stock_take_sessions WHERE status IN ('IN_PROGRESS', 'REVIEW_REQUIRED') ORDER BY startedAt DESC LIMIT 1")
    fun getActiveStockTakeSession(): Flow<StockTakeSession?>

    @Query("SELECT * FROM stock_take_sessions WHERE status IN ('IN_PROGRESS', 'REVIEW_REQUIRED') ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveStockTakeSessionSync(): StockTakeSession?

    @Query("SELECT * FROM stock_take_sessions ORDER BY startedAt DESC")
    fun getAllStockTakeSessions(): Flow<List<StockTakeSession>>

    @Query("SELECT * FROM stock_take_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getStockTakeSessionById(sessionId: Long): StockTakeSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockTakeSession(session: StockTakeSession): Long

    @Update
    suspend fun updateStockTakeSession(session: StockTakeSession)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockTakeItems(items: List<StockTakeItem>)

    @Update
    suspend fun updateStockTakeItem(item: StockTakeItem)

    @Query("SELECT * FROM stock_take_items WHERE sessionId = :sessionId ORDER BY id ASC")
    fun getStockTakeItemsForSession(sessionId: Long): Flow<List<StockTakeItem>>

    @Query("SELECT * FROM stock_take_items WHERE sessionId = :sessionId ORDER BY id ASC")
    suspend fun getStockTakeItemsForSessionSync(sessionId: Long): List<StockTakeItem>

    @Query("SELECT * FROM stock_take_items WHERE sessionId = :sessionId AND productBarcode = :barcode LIMIT 1")
    suspend fun getStockTakeItemByBarcode(sessionId: Long, barcode: String): StockTakeItem?

    @Query("SELECT * FROM stock_take_items WHERE sessionId = :sessionId AND productId = :productId LIMIT 1")
    suspend fun getStockTakeItemByProduct(sessionId: Long, productId: Int): StockTakeItem?

    @Query("SELECT * FROM stock_take_sessions")
    suspend fun getAllStockTakeSessionsSync(): List<StockTakeSession>

    @Query("SELECT * FROM stock_take_items")
    suspend fun getAllStockTakeItemsSync(): List<StockTakeItem>

    @Query("DELETE FROM stock_take_sessions")
    suspend fun clearStockTakeSessions()

    @Query("DELETE FROM stock_take_items")
    suspend fun clearStockTakeItems()
}
