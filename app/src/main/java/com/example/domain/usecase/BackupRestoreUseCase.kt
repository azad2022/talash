package com.example.domain.usecase

import com.example.data.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal

data class BackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val customers: List<Customer> = emptyList(),
    val products: List<Product> = emptyList(),
    val invoices: List<SaleInvoice> = emptyList(),
    val saleItems: List<SaleItem> = emptyList(),
    val installments: List<Installment> = emptyList(),
    val repairs: List<Repair> = emptyList(),
    val goldPriceHistory: List<GoldPriceHistory> = emptyList(),
    val auditLogs: List<AuditLog> = emptyList(),
    val dailyClosings: List<DailyClosing> = emptyList(),
    val stockTakeSessions: List<StockTakeSession> = emptyList(),
    val stockTakeItems: List<StockTakeItem> = emptyList(),
    val userConfig: User? = null
)

class BackupRestoreUseCase {

    fun exportToJson(data: BackupData): String {
        val root = JSONObject().apply {
            put("version", data.version)
            put("timestamp", data.timestamp)

            // User Config
            data.userConfig?.let { u ->
                put("userConfig", JSONObject().apply {
                    put("id", u.id)
                    put("dailyGoldPrice", u.dailyGoldPrice.toPlainString())
                    put("taxPercent", u.taxPercent.toPlainString())
                    put("minStockAlert", u.minStockAlert)
                    put("pinHash", u.pinHash)
                    put("fingerprintEnabled", u.fingerprintEnabled)
                })
            }

            // Customers
            val customersArray = JSONArray()
            data.customers.forEach { c ->
                customersArray.put(JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("phone", c.phone)
                    put("address", c.address)
                    put("nationalId", c.nationalId)
                    put("about", c.about)
                    put("createdAt", c.createdAt)
                    put("updatedAt", c.updatedAt)
                })
            }
            put("customers", customersArray)

            // Products
            val productsArray = JSONArray()
            data.products.forEach { p ->
                productsArray.put(JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("category", p.category)
                    put("weightGram", p.weightGram.toPlainString())
                    put("karat", p.karat)
                    put("wagePrice", p.wagePrice.toPlainString())
                    put("wageType", p.wageType)
                    put("stock", p.stock)
                    put("minStock", p.minStock)
                    put("purchasePrice", p.purchasePrice.toPlainString())
                    put("customBarcode", p.customBarcode)
                    put("isDeleted", p.isDeleted)
                    put("createdAt", p.createdAt)
                })
            }
            put("products", productsArray)

            // Invoices
            val invoicesArray = JSONArray()
            data.invoices.forEach { inv ->
                invoicesArray.put(JSONObject().apply {
                    put("id", inv.id)
                    put("customerId", inv.customerId)
                    put("date", inv.date)
                    put("totalAmount", inv.totalAmount.toPlainString())
                    put("discount", inv.discount.toPlainString())
                    put("tax", inv.tax.toPlainString())
                    put("paidAmount", inv.paidAmount.toPlainString())
                    put("paymentType", inv.paymentType)
                    put("installmentsCount", inv.installmentsCount)
                    put("prepayment", inv.prepayment.toPlainString())
                    put("createdAt", inv.createdAt)
                })
            }
            put("invoices", invoicesArray)

            // Sale Items
            val saleItemsArray = JSONArray()
            data.saleItems.forEach { item ->
                saleItemsArray.put(JSONObject().apply {
                    put("id", item.id)
                    put("invoiceId", item.invoiceId)
                    put("productId", item.productId)
                    put("quantity", item.quantity)
                    put("unitPrice", item.unitPrice.toPlainString())
                    put("total", item.total.toPlainString())
                    put("customWeight", item.customWeight?.toPlainString() ?: JSONObject.NULL)
                    put("customName", item.customName ?: JSONObject.NULL)
                })
            }
            put("saleItems", saleItemsArray)

