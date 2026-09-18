package com.android.consumely

import com.android.consumely.data.local.model.LocationType
import com.android.consumely.ui.inventory.ItemStatusInfo
import com.android.consumely.ui.inventory.calculateItemStatus
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class ItemStatusTest {

    @Test
    fun testFreshItem() {
        val now = System.currentTimeMillis()
        val status = calculateItemStatus(
            expiryDate = null,
            dateAdded = now - TimeUnit.DAYS.toMillis(30),
            locationType = LocationType.FREEZER,
            yellowMonths = 6,
            redMonths = 9
        )
        assertTrue(status is ItemStatusInfo.Fresh)
    }

    @Test
    fun testFreezerYellowWarning() {
        val now = System.currentTimeMillis()
        val status = calculateItemStatus(
            expiryDate = null,
            dateAdded = now - TimeUnit.DAYS.toMillis(200),
            locationType = LocationType.FREEZER,
            yellowMonths = 6,
            redMonths = 9
        )
        assertTrue(status is ItemStatusInfo.FreezerWarning)
    }

    @Test
    fun testFreezerRedAlert() {
        val now = System.currentTimeMillis()
        val status = calculateItemStatus(
            expiryDate = null,
            dateAdded = now - TimeUnit.DAYS.toMillis(300),
            locationType = LocationType.FREEZER,
            yellowMonths = 6,
            redMonths = 9
        )
        assertTrue(status is ItemStatusInfo.FreezerAlert)
    }

    @Test
    fun testExplicitExpiryDateTakesPrecedence() {
        val now = System.currentTimeMillis()
        val status = calculateItemStatus(
            expiryDate = now + TimeUnit.DAYS.toMillis(10),
            dateAdded = now - TimeUnit.DAYS.toMillis(300),
            locationType = LocationType.FREEZER,
            yellowMonths = 6,
            redMonths = 9
        )
        assertTrue(status is ItemStatusInfo.Fresh)
    }
}
