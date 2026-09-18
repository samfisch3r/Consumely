package com.android.consumely.data.remote.jsonbin

import android.content.Context
import com.android.consumely.data.local.AppDatabase
import com.android.consumely.data.local.entity.ItemEntity
import com.android.consumely.data.local.entity.LocationEntity
import com.android.consumely.data.local.model.LocationType
import com.android.consumely.data.local.preferences.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class SyncRepository(private val db: AppDatabase) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.jsonbin.io/")
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val jsonBinApi = retrofit.create(JsonBinApi::class.java)

    suspend fun syncIfConfigured(context: Context) {
        val settings = SettingsManager(context)
        val key = settings.apiKey.trim()
        val bin = settings.binId.trim()
        if (key.isNotBlank() && bin.isNotBlank()) {
            performSync(key, bin)
        }
    }

    suspend fun performSync(apiKey: String, binId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanApiKey = apiKey.trim()
            val cleanBinId = binId.trim()

            if (cleanApiKey.isBlank() || cleanBinId.isBlank()) {
                error("API Key and Bin ID must not be empty.")
            }

            val itemDao = db.itemDao()
            val locationDao = db.locationDao()

            // 1. Fetch remote data from JSONBin using X-Access-Key
            val response = jsonBinApi.getBin(
                binId = cleanBinId,
                apiKey = cleanApiKey
            )

            val remotePayload = when {
                response.isSuccessful -> {
                    val rawRecord = response.body()?.record
                    if (rawRecord != null) {
                        runCatching {
                            json.decodeFromJsonElement<SyncPayload>(rawRecord)
                        }.getOrDefault(SyncPayload())
                    } else {
                        SyncPayload()
                    }
                }
                response.code() == 404 || response.code() == 400 -> {
                    // New or uninitialized Bin on JSONBin: proceed with empty remote payload
                    SyncPayload()
                }
                response.code() == 401 || response.code() == 403 -> {
                    error("Invalid JSONBin Access Key or access forbidden (${response.code()})")
                }
                else -> {
                    error("Failed to connect to JSONBin (${response.code()}: ${response.message()})")
                }
            }

            // 2. Fetch local data
            val localItems = itemDao.getAllItemsIncludingDeleted()
            val localLocations = locationDao.getAllLocations()

            val localItemMap = localItems.associateBy { it.id }.toMutableMap()
            val remoteItemMap = remotePayload.items.associateBy { it.id }

            val localLocationMap = localLocations.associateBy { it.id }.toMutableMap()
            val remoteLocationMap = remotePayload.locations.associateBy { it.id }

            // 3. Merge Locations
            val mergedLocations = mutableListOf<LocationEntity>()
            val allLocationIds = localLocationMap.keys + remoteLocationMap.keys
            for (id in allLocationIds) {
                val localLoc = localLocationMap[id]
                val remoteLoc = remoteLocationMap[id]

                if (localLoc != null) {
                    mergedLocations.add(localLoc)
                } else if (remoteLoc != null) {
                    mergedLocations.add(
                        LocationEntity(
                            id = remoteLoc.id,
                            name = remoteLoc.name,
                            type = runCatching { LocationType.valueOf(remoteLoc.type) }.getOrDefault(LocationType.PANTRY),
                            isDefault = remoteLoc.isDefault,
                            sortOrder = remoteLoc.sortOrder
                        )
                    )
                }
            }

            // Update local locations in Room
            locationDao.insertAll(mergedLocations)

            // 4. Merge Items using Last-Write-Wins (LWW)
            val mergedItems = mutableListOf<ItemEntity>()
            val allItemIds = localItemMap.keys + remoteItemMap.keys

            for (id in allItemIds) {
                val localItem = localItemMap[id]
                val remoteItem = remoteItemMap[id]

                if (localItem != null && remoteItem != null) {
                    if (localItem.lastUpdated >= remoteItem.lastUpdated) {
                        mergedItems.add(localItem)
                    } else {
                        mergedItems.add(
                            ItemEntity(
                                id = remoteItem.id,
                                name = remoteItem.name,
                                locationId = remoteItem.locationId,
                                quantity = remoteItem.quantity,
                                unit = remoteItem.unit,
                                dateAdded = remoteItem.dateAdded,
                                expiryDate = remoteItem.expiryDate,
                                imagePath = localItem.imagePath,
                                barcode = remoteItem.barcode,
                                lastUpdated = remoteItem.lastUpdated,
                                isDeleted = remoteItem.isDeleted
                            )
                        )
                    }
                } else if (localItem != null) {
                    mergedItems.add(localItem)
                } else if (remoteItem != null) {
                    mergedItems.add(
                        ItemEntity(
                            id = remoteItem.id,
                            name = remoteItem.name,
                            locationId = remoteItem.locationId,
                            quantity = remoteItem.quantity,
                            unit = remoteItem.unit,
                            dateAdded = remoteItem.dateAdded,
                            expiryDate = remoteItem.expiryDate,
                            imagePath = null,
                            barcode = remoteItem.barcode,
                            lastUpdated = remoteItem.lastUpdated,
                            isDeleted = remoteItem.isDeleted
                        )
                    )
                }
            }

            // Save merged items locally to Room
            itemDao.insertAll(mergedItems)

            // 5. Upload merged dataset back to JSONBin
            val newPayload = SyncPayload(
                lastUpdated = System.currentTimeMillis(),
                locations = mergedLocations.map { loc ->
                    SyncLocationDto(
                        id = loc.id,
                        name = loc.name,
                        type = loc.type.name,
                        isDefault = loc.isDefault,
                        sortOrder = loc.sortOrder
                    )
                },
                items = mergedItems.map { item ->
                    SyncItemDto(
                        id = item.id,
                        name = item.name,
                        locationId = item.locationId,
                        quantity = item.quantity,
                        unit = item.unit,
                        dateAdded = item.dateAdded,
                        expiryDate = item.expiryDate,
                        barcode = item.barcode,
                        lastUpdated = item.lastUpdated,
                        isDeleted = item.isDeleted
                    )
                }
            )

            val updateResp = jsonBinApi.updateBin(
                binId = cleanBinId,
                apiKey = cleanApiKey,
                payload = newPayload
            )

            if (!updateResp.isSuccessful) {
                error("Failed to update JSONBin: ${updateResp.code()} ${updateResp.message()}")
            }
        }
    }
}
