package com.example.domain.usecase

import com.example.data.model.*
import org.json.JSONArray
import org.json.JSONObject

data class BackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val customers: List<Customer>,
    val products: List<Product>,
    val invoices: List<SaleInvoice>,
    val saleItems: List<SaleItem>,
    val repairs: List<Repair>,
    val goldPriceHistory: List<GoldPriceHistory>
)

class BackupRestoreUseCase {

    fun exportToJson(data: BackupData): String {
        val root = JSONObject().apply {
            put("version", data.version)
            put("timestamp", data.timestamp)

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
                    put("weightGram", p.weightGram)
                    put("karat", p.karat)
                    put("wagePrice", p.wagePrice)
                    put("wageType", p.wageType)
                    put("stock", p.stock)
                    put("minStock", p.minStock)
                    put("purchasePrice", p.purchasePrice)
                    put("customBarcode", p.customBarcode)
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
                    put("totalAmount", inv.totalAmount)
                    put("discount", inv.discount)
                    put("tax", inv.tax)
                    put("paidAmount", inv.paidAmount)
                    put("paymentType", inv.paymentType)
                    put("installmentsCount", inv.installmentsCount)
                    put("prepayment", inv.prepayment)
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
                    put("unitPrice", item.unitPrice)
                    put("total", item.total)
                    put("customWeight", item.customWeight ?: JSONObject.NULL)
                    put("customName", item.customName ?: JSONObject.NULL)
                })
            }
            put("saleItems", saleItemsArray)

            // Repairs
            val repairsArray = JSONArray()
            data.repairs.forEach { rep ->
                repairsArray.put(JSONObject().apply {
                    put("id", rep.id)
                    put("customerId", rep.customerId)
                    put("description", rep.description)
                    put("estimatedCost", rep.estimatedCost)
                    put("upfrontPayment", rep.upfrontPayment)
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
                    put("pricePerGram", h.pricePerGram)
                })
            }
            put("goldPriceHistory", goldHistoryArray)
        }

        return root.toString(2)
    }

    fun parseFromJson(jsonString: String): Result<BackupData> {
        return try {
            val root = JSONObject(jsonString)
            val version = root.optInt("version", 1)
            val timestamp = root.optLong("timestamp", System.currentTimeMillis())

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
                            weightGram = obj.optDouble("weightGram", 0.0),
                            karat = obj.optInt("karat", 18),
                            wagePrice = obj.optDouble("wagePrice", 0.0),
                            wageType = obj.optString("wageType", "FIXED"),
                            stock = obj.optInt("stock", 1),
                            minStock = obj.optInt("minStock", 1),
                            purchasePrice = obj.optDouble("purchasePrice", 0.0),
                            customBarcode = obj.optString("customBarcode", ""),
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
                            totalAmount = obj.optDouble("totalAmount", 0.0),
                            discount = obj.optDouble("discount", 0.0),
                            tax = obj.optDouble("tax", 0.0),
                            paidAmount = obj.optDouble("paidAmount", 0.0),
                            paymentType = obj.optString("paymentType", "CASH"),
                            installmentsCount = obj.optInt("installmentsCount", 0),
                            prepayment = obj.optDouble("prepayment", 0.0),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val saleItemsList = mutableListOf<SaleItem>()
            root.optJSONArray("saleItems")?.let { array ->
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    saleItemsList.add(
                        SaleItem(
                            id = obj.optInt("id", 0),
                            invoiceId = obj.optInt("invoiceId", 0),
                            productId = obj.optInt("productId", 0),
                            quantity = obj.optInt("quantity", 1),
                            unitPrice = obj.optDouble("unitPrice", 0.0),
                            total = obj.optDouble("total", 0.0),
                            customWeight = if (obj.isNull("customWeight")) null else obj.optDouble("customWeight"),
                            customName = if (obj.isNull("customName")) null else obj.optString("customName")
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
                            estimatedCost = obj.optDouble("estimatedCost", 0.0),
                            upfrontPayment = obj.optDouble("upfrontPayment", 0.0),
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
                            pricePerGram = obj.optDouble("pricePerGram", 0.0)
                        )
                    )
                }
            }

            Result.success(
                BackupData(
                    version = version,
                    timestamp = timestamp,
                    customers = customersList,
                    products = productsList,
                    invoices = invoicesList,
                    saleItems = saleItemsList,
                    repairs = repairsList,
                    goldPriceHistory = goldHistoryList
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