            // Installments
            val installmentsArray = JSONArray()
            data.installments.forEach { inst ->
                installmentsArray.put(JSONObject().apply {
                    put("id", inst.id)
                    put("invoiceId", inst.invoiceId)
                    put("dueDate", inst.dueDate)
                    put("amount", inst.amount.toPlainString())
                    put("paid", inst.paid)
                    put("paymentDate", inst.paymentDate ?: JSONObject.NULL)
                })
            }
            put("installments", installmentsArray)

            // Repairs
            val repairsArray = JSONArray()
            data.repairs.forEach { rep ->
                repairsArray.put(JSONObject().apply {
                    put("id", rep.id)
                    put("customerId", rep.customerId)
                    put("description", rep.description)
                    put("estimatedCost", rep.estimatedCost.toPlainString())
                    put("upfrontPayment", rep.upfrontPayment.toPlainString())
                    put("status", rep.status)
                    put("createdAt", rep.createdAt)
                })
            }
            put("repairs", repairsArray)

            // Gold Price History
            val goldHistoryArray = JSONArray()
            data.goldPriceHistory.forEach { h ->
                goldHistoryArray.put(JSONObject().apply {
                    put("id", h.id)
                    put("date", h.date)
                    put("pricePerGram", h.pricePerGram.toPlainString())
                })
            }
            put("goldPriceHistory", goldHistoryArray)

            // Audit Logs
            val auditLogsArray = JSONArray()
            data.auditLogs.forEach { log ->
                auditLogsArray.put(JSONObject().apply {
                    put("id", log.id)
                    put("userId", log.userId)
                    put("action", log.action)
                    put("timestamp", log.timestamp)
                    put("details", log.details)
                })
            }
            put("auditLogs", auditLogsArray)

            // Daily Closings
            val dailyClosingsArray = JSONArray()
            data.dailyClosings.forEach { dc ->
                dailyClosingsArray.put(JSONObject().apply {
                    put("id", dc.id)
                    put("businessDateKey", dc.businessDateKey)
                    put("closedAt", dc.closedAt)
                    put("displayedPersianDate", dc.displayedPersianDate)
                    put("displayedGregorianDate", dc.displayedGregorianDate)
                    put("invoiceCount", dc.invoiceCount)
                    put("salesTotal", dc.salesTotal.toPlainString())
                    put("paidTotal", dc.paidTotal.toPlainString())
                    put("installmentCreatedTotal", dc.installmentCreatedTotal.toPlainString())
                    put("installmentCreatedCount", dc.installmentCreatedCount)
                    put("installmentCollectedTotal", dc.installmentCollectedTotal.toPlainString())
                    put("overdueInstallmentCount", dc.overdueInstallmentCount)
                    put("inventoryPieceCount", dc.inventoryPieceCount)
                    put("inventoryWeight", dc.inventoryWeight.toPlainString())
                    put("inventoryValue", dc.inventoryValue.toPlainString())
                    put("lowStockCount", dc.lowStockCount)
                    put("openRepairsCount", dc.openRepairsCount)
                    put("readyRepairsCount", dc.readyRepairsCount)
                    put("goldRateAtClose", dc.goldRateAtClose.toPlainString())
                    put("optionalPhysicalCash", dc.optionalPhysicalCash?.toPlainString() ?: JSONObject.NULL)
                    put("optionalPhysicalGoldWeight", dc.optionalPhysicalGoldWeight?.toPlainString() ?: JSONObject.NULL)
                    put("optionalNotes", dc.optionalNotes ?: JSONObject.NULL)
                    put("status", dc.status)
                    put("revision", dc.revision)
                    put("reopenReason", dc.reopenReason ?: JSONObject.NULL)
                    put("reopenedAt", dc.reopenedAt ?: JSONObject.NULL)
                })
            }
            put("dailyClosings", dailyClosingsArray)

