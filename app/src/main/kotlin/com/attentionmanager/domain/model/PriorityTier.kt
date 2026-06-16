package com.attentionmanager.domain.model

sealed class PriorityTier(val storageName: String, val rank: Int) {
    data object URGENT : PriorityTier("URGENT", 0)
    data object IMPORTANT : PriorityTier("IMPORTANT", 1)
    data object LOW : PriorityTier("LOW", 2)

    companion object {
        val all: List<PriorityTier> = listOf(URGENT, IMPORTANT, LOW)

        fun fromStorage(value: String): PriorityTier = when (value.uppercase()) {
            URGENT.storageName -> URGENT
            IMPORTANT.storageName -> IMPORTANT
            LOW.storageName -> LOW
            else -> IMPORTANT
        }
    }
}
