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

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    // --- PRODUCTS ---
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Int): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Query("UPDATE products SET stock = :newStock WHERE id = :productId")
    suspend fun updateProductStock(productId: Int, newStock: Int)

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
}
