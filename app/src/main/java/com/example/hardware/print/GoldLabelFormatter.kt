package com.example.hardware.print

import com.example.data.model.Product
import java.math.RoundingMode

data class GoldLabelProfile(
    val widthMm: Int = 40,
    val heightMm: Int = 25,
    val includePrice: Boolean = false,
    val includeBarcode: Boolean = true
)

object GoldLabelFormatter {
    fun text(
        product: Product,
        canonicalBarcode: String,
        profile: GoldLabelProfile = GoldLabelProfile()
    ): String {
        val lines = mutableListOf<String>()
        lines += product.name
        lines += "وزن: ${{product.weightGram.setScale(3, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()} گرم"
        lines += "عیار: ${{product.karat}"
        if (profile.includePrice) {
            lines += "قیمت ثبت‌شده: ${{product.purchasePrice.setScale(0, RoundingMode.HALF_UP).toPlainString()}"
        }
        if (profile.includeBarcode) lines += canonicalBarcode
        return lines.joinToString("\n")
    }
}
