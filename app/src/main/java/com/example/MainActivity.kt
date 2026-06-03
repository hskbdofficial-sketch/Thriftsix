package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.ui.*
import com.example.ui.theme.ThriftSixTheme
import com.example.viewmodels.ThriftSixViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: ThriftSixViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // --- SECURITY SESSION TIMEOUT MONITORING COROUTINE ---
        lifecycleScope.launch {
            while (true) {
                delay(10000) // check every 10 seconds
                viewModel.checkSessionTimeout()
            }
        }

        setContent {
            ThriftSixTheme {
                val loggedIn by viewModel.isLoggedIn

                if (!loggedIn) {
                    AuthScreen(viewModel = viewModel)
                } else {
                    MainAppScaffold(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(viewModel: ThriftSixViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val products by viewModel.allProducts.collectAsState()
    val lowStockCount = remember(products) {
        products.count { prod ->
            prod.getSizesAndStocksMap().values.any { it <= 3 }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_app_scaffold"),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("app_bottom_bar")
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        viewModel.updateSessionActivity()
                    },
                    icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Dashboard") },
                    label = { Text("Dashboard", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("nav_dashboard")
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        viewModel.updateSessionActivity()
                    },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (lowStockCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = Color.White
                                    ) {
                                        Text("$lowStockCount")
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.ShoppingBag, contentDescription = "Inventory")
                        }
                    },
                    label = { Text("Showcase", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("nav_inventory")
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        viewModel.updateSessionActivity()
                    },
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = "Scanner") },
                    label = { Text("Scanner", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("nav_scanner")
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = {
                        selectedTab = 3
                        viewModel.updateSessionActivity()
                    },
                    icon = { Icon(Icons.Default.Receipt, contentDescription = "Invoices") },
                    label = { Text("Invoices", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("nav_invoices")
                )

                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = {
                        selectedTab = 4
                        viewModel.updateSessionActivity()
                    },
                    icon = { Icon(Icons.Default.GroupAdd, contentDescription = "Team") },
                    label = { Text("Team", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.testTag("nav_team")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (lowStockCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .clickable { selectedTab = 1 }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Low Stock Warning: $lowStockCount item style(s) have sizing stock tiers <= 3! Tap to replenish.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Replenish Link",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    0 -> DashboardScreen(viewModel = viewModel)
                    1 -> InventoryScreen(viewModel = viewModel)
                    2 -> ScannerScreen(viewModel = viewModel)
                    3 -> OrderInvoiceScreen(viewModel = viewModel)
                    4 -> TeamScreen(viewModel = viewModel)
                }
            }
        }
    }
}
