package com.example.ui.util

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer

class BarcodeAnalyzer(
    private val onBarcodeScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        val hints = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(
                BarcodeFormat.CODE_128,
                BarcodeFormat.QR_CODE,
                BarcodeFormat.EAN_13,
                BarcodeFormat.EAN_8,
                BarcodeFormat.UPC_A,
                BarcodeFormat.UPC_E,
                BarcodeFormat.CODE_39
            ),
            DecodeHintType.TRY_HARDER to true,
            DecodeHintType.CHARACTER_SET to "UTF-8"
        )
        setHints(hints)
    }

    override fun analyze(imageProxy: ImageProxy) {
        try {
            val width = imageProxy.width
            val height = imageProxy.height
            val rotation = imageProxy.imageInfo.rotationDegrees
            val data = imageProxy.toGrayBytes()

            var decodedText: String? = null

            // Try first with original orientation
            try {
                val source = PlanarYUVLuminanceSource(
                    data, width, height, 0, 0, width, height, false
                )
                val bitmap = BinaryBitmap(HybridBinarizer(source))
                val result = reader.decodeWithState(bitmap)
                decodedText = result.text
            } catch (e: Exception) {
                // Ignore, try next
            }

            // If failed, and we have rotation, let's rotate by 90 degrees and try decoding again
            if (decodedText == null) {
                try {
                    val rotatedData = rotateGrayscale90(data, width, height)
                    val source = PlanarYUVLuminanceSource(
                        rotatedData, height, width, 0, 0, height, width, false
                    )
                    val bitmap = BinaryBitmap(HybridBinarizer(source))
                    val result = reader.decodeWithState(bitmap)
                    decodedText = result.text
                } catch (e: Exception) {
                    // Ignore
                }
            }

            // If still failed, try 180 degrees rotate (reverse horizontal)
            if (decodedText == null) {
                try {
                    val invertedData = invertGrayscaleHorizontal(data, width, height)
                    val source = PlanarYUVLuminanceSource(
                        invertedData, width, height, 0, 0, width, height, false
                    )
                    val bitmap = BinaryBitmap(HybridBinarizer(source))
                    val result = reader.decodeWithState(bitmap)
                    decodedText = result.text
                } catch (e: Exception) {
                    // Ignore
                }
            }

            if (decodedText != null) {
                onBarcodeScanned(decodedText)
            }
        } catch (e: Exception) {
            // Failed to decode in this frame, try next
        } finally {
            reader.reset()
            imageProxy.close()
        }
    }

    private fun rotateGrayscale90(data: ByteArray, width: Int, height: Int): ByteArray {
        val rotated = ByteArray(width * height)
        var i = 0
        for (x in 0 until width) {
            for (y in height - 1 downTo 0) {
                rotated[i++] = data[y * width + x]
            }
        }
        return rotated
    }

    private fun invertGrayscaleHorizontal(data: ByteArray, width: Int, height: Int): ByteArray {
        val inverted = ByteArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                inverted[y * width + (width - 1 - x)] = data[y * width + x]
            }
        }
        return inverted
    }

    private fun ImageProxy.toGrayBytes(): ByteArray {
        val yPlane = planes[0]
        val yBuffer = yPlane.buffer
        val rowStride = yPlane.rowStride
        val pixelStride = yPlane.pixelStride
        val w = width
        val h = height
        val target = ByteArray(w * h)
        var rowOffset = 0
        for (y in 0 until h) {
            yBuffer.position(rowOffset)
            if (pixelStride == 1) {
                yBuffer.get(target, y * w, w)
            } else {
                for (x in 0 until w) {
                    target[y * w + x] = yBuffer.get(rowOffset + x * pixelStride)
                }
            }
            rowOffset += rowStride
        }
        return target
    }
}
