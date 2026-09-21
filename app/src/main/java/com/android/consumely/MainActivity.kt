package com.android.consumely

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.android.consumely.ui.addedit.AddEditItemScreen
import com.android.consumely.ui.addedit.AddEditViewModel
import com.android.consumely.ui.inventory.InventoryScreen
import com.android.consumely.ui.inventory.InventoryViewModel
import com.android.consumely.ui.scanner.BarcodeScannerScreen
import com.android.consumely.ui.settings.SettingsScreen
import com.android.consumely.ui.settings.SettingsViewModel
import com.android.consumely.ui.theme.ConsumelyTheme
import com.android.consumely.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ConsumelyTheme {
                RequestNotificationPermissionEffect()
                MainAppScreen()
            }
        }
    }
}

@Composable
fun RequestNotificationPermissionEffect() {
    val context = LocalContext.current
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted ->
                if (isGranted) {
                    CoroutineScope(Dispatchers.IO).launch {
                        NotificationHelper.checkAndNotifyExpiringItems(context)
                    }
                }
            }
        )

        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    val inventoryViewModel: InventoryViewModel = viewModel()
    val addEditViewModel: AddEditViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            if (currentDestination == "inventory") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = { navController.navigate("scanner") },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = stringResource(R.string.nav_scanner))
                    }

                    FloatingActionButton(
                        onClick = { navController.navigate("add_edit") }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.nav_add_item))
                    }
                }
            }
        }
    ) { outerPadding ->
        NavHost(
            navController = navController,
            startDestination = "inventory",
            modifier = Modifier.padding(bottom = outerPadding.calculateBottomPadding())
        ) {
            composable("inventory") {
                InventoryScreen(
                    viewModel = inventoryViewModel,
                    onEditItemClick = { id -> navController.navigate("add_edit?itemId=$id") },
                    onSettingsClick = { navController.navigate("settings") }
                )
            }

            composable(
                route = "add_edit?itemId={itemId}&scannedBarcode={scannedBarcode}",
                arguments = listOf(
                    navArgument("itemId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("scannedBarcode") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId")
                val scannedBarcode = backStackEntry.arguments?.getString("scannedBarcode")

                AddEditItemScreen(
                    viewModel = addEditViewModel,
                    itemId = itemId,
                    scannedBarcode = scannedBarcode,
                    onNavigateBack = { navController.popBackStack() },
                    onScanBarcodeClick = { navController.navigate("scanner") }
                )
            }

            composable("scanner") {
                BarcodeScannerScreen(
                    onBarcodeScanned = { barcode ->
                        navController.navigate("add_edit?scannedBarcode=$barcode") {
                            popUpTo("inventory")
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("settings") {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
