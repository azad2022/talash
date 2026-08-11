package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.Embedded

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: Int = 1,
    val pinHash: String = "", // empty means no pin set yet
    val fingerprintEnabled: Boolean = false,
    val dailyGoldPrice: Double = 0.0, // Default 18k price in Toman per gram is 0.0 (not set)
    val taxPercent: Double = 9.0,
    val minStockAlert: Int = 2,
    val selectedPrinterName: String? = null,
    val selectedPrinterAddress: String? = null
)

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
    val weightGram: Double,
    val karat: Int = 18, // 18, 21, 22, 24
    val wagePrice: Double, // Wage (اجرت) per gram (fixed) or total percentage
    val wageType: String, // "FIXED" or "PERCENT"
    val stock: Int = 1,
    val minStock: Int = 1,
    val imagePath: String? = null,
    val imagePath2: String? = null,
    val imagePath3: String? = null,
    val imagePath4: String? = null,
    val imagePath5: String? = null,
    val purchasePrice: Double = 0.0,
    val customBarcode: String = "",
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    // Convenience helper to estimate potential selling price based on basic gold price
    fun estimatePrice(goldPricePerGram18k: Double, taxRate: Double): Double {
        return com.example.domain.usecase.CalculateGoldPriceUseCase().execute(
            weightGram = weightGram,
            karat = karat,
            wagePrice = wagePrice,
            wageType = wageType,
            goldPricePerGram18k = goldPricePerGram18k,
            profitPercent = 7.0,
            taxPercent = taxRate
        ).totalPrice
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
        return when (category) {
            "طلای ۲۴ عیار" -> weightGram * rateGold24k
            "طلای آبشده نقدی" -> weightGram * rateGoldMelted
            "انس جهانی طلا" -> weightGram * (rateGoldOunce * rateCurrencyUsd)
            "سکه یک گرمی" -> rateCoin1g
            "ربع سکه" -> rateCoinQuarter
            "نیم سکه" -> rateCoinHalf
            "سکه امامی" -> rateCoinEmami
            "سکه بهار آزادی" -> rateCoinBahar
            "دلار آمریکا" -> weightGram * rateCurrencyUsd
            "دلار تتر" -> weightGram * rateCurrencyTether
            "یورو" -> weightGram * rateCurrencyEur
            "درهم امارات" -> weightGram * rateCurrencyAed
            "پوند انگلیس" -> weightGram * rateCurrencyGbp
            else -> estimatePrice(dailyPrice18k, taxRate)
        }
    }
}

@Entity(tableName = "sale_invoices")
data class SaleInvoice(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int,
    val date: Long = System.currentTimeMillis(),
    val totalAmount: Double,
    val discount: Double,
    val tax: Double,
    val paidAmount: Double,
    val paymentType: String, // "CASH" (نقدی), "INSTALLMENT" (اقساطی)
    val installmentsCount: Int = 0,
    val prepayment: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sale_items")
data class SaleItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val invoiceId: Int,
    val productId: Int,
    val quantity: Int,
    val unitPrice: Double,
    val total: Double,
    val customWeight: Double? = null,
    val customName: String? = null
)

@Entity(tableName = "installments")
data class Installment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val invoiceId: Int,
    val dueDate: Long,
    val amount: Double,
    val paid: Boolean = false,
    val paymentDate: Long? = null
)

@Entity(tableName = "repairs")
data class Repair(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerId: Int,
    val description: String,
    val imagePath: String? = null,
    val estimatedCost: Double,
    val upfrontPayment: Double,
    val status: String, // PENDING_APPROVAL, UNDER_REPAIR, READY, DELIVERED
    val createdAt: Long = System.currentTimeMillis(),
    val deliveredAt: Long? = null
)

@Entity(tableName = "gold_price_history")
data class GoldPriceHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long,
    val pricePerGram: Double
)

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
