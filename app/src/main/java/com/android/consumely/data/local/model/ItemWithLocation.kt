package com.android.consumely.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.android.consumely.data.local.entity.ItemEntity
import com.android.consumely.data.local.entity.LocationEntity

data class ItemWithLocation(
    @Embedded val item: ItemEntity,
    @Relation(
        parentColumn = "locationId",
        entityColumn = "id"
    )
    val location: LocationEntity
)
