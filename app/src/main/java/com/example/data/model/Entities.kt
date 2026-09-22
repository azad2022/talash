package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.Embedded
import java.math.BigDecimal

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: Int = 1,
    val pinHash: String = "", // empty means no pin set yet
    val fingerprintEnabled: Boolean = false,
    val dailyGoldPrice: BigDecimal = BigDecimal.ZERO, // Default 18k price in Toman per gram is 0.0 (not set)
    val taxPercent: BigDecimal = BigDecimal("9.0"),
    val minStockAlert: Int = 2,
    val selectedPrinterName: String? = null,
    val selectedPrinterAddress: String? = null
) {
    constructor(
        id: Int = 1,
        pinHash: String = "",
        fingerprintEnabled: Boolean = false,
        dailyGoldPrice: Double,
        taxPercent: Double = 9.0,
        minStockAlert: Int = 2,
        selectedPrinterName: String? = null,
        selectedPrinterAddress: String? = null
    ) : this(
        id = id,
        pinHash = pinHash,
        fingerprintEnabled = fingerprintEnabled,
        dailyGoldPrice = BigDecimal.valueOf(dailyGoldPrice),
        taxPercent = BigDecimal.valueOf(taxPercent),
        minStockAlert = minStockAlert,
        selectedPrinterName = selectedPrinterName,
        selectedPrinterAddress = selectedPrinterAddress
    )
}

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val address: String,
    val nationalId: String = "",
    val avatarPath: String? = null,
    val about: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String, // e.g., Ring, Necklace, Earring, Bracelet, GoldBar, Coin
    val weightGram: BigDecimal,
    val karat: Int = 18, // 18, 21, 22, 24
    val wagePrice: BigDecimal, // Wage (اجرت) per gram (fixed) or total percentage
    val wageType: String, // "FIXED" or "PERCENT"
    val stock: Int = 1,
    val minStock: Int = 1,
    val imagePath: String? = null,
    val imagePath2: String? = null,
    val imagePath3: String? = null,
    val imagePath4: String? = null,
    val imagePath5: String? = null,
    val purchasePrice: BigDecimal = BigDecimal.ZERO,
    val customBarcode: String = "",
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    constructor(
        id: Int = 0,
        name: String,
        category: String,
        weightGram: Double,
        karat: Int = 18,
        wagePrice: Double,
        wageType: String,
        stock: Int = 1,
        minStock: Int = 1,
        imagePath: String? = null,
        imagePath2: String? = null,
        imagePath3: String? = null,
        imagePath4: String? = null,
        imagePath5: String? = null,
        purchasePrice: Double = 0.0,
        customBarcode: String = "",
        isDeleted: Boolean = false,
        createdAt: Long = System.currentTimeMillis()
    ) : this(
        id = id,
        name = name,
        category = category,
        weightGram = BigDecimal.valueOf(weightGram),
        karat = karat,
        wagePrice = BigDecimal.valueOf(wagePrice),
        wageType = wageType,
        stock = stock,
        minStock = minStock,
        imagePath = imagePath,
        imagePath2 = imagePath2,
        imagePath3 = imagePath3,
        imagePath4 = imagePath4,
        imagePath5 = imagePath5,
        purchasePrice = BigDecimal.valueOf(purchasePrice),
        customBarcode = customBarcode,
        isDeleted = isDeleted,
        createdAt = createdAt
    )

    // Convenience helper to estimate potential selling price based on basic gold price
    fun estimatePrice(goldPricePerGram18k: BigDecimal, taxRate: BigDecimal): BigDecimal {
        return com.example.domain.usecase.CalculateGoldPriceUseCase().execute(
            weightGram = weightGram,
            karat = karat,
            wagePrice = wagePrice,
            wageType = wageType,
            goldPricePerGram18k = goldPricePerGram18k,
            profitPercent = BigDecimal("7.0"),
            taxPercent = taxRate
        ).totalPriceBd
    }

    fun estimatePrice(goldPricePerGram18k: Double, taxRate: Double): Double {
        return estimatePrice(BigDecimal.valueOf(goldPricePerGram18k), BigDecimal.valueOf(taxRate)).toDouble()
    }

    fun calculateAssetValue(
        dailyPrice18k: BigDecimal,
        rateGold24k: BigDecimal,
        rateGoldMelted: BigDecimal,
        rateGoldOunce: BigDecimal,
        rateCoin1g: BigDecimal,
        rateCoinQuarter: BigDecimal,
        rateCoinHalf: BigDecimal,
        rateCoinEmami: BigDecimal,
        rateCoinBahar: BigDecimal,
        rateCurrencyUsd: BigDecimal,
        rateCurrencyTether: BigDecimal,
        rateCurrencyEur: BigDecimal,
        rateCurrencyAed: BigDecimal,
        rateCurrencyGbp: BigDecimal,
        taxRate: BigDecimal
    ): BigDecimal {
        return when (category) {
            "طلای ۲۴ عیار" -> weightGram.multiply(rateGold24k)
            "طلای آبشده نقدی" -> weightGram.multiply(rateGoldMelted)
            "انس جهانی طلا" -> weightGram.multiply(rateGoldOunce).multiply(rateCurrencyUsd)
            "سکه یک گرمی" -> rateCoin1g
            "ربع سکه" -> rateCoinQuarter
            "نیم سکه" -> rateCoinHalf
            "سکه امامی" -> rateCoinEmami
            "سکه بهار آزادی" -> rateCoinBahar
            "دلار آمریکا" -> weightGram.multiply(rateCurrencyUsd)
            "دلار تتر" -> weightGram.multiply(rateCurrencyTether)
            "یورو" -> weightGram.multiply(rateCurrencyEur)
            "درهم امارات" -> weightGram.multiply(rateCurrencyAed)
            "پوند انگلیس" -> weightGram.multiply(rateCurrencyGbp)
            else -> estimatePrice(dailyPrice18k, taxRate)
        }
    }

    fun calculateAssetValue(
        dailyPrice18k: Double,
        rateGold24k: Double,
        rateGoldMelted: Double,
        rateGoldOunce: Double,
        rateCoin1g: Double,
        rateCoinQuarter: Double,
        rateCoinHalf: Double,
        rateCoinEmami: Double,
        rateCoinBahar: Double,
        rateCurrencyUsd: Double,
        rateCurrencyTether: Double,
        rateCurrencyEur: Double,
        rateCurrencyAed: Double,
        rateCurrencyGbp: Double,
        taxRate: Double
    ): Double {
        return calculateAssetValue(
            BigDecimal.valueOf(dailyPrice18k),
            BigDecimal.valueOf(rateGold24k),
            BigDecimal.valueOf(rateGoldMelted),
            BigDecimal.valueOf(rateGoldOunce),
            BigDecimal.valueOf(rateCoin1g),
            BigDecimal.valueOf(rateCoinQuarter),
            BigDecimal.valueOf(rateCoinHalf),
            BigDecimal.valueOf(rateCoinEmami),
            BigDecimal.valueOf(rateCoinBahar),
            BigDecimal.valueOf(rateCurrencyUsd),
            BigDecimal.valueOf(rateCurrencyTether),
            BigDecimal.valueOf(rateCurrencyEur),
            BigDecimal.valueOf(rateCurrencyAed),
            BigDecimal.valueOf(rateCurrencyGbp),
            BigDecimal.valueOf(taxRate)
        ).toDouble()
    }
}

