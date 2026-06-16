package com.attentionmanager.data.database

import androidx.room.TypeConverter
import com.attentionmanager.domain.model.PriorityTier
import org.json.JSONArray

class PriorityTierConverter {
    @TypeConverter
    fun fromTier(tier: PriorityTier): String = tier.storageName

    @TypeConverter
    fun toTier(value: String): PriorityTier = PriorityTier.fromStorage(value)
}

class LongListConverter {
    @TypeConverter
    fun fromList(values: List<Long>): String = JSONArray(values).toString()

    @TypeConverter
    fun toList(value: String): List<Long> {
        if (value.isBlank()) return emptyList()
        val array = JSONArray(value)
        return buildList {
            repeat(array.length()) { index -> add(array.getLong(index)) }
        }
    }
}

class StringListConverter {
    @TypeConverter
    fun fromList(values: List<String>): String = JSONArray(values).toString()

    @TypeConverter
    fun toList(value: String): List<String> {
        if (value.isBlank()) return emptyList()
        val array = JSONArray(value)
        return buildList {
            repeat(array.length()) { index -> add(array.getString(index)) }
        }
    }
}
