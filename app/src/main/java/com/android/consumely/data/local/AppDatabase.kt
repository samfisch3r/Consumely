package com.android.consumely.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.android.consumely.data.local.converter.Converters
import com.android.consumely.data.local.dao.ItemDao
import com.android.consumely.data.local.dao.LocationDao
import com.android.consumely.data.local.entity.ItemEntity
import com.android.consumely.data.local.entity.LocationEntity
import com.android.consumely.data.local.model.LocationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [LocationEntity::class, ItemEntity::class],
    version = 2,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ],
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun locationDao(): LocationDao
    abstract fun itemDao(): ItemDao

    companion object {
        const val PANTRY_ID = "location_pantry_default"
        const val FRIDGE_ID = "location_fridge_default"
        const val FREEZER_ID = "location_freezer_default"
        const val BASEMENT_ID = "location_basement_default"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "consumely_database"
                )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            getDatabase(context).locationDao().insertAll(
                                listOf(
                                    LocationEntity(
                                        id = PANTRY_ID,
                                        name = "Pantry",
                                        type = LocationType.PANTRY,
                                        isDefault = true,
                                        sortOrder = 0
                                    ),
                                    LocationEntity(
                                        id = FRIDGE_ID,
                                        name = "Fridge",
                                        type = LocationType.FRIDGE,
                                        isDefault = true,
                                        sortOrder = 1
                                    ),
                                    LocationEntity(
                                        id = FREEZER_ID,
                                        name = "Freezer",
                                        type = LocationType.FREEZER,
                                        isDefault = true,
                                        sortOrder = 2
                                    ),
                                    LocationEntity(
                                        id = BASEMENT_ID,
                                        name = "Basement",
                                        type = LocationType.PANTRY,
                                        isDefault = true,
                                        sortOrder = 3
                                    )
                                )
                            )
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
