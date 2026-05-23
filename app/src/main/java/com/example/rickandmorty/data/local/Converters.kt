package com.example.rickandmorty.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String = value?.joinToString(",") ?: ""

    @TypeConverter
    fun toStringList(value: String?): List<String> =
        if (value.isNullOrEmpty()) emptyList() else value.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    @TypeConverter
    fun fromLongList(value: List<Long>?): String = value?.joinToString(",") ?: ""

    @TypeConverter
    fun toLongList(value: String?): List<Long> =
        if (value.isNullOrEmpty()) emptyList() else value.split(",").mapNotNull { it.trim().toLongOrNull() }
}
