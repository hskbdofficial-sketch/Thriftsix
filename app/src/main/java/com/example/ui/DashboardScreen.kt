package com.example.ui

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodels.ThriftSixViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(viewModel: ThriftSixViewModel) {
    val products by viewModel.allProducts.collectAsState()
    val orders by viewModel.allOrders.collectAsState()
    val returns by viewModel.allReturns.collectAsState()
    val syncState by viewModel.syncStatus.collectAsState()

    // Calculate financials
    val totalRevenue = orders.sumOf { it.totalAmount }
    val totalDiscounts = orders.sumOf { it.discount }
    val totalExpenses = orders.sumOf { it.expense }

    // Cost of goods sold based on purchased items
    var totalCostOfGoods = 0.0
    orders.forEach { order ->
        order.getItemsList().forEach { spec ->
            val matchingProd = products.find { it.sku == spec.sku }
            val unitCost = matchingProd?.costPrice ?: (spec.price * 0.4) // default 40% cost if missing
            totalCostOfGoods += unitCost * spec.qty
        }
    }

    // Returns loss tracking
    val totalReturnsQty = returns.sumOf { it.quantity }
    val estimatedReturnsLoss = returns.sumOf { r ->
        val prod = products.find { it.sku == r.sku }
        (prod?.sellingPrice ?: 0.0) * r.quantity
    }

    val netProfit = (totalRevenue - totalCostOfGoods - totalExpenses - totalDiscounts).coerceAtAtLeastZero()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("dashboard_screen_container")
    ) {
        // --- DYNAMIC BRAND SHOP HEADER & SETTINGS ---
        var showSettingsDialog by remember { mutableStateOf(false) }
        if (showSettingsDialog) {
            ShopSettingsDialog(viewModel = viewModel, onDismiss = { showSettingsDialog = false })
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = viewModel.shopName.value.uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Operator: " + viewModel.shopOwner.value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            IconButton(
                onClick = { showSettingsDialog = true },
                modifier = Modifier.testTag("dashboard_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Shop Settings",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Sync Indicator
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (syncState?.syncPending == true)
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                    else
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clickable { viewModel.triggerSyncNow() }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (syncState?.syncPending == true) Icons.Default.Sync else Icons.Default.CloudQueue,
                        contentDescription = "Sync",
                        tint = if (syncState?.syncPending == true)
                            MaterialTheme.colorScheme.secondary
                        else
                            MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .size(16.dp)
                            .testTag("sync_indicator_icon")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (syncState?.syncPending == true) "Syncing..." else "Synced",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (syncState?.syncPending == true)
                            MaterialTheme.colorScheme.secondary
                        else
                            MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- PROFIT AND LOSS ANALYTICAL STATS ---
        Text(
            text = "Profit & Loss Executive Summary",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Two-column grid of financial stats
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            maxItemsInEachRow = 2,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val itemWidthModifier = Modifier
                .weight(1f)
                .widthIn(min = 150.dp)

            // Net Profit Card
            StatCard(
                title = "Net Profits (BDT)",
                value = "৳${String.format("%,.2f", netProfit)}",
                info = "Revenue less cost & expenses",
                icon = Icons.Default.MonetizationOn,
                color = MaterialTheme.colorScheme.primary,
                modifier = itemWidthModifier
            )

            // Gross Revenue Card
            StatCard(
                title = "Total Revenue",
                value = "৳${String.format("%,.2f", totalRevenue)}",
                info = "$${orders.size} completed sales",
                icon = Icons.Default.TrendingUp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = itemWidthModifier
            )

            // Dynamic Costs Card
            StatCard(
                title = "Cost of Goods",
                value = "৳${String.format("%,.2f", totalCostOfGoods)}",
                info = "Initial wholesale buying price",
                icon = Icons.Default.ShoppingBag,
                color = Color.LightGray,
                modifier = itemWidthModifier
            )

            // Expenses & Returns Card
            StatCard(
                title = "Fees & Lost Stock",
                value = "৳${String.format("%,.0f", totalExpenses + estimatedReturnsLoss)}",
                info = "$totalReturnsQty returned units",
                icon = Icons.Default.AssignmentReturn,
                color = Color.Red,
                modifier = itemWidthModifier
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- CUSTOM DRAWN ANALYTICAL TREND CHART ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Weekly Financial Performance Trends",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Live offline sync area chart tracker • Currency in BDT (৳)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Custom charts drawn on Canvas!
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.0f)
                ) {
                    val lineColor = MaterialTheme.colorScheme.primary
                    val areaColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)

                    val dataPoints = remember(orders) {
                        if (orders.isEmpty()) {
                            listOf(0.0, 4200.0, 3100.0, 7500.0, 4100.0, 8900.0, 11500.0)
                        } else {
                            // Accumulate orders value by days
                            val list = mutableListOf(0.0)
                            var cum = 0.0
                            orders.reversed().forEach {
                                cum += it.totalAmount
                                list.add(cum)
                            }
                            while (list.size < 7) {
                                list.add(0, 0.0)
                            }
                            list.takeLast(7)
                        }
                    }

                    Canvas(modifier = Modifier.fillMaxSize().testTag("performance_trend_chart")) {
                        val width = size.width
                        val height = size.height

                        // Draw Grid lines
                        val steps = 4
                        for (i in 0..steps) {
                            val y = height - (height / steps) * i
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Determine scales
                        val maxVal = (dataPoints.maxOrNull() ?: 1.0).coerceAtLeast(100.0)
                        val pointsCount = dataPoints.size
                        val stepX = width / (pointsCount - 1)

                        // Compose Path
                        val path = Path()
                        val fillPath = Path()

                        dataPoints.forEachIndexed { i, valPoint ->
                            val x = stepX * i
                            val y = height - ((valPoint / maxVal) * (height - 20.dp.toPx())).toFloat() - 10.dp.toPx()

                            if (i == 0) {
                                path.moveTo(x, y)
                                fillPath.moveTo(x, height)
                                fillPath.lineTo(x, y)
                            } else {
                                path.lineTo(x, y)
                                fillPath.lineTo(x, y)
                            }

                            if (i == pointsCount - 1) {
                                fillPath.lineTo(x, height)
                                fillPath.close()
                            }

                            // Draw point circle
                            drawCircle(
                                color = lineColor,
                                radius = 4.dp.toPx(),
                                center = Offset(x, y)
                            )
                        }

                        // Draw filled area
                        drawPath(
                            path = fillPath,
                            color = areaColor
                        )

                        // Draw sleek line
                        drawPath(
                            path = path,
                            color = lineColor,
                            style = Stroke(width = 3.dp.toPx(), join = StrokeJoin.Round)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // X-Axis Labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    days.forEach {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- EXPORT EXPENSES & REPORTS SECTION ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Need CSV/PDF Ledger?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Export complete catalog pricing audits & sales transaction ledgers to device downloads.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Button(
                    onClick = {
                        viewModel.updateSessionActivity()
                        // Mock downloading audits
                        viewModel.triggerSyncNow()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("export_reports_button")
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = "Export")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Export", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    info: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(115.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = value,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp
            )

            Text(
                text = info,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                maxLines = 1
            )
        }
    }
}

fun Double.coerceAtAtLeastZero(): Double = if (this < 0.0) 0.0 else this
