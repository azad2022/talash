package com.example.data.database

import androidx.room.TypeConverter
import java.math.BigDecimal

class Converters {
    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? {
        return value?.toPlainString()
    }

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? {
        return value?.let {
            if (it.isBlank()) BigDecimal.ZERO else try { BigDecimal(it) } catch (e: Exception) { BigDecimal.ZERO }
        }
    }
}
