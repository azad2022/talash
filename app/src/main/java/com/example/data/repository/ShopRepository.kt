package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.database.ShopDao
import com.example.data.database.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class ShopRepository(
    private val shopDao: ShopDao,
    private val context: android.content.Context,
    private val appDatabase: AppDatabase = AppDatabase.getInstance(context)
) {

    private val prefs = context.getSharedPreferences("gold_settings", android.content.Context.MODE_PRIVATE)

    fun getGoldPriceMode(): String {
        return prefs.getString("gold_price_mode", "OFFLINE") ?: "OFFLINE"
    }

    fun setGoldPriceMode(mode: String) {
        prefs.edit().putString("gold_price_mode", mode).apply()
    }

    fun getGoldPriceApiKey(): String {
        return prefs.getString("gold_price_api_key", "") ?: ""
    }

    fun setGoldPriceApiKey(key: String) {
        prefs.edit().putString("gold_price_api_key", key).apply()
    }

    fun getRate(key: String, default: Double): Double {
        return prefs.getFloat(key, default.toFloat()).toDouble()
    }

    fun setRate(key: String, value: Double) {
        prefs.edit().putFloat(key, value.toFloat()).apply()
    }

    suspend fun fetchOnlineRates(apiKey: String): Map<String, Double> {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val rates = mutableMapOf<String, Double>()
            
            // Set defaults in case server is slow or doesn't have certain keys
            rates["rate_gold_18k"] = 0.0
            rates["rate_gold_24k"] = 0.0
            rates["rate_gold_melted"] = 0.0
            rates["rate_gold_ounce"] = 0.0
            rates["rate_coin_1g"] = 0.0
            rates["rate_coin_quarter"] = 0.0
            rates["rate_coin_half"] = 0.0
            rates["rate_coin_emami"] = 0.0
            rates["rate_coin_bahar"] = 0.0
            rates["rate_currency_usd"] = 0.0
            rates["rate_currency_tether"] = 0.0
            rates["rate_currency_eur"] = 0.0
            rates["rate_currency_aed"] = 0.0
            rates["rate_currency_gbp"] = 0.0

            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            
            // Using exact URL structure provided by the user with standard browser User-Agent
            val request = okhttp3.Request.Builder()
                .url("https://api.brsapi.ir/Market/Gold_Currency.php?key=$apiKey")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "application/json")
                .build()
            
            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string() ?: ""
                        val json = org.json.JSONObject(bodyString)
                        
                        // Parse gold
                        val goldArray = json.optJSONArray("gold")
                        if (goldArray != null) {
                            for (i in 0 until goldArray.length()) {
                                val obj = goldArray.getJSONObject(i)
                                val symbol = obj.optString("symbol", "")
                                val price = obj.optDouble("price", 0.0)
                                if (price > 0) {
                                    if (symbol == "IR_GOLD_18K") rates["rate_gold_18k"] = price
                                    if (symbol == "IR_GOLD_24K") rates["rate_gold_24k"] = price
                                    if (symbol == "IR_GOLD_SHEM" || symbol == "IR_GOLD_MITHQAAL") rates["rate_gold_melted"] = price
                                    if (symbol == "ONS") rates["rate_gold_ounce"] = price
                                }
                            }
                        }

                        // Parse coins
                        val coinArray = json.optJSONArray("coin")
                        if (coinArray != null) {
                            for (i in 0 until coinArray.length()) {
                                val obj = coinArray.getJSONObject(i)
                                val symbol = obj.optString("symbol", "")
                                val price = obj.optDouble("price", 0.0)
                                if (price > 0) {
                                    if (symbol == "SEC_EMAMI") rates["rate_coin_emami"] = price
                                    if (symbol == "SEC_BAHAR") rates["rate_coin_bahar"] = price
                                    if (symbol == "SEC_NIM") rates["rate_coin_half"] = price
                                    if (symbol == "SEC_ROB") rates["rate_coin_quarter"] = price
                                    if (symbol == "SEC_GERAM") rates["rate_coin_1g"] = price
                                }
                            }
                        }

                        // Parse currency
                        val currencyArray = json.optJSONArray("currency")
                        if (currencyArray != null) {
                            for (i in 0 until currencyArray.length()) {
                                val obj = currencyArray.getJSONObject(i)
                                val symbol = obj.optString("symbol", "")
                                val price = obj.optDouble("price", 0.0)
                                if (price > 0) {
                                    if (symbol == "US_DOLLAR" || symbol == "USD") rates["rate_currency_usd"] = price
                                    if (symbol == "US_TETHER" || symbol == "USDT") rates["rate_currency_tether"] = price
                                    if (symbol == "EURO" || symbol == "EUR") rates["rate_currency_eur"] = price
                                    if (symbol == "AED") rates["rate_currency_aed"] = price
                                    if (symbol == "GBP") rates["rate_currency_gbp"] = price
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Return defaults on connection failure
                e.printStackTrace()
            }
            rates
        }
    }

    suspend fun fetchOnlineGoldPrice(apiKey: String): Double {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            
            // Using exact URL structure provided by the user with standard browser User-Agent
            val request = okhttp3.Request.Builder()
                .url("https://api.brsapi.ir/Market/Gold_Currency.php?key=$apiKey")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "application/json")
                .build()
            
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("پاسخ ناموفق سرور با کد متناظر ${response.code}")
                }
                
                val bodyString = response.body?.string() ?: throw Exception("پاسخ دریافتی خالی است.")
                
                val json = try {
                    org.json.JSONObject(bodyString)
                } catch (e: Exception) {
                    throw Exception("فرمت پاسخ نامعتبر است (غیر JSON).")
                }
                
                // Handle API error fields if any
                if (json.has("error") || (json.has("status") && json.optString("status") == "error")) {
                    val err = json.optString("error", json.optString("message", "خطای وب‌سرویس BarsApi"))
                    throw Exception(err)
                }
                
                val goldArray = json.optJSONArray("gold")
                if (goldArray != null) {
                    for (i in 0 until goldArray.length()) {
                        val obj = goldArray.getJSONObject(i)
                        if (obj.optString("symbol") == "IR_GOLD_18K") {
                            val price = obj.optDouble("price", -1.0)
                            if (price > 0) {
                                return@withContext price
                            }
                        }
                    }
                    throw Exception("نماد طلا IR_GOLD_18K در لیست موجود نیست.")
                } else {
                    throw Exception("اطلاعات طلا در ساختار خروجی یافت نشد.")
                }
            }
        }
    }

    // --- USER CONFIG ---
    val userConfig: Flow<User?> = shopDao.getUserFlow()

    suspend fun getOrInitializeUser(): User {
        val existing = shopDao.getUserSync()
        if (existing == null) {
            val newUser = User()
            shopDao.insertUser(newUser)
            return newUser
        }
        return existing
    }

    suspend fun updateUser(user: User) {
        shopDao.insertUser(user)
        logAction("UPDATE_CONFIG", "تغییر تنظیمات سیستم و قیمت مبنای طلا")
    }

    // --- CUSTOMERS ---
    val customers: Flow<List<Customer>> = shopDao.getAllCustomers()

    suspend fun insertCustomer(customer: Customer): Long {
        val id = shopDao.insertCustomer(customer)
        val action = if (customer.id == 0) "ثبت مشتری جدید: " else "ویرایش مشخصات مشتری: "
        logAction(if (customer.id == 0) "ADD_CUSTOMER" else "EDIT_CUSTOMER", "$action ${customer.name}")
        return id
    }

    suspend fun deleteCustomer(customer: Customer) {
        shopDao.deleteCustomer(customer)
        logAction("DELETE_CUSTOMER", "حذف مشتری و سوابق: ${customer.name}")
    }

    // --- PRODUCTS (INVENTORY) ---
    val products: Flow<List<Product>> = shopDao.getAllProducts()

    suspend fun insertProduct(product: Product): Long {
        val id = shopDao.insertProduct(product)
        val action = if (product.id == 0) "افزودن کالا به انبار: " else "بروزرسانی مشخصات کالا: "
        logAction(if (product.id == 0) "ADD_PRODUCT" else "EDIT_PRODUCT", "$action ${product.name} (${product.weightGram} گرم)")
        return id
    }

    suspend fun deleteProduct(product: Product) {
        shopDao.deleteProduct(product)
        logAction("DELETE_PRODUCT", "حذف کالا از انبار: ${product.name}")
    }

    // --- INVOICES (SALES TRANSACTION) ---
    val invoices: Flow<List<InvoiceWithDetails>> = shopDao.getInvoicesWithDetails()

    fun getInvoiceById(id: Int): Flow<InvoiceWithDetails?> = shopDao.getInvoiceWithDetailsById(id)

    suspend fun createInvoice(
        invoice: SaleInvoice,
        items: List<SaleItem>,
        installments: List<Installment>
    ): Long {
        if (items.isEmpty()) {
            throw IllegalArgumentException("فاکتور باید حداقل شامل یک کالا باشد.")
        }

        return appDatabase.withTransaction {
            // Validate stock and decrease stock atomically for all items first
            items.forEach { item ->
                if (item.quantity <= 0) {
                    throw IllegalArgumentException("تعداد درخواست شده برای کالا باید بیشتر از صفر باشد.")
                }

                val prod = shopDao.getProductById(item.productId)
                    ?: throw IllegalStateException("کالای مورد نظر یافت نشد.")

                if (item.quantity > prod.stock) {
                    throw IllegalStateException("موجودی کالا (${prod.name}) کافی نیست. موجودی فعلی: ${prod.stock}، مقدار درخواستی: ${item.quantity}")
                }

                val updatedRows = shopDao.decreaseProductStock(prod.id, item.quantity)
                if (updatedRows == 0) {
                    throw IllegalStateException("موجودی کالا (${prod.name}) کافی نیست یا همزمان تغییر کرده است.")
                }
            }

            // Save central invoice
            val invoiceId = shopDao.insertInvoice(invoice).toInt()

            // Save all sub-items
            items.forEach { item ->
                val finalItem = item.copy(invoiceId = invoiceId)
                shopDao.insertSaleItem(finalItem)
            }

            // Save installments if it is an installment payment
            if (invoice.paymentType == "INSTALLMENT") {
                installments.forEach { installment ->
                    val finalInstallment = installment.copy(invoiceId = invoiceId)
                    shopDao.insertInstallment(finalInstallment)
                }
            }

            logAction("CREATE_INVOICE", "صدور فاکتور شماره $invoiceId به مبلغ ${invoice.totalAmount} تومان")
            invoiceId.toLong()
        }
    }

    suspend fun deleteInvoice(invoice: SaleInvoice) {
        appDatabase.withTransaction {
            shopDao.getItemsForInvoice(invoice.id).firstOrNull()?.forEach { item ->
                val prod = shopDao.getProductById(item.productId)
                if (prod != null) {
                    shopDao.updateProductStock(prod.id, prod.stock + item.quantity)
                }
            }
            shopDao.deleteSaleItemsForInvoice(invoice.id)
            shopDao.deleteInstallmentsForInvoice(invoice.id)
            shopDao.deleteInvoice(invoice)
            logAction("DELETE_INVOICE", "حذف فاکتور شماره ${invoice.id} و بازگردانی اقلام به انبار")
        }
    }

    // --- INSTALLMENTS ---
    val installments: Flow<List<Installment>> = shopDao.getAllInstallments()

    suspend fun payInstallment(installmentId: Int, paid: Boolean) {
        val now = if (paid) System.currentTimeMillis() else null
        shopDao.updateInstallmentPayment(installmentId, paid, now)
        logAction("PAY_INSTALLMENT", "ثبت وضعیت پرداخت قسط کد $installmentId به مقدار: $paid")
    }

    // --- REPAIRS ---
    val repairs: Flow<List<RepairWithCustomer>> = shopDao.getAllRepairs()

    suspend fun insertRepair(repair: Repair): Long {
        val id = shopDao.insertRepair(repair)
        val action = if (repair.id == 0) "ثبت سفارش تعمیر جدید" else "ویرایش سفارش تعمیر"
        logAction("REPAIR_RECORD", "$action: ${repair.description}")
        return id
    }

    suspend fun updateRepairStatus(repairId: Int, status: String) {
        val deliveredTime = if (status == "DELIVERED") System.currentTimeMillis() else null
        shopDao.updateRepairStatus(repairId, status, deliveredTime)
        logAction("REPAIR_STATUS", "تغییر وضعیت تعمیر کد $repairId به $status")
    }

    suspend fun deleteRepair(repair: Repair) {
        shopDao.deleteRepair(repair)
        logAction("REPAIR_DELETE", "حذف سفارش تعمیر کد ${repair.id}")
    }

    // --- AUDIT LOGS ---
    val logs: Flow<List<AuditLog>> = shopDao.getAllLogs()

    suspend fun logAction(action: String, details: String) {
        val log = AuditLog(action = action, details = details)
        shopDao.insertLog(log)
    }

    // --- BACKUP & RESTORE ---
    private val backupRestoreUseCase = com.example.domain.usecase.BackupRestoreUseCase()

    suspend fun exportBackupJson(): String {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val customersList = shopDao.getAllCustomersSync()
            val productsList = shopDao.getAllProductsSync()
            val invoicesList = shopDao.getAllInvoicesSync()
            val itemsList = shopDao.getAllSaleItemsSync()
            val installmentsList = shopDao.getAllInstallmentsSync()
            val repairsList = shopDao.getAllRepairsSync()
            val goldHistory = shopDao.getGoldHistorySync()
            val logsList = shopDao.getAllLogsSync()
            val currentUser = shopDao.getUserSync()

            val backupData = com.example.domain.usecase.BackupData(
                customers = customersList,
                products = productsList,
                invoices = invoicesList,
                saleItems = itemsList,
                installments = installmentsList,
                repairs = repairsList,
                goldPriceHistory = goldHistory,
                auditLogs = logsList,
                userConfig = currentUser
            )

            val json = backupRestoreUseCase.exportToJson(backupData)
            logAction("BACKUP_EXPORT", "پشتیبان‌گیری کامل از داده‌ها به فرمت JSON انجام شد")
            json
        }
    }

    suspend fun importRestoreJson(jsonString: String): Result<Unit> {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val parseResult = backupRestoreUseCase.parseFromJson(jsonString)
            if (parseResult.isFailure) {
                return@withContext Result.failure(parseResult.exceptionOrNull() ?: Exception("خطای نامشخص در بازخوانی پشتیبان"))
            }

            val backupData = parseResult.getOrThrow()

            try {
                appDatabase.withTransaction {
                    shopDao.clearSaleItems()
                    shopDao.clearInstallments()
                    shopDao.clearInvoices()
                    shopDao.clearRepairs()
                    shopDao.clearProducts()
                    shopDao.clearCustomers()
                    shopDao.clearGoldHistory()
                    shopDao.clearLogs()

                    backupData.customers.forEach { shopDao.insertCustomer(it) }
                    backupData.products.forEach { shopDao.insertProduct(it) }
                    backupData.invoices.forEach { shopDao.insertInvoice(it) }
                    backupData.saleItems.forEach { shopDao.insertSaleItem(it) }
                    backupData.installments.forEach { shopDao.insertInstallment(it) }
                    backupData.repairs.forEach { shopDao.insertRepair(it) }
                    backupData.goldPriceHistory.forEach { shopDao.insertGoldPriceHistory(it) }
                    backupData.auditLogs.forEach { shopDao.insertLog(it) }
                    backupData.userConfig?.let { shopDao.insertUser(it) }

                    shopDao.insertLog(
                        AuditLog(
                            action = "BACKUP_RESTORE",
                            details = "بازیابی کامل داده‌ها از فایل پشتیبان نسخه ${backupData.version} انجام شد"
                        )
                    )
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // --- RESET ---
    suspend fun clearAllData() {
        shopDao.clearCustomers()
        shopDao.clearProducts()
        shopDao.clearInvoices()
        shopDao.clearSaleItems()
        shopDao.clearInstallments()
        shopDao.clearRepairs()
        shopDao.clearGoldHistory()
        shopDao.clearLogs()
        // recheck or initialize user config
        shopDao.insertUser(User())
    }
}
