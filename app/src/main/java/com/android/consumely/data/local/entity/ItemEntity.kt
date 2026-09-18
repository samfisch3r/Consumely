package com.android.consumely.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["locationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("locationId"),
        Index("lastUpdated")
    ]
)
data class ItemEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val locationId: String,
    val quantity: Int = 1,
    val unit: String? = null,
    val dateAdded: Long = System.currentTimeMillis(),
    val expiryDate: Long? = null,
    val imagePath: String? = null,
    val barcode: String? = null,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val hasNotifiedExpiry: Boolean = false
)
