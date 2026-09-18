package com.android.consumely.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.android.consumely.R
import com.android.consumely.data.local.AppDatabase
import com.android.consumely.data.local.preferences.SettingsManager
import com.android.consumely.ui.inventory.ItemStatusInfo
import com.android.consumely.ui.inventory.calculateItemStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

object NotificationHelper {

    private const val CHANNEL_ID = "consumely_expiry_channel"

    fun createNotificationChannel(context: Context) {
        val name = context.getString(R.string.notification_channel_name)
        val descriptionText = context.getString(R.string.notification_channel_desc)
        val channel = NotificationChannel(
            CHANNEL_ID,
            name,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = descriptionText
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    suspend fun checkAndNotifyExpiringItems(context: Context) = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return@withContext
            }
        }

        createNotificationChannel(context)

        val db = AppDatabase.getDatabase(context)
        val settings = SettingsManager(context)
        val itemsWithLoc = db.itemDao().getAllItemsWithLocationFlow().first()

        val yellowMonths = settings.yellowThresholdMonths
        val redMonths = settings.redThresholdMonths

        for (itemWithLoc in itemsWithLoc) {
            val item = itemWithLoc.item
            val location = itemWithLoc.location

            if (!item.hasNotifiedExpiry) {
                val status = calculateItemStatus(
                    expiryDate = item.expiryDate,
                    dateAdded = item.dateAdded,
                    locationType = location.type,
                    yellowMonths = yellowMonths,
                    redMonths = redMonths
                )

                if (status is ItemStatusInfo.ExpiringSoon || status is ItemStatusInfo.Expired || status is ItemStatusInfo.FreezerWarning || status is ItemStatusInfo.FreezerAlert) {
                    val title = item.name
                    val message = when (status) {
                        is ItemStatusInfo.ExpiringSoon -> context.getString(R.string.status_expiring_soon)
                        is ItemStatusInfo.Expired -> context.getString(R.string.status_expired)
                        is ItemStatusInfo.FreezerWarning -> context.getString(R.string.status_freezer_warning, yellowMonths)
                        is ItemStatusInfo.FreezerAlert -> context.getString(R.string.status_freezer_alert, redMonths)
                        else -> ""
                    }

                    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)

                    val notificationId = item.id.hashCode()
                    runCatching {
                        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
                        db.itemDao().updateHasNotifiedExpiry(item.id, true)
                    }
                }
            }
        }
    }
}
