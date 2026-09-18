package com.android.consumely.data.local.entity

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.android.consumely.R
import com.android.consumely.data.local.AppDatabase
import com.android.consumely.data.local.model.LocationType

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: LocationType,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0
)

@Composable
fun LocationEntity.getDisplayName(): String {
    return if (isDefault) {
        when (id) {
            AppDatabase.PANTRY_ID -> stringResource(R.string.location_pantry)
            AppDatabase.FRIDGE_ID -> stringResource(R.string.location_fridge)
            AppDatabase.FREEZER_ID -> stringResource(R.string.location_freezer)
            AppDatabase.BASEMENT_ID -> stringResource(R.string.location_basement)
            else -> name
        }
    } else {
        name
    }
}
