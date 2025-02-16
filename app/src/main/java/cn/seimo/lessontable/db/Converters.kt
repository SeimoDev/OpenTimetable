package cn.seimo.lessontable.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromString(value: String?): List<Int> {
        return value?.split(",")?.map { it.toInt() } ?: emptyList()
    }
    
    @TypeConverter
    fun fromList(list: List<Int>?): String {
        return list?.joinToString(",") ?: ""
    }
}
