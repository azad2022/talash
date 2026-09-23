package com.example.hardware.print

import com.example.data.model.InvoiceWithDetails
import com.example.data.model.Product
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

data class ReceiptProfile(
    val shopName: String = "گالری طلای گیلدار (شعبه مرکزی)",
    val title: String = "فاکتور فروش معتبر کالا",
    val footer: String = "از خرید و حسن انتخاب شما سپاسگزاریم.",
    val address: String = "آدرس گالری",
    val paperWidthColumns: Int = 42
)

object ReceiptFormatter {
    fun format(
        invoice: InvoiceWithDetails,
        productsById: Map<Int, Product>,
        profile: ReceiptProfile = ReceiptProfile()
    ): String {
        val b = StringBuilder()
        val separator = "-".repeat(profile.paperWidthColumns)

        b.appendLine("=".repeat(profile.paperWidthColumns))
        b.appendLine(center(profile.shopName, profile.paperWidthColumns))
        b.appendLine(center(profile.title, profile.paperWidthColumns))
        b.appendLine("=".repeat(profile.paperWidthColumns))
        b.appendLine("شماره فاکتور: ${{invoice.invoice.id}")
        b.appendLine("تاریخ صدور: ${{com.example.ui.util.JalaliCalendar.getJalaliDateTime(invoice.invoice.date)}")
        b.appendLine("نام مشتری: ${{invoice.customer?.name ?: "مشتری متفرقه"}")
        b.appendLine("تلفن همراه: ${{invoice.customer?.phone ?: "-"}")
        b.appendLine(separator)

        invoice.items.forEach { item ->
            val product = productsById[item.productId]
            val name = item.customName?.takeIf { it.isNotBlank() } ?: product?.name ?: "کالا"
            val weight = item.customWeight ?: product?.weightGram ?: BigDecimal.ZERO
            val karat = product?.karat ?: 18
            b.appendLine(name)
            b.appendLine("عیار: ${karat | وزن: ${{formatWeight(weight)} گرم")
            b.appendLine("تعداد: ${{item.quantity} | مبلغ واحد: ${{formatMoney(item.unitPrice)} تومان")
            b.appendLine("مبلغ قلم: ${{formatMoney(item.total)} تومان")
            b.appendLine(separator)
        }

        b.appendLine("جمع اقلام: ${{formatMoney(invoice.invoice.totalAmount.add(invoice.invoice.discount))} تومان")
        if (invoice.invoice.discount > BigDecimal.ZERO) {
            b.appendLine("تخفیف: ${{formatMoney(invoice.invoice.discount)} تومان")
        }
        b.appendLine("مالیات محاسبه‌شده: ${{formatMoney(invoice.invoice.tax)} تومان")
        b.appendLine("مبلغ نهایی: ${{formatMoney(invoice.invoice.totalAmount)} تومان")
        b.appendLine("نوع تسویه: " + if (invoice.invoice.paymentType == "CASH") "نقدی (کامل)" else "اقساطی")

        if (invoice.invoice.paymentType == "INSTALLMENT") {
            b.appendLine("پیش‌پرداخت: ${{formatMoney(invoice.invoice.prepayment)} تومان")
            b.appendLine("تعداد اقساط: ${{invoice.invoice.installmentsCount}")
        }

        b.appendLine(separator)
        b.appendLine(center(profile.footer, profile.paperWidthColumns))
        b.appendLine(center(profile.address, profile.paperWidthColumns))
        b.appendLine("=".repeat(profile.paperWidthColumns))
        return b.toString()
    }

    private fun formatMoney(value: BigDecimal): String =
        NumberFormat.getInstance(Locale("fa", "IR")).format(
            value.setScale(0, RoundingMode.HALF_UP).toBigInteger()
        )

    private fun formatWeight(value: BigDecimal): String {
        val scaled = value.setScale(3, RoundingMode.HALF_UP).stripTrailingZeros()
        return if (scaled.scale() < 0) "0" else scaled.toPlainString()
    }

    private fun center(text: String, width: Int): String {
        if (text.length >= width) return text.take(width)
        return " ".repeat((width - text.length) / 2) + text
    }
}
