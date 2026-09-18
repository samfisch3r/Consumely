package com.android.consumely.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.consumely.data.local.AppDatabase
import com.android.consumely.data.local.entity.LocationEntity
import com.android.consumely.data.local.model.LocationType
import com.android.consumely.data.local.preferences.SettingsManager
import com.android.consumely.data.remote.jsonbin.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class SettingsUiState(
    val apiKey: String = "",
    val binId: String = "",
    val yellowThresholdMonths: Int = 6,
    val redThresholdMonths: Int = 9,
    val locations: List<LocationEntity> = emptyList(),
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val lastSyncedTime: Long = 0L
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val settingsManager = SettingsManager(application)
    private val syncRepository = SyncRepository(db)

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            apiKey = settingsManager.apiKey,
            binId = settingsManager.binId,
            yellowThresholdMonths = settingsManager.yellowThresholdMonths,
            redThresholdMonths = settingsManager.redThresholdMonths,
            lastSyncedTime = settingsManager.lastSyncedTimestamp
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            db.locationDao().getAllLocationsFlow().collect { locations ->
                _uiState.value = _uiState.value.copy(locations = locations)
            }
        }
    }

    fun onApiKeyChanged(key: String) {
        settingsManager.apiKey = key
        _uiState.value = _uiState.value.copy(apiKey = key)
    }

    fun onBinIdChanged(binId: String) {
        settingsManager.binId = binId
        _uiState.value = _uiState.value.copy(binId = binId)
    }

    fun onYellowThresholdChanged(months: Int) {
        val validMonths = months.coerceIn(1, 36)
        settingsManager.yellowThresholdMonths = validMonths
        _uiState.value = _uiState.value.copy(yellowThresholdMonths = validMonths)
    }

    fun onRedThresholdChanged(months: Int) {
        val validMonths = months.coerceIn(1, 36)
        settingsManager.redThresholdMonths = validMonths
        _uiState.value = _uiState.value.copy(redThresholdMonths = validMonths)
    }

    fun addCustomLocation(name: String, type: LocationType) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val location = LocationEntity(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                type = type,
                isDefault = false,
                sortOrder = _uiState.value.locations.size
            )
            db.locationDao().insertOrUpdate(location)
        }
    }

    fun deleteLocation(id: String) {
        viewModelScope.launch {
            db.locationDao().deleteLocation(id)
        }
    }

    fun triggerSync() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(isSyncing = true, syncMessage = null)

        viewModelScope.launch {
            val result = syncRepository.performSync(
                apiKey = currentState.apiKey,
                binId = currentState.binId
            )
            result.onSuccess {
                val now = System.currentTimeMillis()
                settingsManager.lastSyncedTimestamp = now
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    lastSyncedTime = now,
                    syncMessage = "SUCCESS"
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncMessage = error.message ?: "Sync Failed"
                )
            }
        }
    }
}
