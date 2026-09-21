package com.android.consumely.ui.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.consumely.R
import com.android.consumely.data.local.entity.getDisplayName
import com.android.consumely.data.local.model.ItemWithLocation
import com.android.consumely.data.local.model.LocationType
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    onEditItemClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.nav_settings)
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Search & Sort Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text(stringResource(R.string.hint_search)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp)
                )

                Box {
                    IconButton(onClick = { sortMenuExpanded = true }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.sort_by))
                    }
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_expiry_date)) },
                            onClick = {
                                viewModel.setSortOrder(SortOrder.EXPIRY_DATE)
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_date_added)) },
                            onClick = {
                                viewModel.setSortOrder(SortOrder.DATE_ADDED)
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_name)) },
                            onClick = {
                                viewModel.setSortOrder(SortOrder.NAME)
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_quantity)) },
                            onClick = {
                                viewModel.setSortOrder(SortOrder.QUANTITY)
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Location Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedLocationId == null,
                        onClick = { viewModel.selectLocation(null) },
                        label = { Text(stringResource(R.string.location_all)) }
                    )
                }
                items(uiState.locations, key = { it.id }) { location ->
                    FilterChip(
                        selected = uiState.selectedLocationId == location.id,
                        onClick = { viewModel.selectLocation(location.id) },
                        label = { Text(location.getDisplayName()) }
                    )
                }
            }

            // Item List or Empty State
            if (uiState.items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.msg_no_items),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(uiState.items, key = { it.item.id }) { itemWithLoc ->
                        ItemCard(
                            itemWithLoc = itemWithLoc,
                            yellowThresholdMonths = uiState.yellowThresholdMonths,
                            redThresholdMonths = uiState.redThresholdMonths,
                            onCardClick = { onEditItemClick(itemWithLoc.item.id) },
                            onIncrement = { viewModel.updateQuantity(itemWithLoc.item.id, itemWithLoc.item.quantity, 1) },
                            onDecrement = { viewModel.updateQuantity(itemWithLoc.item.id, itemWithLoc.item.quantity, -1) },
                            onDelete = { viewModel.deleteItem(itemWithLoc.item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ItemCard(
    itemWithLoc: ItemWithLocation,
    yellowThresholdMonths: Int,
    redThresholdMonths: Int,
    onCardClick: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onDelete: () -> Unit
) {
    val item = itemWithLoc.item
    val location = itemWithLoc.location

    val statusGroup = remember(item, location, yellowThresholdMonths, redThresholdMonths) {
        calculateItemStatusGroup(item.expiryDate, item.dateAdded, location.type, yellowThresholdMonths, redThresholdMonths)
    }

    val isDark = isSystemInDarkTheme()
    val cardBgColor = if (isDark) statusGroup.containerColorDark else statusGroup.containerColorLight

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left Column: Item Name, Location & Dates
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${location.getDisplayName()} • ${stringResource(R.string.label_date_added)}: ${formatDate(item.dateAdded)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                item.expiryDate?.let { exp ->
                    Text(
                        text = "${stringResource(R.string.label_expiry_date)}: ${formatDate(exp)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (exp < System.currentTimeMillis()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right Row: Quantity Stepper & Delete Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedIconButton(
                    onClick = onDecrement,
                    modifier = Modifier.size(32.dp)
                ) {
                    Text("-", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Text(
                    text = item.quantity.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                OutlinedIconButton(
                    onClick = onIncrement,
                    modifier = Modifier.size(32.dp)
                ) {
                    Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.btn_delete),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

enum class ItemStatusGroup(
    val priority: Int,
    val containerColorLight: Color,
    val containerColorDark: Color
) {
    EXPIRED(
        priority = 0,
        containerColorLight = Color(0xFFFFEBEE), // Soft Red
        containerColorDark = Color(0xFF5A2B2B)   // Lighter soft red for dark mode
    ),
    EXPIRING_SOON(
        priority = 1,
        containerColorLight = Color(0xFFFFF8E1), // Soft Yellow / Amber
        containerColorDark = Color(0xFF52441E)   // Lighter soft amber for dark mode
    ),
    FRESH(
        priority = 2,
        containerColorLight = Color(0xFFE8F5E9), // Soft Green
        containerColorDark = Color(0xFF29452E)   // Lighter soft green for dark mode
    )
}

fun calculateItemStatusGroup(
    expiryDate: Long?,
    dateAdded: Long,
    locationType: LocationType,
    yellowMonths: Int,
    redMonths: Int
): ItemStatusGroup {
    val now = System.currentTimeMillis()

    if (expiryDate != null) {
        val daysRemaining = TimeUnit.MILLISECONDS.toDays(expiryDate - now)
        return when {
            daysRemaining < 0 -> ItemStatusGroup.EXPIRED
            daysRemaining <= 3 -> ItemStatusGroup.EXPIRING_SOON
            else -> ItemStatusGroup.FRESH
        }
    }

    if (locationType == LocationType.FREEZER) {
        val daysInFreezer = TimeUnit.MILLISECONDS.toDays(now - dateAdded)
        val yellowDays = yellowMonths * 30L
        val redDays = redMonths * 30L

        return when {
            daysInFreezer >= redDays -> ItemStatusGroup.EXPIRED
            daysInFreezer >= yellowDays -> ItemStatusGroup.EXPIRING_SOON
            else -> ItemStatusGroup.FRESH
        }
    }

    return ItemStatusGroup.FRESH
}

fun formatDate(millis: Long): String {
    return DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))
}
