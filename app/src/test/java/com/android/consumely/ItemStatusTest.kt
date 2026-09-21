package com.android.consumely

import com.android.consumely.data.local.model.LocationType
import com.android.consumely.ui.inventory.ItemStatusGroup
import com.android.consumely.ui.inventory.calculateItemStatusGroup
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class ItemStatusTest {

    @Test
    fun testFreshItem() {
        val now = System.currentTimeMillis()
        val status = calculateItemStatusGroup(
            expiryDate = null,
            dateAdded = now - TimeUnit.DAYS.toMillis(30),
            locationType = LocationType.FREEZER,
            yellowMonths = 6,
            redMonths = 9
        )
        assertEquals(ItemStatusGroup.FRESH, status)
    }

    @Test
    fun testFreezerYellowWarning() {
        val now = System.currentTimeMillis()
        val status = calculateItemStatusGroup(
            expiryDate = null,
            dateAdded = now - TimeUnit.DAYS.toMillis(200),
            locationType = LocationType.FREEZER,
            yellowMonths = 6,
            redMonths = 9
        )
        assertEquals(ItemStatusGroup.EXPIRING_SOON, status)
    }

    @Test
    fun testFreezerRedAlert() {
        val now = System.currentTimeMillis()
        val status = calculateItemStatusGroup(
            expiryDate = null,
            dateAdded = now - TimeUnit.DAYS.toMillis(300),
            locationType = LocationType.FREEZER,
            yellowMonths = 6,
            redMonths = 9
        )
        assertEquals(ItemStatusGroup.EXPIRED, status)
    }

    @Test
    fun testExplicitExpiryDateTakesPrecedence() {
        val now = System.currentTimeMillis()
        val status = calculateItemStatusGroup(
            expiryDate = now + TimeUnit.DAYS.toMillis(10),
            dateAdded = now - TimeUnit.DAYS.toMillis(300),
            locationType = LocationType.FREEZER,
            yellowMonths = 6,
            redMonths = 9
        )
        assertEquals(ItemStatusGroup.FRESH, status)
    }

    @Test
    fun testGroupPriorities() {
        assertEquals(0, ItemStatusGroup.EXPIRED.priority)
        assertEquals(1, ItemStatusGroup.EXPIRING_SOON.priority)
        assertEquals(2, ItemStatusGroup.FRESH.priority)
    }
}
