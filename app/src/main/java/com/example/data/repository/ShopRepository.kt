package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.database.ShopDao
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.example.domain.util.BarcodeResolver
import com.example.ui.util.JalaliCalendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.Calendar
import java.util.Locale

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
        val str = prefs.getString(key, null)
        if (str != null) {
            return str.toDoubleOrNull() ?: default
        }
        return try {
            prefs.getFloat(key, default.toFloat()).toDouble()
        } catch (e: Exception) {
            default
        }
    }

    fun setRate(key: String, value: Double) {
        prefs.edit().putString(key, BigDecimal.valueOf(value).toPlainString()).apply()
    }

    suspend fun fetchOnlineRates(apiKey: String): Map<String, Double> {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val rates = mutableMapOf<String, Double>()
            
            // Set defaults to existing stored rates rather than 0.0 to prevent wiping valid financial rates on connection failure
            rates["rate_gold_18k"] = getRate("rate_gold_18k", 0.0)
            rates["rate_gold_24k"] = getRate("rate_gold_24k", 0.0)
            rates["rate_gold_melted"] = getRate("rate_gold_melted", 0.0)
            rates["rate_gold_ounce"] = getRate("rate_gold_ounce", 0.0)
            rates["rate_coin_1g"] = getRate("rate_coin_1g", 0.0)
            rates["rate_coin_quarter"] = getRate("rate_coin_quarter", 0.0)
            rates["rate_coin_half"] = getRate("rate_coin_half", 0.0)
            rates["rate_coin_emami"] = getRate("rate_coin_emami", 0.0)
            rates["rate_coin_bahar"] = getRate("rate_coin_bahar", 0.0)
            rates["rate_currency_usd"] = getRate("rate_currency_usd", 0.0)
            rates["rate_currency_tether"] = getRate("rate_currency_tether", 0.0)
            rates["rate_currency_eur"] = getRate("rate_currency_eur", 0.0)
            rates["rate_currency_aed"] = getRate("rate_currency_aed", 0.0)
            rates["rate_currency_gbp"] = getRate("rate_currency_gbp", 0.0)

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
        val invoiceCount = shopDao.getInvoiceCountForCustomer(customer.id)
        if (invoiceCount > 0) {
            logAction("DELETE_CUSTOMER_BLOCKED", "حذف مشتری «${customer.name}» به دلیل داشتن $invoiceCount فاکتور مالی مسدود گردید.")
            throw IllegalStateException("مشتری «${customer.name}» دارای سابقه فاکتور است و به دلایل قانونی و مالی امکان حذف فیزیکی آن وجود ندارد.")
        }
        shopDao.deleteCustomer(customer)
        logAction("DELETE_CUSTOMER", "حذف مشتری بدون سابقه فروش: ${customer.name}")
    }

    // --- PRODUCTS (INVENTORY) ---
    val products: Flow<List<Product>> = shopDao.getAllProducts()

    suspend fun insertProduct(product: Product): Long {
        return appDatabase.withTransaction {
            val nowTs = System.currentTimeMillis()
            if (isBusinessDateClosed(nowTs)) {
                val dateKey = BusinessDayUtils.getBusinessDateKey(nowTs)
                throw IllegalStateException("روز کاری جاری ($dateKey) بسته شده است و امکان تغییر در موجودی یا لیست کالاها وجود ندارد. ابتدا باید روز را بازگشایی کنید.")
            }
            val id = shopDao.insertProduct(product)
            val action = if (product.id == 0) "افزودن کالا به انبار: " else "بروزرسانی مشخصات کالا: "
            logAction(if (product.id == 0) "ADD_PRODUCT" else "EDIT_PRODUCT", "$action ${product.name} (${product.weightGram} گرم)")

            // Concurrency guard: If a stock take session is active, flag it as REVIEW_REQUIRED
            val activeSession = shopDao.getActiveStockTakeSessionSync()
            if (activeSession != null && activeSession.status == "IN_PROGRESS") {
                shopDao.updateStockTakeSession(activeSession.copy(status = "REVIEW_REQUIRED"))
                logAction(
                    "STOCK_TAKE_SESSION_FLAGGED_REVIEW",
                    "کالای «${product.name}» حین نشست انبارگردانی #${activeSession.id} ثبت یا ویرایش شد. وضعیت نشست به REVIEW_REQUIRED تغییر یافت."
                )
            }
            id
        }
    }

    suspend fun deleteProduct(product: Product) {
        appDatabase.withTransaction {
            val nowTs = System.currentTimeMillis()
            if (isBusinessDateClosed(nowTs)) {
                val dateKey = BusinessDayUtils.getBusinessDateKey(nowTs)
                throw IllegalStateException("روز کاری جاری ($dateKey) بسته شده است و امکان حذف کالا از انبار وجود ندارد. ابتدا باید روز را بازگشایی کنید.")
            }
            val saleCount = shopDao.getSaleItemCountForProduct(product.id)
            if (saleCount > 0) {
                shopDao.softDeleteProduct(product.id)
                logAction("SOFT_DELETE_PRODUCT", "آرشیو کالا به دلیل داشتن سابقه فروش: ${product.name}")
            } else {
                shopDao.deleteProduct(product)
                logAction("DELETE_PRODUCT", "حذف کالا از انبار: ${product.name}")
            }

            val activeSession = shopDao.getActiveStockTakeSessionSync()
            if (activeSession != null && activeSession.status == "IN_PROGRESS") {
                shopDao.updateStockTakeSession(activeSession.copy(status = "REVIEW_REQUIRED"))
            }
        }
    }

    suspend fun unarchiveProduct(productId: Int) {
        appDatabase.withTransaction {
            val nowTs = System.currentTimeMillis()
            if (isBusinessDateClosed(nowTs)) {
                val dateKey = BusinessDayUtils.getBusinessDateKey(nowTs)
                throw IllegalStateException("روز کاری جاری ($dateKey) بسته شده است و امکان بازگردانی کالا به انبار وجود ندارد. ابتدا باید روز را بازگشایی کنید.")
            }
            shopDao.unarchiveProduct(productId)
            logAction("UNARCHIVE_PRODUCT", "خروج کالا از حالت آرشیو با شناسه: $productId")

            val activeSession = shopDao.getActiveStockTakeSessionSync()
            if (activeSession != null && activeSession.status == "IN_PROGRESS") {
                shopDao.updateStockTakeSession(activeSession.copy(status = "REVIEW_REQUIRED"))
            }
        }
    }

    suspend fun updateProductStock(productId: Int, newStock: Int) {
        appDatabase.withTransaction {
            val nowTs = System.currentTimeMillis()
            if (isBusinessDateClosed(nowTs)) {
                val dateKey = BusinessDayUtils.getBusinessDateKey(nowTs)
                throw IllegalStateException("روز کاری جاری ($dateKey) بسته شده است و امکان تغییر مستقیم موجودی کالا وجود ندارد. ابتدا باید روز را بازگشایی کنید.")
            }
            shopDao.updateProductStock(productId, newStock)
            logAction("UPDATE_PRODUCT_STOCK", "تغییر مستقیم موجودی کالای کد $productId به $newStock")

            val activeSession = shopDao.getActiveStockTakeSessionSync()
            if (activeSession != null && activeSession.status == "IN_PROGRESS") {
                shopDao.updateStockTakeSession(activeSession.copy(status = "REVIEW_REQUIRED"))
            }
        }
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
            // Atomic business-day lock check INSIDE transaction!
            if (isBusinessDateClosed(invoice.date)) {
                val dateKey = BusinessDayUtils.getBusinessDateKey(invoice.date)
                throw IllegalStateException("روز کاری این فاکتور ($dateKey) بسته شده است و امکان ثبت فاکتور جدید وجود ندارد. برای انجام عملیات، ابتدا روز را بازگشایی کنید.")
            }

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
            val allInvoices = shopDao.getAllInvoicesSync()
            val currentInvoice = allInvoices.firstOrNull { it.id == invoice.id } ?: invoice
            val targetDate = currentInvoice.date

            // Atomic business-day lock check INSIDE transaction!
            if (isBusinessDateClosed(targetDate)) {
                val dateKey = BusinessDayUtils.getBusinessDateKey(targetDate)
                throw IllegalStateException("روز کاری این فاکتور ($dateKey) بسته شده است و امکان ابطال آن وجود ندارد. برای انجام عملیات، ابتدا روز را بازگشایی کنید.")
            }

            if (currentInvoice.paymentType.startsWith("CANCELLED") || currentInvoice.paymentType == "VOID") {
                throw IllegalStateException("این فاکتور قبلاً ابطال شده است و امکان ابطال مجدد ندارد.")
            }

            shopDao.getItemsForInvoice(currentInvoice.id).firstOrNull()?.forEach { item ->
                val prod = shopDao.getProductById(item.productId)
                if (prod != null) {
                    shopDao.updateProductStock(prod.id, prod.stock + item.quantity)
                }
            }
            // Preserve historical invoice, sale items, and installments; mark status as CANCELLED
            val cancelledInvoice = currentInvoice.copy(paymentType = "CANCELLED_${currentInvoice.paymentType}")
            shopDao.updateInvoice(cancelledInvoice)
            logAction("CANCEL_INVOICE", "ابطال فاکتور شماره ${currentInvoice.id} و بازگردانی اقلام به انبار با حفظ کامل اسناد مالی")
        }
    }

    // --- INSTALLMENTS ---
    val installments: Flow<List<Installment>> = shopDao.getAllInstallments()

    suspend fun payInstallment(installmentId: Int, paid: Boolean) {
        appDatabase.withTransaction {
            val inst = shopDao.getInstallmentById(installmentId)
                ?: throw IllegalArgumentException("قسط با شناسه $installmentId یافت نشد.")

            val now = System.currentTimeMillis()
            val targetTimestamp = if (!paid && inst.paymentDate != null) inst.paymentDate!! else now

            if (isBusinessDateClosed(targetTimestamp)) {
                val dateKey = BusinessDayUtils.getBusinessDateKey(targetTimestamp)
                throw IllegalStateException("روز کاری متناظر ($dateKey) بسته شده است و امکان تغییر وضعیت یا دریافت قسط وجود ندارد. ابتدا روز را بازگشایی نمایید.")
            }

            val paymentDate = if (paid) now else null
            shopDao.updateInstallmentPayment(installmentId, paid, paymentDate)
            logAction("PAY_INSTALLMENT", "ثبت وضعیت پرداخت قسط کد $installmentId به مقدار: $paid")
        }
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

    // --- DAILY CLOSING ---
    val dailyClosings: Flow<List<DailyClosing>> = shopDao.getAllDailyClosings()

    fun getDailyClosingById(id: Long): Flow<DailyClosing?> = shopDao.getDailyClosingById(id)

    suspend fun isBusinessDateClosed(timestamp: Long): Boolean {
        val dateKey = BusinessDayUtils.getBusinessDateKey(timestamp)
        val latest = shopDao.getLatestDailyClosingForDateSync(dateKey)
        return latest != null && latest.status == "CLOSED"
    }

    private suspend fun calculateLiveSummary(now: Long): TodaySummaryPreview {
        val startOfDay = BusinessDayUtils.getStartOfDay(now)
        val endOfDay = BusinessDayUtils.getEndOfDay(now)
        val dateKey = BusinessDayUtils.getBusinessDateKey(now)
        val displayedPersian = JalaliCalendar.getJalaliDate(now)
        val displayedGregorian = BusinessDayUtils.getGregorianDateDisplay(now)

        val existingClosing = shopDao.getLatestDailyClosingForDateSync(dateKey)

        val allInvoices = shopDao.getAllInvoicesSync()
        val todayInvoices = allInvoices.filter {
            it.date in startOfDay..endOfDay && !it.paymentType.startsWith("CANCELLED")
        }
        val invoiceCount = todayInvoices.size
        val salesTotal = todayInvoices.fold(BigDecimal.ZERO) { acc, inv -> acc.add(inv.totalAmount) }
        val paidTotal = todayInvoices.fold(BigDecimal.ZERO) { acc, inv -> acc.add(inv.paidAmount) }

        val todayInvoiceIds = todayInvoices.map { it.id }.toSet()
        val allInstallments = shopDao.getAllInstallmentsSync()
        val todayCreatedInstallments = allInstallments.filter { todayInvoiceIds.contains(it.invoiceId) }
        val installmentCreatedTotal = todayCreatedInstallments.fold(BigDecimal.ZERO) { acc, inst -> acc.add(inst.amount) }
        val installmentCreatedCount = todayCreatedInstallments.size

        val todayCollectedInstallments = allInstallments.filter {
            it.paid && it.paymentDate != null && it.paymentDate in startOfDay..endOfDay
        }
        val installmentCollectedTotal = todayCollectedInstallments.fold(BigDecimal.ZERO) { acc, inst -> acc.add(inst.amount) }

        val overdueInstallmentCount = allInstallments.count { !it.paid && it.dueDate < now }

        val allProducts = shopDao.getAllProductsSync().filter { !it.isDeleted }
        val inventoryPieceCount = allProducts.sumOf { it.stock }
        val inventoryWeight = allProducts.fold(BigDecimal.ZERO) { acc, p ->
            acc.add(p.weightGram.multiply(BigDecimal.valueOf(p.stock.toLong())))
        }

        val userConfig = shopDao.getUserSync()
        val dailyGoldPrice = userConfig?.dailyGoldPrice ?: BigDecimal.ZERO
        val taxPercent = userConfig?.taxPercent ?: BigDecimal("9.0")

        val r24k = BigDecimal.valueOf(getRate("rate_gold_24k", 0.0))
        val rMelted = BigDecimal.valueOf(getRate("rate_gold_melted", 0.0))
        val rOunce = BigDecimal.valueOf(getRate("rate_gold_ounce", 0.0))
        val r1g = BigDecimal.valueOf(getRate("rate_coin_1g", 0.0))
        val rQuarter = BigDecimal.valueOf(getRate("rate_coin_quarter", 0.0))
        val rHalf = BigDecimal.valueOf(getRate("rate_coin_half", 0.0))
        val rEmami = BigDecimal.valueOf(getRate("rate_coin_emami", 0.0))
        val rBahar = BigDecimal.valueOf(getRate("rate_coin_bahar", 0.0))
        val rUsd = BigDecimal.valueOf(getRate("rate_currency_usd", 0.0))
        val rTether = BigDecimal.valueOf(getRate("rate_currency_tether", 0.0))
        val rEur = BigDecimal.valueOf(getRate("rate_currency_eur", 0.0))
        val rAed = BigDecimal.valueOf(getRate("rate_currency_aed", 0.0))
        val rGbp = BigDecimal.valueOf(getRate("rate_currency_gbp", 0.0))

        val inventoryValue = allProducts.fold(BigDecimal.ZERO) { acc, p ->
            val singleEst = p.calculateAssetValue(
                dailyPrice18k = dailyGoldPrice,
                rateGold24k = r24k,
                rateGoldMelted = rMelted,
                rateGoldOunce = rOunce,
                rateCoin1g = r1g,
                rateCoinQuarter = rQuarter,
                rateCoinHalf = rHalf,
                rateCoinEmami = rEmami,
                rateCoinBahar = rBahar,
                rateCurrencyUsd = rUsd,
                rateCurrencyTether = rTether,
                rateCurrencyEur = rEur,
                rateCurrencyAed = rAed,
                rateCurrencyGbp = rGbp,
                taxRate = taxPercent
            )
            acc.add(singleEst.multiply(BigDecimal.valueOf(p.stock.toLong())))
        }

        val lowStockCount = allProducts.count { it.stock <= it.minStock }

        val allRepairs = shopDao.getAllRepairsSync()
        val openRepairsCount = allRepairs.count { it.status in listOf("PENDING_APPROVAL", "UNDER_REPAIR") }
        val readyRepairsCount = allRepairs.count { it.status == "READY" }

        return TodaySummaryPreview(
            businessDateKey = dateKey,
            displayedPersianDate = displayedPersian,
            displayedGregorianDate = displayedGregorian,
            invoiceCount = invoiceCount,
            salesTotal = salesTotal,
            paidTotal = paidTotal,
            installmentCreatedTotal = installmentCreatedTotal,
            installmentCreatedCount = installmentCreatedCount,
            installmentCollectedTotal = installmentCollectedTotal,
            overdueInstallmentCount = overdueInstallmentCount,
            inventoryPieceCount = inventoryPieceCount,
            inventoryWeight = inventoryWeight,
            inventoryValue = inventoryValue,
            lowStockCount = lowStockCount,
            openRepairsCount = openRepairsCount,
            readyRepairsCount = readyRepairsCount,
            goldRateAtClose = dailyGoldPrice,
            existingClosing = existingClosing
        )
    }

    suspend fun getTodaySummaryPreview(now: Long = System.currentTimeMillis()): TodaySummaryPreview {
        return withContext(Dispatchers.IO) {
            calculateLiveSummary(now)
        }
    }

    suspend fun closeDay(
        summary: TodaySummaryPreview,
        physicalCash: BigDecimal?,
        physicalGoldWeight: BigDecimal?,
        notes: String?
    ): Result<DailyClosing> {
        return withContext(Dispatchers.IO) {
            try {
                val created = appDatabase.withTransaction {
                    val now = System.currentTimeMillis()

                    // Concurrency guard: check if any active Stock Take session is in progress
                    val activeStockTake = shopDao.getActiveStockTakeSessionSync()
                    if (activeStockTake != null) {
                        throw IllegalStateException("امکان بستن روز در حین نشست انبارگردانی فعال (شماره #${activeStockTake.id}) وجود ندارد. لطفاً ابتدا انبارگردانی را نهایی یا لغو نمایید.")
                    }

                    val dateKey = BusinessDayUtils.getBusinessDateKey(now)
                    val latest = shopDao.getLatestDailyClosingForDateSync(dateKey)
                    if (latest != null && latest.status == "CLOSED") {
                        throw IllegalStateException("روز کاری $dateKey قبلاً بسته شده است. برای اعمال تغییرات ابتدا روز را بازگشایی کنید.")
                    }

                    // Authoritative recalculation at the exact moment of closing inside the transaction
                    val live = calculateLiveSummary(now)
                    val revision = if (latest != null) latest.revision + 1 else 1

                    val closing = DailyClosing(
                        businessDateKey = live.businessDateKey,
                        closedAt = now,
                        displayedPersianDate = live.displayedPersianDate,
                        displayedGregorianDate = live.displayedGregorianDate,
                        invoiceCount = live.invoiceCount,
                        salesTotal = live.salesTotal,
                        paidTotal = live.paidTotal,
                        installmentCreatedTotal = live.installmentCreatedTotal,
                        installmentCreatedCount = live.installmentCreatedCount,
                        installmentCollectedTotal = live.installmentCollectedTotal,
                        overdueInstallmentCount = live.overdueInstallmentCount,
                        inventoryPieceCount = live.inventoryPieceCount,
                        inventoryWeight = live.inventoryWeight,
                        inventoryValue = live.inventoryValue,
                        lowStockCount = live.lowStockCount,
                        openRepairsCount = live.openRepairsCount,
                        readyRepairsCount = live.readyRepairsCount,
                        goldRateAtClose = live.goldRateAtClose,
                        optionalPhysicalCash = physicalCash,
                        optionalPhysicalGoldWeight = physicalGoldWeight,
                        optionalNotes = notes,
                        status = "CLOSED",
                        revision = revision
                    )

                    val id = shopDao.insertDailyClosing(closing)
                    val action = if (revision > 1) "DAILY_CLOSING_RE_CLOSED" else "DAILY_CLOSING_CREATED"
                    logAction(
                        action,
                        "بستن روز کاری ${live.displayedPersianDate} (نسخه $revision) با شناسه $id با موفقیت ثبت شد."
                    )
                    closing.copy(id = id)
                }
                Result.success(created)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun reopenDay(closingId: Long, reason: String = "بازگشایی توسط کاربر"): Result<DailyClosing> {
        val cleanReason = reason.trim()
        if (cleanReason.isEmpty()) {
            return Result.failure(IllegalArgumentException("ثبت دلیل برای بازگشایی روز الزامی است."))
        }
        return withContext(Dispatchers.IO) {
            try {
                val updated = appDatabase.withTransaction {
                    val existing = shopDao.getAllDailyClosingsSync().find { it.id == closingId }
                        ?: throw IllegalArgumentException("شناسه بستن روز یافت نشد: $closingId")

                    if (existing.status == "REOPENED") {
                        return@withTransaction existing
                    }

                    val updatedRecord = existing.copy(
                        status = "REOPENED",
                        reopenReason = cleanReason,
                        reopenedAt = System.currentTimeMillis()
                    )
                    shopDao.updateDailyClosing(updatedRecord)
                    logAction(
                        "DAILY_CLOSING_REOPENED",
                        "روز کاری ${existing.displayedPersianDate} (نسخه ${existing.revision}) بازگشایی شد. دلیل: $cleanReason"
                    )
                    updatedRecord
                }
                Result.success(updated)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // --- STOCK TAKE (MOBILE BARCODE INVENTORY AUDIT) ---
    val activeStockTakeSession: Flow<StockTakeSession?> = shopDao.getActiveStockTakeSession()
    val stockTakeSessions: Flow<List<StockTakeSession>> = shopDao.getAllStockTakeSessions()

    fun getStockTakeItems(sessionId: Long): Flow<List<StockTakeItem>> =
        shopDao.getStockTakeItemsForSession(sessionId)

    suspend fun startStockTakeSession(notes: String? = null): Result<StockTakeSession> {
        return withContext(Dispatchers.IO) {
            try {
                val session = appDatabase.withTransaction {
                    val now = System.currentTimeMillis()
                    if (isBusinessDateClosed(now)) {
                        val dateKey = BusinessDayUtils.getBusinessDateKey(now)
                        throw IllegalStateException("روز کاری جاری ($dateKey) بسته شده است و امکان شروع نشست انبارگردانی وجود ندارد. ابتدا روز را بازگشایی نمایید.")
                    }

                    val existingActive = shopDao.getActiveStockTakeSessionSync()
                    if (existingActive != null) {
                        return@withTransaction existingActive
                    }

                    val activeProducts = shopDao.getAllProductsSync().filter { !it.isDeleted }

                    // Barcode ambiguity check: find duplicate custom barcodes among active products
                    val duplicateBarcodes = BarcodeResolver.findDuplicateCustomBarcodes(activeProducts)
                    if (duplicateBarcodes.isNotEmpty()) {
                        throw IllegalStateException(
                            "امکان شروع انبارگردانی وجود ندارد زیرا بارکدهای تکراری در بین کالاهای فعال یافت شد: [${duplicateBarcodes.joinToString(", ")}]. لطفاً ابتدا بارکدهای تکراری را اصلاح نمایید."
                        )
                    }

                    val totalExpected = activeProducts.sumOf { it.stock }

                    val newSession = StockTakeSession(
                        startedAt = now,
                        status = "IN_PROGRESS",
                        notes = notes,
                        totalExpectedPieces = totalExpected,
                        totalCountedPieces = 0
                    )
                    val sessionId = shopDao.insertStockTakeSession(newSession)

                    val items = activeProducts.map { prod ->
                        StockTakeItem(
                            sessionId = sessionId,
                            productId = prod.id,
                            productName = prod.name,
                            productCategory = prod.category,
                            productBarcode = BarcodeResolver.getCanonicalBarcode(prod),
                            expectedStockAtStart = prod.stock,
                            countedStock = 0,
                            isCounted = false, // Explicitly not yet counted
                            systemStockAtFinalize = null,
                            difference = -prod.stock,
                            changedDuringSession = false,
                            status = "PENDING"
                        )
                    }
                    shopDao.insertStockTakeItems(items)

                    logAction(
                        "STOCK_TAKE_STARTED",
                        "نشست انبارگردانی شماره $sessionId با $totalExpected قطعه موجودی مورد انتظار آغاز گردید."
                    )
                    newSession.copy(id = sessionId)
                }
                Result.success(session)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun scanBarcodeForStockTake(sessionId: Long, barcode: String): Result<StockTakeScanResult> {
        return withContext(Dispatchers.IO) {
            try {
                val res = appDatabase.withTransaction {
                    val session = shopDao.getStockTakeSessionById(sessionId)
                        ?: throw IllegalArgumentException("نشست انبارگردانی یافت نشد")

                    if (session.status != "IN_PROGRESS" && session.status != "REVIEW_REQUIRED") {
                        throw IllegalStateException("این نشست در وضعیت ${session.status} است و امکان اسکن جدید ندارد.")
                    }

                    val allItems = shopDao.getStockTakeItemsForSessionSync(sessionId)
                    val item = BarcodeResolver.resolveStockTakeItem(barcode, allItems)
                        ?: return@withTransaction StockTakeScanResult.NotFound(barcode.trim())

                    val newCount = item.countedStock + 1
                    val updatedItem = item.copy(
                        countedStock = newCount,
                        isCounted = true,
                        difference = newCount - item.expectedStockAtStart
                    )
                    shopDao.updateStockTakeItem(updatedItem)

                    val newSessionTotal = session.totalCountedPieces + 1
                    shopDao.updateStockTakeSession(session.copy(totalCountedPieces = newSessionTotal))

                    StockTakeScanResult.Success(updatedItem, newSessionTotal)
                }
                Result.success(res)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun undoLastStockTakeScan(sessionId: Long, productId: Int): Result<StockTakeItem> {
        return withContext(Dispatchers.IO) {
            try {
                val updated = appDatabase.withTransaction {
                    val session = shopDao.getStockTakeSessionById(sessionId)
                        ?: throw IllegalArgumentException("نشست انبارگردانی یافت نشد")

                    val item = shopDao.getStockTakeItemByProduct(sessionId, productId)
                        ?: throw IllegalArgumentException("کالا در این انبارگردانی یافت نشد")

                    if (item.countedStock <= 0) {
                        return@withTransaction item
                    }

                    val newCount = item.countedStock - 1
                    val updatedItem = item.copy(
                        countedStock = newCount,
                        isCounted = true,
                        difference = newCount - item.expectedStockAtStart
                    )
                    shopDao.updateStockTakeItem(updatedItem)

                    if (session.totalCountedPieces > 0) {
                        shopDao.updateStockTakeSession(session.copy(totalCountedPieces = session.totalCountedPieces - 1))
                    }

                    updatedItem
                }
                Result.success(updated)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun manualUpdateStockTakeItemCount(
        sessionId: Long,
        productId: Int,
        newCountedStock: Int
    ): Result<StockTakeItem> {
        return withContext(Dispatchers.IO) {
            try {
                val updated = appDatabase.withTransaction {
                    val session = shopDao.getStockTakeSessionById(sessionId)
                        ?: throw IllegalArgumentException("نشست انبارگردانی یافت نشد")

                    if (session.status != "IN_PROGRESS" && session.status != "REVIEW_REQUIRED") {
                        throw IllegalStateException("این نشست در وضعیت ${session.status} است و امکان ویرایش شمارش ندارد.")
                    }

                    val item = shopDao.getStockTakeItemByProduct(sessionId, productId)
                        ?: throw IllegalArgumentException("کالا در این انبارگردانی یافت نشد")

                    val safeCount = newCountedStock.coerceAtLeast(0)
                    val diffCount = if (item.isCounted) {
                        safeCount - item.countedStock
                    } else {
                        safeCount
                    }

                    val updatedItem = item.copy(
                        countedStock = safeCount,
                        isCounted = true,
                        difference = safeCount - item.expectedStockAtStart
                    )
                    shopDao.updateStockTakeItem(updatedItem)

                    val newTotal = (session.totalCountedPieces + diffCount).coerceAtLeast(0)
                    shopDao.updateStockTakeSession(session.copy(totalCountedPieces = newTotal))

                    updatedItem
                }
                Result.success(updated)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun prepareReconciliationReview(sessionId: Long): Result<List<StockTakeItem>> {
        return withContext(Dispatchers.IO) {
            try {
                val list = appDatabase.withTransaction {
                    val session = shopDao.getStockTakeSessionById(sessionId)
                        ?: throw IllegalArgumentException("نشست انبارگردانی یافت نشد: $sessionId")

                    val items = shopDao.getStockTakeItemsForSessionSync(sessionId).toMutableList()
                    val existingProductIds = items.map { it.productId }.toSet()

                    // Check for new products added to the shop during the active session
                    val allActiveProducts = shopDao.getAllProductsSync().filter { !it.isDeleted }
                    val newProducts = allActiveProducts.filter { !existingProductIds.contains(it.id) }
                    if (newProducts.isNotEmpty()) {
                        val newItems = newProducts.map { prod ->
                            StockTakeItem(
                                sessionId = sessionId,
                                productId = prod.id,
                                productName = prod.name,
                                productCategory = prod.category,
                                productBarcode = BarcodeResolver.getCanonicalBarcode(prod),
                                expectedStockAtStart = prod.stock,
                                countedStock = 0,
                                isCounted = false,
                                systemStockAtFinalize = prod.stock,
                                difference = -prod.stock,
                                changedDuringSession = true,
                                status = "NEW_PRODUCT_DURING_SESSION"
                            )
                        }
                        shopDao.insertStockTakeItems(newItems)
                        items.addAll(newItems)
                        if (session.status != "REVIEW_REQUIRED") {
                            shopDao.updateStockTakeSession(session.copy(status = "REVIEW_REQUIRED"))
                        }
                    }

                    val updatedList = mutableListOf<StockTakeItem>()

                    for (item in items) {
                        val currentProd = shopDao.getProductById(item.productId)
                        val currentSysStock = currentProd?.stock ?: 0
                        val changedDuring = (currentSysStock != item.expectedStockAtStart) || item.changedDuringSession

                        val status = when {
                            item.status == "NEW_PRODUCT_DURING_SESSION" -> "NEW_PRODUCT_DURING_SESSION"
                            !item.isCounted -> "UNCOUNTED"
                            changedDuring -> "NEEDS_REVIEW"
                            item.countedStock == currentSysStock -> "MATCHED"
                            else -> "DISCREPANCY"
                        }

                        val updatedItem = item.copy(
                            systemStockAtFinalize = currentSysStock,
                            difference = if (item.isCounted) item.countedStock - currentSysStock else -currentSysStock,
                            changedDuringSession = changedDuring,
                            status = status
                        )
                        shopDao.updateStockTakeItem(updatedItem)
                        updatedList.add(updatedItem)
                    }

                    val hasReviewFlags = updatedList.any {
                        it.status in listOf("UNCOUNTED", "NEEDS_REVIEW", "NEW_PRODUCT_DURING_SESSION")
                    }
                    if (hasReviewFlags && session.status == "IN_PROGRESS") {
                        shopDao.updateStockTakeSession(session.copy(status = "REVIEW_REQUIRED"))
                    }

                    updatedList
                }
                Result.success(list)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun resolveStockTakeItemReview(
        sessionId: Long,
        productId: Int,
        verifiedCount: Int
    ): Result<StockTakeItem> {
        return withContext(Dispatchers.IO) {
            try {
                val updated = appDatabase.withTransaction {
                    val session = shopDao.getStockTakeSessionById(sessionId)
                        ?: throw IllegalArgumentException("نشست انبارگردانی یافت نشد")

                    if (session.status != "IN_PROGRESS" && session.status != "REVIEW_REQUIRED") {
                        throw IllegalStateException("این نشست در وضعیت ${session.status} است و امکان بازبینی ندارد.")
                    }

                    val item = shopDao.getStockTakeItemByProduct(sessionId, productId)
                        ?: throw IllegalArgumentException("کالا در این انبارگردانی یافت نشد")

                    val currentProd = shopDao.getProductById(productId)
                        ?: throw IllegalArgumentException("کالای اصلی در انبار یافت نشد")

                    val safeCount = verifiedCount.coerceAtLeast(0)
                    val currentSysStock = currentProd.stock

                    // Update baseline to the current system stock and mark as counted and resolved
                    val resolvedItem = item.copy(
                        expectedStockAtStart = currentSysStock,
                        systemStockAtFinalize = currentSysStock,
                        countedStock = safeCount,
                        isCounted = true,
                        difference = safeCount - currentSysStock,
                        changedDuringSession = false,
                        status = if (safeCount == currentSysStock) "MATCHED" else "DISCREPANCY"
                    )
                    shopDao.updateStockTakeItem(resolvedItem)

                    // Recalculate session total counted pieces
                    val allItems = shopDao.getStockTakeItemsForSessionSync(sessionId)
                    val newTotal = allItems.sumOf { if (it.productId == productId) safeCount else it.countedStock }

                    // Check if any other items still require review
                    val remainingUnresolved = allItems.filter {
                        if (it.productId == productId) false
                        else (it.status == "NEEDS_REVIEW" || it.status == "NEW_PRODUCT_DURING_SESSION" || !it.isCounted)
                    }

                    val newSessionStatus = if (remainingUnresolved.isEmpty()) "IN_PROGRESS" else "REVIEW_REQUIRED"
                    shopDao.updateStockTakeSession(
                        session.copy(
                            totalCountedPieces = newTotal,
                            status = newSessionStatus
                        )
                    )

                    logAction(
                        "STOCK_TAKE_ITEM_RESOLVED",
                        "بازبینی و تأیید مبنای قلم «${item.productName}»: مبنای سیستم $currentSysStock، شمارش $safeCount"
                    )

                    resolvedItem
                }
                Result.success(updated)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun applyStockTakeAdjustments(sessionId: Long): Result<StockTakeApplyResult> {
        return withContext(Dispatchers.IO) {
            try {
                val result = appDatabase.withTransaction {
                    val now = System.currentTimeMillis()
                    if (isBusinessDateClosed(now)) {
                        val dateKey = BusinessDayUtils.getBusinessDateKey(now)
                        throw IllegalStateException("روز کاری جاری ($dateKey) بسته شده است و امکان تعدیل موجودی انبار وجود ندارد. ابتدا روز را بازگشایی نمایید.")
                    }

                    val session = shopDao.getStockTakeSessionById(sessionId)
                        ?: throw IllegalArgumentException("نشست انبارگردانی یافت نشد")

                    if (session.status == "COMPLETED") {
                        throw IllegalStateException("این انبارگردانی قبلاً اعمال و نهایی شده است.")
                    }

                    val items = shopDao.getStockTakeItemsForSessionSync(sessionId)

                    // 1. Guard against uncounted items: Cannot finalize while any item is uncounted
                    val uncounted = items.filter { !it.isCounted }
                    if (uncounted.isNotEmpty()) {
                        val sample = uncounted.first().productName
                        throw IllegalStateException(
                            "تعداد ${uncounted.size} قلم کالا هنوز شمارش نشده‌اند (مانند «$sample»). جهت نهایی‌سازی انبارگردانی، تمام اقلام باید شمرده شوند."
                        )
                    }

                    // 2. Guard against unresolved review items
                    val unresolved = items.filter { it.status == "NEEDS_REVIEW" || it.status == "NEW_PRODUCT_DURING_SESSION" }
                    if (unresolved.isNotEmpty()) {
                        throw IllegalStateException(
                            "تعداد ${unresolved.size} قلم کالا نیازمند بازبینی و تعیین تکلیف هستند (وضعیت نشست: REVIEW_REQUIRED). لطفاً ابتدا آنها را بازبینی نمایید."
                        )
                    }

                    // 3. Guard against concurrent modifications during session
                    var blockedCount = 0
                    val blockedItems = mutableListOf<String>()

                    for (item in items) {
                        val currentProd = shopDao.getProductById(item.productId)
                        val currentStock = currentProd?.stock ?: 0

                        if (currentStock != item.expectedStockAtStart) {
                            blockedCount++
                            blockedItems.add("${item.productName} (موجودی اولیه: ${item.expectedStockAtStart}، موجودی فعلی: $currentStock)")
                            val updated = item.copy(
                                changedDuringSession = true,
                                systemStockAtFinalize = currentStock,
                                difference = item.countedStock - currentStock,
                                status = "NEEDS_REVIEW"
                            )
                            shopDao.updateStockTakeItem(updated)
                        }
                    }

                    if (blockedCount > 0) {
                        shopDao.updateStockTakeSession(session.copy(status = "REVIEW_REQUIRED"))
                        logAction(
                            "STOCK_TAKE_FINAL_APPLY_BLOCKED",
                            "اعمال نهایی انبارگردانی #${session.id} به دلیل تغییر همزمان $blockedCount قلم کالا مسدود شد: [${blockedItems.take(3).joinToString("، ")}]."
                        )
                        throw IllegalStateException(
                            "تعداد $blockedCount قلم کالا حین انبارگردانی توسط سایر بخش‌های نرم‌افزار تغییر یافته‌اند: [${blockedItems.take(3).joinToString("، ")}]. وضعیت نشست به REVIEW_REQUIRED تغییر یافت و اعمال نهایی مسدود شد. لطفاً اقلام را مجدداً بازبینی فرمایید."
                        )
                    }

                    var adjustedCount = 0
                    var matchedCount = 0

                    for (item in items) {
                        val currentProd = shopDao.getProductById(item.productId)
                        val currentStock = currentProd?.stock ?: 0

                        if (item.countedStock == currentStock) {
                            matchedCount++
                            val updated = item.copy(
                                systemStockAtFinalize = currentStock,
                                difference = 0,
                                status = "MATCHED"
                            )
                            shopDao.updateStockTakeItem(updated)
                        } else {
                            shopDao.updateProductStock(item.productId, item.countedStock)
                            adjustedCount++
                            val updated = item.copy(
                                systemStockAtFinalize = currentStock,
                                difference = item.countedStock - currentStock,
                                status = "ADJUSTED"
                            )
                            shopDao.updateStockTakeItem(updated)
                            logAction(
                                "STOCK_ADJUSTMENT_APPLIED",
                                "تعدیل موجودی «${item.productName}» (کد ${item.productId}): از $currentStock به ${item.countedStock} در جلسه انبارگردانی $sessionId."
                            )
                        }
                    }

                    val completedSession = session.copy(
                        status = "COMPLETED",
                        completedAt = System.currentTimeMillis()
                    )
                    shopDao.updateStockTakeSession(completedSession)
                    logAction(
                        "STOCK_TAKE_COMPLETED",
                        "انبارگردانی شماره $sessionId با موفقیت نهایی شد. $adjustedCount قلم اصلاح شد و $matchedCount قلم منطبق بود."
                    )

                    StockTakeApplyResult(
                        adjustedCount = adjustedCount,
                        matchedCount = matchedCount,
                        blockedCount = 0,
                        completedSession = completedSession
                    )
                }
                Result.success(result)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun cancelStockTakeSession(sessionId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                appDatabase.withTransaction {
                    val session = shopDao.getStockTakeSessionById(sessionId)
                        ?: throw IllegalArgumentException("نشست انبارگردانی یافت نشد")

                    val cancelledSession = session.copy(
                        status = "CANCELLED",
                        completedAt = System.currentTimeMillis()
                    )
                    shopDao.updateStockTakeSession(cancelledSession)
                    logAction(
                        "STOCK_TAKE_CANCELLED",
                        "انبارگردانی شماره $sessionId توسط کاربر لغو گردید و موجودی‌ها دست‌نخورده باقی ماند."
                    )
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
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
            val dailyClosingsList = shopDao.getAllDailyClosingsSync()
            val stockTakeSessionsList = shopDao.getAllStockTakeSessionsSync()
            val stockTakeItemsList = shopDao.getAllStockTakeItemsSync()
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
                dailyClosings = dailyClosingsList,
                stockTakeSessions = stockTakeSessionsList,
                stockTakeItems = stockTakeItemsList,
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
                    shopDao.clearDailyClosings()
                    shopDao.clearStockTakeItems()
                    shopDao.clearStockTakeSessions()

                    backupData.customers.forEach { shopDao.insertCustomer(it) }
                    backupData.products.forEach { shopDao.insertProduct(it) }
                    backupData.invoices.forEach { shopDao.insertInvoice(it) }
                    backupData.saleItems.forEach { shopDao.insertSaleItem(it) }
                    backupData.installments.forEach { shopDao.insertInstallment(it) }
                    backupData.repairs.forEach { shopDao.insertRepair(it) }
                    backupData.goldPriceHistory.forEach { shopDao.insertGoldPriceHistory(it) }
                    backupData.auditLogs.forEach { shopDao.insertLog(it) }
                    backupData.dailyClosings.forEach { shopDao.insertDailyClosing(it) }
                    backupData.stockTakeSessions.forEach { shopDao.insertStockTakeSession(it) }
                    if (backupData.stockTakeItems.isNotEmpty()) {
                        shopDao.insertStockTakeItems(backupData.stockTakeItems)
                    }
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
        shopDao.clearDailyClosings()
        shopDao.clearStockTakeItems()
        shopDao.clearStockTakeSessions()
        // recheck or initialize user config
        shopDao.insertUser(User())
    }
}

object BusinessDayUtils {
    fun getStartOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getEndOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    fun getBusinessDateKey(timestamp: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        return String.format(Locale.US, "%04d-%02d-%02d", y, m, d)
    }

    fun getGregorianDateDisplay(timestamp: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        return String.format(Locale.US, "%04d/%02d/%02d", y, m, d)
    }
}

data class TodaySummaryPreview(
    val businessDateKey: String,
    val displayedPersianDate: String,
    val displayedGregorianDate: String,
    val invoiceCount: Int,
    val salesTotal: BigDecimal,
    val paidTotal: BigDecimal,
    val installmentCreatedTotal: BigDecimal,
    val installmentCreatedCount: Int,
    val installmentCollectedTotal: BigDecimal,
    val overdueInstallmentCount: Int,
    val inventoryPieceCount: Int,
    val inventoryWeight: BigDecimal,
    val inventoryValue: BigDecimal,
    val lowStockCount: Int,
    val openRepairsCount: Int,
    val readyRepairsCount: Int,
    val goldRateAtClose: BigDecimal,
    val existingClosing: DailyClosing?
)

sealed class StockTakeScanResult {
    data class Success(val item: StockTakeItem, val totalCounted: Int) : StockTakeScanResult()
    data class NotFound(val barcode: String) : StockTakeScanResult()
}

data class StockTakeApplyResult(
    val adjustedCount: Int,
    val matchedCount: Int,
    val blockedCount: Int,
    val completedSession: StockTakeSession
)
