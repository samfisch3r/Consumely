package com.android.consumely.ui.addedit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.consumely.data.local.AppDatabase
import com.android.consumely.data.local.entity.ItemEntity
import com.android.consumely.data.local.entity.LocationEntity
import com.android.consumely.data.remote.jsonbin.SyncRepository
import com.android.consumely.data.remote.openfoodfacts.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class AddEditUiState(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val locationId: String = AppDatabase.PANTRY_ID,
    val quantity: Int = 1,
    val unit: String? = null,
    val dateAdded: Long = System.currentTimeMillis(),
    val expiryDate: Long? = null,
    val barcode: String? = null,
    val locations: List<LocationEntity> = emptyList(),
    val isEditMode: Boolean = false,
    val isLoadingBarcode: Boolean = false,
    val barcodeError: String? = null,
    val isSaved: Boolean = false
)

class AddEditViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val productRepository = ProductRepository()
    private val syncRepository = SyncRepository(db)

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            db.locationDao().getAllLocationsFlow().collect { locations ->
                _uiState.value = _uiState.value.copy(
                    locations = locations,
                    locationId = if (_uiState.value.locationId.isEmpty() && locations.isNotEmpty()) locations.first().id else _uiState.value.locationId
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = AddEditUiState(
            id = UUID.randomUUID().toString(),
            name = "",
            locationId = if (_uiState.value.locations.isNotEmpty()) _uiState.value.locations.first().id else AppDatabase.PANTRY_ID,
            quantity = 1,
            unit = null,
            dateAdded = System.currentTimeMillis(),
            expiryDate = null,
            barcode = null,
            locations = _uiState.value.locations,
            isEditMode = false,
            isLoadingBarcode = false,
            barcodeError = null,
            isSaved = false
        )
    }

    fun loadItem(itemId: String?) {
        if (itemId.isNullOrBlank()) {
            resetState()
            return
        }
        viewModelScope.launch {
            val item = db.itemDao().getItemById(itemId)
            if (item != null) {
                _uiState.value = _uiState.value.copy(
                    id = item.id,
                    name = item.name,
                    locationId = item.locationId,
                    quantity = item.quantity,
                    unit = item.unit,
                    dateAdded = item.dateAdded,
                    expiryDate = item.expiryDate,
                    barcode = item.barcode,
                    isEditMode = true,
                    isSaved = false
                )
            }
        }
    }

    fun onNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun onLocationChanged(locationId: String) {
        _uiState.value = _uiState.value.copy(locationId = locationId)
    }

    fun onQuantityChanged(quantity: Int) {
        _uiState.value = _uiState.value.copy(quantity = quantity.coerceAtLeast(1))
    }

    fun onExpiryDateChanged(expiryDate: Long?) {
        _uiState.value = _uiState.value.copy(expiryDate = expiryDate)
    }

    fun onBarcodeScanned(barcode: String) {
        _uiState.value = _uiState.value.copy(
            barcode = barcode,
            isLoadingBarcode = true,
            barcodeError = null
        )
        viewModelScope.launch {
            val result = productRepository.fetchProductName(barcode)
            result.onSuccess { productName ->
                _uiState.value = _uiState.value.copy(
                    name = if (_uiState.value.name.isBlank()) productName else _uiState.value.name,
                    isLoadingBarcode = false
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoadingBarcode = false,
                    barcodeError = error.message
                )
            }
        }
    }

    fun saveItem() {
        val currentState = _uiState.value
        if (currentState.name.isBlank()) return

        viewModelScope.launch {
            val item = ItemEntity(
                id = currentState.id,
                name = currentState.name.trim(),
                locationId = currentState.locationId,
                quantity = currentState.quantity,
                unit = currentState.unit,
                dateAdded = currentState.dateAdded,
                expiryDate = currentState.expiryDate,
                barcode = currentState.barcode,
                lastUpdated = System.currentTimeMillis(),
                isDeleted = false
            )
            db.itemDao().insertOrUpdate(item)
            _uiState.value = _uiState.value.copy(isSaved = true)
            runCatching {
                syncRepository.syncIfConfigured(getApplication())
            }
        }
    }
}
