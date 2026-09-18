package com.android.consumely.ui.inventory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.consumely.data.local.AppDatabase
import com.android.consumely.data.local.entity.LocationEntity
import com.android.consumely.data.local.model.ItemWithLocation
import com.android.consumely.data.local.preferences.SettingsManager
import com.android.consumely.data.remote.jsonbin.SyncRepository
import com.android.consumely.util.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class SortOrder {
    EXPIRY_DATE,
    DATE_ADDED,
    NAME,
    QUANTITY
}

data class InventoryUiState(
    val locations: List<LocationEntity> = emptyList(),
    val selectedLocationId: String? = null,
    val items: List<ItemWithLocation> = emptyList(),
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.EXPIRY_DATE,
    val yellowThresholdMonths: Int = 6,
    val redThresholdMonths: Int = 9
)

class InventoryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val settingsManager = SettingsManager(application)
    private val syncRepository = SyncRepository(db)

    private val _selectedLocationId = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(SortOrder.EXPIRY_DATE)

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching {
                syncRepository.syncIfConfigured(getApplication())
            }
            runCatching {
                NotificationHelper.checkAndNotifyExpiringItems(getApplication())
            }
        }

        viewModelScope.launch {
            combine(
                db.locationDao().getAllLocationsFlow(),
                db.itemDao().getAllItemsWithLocationFlow(),
                _selectedLocationId,
                _searchQuery,
                _sortOrder
            ) { locations, allItemsWithLoc, selectedLocId, query, sort ->
                val filteredByLoc = if (selectedLocId == null) {
                    allItemsWithLoc
                } else {
                    allItemsWithLoc.filter { it.item.locationId == selectedLocId }
                }

                val filteredByQuery = if (query.isBlank()) {
                    filteredByLoc
                } else {
                    filteredByLoc.filter {
                        it.item.name.contains(query, ignoreCase = true) ||
                        it.location.name.contains(query, ignoreCase = true)
                    }
                }

                val sorted = when (sort) {
                    SortOrder.EXPIRY_DATE -> filteredByQuery.sortedWith(
                        compareBy<ItemWithLocation> { it.item.expiryDate ?: Long.MAX_VALUE }
                            .thenByDescending { it.item.dateAdded }
                    )
                    SortOrder.DATE_ADDED -> filteredByQuery.sortedByDescending { it.item.dateAdded }
                    SortOrder.NAME -> filteredByQuery.sortedBy { it.item.name.lowercase() }
                    SortOrder.QUANTITY -> filteredByQuery.sortedByDescending { it.item.quantity }
                }

                InventoryUiState(
                    locations = locations,
                    selectedLocationId = selectedLocId,
                    items = sorted,
                    searchQuery = query,
                    sortOrder = sort,
                    yellowThresholdMonths = settingsManager.yellowThresholdMonths,
                    redThresholdMonths = settingsManager.redThresholdMonths
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun selectLocation(locationId: String?) {
        _selectedLocationId.value = locationId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(sortOrder: SortOrder) {
        _sortOrder.value = sortOrder
    }

    fun updateQuantity(itemId: String, currentQuantity: Int, delta: Int) {
        val newQuantity = (currentQuantity + delta).coerceAtLeast(0)
        viewModelScope.launch {
            if (newQuantity == 0) {
                db.itemDao().softDelete(itemId)
            } else {
                db.itemDao().updateQuantity(itemId, newQuantity)
            }
            runCatching {
                syncRepository.syncIfConfigured(getApplication())
            }
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            db.itemDao().softDelete(itemId)
            runCatching {
                syncRepository.syncIfConfigured(getApplication())
            }
        }
    }
}
