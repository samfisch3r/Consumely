package com.android.consumely.data.remote.jsonbin

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SyncLocationDto(
    val id: String,
    val name: String,
    val type: String,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0
)

@Serializable
data class SyncItemDto(
    val id: String,
    val name: String,
    val locationId: String,
    val quantity: Int = 1,
    val unit: String? = null,
    val dateAdded: Long,
    val expiryDate: Long? = null,
    val barcode: String? = null,
    val lastUpdated: Long,
    val isDeleted: Boolean = false
)

@Serializable
data class SyncPayload(
    val lastUpdated: Long = System.currentTimeMillis(),
    val locations: List<SyncLocationDto> = emptyList(),
    val items: List<SyncItemDto> = emptyList()
)

@Serializable
data class JsonBinRawResponse(
    val record: JsonElement? = null,
    val metadata: JsonBinMetadata? = null
)

@Serializable
data class JsonBinMetadata(
    val id: String? = null,
    val createdAt: String? = null,
    val private: Boolean? = null
)