@Entity(tableName = "sale_invoices")
data class SaleInvoice(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int,
    val date: Long = System.currentTimeMillis(),
    val totalAmount: BigDecimal,
    val discount: BigDecimal,
    val tax: BigDecimal,
    val paidAmount: BigDecimal,
    val paymentType: String, // "CASH" (نقدی), "INSTALLMENT" (اقساطی)
    val installmentsCount: Int = 0,
    val prepayment: BigDecimal = BigDecimal.ZERO,
    val createdAt: Long = System.currentTimeMillis()
) {
    constructor(
        id: Int = 0,
        customerId: Int,
        date: Long = System.currentTimeMillis(),
        totalAmount: Double,
        discount: Double,
        tax: Double,
        paidAmount: Double,
        paymentType: String,
        installmentsCount: Int = 0,
        prepayment: Double = 0.0,
        createdAt: Long = System.currentTimeMillis()
    ) : this(
        id = id,
        customerId = customerId,
        date = date,
        totalAmount = BigDecimal.valueOf(totalAmount),
        discount = BigDecimal.valueOf(discount),
        tax = BigDecimal.valueOf(tax),
        paidAmount = BigDecimal.valueOf(paidAmount),
        paymentType = paymentType,
        installmentsCount = installmentsCount,
        prepayment = BigDecimal.valueOf(prepayment),
        createdAt = createdAt
    )
}

@Entity(tableName = "sale_items")
data class SaleItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val invoiceId: Int,
    val productId: Int,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val total: BigDecimal,
    val customWeight: BigDecimal? = null,
    val customName: String? = null
) {
    constructor(
        id: Int = 0,
        invoiceId: Int,
        productId: Int,
        quantity: Int,
        unitPrice: Double,
        total: Double,
        customWeight: Double? = null,
        customName: String? = null
    ) : this(
        id = id,
        invoiceId = invoiceId,
        productId = productId,
        quantity = quantity,
        unitPrice = BigDecimal.valueOf(unitPrice),
        total = BigDecimal.valueOf(total),
        customWeight = customWeight?.let { BigDecimal.valueOf(it) },
        customName = customName
    )
}