            // Stock Take Sessions
            val stockTakeSessionsArray = JSONArray()
            data.stockTakeSessions.forEach { st ->
                stockTakeSessionsArray.put(JSONObject().apply {
                    put("id", st.id)
                    put("startedAt", st.startedAt)
                    put("completedAt", st.completedAt ?: JSONObject.NULL)
                    put("status", st.status)
                    put("notes", st.notes ?: JSONObject.NULL)
                    put("totalExpectedPieces", st.totalExpectedPieces)
                    put("totalCountedPieces", st.totalCountedPieces)
                })
            }
            put("stockTakeSessions", stockTakeSessionsArray)

            // Stock Take Items
            val stockTakeItemsArray = JSONArray()
            data.stockTakeItems.forEach { sti ->
                stockTakeItemsArray.put(JSONObject().apply {
                    put("id", sti.id)
                    put("sessionId", sti.sessionId)
                    put("productId", sti.productId)
                    put("productName", sti.productName)
                    put("productCategory", sti.productCategory)
                    put("productBarcode", sti.productBarcode)
                    put("expectedStockAtStart", sti.expectedStockAtStart)
                    put("countedStock", sti.countedStock)
                    put("isCounted", sti.isCounted)
                    put("systemStockAtFinalize", sti.systemStockAtFinalize ?: JSONObject.NULL)
                    put("difference", sti.difference)
                    put("changedDuringSession", sti.changedDuringSession)
                    put("status", sti.status)
                })
            }
            put("stockTakeItems", stockTakeItemsArray)
        }

        return root.toString(2)
    }

    private fun JSONObject.optBigDecimal(key: String, default: BigDecimal = BigDecimal.ZERO): BigDecimal {
        if (this.isNull(key)) return default
        val strVal = this.optString(key, "")
        if (strVal.isNotBlank()) {
            return try { BigDecimal(strVal) } catch (e: Exception) { default }
        }
        val dVal = this.optDouble(key, Double.NaN)
        if (!dVal.isNaN()) {
            return BigDecimal.valueOf(dVal)
        }
        return default
    }

    fun validateBackupData(data: BackupData): Result<Unit> {
        val customerIds = data.customers.map { it.id }.toSet()
        val productIds = data.products.map { it.id }.toSet()
        val invoiceIds = data.invoices.map { it.id }.toSet()
        val sessionIds = data.stockTakeSessions.map { it.id }.toSet()

        // Validate Invoices -> Customer
        for (inv in data.invoices) {
            if (inv.customerId > 0 && !customerIds.contains(inv.customerId)) {
                return Result.failure(
                    IllegalArgumentException("فایل پشتیبان نامعتبر است: فاکتور شماره ${inv.id} به مشتری شناسه ${inv.customerId} اشاره دارد که وجود ندارد.")
                )
            }
        }

        // Validate SaleItems -> Invoice and Product
        for (item in data.saleItems) {
            if (!invoiceIds.contains(item.invoiceId)) {
                return Result.failure(
                    IllegalArgumentException("فایل پشتیبان نامعتبر است: اقلام فاکتور (کد ${item.id}) به فاکتور شماره ${item.invoiceId} اشاره دارد که وجود ندارد.")
                )
            }
            if (!productIds.contains(item.productId)) {
                return Result.failure(
                    IllegalArgumentException("فایل پشتیبان نامعتبر است: قلم فاکتور (کد ${item.id}) به کالای شناسه ${item.productId} اشاره دارد که وجود ندارد.")
                )
            }
        }

        // Validate Installments -> Invoice
        for (inst in data.installments) {
            if (!invoiceIds.contains(inst.invoiceId)) {
                return Result.failure(
                    IllegalArgumentException("فایل پشتیبان نامعتبر است: قسط شماره ${inst.id} به فاکتور شماره ${inst.invoiceId} اشاره دارد که وجود ندارد.")
                )
            }
        }

        // Validate Repairs -> Customer
        for (rep in data.repairs) {
            if (rep.customerId > 0 && !customerIds.contains(rep.customerId)) {
                return Result.failure(
                    IllegalArgumentException("فایل پشتیبان نامعتبر است: سفارش تعمیر شماره ${rep.id} به مشتری شناسه ${rep.customerId} اشاره دارد که وجود ندارد.")
                )
            }
        }

        // Validate StockTakeItems -> Session
        for (sti in data.stockTakeItems) {
            if (sessionIds.isNotEmpty() && !sessionIds.contains(sti.sessionId)) {
                return Result.failure(
                    IllegalArgumentException("فایل پشتیبان نامعتبر است: آیتم انبارگردانی به نشست شناسه ${sti.sessionId} اشاره دارد که وجود ندارد.")
                )
            }
        }

        return Result.success(Unit)
    }

    fun parseFromJson(jsonString: String): Result<BackupData> {
        return try {
            if (jsonString.isBlank()) {
                return Result.failure(IllegalArgumentException("فایل پشتیبان خالی است."))
            }

            val root = JSONObject(jsonString)
            
            // Check essential keys to validate backup schema
            val hasEntities = root.has("customers") || root.has("products") || root.has("invoices")
            if (!hasEntities) {
                return Result.failure(IllegalArgumentException("فرمت ساختار فایل پشتیبان نامعتبر است."))
            }

            val version = root.optInt("version", 1)
            val timestamp = root.optLong("timestamp", System.currentTimeMillis())

            val userConfigObj = root.optJSONObject("userConfig")
            val userConfig = userConfigObj?.let { obj ->
                User(
                    id = obj.optInt("id", 1),
                    dailyGoldPrice = obj.optBigDecimal("dailyGoldPrice", BigDecimal.ZERO),
                    taxPercent = obj.optBigDecimal("taxPercent", BigDecimal("9.0")),
                    minStockAlert = obj.optInt("minStockAlert", 2),
                    pinHash = obj.optString("pinHash", ""),
                    fingerprintEnabled = obj.optBoolean("fingerprintEnabled", false)
                )
            }

            val customersList = mutableListOf<Customer>()
            root.optJSONArray("customers")?.let { array ->
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    customersList.add(
                        Customer(
                            id = obj.optInt("id", 0),
                            name = obj.optString("name", ""),
                            phone = obj.optString("phone", ""),
                            address = obj.optString("address", ""),
                            nationalId = obj.optString("nationalId", ""),
                            about = obj.optString("about", ""),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val productsList = mutableListOf<Product>()
            root.optJSONArray("products")?.let { array ->
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    productsList.add(
                        Product(
                            id = obj.optInt("id", 0),
                            name = obj.optString("name", ""),
                            category = obj.optString("category", "طلا"),
                            weightGram = obj.optBigDecimal("weightGram", BigDecimal.ZERO),
                            karat = obj.optInt("karat", 18),
                            wagePrice = obj.optBigDecimal("wagePrice", BigDecimal.ZERO),
                            wageType = obj.optString("wageType", "FIXED"),
                            stock = obj.optInt("stock", 1),
                            minStock = obj.optInt("minStock", 1),
                            purchasePrice = obj.optBigDecimal("purchasePrice", BigDecimal.ZERO),
                            customBarcode = obj.optString("customBarcode", ""),
                            isDeleted = obj.optBoolean("isDeleted", false),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val invoicesList = mutableListOf<SaleInvoice>()
            root.optJSONArray("invoices")?.let { array ->
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    invoicesList.add(
                        SaleInvoice(
                            id = obj.optInt("id", 0),
                            customerId = obj.optInt("customerId", 0),
                            date = obj.optLong("date", System.currentTimeMillis()),
                            totalAmount = obj.optBigDecimal("totalAmount", BigDecimal.ZERO),
                            discount = obj.optBigDecimal("discount", BigDecimal.ZERO),
                            tax = obj.optBigDecimal("tax", BigDecimal.ZERO),
                            paidAmount = obj.optBigDecimal("paidAmount", BigDecimal.ZERO),
                            paymentType = obj.optString("paymentType", "CASH"),
                            installmentsCount = obj.optInt("installmentsCount", 0),
                            prepayment = obj.optBigDecimal("prepayment", BigDecimal.ZERO),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val saleItemsList = mutableListOf<SaleItem>()
            root.optJSONArray("saleItems")?.let { array ->
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val customWeightVal = if (obj.isNull("customWeight")) null else obj.optBigDecimal("customWeight")
                    saleItemsList.add(
                        SaleItem(
                            id = obj.optInt("id", 0),
                            invoiceId = obj.optInt("invoiceId", 0),
                            productId = obj.optInt("productId", 0),
                            quantity = obj.optInt("quantity", 1),
                            unitPrice = obj.optBigDecimal("unitPrice", BigDecimal.ZERO),
                            total = obj.optBigDecimal("total", BigDecimal.ZERO),
                            customWeight = customWeightVal,
                            customName = if (obj.isNull("customName")) null else obj.optString("customName")
                        )
                    )
                }
            }

            val installmentsList = mutableListOf<Installment>()
            root.optJSONArray("installments")?.let { array ->
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    installmentsList.add(
                        Installment(
                            id = obj.optInt("id", 0),
                            invoiceId = obj.optInt("invoiceId", 0),
                            dueDate = obj.optLong("dueDate", System.currentTimeMillis()),
                            amount = obj.optBigDecimal("amount", BigDecimal.ZERO),
                            paid = obj.optBoolean("paid", false),
                            paymentDate = if (obj.isNull("paymentDate")) null else obj.optLong("paymentDate")
                        )
                    )
                }
            }

            val repairsList = mutableListOf<Repair>()
            root.optJSONArray("repairs")?.let { array ->
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    repairsList.add(
                        Repair(
                            id = obj.optInt("id", 0),
                            customerId = obj.optInt("customerId", 0),
                            description = obj.optString("description", ""),
                            estimatedCost = obj.optBigDecimal("estimatedCost", BigDecimal.ZERO),
                            upfrontPayment = obj.optBigDecimal("upfrontPayment", BigDecimal.ZERO),
                            status = obj.optString("status", "PENDING_APPROVAL"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val goldHistoryList = mutableListOf<GoldPriceHistory>()
            root.optJSONArray("goldPriceHistory")?.let { array ->
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    goldHistoryList.add(
                        GoldPriceHistory(
                            id = obj.optInt("id", 0),
                            date = obj.optLong("date", System.currentTimeMillis()),
                            pricePerGram = obj.optBigDecimal("pricePerGram", BigDecimal.ZERO)
                        )
                    )
                }
            }

            val auditLogsList = mutableListOf<AuditLog>()
            root.optJSONArray("auditLogs")?.let { array ->
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    auditLogsList.add(
                        AuditLog(
                            id = obj.optInt("id", 0),
                            userId = obj.optInt("userId", 1),
                            action = obj.optString("action", "UNKNOWN"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            details = obj.optString("details", "")
                        )
                    )
                }
            }

            val dailyClosingsList = mutableListOf<DailyClosing>()
            root.optJSONArray("dailyClosings")?.let { array ->
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    dailyClosingsList.add(
                        DailyClosing(
                            id = obj.optLong("id", 0),
                            businessDateKey = obj.optString("businessDateKey", ""),
                            closedAt = obj.optLong("closedAt", System.currentTimeMillis()),
                            displayedPersianDate = obj.optString("displayedPersianDate", ""),
                            displayedGregorianDate = obj.optString("displayedGregorianDate", ""),
                            invoiceCount = obj.optInt("invoiceCount", 0),
                            salesTotal = obj.optBigDecimal("salesTotal", BigDecimal.ZERO),
                            paidTotal = obj.optBigDecimal("paidTotal", BigDecimal.ZERO),
                            installmentCreatedTotal = obj.optBigDecimal("installmentCreatedTotal", BigDecimal.ZERO),
                            installmentCreatedCount = obj.optInt("installmentCreatedCount", 0),
                            installmentCollectedTotal = obj.optBigDecimal("installmentCollectedTotal", BigDecimal.ZERO),
                            overdueInstallmentCount = obj.optInt("overdueInstallmentCount", 0),
                            inventoryPieceCount = obj.optInt("inventoryPieceCount", 0),
                            inventoryWeight = obj.optBigDecimal("inventoryWeight", BigDecimal.ZERO),
                            inventoryValue = obj.optBigDecimal("inventoryValue", BigDecimal.ZERO),
                            lowStockCount = obj.optInt("lowStockCount", 0),
                            openRepairsCount = obj.optInt("openRepairsCount", 0),
                            readyRepairsCount = obj.optInt("readyRepairsCount", 0),
                            goldRateAtClose = obj.optBigDecimal("goldRateAtClose", BigDecimal.ZERO),
                            optionalPhysicalCash = if (obj.isNull("optionalPhysicalCash")) null else obj.optBigDecimal("optionalPhysicalCash"),
                            optionalPhysicalGoldWeight = if (obj.isNull("optionalPhysicalGoldWeight")) null else obj.optBigDecimal("optionalPhysicalGoldWeight"),
                            optionalNotes = if (obj.isNull("optionalNotes")) null else obj.optString("optionalNotes"),
                            status = obj.optString("status", "CLOSED"),
                            revision = obj.optInt("revision", 1),
                            reopenReason = if (obj.isNull("reopenReason")) null else obj.optString("reopenReason"),
                            reopenedAt = if (obj.isNull("reopenedAt")) null else obj.optLong("reopenedAt")
                        )
                    )
                }
            }

            val stockTakeSessionsList = mutableListOf<StockTakeSession>()
            root.optJSONArray("stockTakeSessions")?.let { array ->
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    stockTakeSessionsList.add(
                        StockTakeSession(
                            id = obj.optLong("id", 0),
                            startedAt = obj.optLong("startedAt", System.currentTimeMillis()),
                            completedAt = if (obj.isNull("completedAt")) null else obj.optLong("completedAt"),
                            status = obj.optString("status", "IN_PROGRESS"),
                            notes = if (obj.isNull("notes")) null else obj.optString("notes"),
                            totalExpectedPieces = obj.optInt("totalExpectedPieces", 0),
                            totalCountedPieces = obj.optInt("totalCountedPieces", 0)
                        )
                    )
                }
            }

            val stockTakeItemsList = mutableListOf<StockTakeItem>()
            root.optJSONArray("stockTakeItems")?.let { array ->
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    stockTakeItemsList.add(
                        StockTakeItem(
                            id = obj.optLong("id", 0),
                            sessionId = obj.optLong("sessionId", 0),
                            productId = obj.optInt("productId", 0),
                            productName = obj.optString("productName", ""),
                            productCategory = obj.optString("productCategory", ""),
                            productBarcode = obj.optString("productBarcode", ""),
                            expectedStockAtStart = obj.optInt("expectedStockAtStart", 0),
                            countedStock = obj.optInt("countedStock", 0),
                            isCounted = obj.optBoolean("isCounted", obj.optInt("countedStock", 0) > 0),
                            systemStockAtFinalize = if (obj.isNull("systemStockAtFinalize")) null else obj.optInt("systemStockAtFinalize"),
                            difference = obj.optInt("difference", 0),
                            changedDuringSession = obj.optBoolean("changedDuringSession", false),
                            status = obj.optString("status", "PENDING")
                        )
                    )
                }
            }

            val data = BackupData(
                version = version,
                timestamp = timestamp,
                customers = customersList,
                products = productsList,
                invoices = invoicesList,
                saleItems = saleItemsList,
                installments = installmentsList,
                repairs = repairsList,
                goldPriceHistory = goldHistoryList,
                auditLogs = auditLogsList,
                dailyClosings = dailyClosingsList,
                stockTakeSessions = stockTakeSessionsList,
                stockTakeItems = stockTakeItemsList,
                userConfig = userConfig
            )

            val validationResult = validateBackupData(data)
            if (validationResult.isFailure) {
                return Result.failure(validationResult.exceptionOrNull() ?: Exception("فایل پشتیبان نامعتبر است."))
            }

            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
