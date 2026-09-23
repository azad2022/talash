package com.example.hardware.print

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import java.nio.charset.StandardCharsets
import kotlin.math.roundToInt

object GoldLabelZplEncoder {
    fun encode(
        product: com.example.data.model.Product,
        canonicalBarcode: String,
        dpi: Int = 203,
        widthMm: Int = 40,
        heightMm: Int = 25
    ): ByteArray {
        val widthPx = (widthMm * dpi / 25.4).roundToInt().coerceAtLeast(1)
        val heightPx = (heightMm * dpi / 25.4).roundToInt().coerceAtLeast(1)
        val bitmap = render(product, canonicalBarcode, widthPx, heightPx)
        return bitmapToZpl(bitmap)
    }

    private fun render(product: com.example.data.model.Product, barcode: String, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 21f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        val text = buildString {
            append(product.name).append("\n")
            append("وزن: ").append(product.weightGram.stripTrailingZeros().toPlainString()).append(" گرم\n")
            append("عیار: ").append(product.karat)
        }
        val textLayout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width - 12)
            .setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
            .setTextDirection(TextDirectionHeuristics.RTL)
            .build()
        canvas.save()
        canvas.translate(6f, 4f)
        textLayout.draw(canvas)
        canvas.restore()

        val barcodeMatrix = MultiFormatWriter().encode(barcode, BarcodeFormat.CODE_128, width - 20, 64)
        val barcodeBitmap = Bitmap.createBitmap(barcodeMatrix.width, barcodeMatrix.height, Bitmap.Config.ARGB_8888)
        for (x in 0 until barcodeMatrix.width) {
            for (y in 0 until barcodeMatrix.height) {
                barcodeBitmap.setPixel(x, y, if (barcodeMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        canvas.drawBitmap(barcodeBitmap, 10f, (height - barcodeMatrix.height - 10).toFloat(), null)
        return bitmap
    }

    private fun bitmapToZpl(bitmap: Bitmap): ByteArray {
        val widthBytes = (bitmap.width + 7) / 8
        val totalBytes = widthBytes * bitmap.height
        val hex = StringBuilder(totalBytes * 2)
        for (y in 0 until bitmap.height) {
            for (byteIndex in 0 until widthBytes) {
                var value = 0
                for (bit in 0..7) {
                    val x = byteIndex * 8 + bit
                    if (x < bitmap.width) {
                        val pixel = bitmap.getPixel(x, y)
                        val lum = (Color.red(pixel) * 0.299 + Color.green(pixel) * 0.587 + Color.blue(pixel) * 0.114)
                        if (lum < 128) value = value or (0x80 shr bit)
                    }
                }
                hex.append(value.toString(16).padStart(2, "0").uppercase())
            }
        }
        val zpl = "^XA^PW${bitmap.width}^LL${bitmap.height}^FO0,0^GFA,$totalBytes,$totalBytes,$widthBytes,${hex}^FS^XZ"
        return zpl.toByteArray(StandardCharsets.UTF_8)
    }
}