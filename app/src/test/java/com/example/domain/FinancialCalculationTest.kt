package com.example.domain

import com.example.data.model.SaleItem
import com.example.domain.usecase.CalculateGoldPriceUseCase
import com.example.domain.usecase.InvoiceCalculatorUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class FinancialCalculationTest {

    private lateinit var goldCalculator: CalculateGoldPriceUseCase
    private lateinit var invoiceCalculator: InvoiceCalculatorUseCase

    @Before
    fun setUp() {
        goldCalculator = CalculateGoldPriceUseCase()
        invoiceCalculator = InvoiceCalculatorUseCase()
    }

    // 1. مبلغی که دقیقاً بین 3 قسط تقسیم نمی‌شود
    @Test
    fun test1_amountNotDivisibleBy3Installments() {
        val remainingAmount = 100.0 // 100 Toman
        val installmentsCount = 3

        val installments = invoiceCalculator.calculateInstallments(remainingAmount, installmentsCount)

        assertEquals(3, installments.size)
        assertEquals(33.0, installments[0], 0.0)
        assertEquals(33.0, installments[1], 0.0)
        assertEquals(34.0, installments[2], 0.0)

        val sum = installments.sum()
        assertEquals(remainingAmount, sum, 0.0)
    }

    // 2. مبلغی که بین 7 قسط تقسیم نمی‌شود
    @Test
    fun test2_amountNotDivisibleBy7Installments() {
        val remainingAmount = 1_000_000.0 // 1 Million Toman
        val installmentsCount = 7

        val installments = invoiceCalculator.calculateInstallments(remainingAmount, installmentsCount)

        assertEquals(7, installments.size)
        // 1,000,000 / 7 = 142857 base, remainder = 1 added to last installment -> 142858
        assertEquals(142857.0, installments[0], 0.0)
        assertEquals(142858.0, installments[6], 0.0)

        val sum = installments.sum()
        assertEquals(remainingAmount, sum, 0.0)
    }

    // 3. مبلغ بسیار بزرگ
    @Test
    fun test3_veryLargeAmount() {
        val largeAmount = 1_234_567_890_123.0 // 1.23 Trillion Toman
        val installmentsCount = 12

        val installments = invoiceCalculator.calculateInstallments(largeAmount, installmentsCount)

        assertEquals(12, installments.size)
        val sum = installments.fold(0.0) { acc, amt -> acc + amt }
        assertEquals(largeAmount, sum, 0.0)

        // Also test gold price calculation with large weight/price
        val result = goldCalculator.execute(
            weightGram = 10_000.0, // 10 kg
            karat = 18,
            wagePrice = 500_000.0,
            wageType = "FIXED",
            goldPricePerGram18k = 50_000_000.0,
            profitPercent = 7.0,
            taxPercent = 9.0
        )
        assertTrue(result.totalPrice > 500_000_000_000.0)
        assertEquals(result.totalPrice, Math.floor(result.totalPrice), 0.0)
    }

    // 4. تخفیف اعشاری
    @Test
    fun test4_decimalDiscount() {
        val items = listOf(
            SaleItem(id = 1, invoiceId = 0, productId = 1, quantity = 1, unitPrice = 10_000_000.0, total = 10_000_000.0)
        )
        val decimalDiscount = 123_456.789 // Should round to 123,457 Toman

        val summary = invoiceCalculator.calculateInvoiceSummary(
            items = items,
            discountAmount = decimalDiscount,
            taxPercent = 0.0,
            prepayment = 0.0
        )

        assertEquals(123_457.0, summary.totalDiscount, 0.0)
        assertEquals(9_876_543.0, summary.finalPayable, 0.0)
    }

    // 5. مالیات اعشاری
    @Test
    fun test5_decimalTaxRate() {
        val items = listOf(
            SaleItem(id = 1, invoiceId = 0, productId = 1, quantity = 1, unitPrice = 10_000_000.0, total = 10_000_000.0)
        )
        val decimalTaxRate = 9.25 // 9.25%

        val summary = invoiceCalculator.calculateInvoiceSummary(
            items = items,
            discountAmount = 0.0,
            taxPercent = decimalTaxRate,
            prepayment = 0.0
        )

        // 10,000,000 * 0.0925 = 925,000
        assertEquals(925_000.0, summary.totalTax, 0.0)
        assertEquals(10_925_000.0, summary.finalPayable, 0.0)
    }

    // 6. اجرت درصدی
    @Test
    fun test6_percentageWage() {
        val weightGram = 10.0
        val goldPrice = 4_000_000.0 // 4 Million Toman/g
        val wagePercent = 12.5 // 12.5%

        val result = goldCalculator.execute(
            weightGram = weightGram,
            karat = 18,
            wagePrice = wagePercent,
            wageType = "PERCENT",
            goldPricePerGram18k = goldPrice,
            profitPercent = 7.0,
            taxPercent = 9.0
        )

        // Base gold price = 10 * 4,000,000 = 40,000,000
        assertEquals(40_000_000.0, result.baseGoldPrice, 0.0)
        // Wage = 40,000,000 * 0.125 = 5,000,000
        assertEquals(5_000_000.0, result.wageAmount, 0.0)
    }

    // 7. ترکیب وزن اعشاری + قیمت طلا + اجرت + سود + مالیات
    @Test
    fun test7_complexDecimalWeightAndPriceCombination() {
        val weightGram = 3.333333 // rounded to 3.333 g
        val goldPrice = 3_456_789.123 // rounded to 3,456_789 Toman
        val wagePrice = 7.5 // 7.5%
        val profitPercent = 7.0
        val taxPercent = 9.0

        val result = goldCalculator.execute(
            weightGram = weightGram,
            karat = 18,
            wagePrice = wagePrice,
            wageType = "PERCENT",
            goldPricePerGram18k = goldPrice,
            profitPercent = profitPercent,
            taxPercent = taxPercent
        )

        // Ensure all monetary fields are exact integer Tomans with no fractional drift
        assertEquals(result.baseGoldPrice, Math.floor(result.baseGoldPrice), 0.0)
        assertEquals(result.wageAmount, Math.floor(result.wageAmount), 0.0)
        assertEquals(result.profitAmount, Math.floor(result.profitAmount), 0.0)
        assertEquals(result.taxAmount, Math.floor(result.taxAmount), 0.0)
        assertEquals(result.totalPrice, Math.floor(result.totalPrice), 0.0)
    }

    // 8. اطمینان از اینکه مجموع اقساط دقیقاً برابر مبلغ باقیمانده است
    @Test
    fun test8_installmentsSumEqualsRemainingAmountAlways() {
        val testCases = listOf(
            Pair(100.0, 3),
            Pair(1000000.0, 7),
            Pair(123456789.0, 5),
            Pair(9999999.0, 11),
            Pair(50000000.0, 24),
            Pair(1.0, 3)
        )

        for ((remAmount, count) in testCases) {
            val installments = invoiceCalculator.calculateInstallments(remAmount, count)
            assertEquals(count, installments.size)
            val sum = installments.fold(0.0) { acc, v -> acc + v }
            assertEquals("Failed for amount $remAmount with $count installments", remAmount, sum, 0.0)
        }
    }

    // 9. اطمینان از اینکه مبلغ ذخیره‌شده در Invoice و مجموع SaleItemها با یکدیگر سازگار هستند
    @Test
    fun test9_invoiceTotalMatchesSaleItemsSum() {
        val items = listOf(
            SaleItem(id = 1, invoiceId = 0, productId = 101, quantity = 3, unitPrice = 3_333_333.0, total = 10_000_000.0),
            SaleItem(id = 2, invoiceId = 0, productId = 102, quantity = 2, unitPrice = 7_500_000.0, total = 15_000_000.0)
        )
        val discount = 500_000.0

        val itemsSumBd = items.fold(BigDecimal.ZERO) { acc, item ->
            acc.add(item.total.setScale(0, RoundingMode.HALF_UP))
        }
        val discountBd = BigDecimal.valueOf(discount).setScale(0, RoundingMode.HALF_UP)
        val expectedPayable = itemsSumBd.subtract(discountBd).toDouble()

        val summary = invoiceCalculator.calculateInvoiceSummary(
            items = items,
            discountAmount = discount,
            taxPercent = 0.0,
            prepayment = 0.0
        )

        assertEquals(25_000_000.0, summary.subtotal, 0.0)
        assertEquals(expectedPayable, summary.finalPayable, 0.0)
        assertEquals(itemsSumBd.toDouble(), summary.subtotal, 0.0)
    }
}
