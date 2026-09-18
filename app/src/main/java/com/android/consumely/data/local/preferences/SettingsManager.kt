package com.android.consumely.data.local.preferences

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("consumely_prefs", Context.MODE_PRIVATE)

    var apiKey: String
        get() = prefs.getString("jsonbin_api_key", "") ?: ""
        set(value) = prefs.edit().putString("jsonbin_api_key", value).apply()

    var binId: String
        get() = prefs.getString("jsonbin_bin_id", "") ?: ""
        set(value) = prefs.edit().putString("jsonbin_bin_id", value).apply()

    var yellowThresholdMonths: Int
        get() = prefs.getInt("yellow_threshold_months", 6)
        set(value) = prefs.edit().putInt("yellow_threshold_months", value).apply()

    var redThresholdMonths: Int
        get() = prefs.getInt("red_threshold_months", 9)
        set(value) = prefs.edit().putInt("red_threshold_months", value).apply()

    var lastSyncedTimestamp: Long
        get() = prefs.getLong("last_synced_time", 0L)
        set(value) = prefs.edit().putLong("last_synced_time", value).apply()
}