@Entity(tableName = "installments")
data class Installment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val invoiceId: Int,
    val dueDate: Long,
    val amount: BigDecimal,
    val paid: Boolean = false,
    val paymentDate: Long? = null
) {
    constructor(
        id: Int = 0,
        invoiceId: Int,
        dueDate: Long,
        amount: Double,
        paid: Boolean = false,
        paymentDate: Long? = null
    ) : this(
        id = id,
        invoiceId = invoiceId,
        dueDate = dueDate,
        amount = BigDecimal.valueOf(amount),
        paid = paid,
        paymentDate = paymentDate
    )
}

@Entity(tableName = "repairs")
data class Repair(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int,
    val description: String,
    val imagePath: String? = null,
    val estimatedCost: BigDecimal,
    val upfrontPayment: BigDecimal,
    val status: String, // PENDING_APPROVAL, UNDER_REPAIR, READY, DELIVERED
    val createdAt: Long = System.currentTimeMillis(),
    val deliveredAt: Long? = null
) {
    constructor(
        id: Int = 0,
        customerId: Int,
        description: String,
        imagePath: String? = null,
        estimatedCost: Double,
        upfrontPayment: Double,
        status: String,
        createdAt: Long = System.currentTimeMillis(),
        deliveredAt: Long? = null
    ) : this(
        id = id,
        customerId = customerId,
        description = description,
        imagePath = imagePath,
        estimatedCost = BigDecimal.valueOf(estimatedCost),
        upfrontPayment = BigDecimal.valueOf(upfrontPayment),
        status = status,
        createdAt = createdAt,
        deliveredAt = deliveredAt
    )
}

@Entity(tableName = "gold_price_history")
data class GoldPriceHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long,
    val pricePerGram: BigDecimal
) {
    constructor(
        id: Int = 0,
        date: Long,
        pricePerGram: Double
    ) : this(
        id = id,
        date = date,
        pricePerGram = BigDecimal.valueOf(pricePerGram)
    )
}

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int = 1,
    val action: String, // e.g., "LOGIN", "LOGOUT", "ADD_PRODUCT", "DELETE_CUSTOMER", "BACKUP"
    val timestamp: Long = System.currentTimeMillis(),
    val details: String
)

// Relationships
data class InvoiceWithDetails(
    @Embedded val invoice: SaleInvoice,
    
    @Relation(
        parentColumn = "customerId",
        entityColumn = "id"
    )
    val customer: Customer?,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "invoiceId"
    )
    val items: List<SaleItem>,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "invoiceId"
    )
    val installments: List<Installment>
)

data class RepairWithCustomer(
    @Embedded val repair: Repair,
    
    @Relation(
        parentColumn = "customerId",
        entityColumn = "id"
    )
    val customer: Customer?
)

@Entity(tableName = "daily_closings")
data class DailyClosing(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessDateKey: String, // e.g., "2026-09-21"
    val closedAt: Long = System.currentTimeMillis(),
    val displayedPersianDate: String, // e.g., "1405/06/31"
    val displayedGregorianDate: String, // e.g., "2026/09/21"
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
    val optionalPhysicalCash: BigDecimal? = null,
    val optionalPhysicalGoldWeight: BigDecimal? = null,
    val optionalNotes: String? = null,
    val status: String = "CLOSED", // "CLOSED", "REOPENED"
    val revision: Int = 1
)

@Entity(tableName = "stock_take_sessions")
data class StockTakeSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val status: String = "IN_PROGRESS", // "IN_PROGRESS", "COMPLETED", "CANCELLED"
    val notes: String? = null,
    val totalExpectedPieces: Int = 0,
    val totalCountedPieces: Int = 0
)

@Entity(
    tableName = "stock_take_items",
    indices = [
        androidx.room.Index(value = ["sessionId"]),
        androidx.room.Index(value = ["productId"])
    ]
)
data class StockTakeItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val productId: Int,
    val productName: String,
    val productCategory: String,
    val productBarcode: String,
    val expectedStockAtStart: Int,
    val countedStock: Int = 0,
    val systemStockAtFinalize: Int? = null,
    val difference: Int = 0, // countedStock - expectedStockAtStart
    val changedDuringSession: Boolean = false,
    val status: String = "PENDING" // "PENDING", "MATCHED", "DISCREPANCY", "NEEDS_REVIEW", "ADJUSTED"
)
