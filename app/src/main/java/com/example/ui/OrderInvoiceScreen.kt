package com.example.ui

import com.example.ui.theme.ThriftCoral
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.Order
import com.example.data.OrderItemSpec
import com.example.viewmodels.ThriftSixViewModel
import java.text.SimpleDateFormat
import java.util.*

import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas

private fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
    return try {
        val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) {
        null
    }
}

@Composable
fun BrandedInvoiceQrCode(data: String, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sizePx = size.width
        val cellsCount = 21 // Version 1 QR code grid
        val cellSize = sizePx / cellsCount

        // 1. Draw solid background
        drawRect(color = Color.White, size = Size(sizePx, sizePx))

        // Helper to draw a finder pattern at (row, col)
        fun drawFinderPattern(row: Int, col: Int) {
            // Outer black square 7x7
            drawRect(
                color = Color.Black,
                topLeft = Offset(col * cellSize, row * cellSize),
                size = Size(7 * cellSize, 7 * cellSize)
            )
            // Inner white square 5x5
            drawRect(
                color = Color.White,
                topLeft = Offset((col + 1) * cellSize, (row + 1) * cellSize),
                size = Size(5 * cellSize, 5 * cellSize)
            )
            // Inner solid black square 3x3
            drawRect(
                color = Color.Black,
                topLeft = Offset((col + 2) * cellSize, (row + 2) * cellSize),
                size = Size(3 * cellSize, 3 * cellSize)
            )
        }

        // Draw Finder Patterns:
        // Top-Left (0, 0)
        drawFinderPattern(0, 0)
        // Top-Right (0, 14)
        drawFinderPattern(0, 14)
        // Bottom-Left (14, 0)
        drawFinderPattern(14, 0)

        // Seed random using String hash to make it deterministic
        val seed = data.hashCode().toLong()
        val random = java.util.Random(seed)

        // 2. Fill the rest of the grid with deterministic noise (excluding finder pattern zones)
        for (r in 0 until cellsCount) {
            for (c in 0 until cellsCount) {
                // Skip finder pattern zones
                val isTopLeftFinder = r < 9 && c < 9
                val isTopRightFinder = r < 9 && c >= 12
                val isBottomLeftFinder = r >= 12 && c < 9
                
                if (!isTopLeftFinder && !isTopRightFinder && !isBottomLeftFinder) {
                    // Decide cell color deterministically
                    val isBlack = random.nextBoolean()
                    if (isBlack) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize + 0.5f, cellSize + 0.5f) // avoid subpixel seams
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OrderInvoiceScreen(viewModel: ThriftSixViewModel) {
    val orders by viewModel.allOrders.collectAsState()
    val cart = viewModel.orderCart
    val context = LocalContext.current

    var selectedOrderForInvoice by viewModel.selectedOrderForInvoice

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("order_invoice_root")
    ) {
        // --- TITLE ---
        var showSettingsDialog by remember { mutableStateOf(false) }
        var showAddItemDialog by remember { mutableStateOf(false) }

        if (showSettingsDialog) {
            ShopSettingsDialog(viewModel = viewModel, onDismiss = { showSettingsDialog = false })
        }

        if (showAddItemDialog) {
            AddItemToCartDialog(viewModel = viewModel, onDismiss = { showAddItemDialog = false })
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Orders & Smart Invoicing",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Formulate invoices and email branded PDFs with security seals.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
            IconButton(
                onClick = { showSettingsDialog = true },
                modifier = Modifier.testTag("shop_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Shop Settings",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Toggle state: Cart Draft vs Order History
        var tabIndex by remember { mutableStateOf(0) }
        TabRow(selectedTabIndex = tabIndex) {
            Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }) {
                Text("Order Checkout Form (${cart.size})", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }) {
                Text("Invoices Registry (${orders.size})", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (tabIndex == 0) {
            // --- CART & CUSTOMER FORM SCREEN ---
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Draft cart segment
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Selected Clothes Sizing List", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        if (viewModel.currentUserRole.value != "Viewer") {
                            TextButton(
                                onClick = { showAddItemDialog = true },
                                modifier = Modifier.testTag("add_item_to_cart_dialog_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Item", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    if (cart.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Cart Draft is Empty", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                                Text("Select items from the Showcase first.", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                items(cart) { itemSpec ->
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("cart_draft_item_${itemSpec.sku.lowercase()}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val cartBitmap: Bitmap? = remember(itemSpec.image) {
                                itemSpec.image?.let { decodeBase64ToBitmap(it) }
                            }
                            if (cartBitmap != null) {
                                Image(
                                    bitmap = cartBitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(6.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(itemSpec.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Row {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(itemSpec.sku, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Size: ${itemSpec.size} • Qty: ${itemSpec.qty} pcs", fontSize = 11.sp, color = Color.Gray)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "৳" + (itemSpec.price * itemSpec.qty).toString(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 12.dp)
                                )

                                IconButton(
                                    onClick = { viewModel.removeFromCart(itemSpec) },
                                    modifier = Modifier.size(24.dp).testTag("delete_cart_item_${itemSpec.sku.lowercase()}")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Billing form inputs
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Customer Billing & Deliveries Form", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = viewModel.orderCustName.value,
                        onValueChange = { viewModel.orderCustName.value = it },
                        label = { Text("Customer Account Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("cust_name_field")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = viewModel.orderCustPhone.value,
                        onValueChange = { viewModel.orderCustPhone.value = it },
                        label = { Text("WhatsApp / Phone Number") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("cust_phone_field")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = viewModel.orderCustEmail.value,
                        onValueChange = { viewModel.orderCustEmail.value = it },
                        label = { Text("Email Receipt Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("cust_email_field")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = viewModel.orderCustAddress.value,
                        onValueChange = { viewModel.orderCustAddress.value = it },
                        label = { Text("Delivery Destination Address") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = viewModel.orderDiscount.value,
                            onValueChange = { viewModel.orderDiscount.value = it },
                            label = { Text("Discount Deductions BDT") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("discount_field")
                        )

                        OutlinedTextField(
                            value = viewModel.orderExpense.value,
                            onValueChange = { viewModel.orderExpense.value = it },
                            label = { Text("Shipping Surcharge BDT") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = viewModel.orderNotes.value,
                        onValueChange = { viewModel.orderNotes.value = it },
                        label = { Text("Custom Order Notes") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val cartTotal = cart.sumOf { it.qty * it.price }
                    val discount = viewModel.orderDiscount.value.toDoubleOrNull() ?: 0.0
                    val shippingExpense = viewModel.orderExpense.value.toDoubleOrNull() ?: 0.0
                    val payable = (cartTotal - discount + shippingExpense).coerceAtLeast(0.0)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Sizing Sum total:", fontSize = 13.sp, color = Color.Gray)
                                Text("৳$cartTotal", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Discounts Campaign:", fontSize = 13.sp, color = Color.Gray)
                                Text("-৳$discount", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Shipping Surcharge:", fontSize = 13.sp, color = Color.Gray)
                                Text("+৳$shippingExpense", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Total Net Payable:", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("৳$payable", fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Checkout trigger button
                    Button(
                        onClick = { viewModel.checkoutOrder() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("checkout_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Formulate Branded Invoice & Order", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        } else {
            // --- INVOICES REGISTRY LIST ---
            if (orders.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No Invoices Registered Yet", fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text("Checkout your first order to generate authentications.", fontSize = 12.sp, color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(orders) { ord ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateSessionActivity()
                                    selectedOrderForInvoice = ord
                                }
                                .testTag("invoice_card_${ord.orderId.lowercase()}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = ord.orderId,
                                        fontWeight = FontWeight.Black,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    // Invoice status label
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = when (ord.status) {
                                                    "Finished" -> Color(0xFF10B981).copy(alpha = 0.15f)
                                                    "Processing" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                                    else -> Color.LightGray.copy(alpha = 0.15f)
                                                },
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = ord.status.uppercase(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when(ord.status) {
                                                "Finished" -> Color(0xFF10B981)
                                                "Processing" -> MaterialTheme.colorScheme.secondary
                                                else -> Color.DarkGray
                                            }
                                        )
                                    }
                                }

                                Text(
                                    text = "Customer: ${ord.customerName}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    text = "Date: " + SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(ord.createdAt)),
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Payable Amount:",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "৳" + ord.totalAmount.toString(),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- BRANDED PDF INVOICE VISUAL MOCKUP PREVIEW DIALOG ---
    if (selectedOrderForInvoice != null) {
        val o = selectedOrderForInvoice!!
        val dateString = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(o.createdAt))

        AlertDialog(
            onDismissRequest = { selectedOrderForInvoice = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${viewModel.shopName.value} Invoice", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = { selectedOrderForInvoice = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.sendInvoiceEmailDirect(o)
                        selectedOrderForInvoice = null
                    },
                    modifier = Modifier.testTag("email_invoice_submit")
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Email to Customer", fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.saveInvoiceCopyToFile(o)
                        selectedOrderForInvoice = null
                    },
                    modifier = Modifier.testTag("save_invoice_local")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Copy & Print")
                }
            },
            text = {
                // PDF Layout mockup canvas!
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .verticalScroll(rememberScrollState()),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color.LightGray)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        // SHOP BRAND HEAD
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = viewModel.shopName.value.uppercase(),
                                    fontWeight = FontWeight.Black,
                                    color = ThriftCoral,
                                    fontSize = 18.sp,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                                Text(viewModel.shopOwner.value, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text(viewModel.shopAddress.value, fontSize = 8.sp, color = Color.DarkGray)
                                Text("WhatsApp: " + viewModel.shopPhone.value, fontSize = 8.sp, color = Color.DarkGray)
                                Text("Fb: " + viewModel.shopFacebook.value, fontSize = 8.sp, color = Color.DarkGray)
                            }

                            // ThriftSix custom generated logo watermark in the top corner!
                            Image(
                                painter = painterResource(id = R.drawable.thriftsix_logo),
                                contentDescription = "Logo",
                                modifier = Modifier
                                    .size(45.dp)
                                    .alpha(0.85f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color.LightGray, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Customer ledger
                        Text("INVOICE RECIPIENT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ThriftCoral)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Customer: ${o.customerName}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Text("Contact: ${o.customerPhone}", fontSize = 10.sp, color = Color.DarkGray)
                        Text("Email: ${o.customerEmail}", fontSize = 10.sp, color = Color.DarkGray)
                        Text("Address: ${o.customerAddress}", fontSize = 10.sp, color = Color.DarkGray)

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Invoice ID: ${o.orderId}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text("Date: $dateString", fontSize = 9.sp, color = Color.DarkGray)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color.LightGray, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(6.dp))

                        // Items specification table heading
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Description & Size", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.weight(1.5f))
                            Text("Qty", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.weight(0.3f), textAlign = TextAlign.Center)
                            Text("Price BDT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.weight(0.6f), textAlign = TextAlign.End)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(4.dp))

                        // Items catalog list
                        val itemsList = o.getItemsList()
                        itemsList.forEach { iSpec ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1.5f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val rowBitmap: Bitmap? = remember(iSpec.image) {
                                        iSpec.image?.let { decodeBase64ToBitmap(it) }
                                    }
                                    if (rowBitmap != null) {
                                        Image(
                                            bitmap = rowBitmap.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .border(0.5.dp, Color.LightGray, RoundedCornerShape(4.dp)),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Column {
                                        Text(iSpec.name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        Text("SKU: ${iSpec.sku} | Size: ${iSpec.size}", fontSize = 8.sp, color = Color.Gray)
                                    }
                                }
                                Text("${iSpec.qty} pcs", fontSize = 10.sp, modifier = Modifier.weight(0.3f), textAlign = TextAlign.Center, color = Color.Black)
                                Text("৳" + (iSpec.price * iSpec.qty).toString(), fontSize = 10.sp, modifier = Modifier.weight(0.6f), textAlign = TextAlign.End, color = Color.Black)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(6.dp))

                        // Pricing calculation block
                        val specTotal = itemsList.sumOf { it.qty * it.price }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal", fontSize = 10.sp, color = Color.DarkGray)
                            Text("৳$specTotal", fontSize = 10.sp, color = Color.Black)
                        }
                        if (o.discount > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Promo Discount", fontSize = 10.sp, color = Color.Red)
                                Text("-৳${o.discount}", fontSize = 10.sp, color = Color.Red)
                            }
                        }
                        if (o.expense > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Delivery Courier Charges", fontSize = 10.sp, color = Color.DarkGray)
                                Text("+৳${o.expense}", fontSize = 10.sp, color = Color.Black)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Net Payable Amount", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text("৳${o.totalAmount}", fontSize = 11.sp, fontWeight = FontWeight.Black, color = ThriftCoral)
                        }

                        // Order Notes
                        if (o.orderNotes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB))
                            ) {
                                Column(modifier = Modifier.padding(6.dp)) {
                                    Text("Order Notes:", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Text(o.orderNotes, fontSize = 8.sp, color = Color.DarkGray)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // SECURE BRANDING WATERMARK + NASIF AUTOGRAPH SEAL!
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("CUSTOMER ACKNOWLEDGEMENT", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Spacer(modifier = Modifier.height(14.dp))
                                Row {
                                    HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp, modifier = Modifier.width(90.dp))
                                }
                                Text("Receiver Autograph/Stamp", fontSize = 7.sp, color = Color.Gray)
                            }



                            // Render generated security seal image here!
                            Image(
                                painter = painterResource(id = R.drawable.thriftsix_seal),
                                contentDescription = "Security Seal Stamp",
                                modifier = Modifier
                                    .size(70.dp)
                                    .testTag("invoice_security_seal")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Thank you for buying apparel with ${viewModel.shopName.value.uppercase()}. Autologous ledger security verified, ${viewModel.shopOwner.value} system.",
                            fontSize = 7.sp,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemToCartDialog(viewModel: ThriftSixViewModel, onDismiss: () -> Unit) {
    val products by viewModel.allProducts.collectAsState()
    var tabIndex by remember { mutableStateOf(0) } // 0 for Catalog, 1 for Custom Item

    // Catalog state
    var selectedProductIndex by remember { mutableStateOf(-1) }
    var selectedSize by remember { mutableStateOf("") }
    var catalogQtyInput by remember { mutableStateOf("1") }

    // Custom Item state
    var customSku by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }
    var customSize by remember { mutableStateOf("M") }
    var customQty by remember { mutableStateOf("1") }
    var customPrice by remember { mutableStateOf("") }

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add Item to Invoice Draft", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (tabIndex == 0) {
                        // Catalog Selection
                        if (selectedProductIndex == -1) {
                            Toast.makeText(context, "Please select a product from catalog.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val product = products[selectedProductIndex]
                        if (selectedSize.isEmpty()) {
                            Toast.makeText(context, "Please select a size.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val qty = catalogQtyInput.toIntOrNull() ?: 1
                        val availableStock = product.getStockForSize(selectedSize)
                        if (qty > availableStock) {
                            Toast.makeText(context, "Only $availableStock items left in size $selectedSize!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.addToCart(product, selectedSize, qty)
                        onDismiss()
                    } else {
                        // Custom Item Integration
                        if (customSku.isBlank() || customName.isBlank() || customPrice.isBlank()) {
                            Toast.makeText(context, "SKU, Name, and Price are required.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val qty = customQty.toIntOrNull() ?: 1
                        val price = customPrice.toDoubleOrNull() ?: 0.0
                        if (qty <= 0 || price < 0.0) {
                            Toast.makeText(context, "Invalid Quantity or Price details.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.orderCart.add(OrderItemSpec(
                            sku = customSku.uppercase().trim(),
                            name = customName.trim(),
                            size = customSize.trim(),
                            qty = qty,
                            price = price,
                            image = null
                        ))
                        Toast.makeText(context, "Custom item $customName added!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("dialog_add_to_cart_confirm")
            ) {
                Text("Add to Cart")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TabRow(selectedTabIndex = tabIndex, modifier = Modifier.fillMaxWidth()) {
                    Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }) {
                        Text("From Catalog", modifier = Modifier.padding(8.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }) {
                        Text("Custom Item", modifier = Modifier.padding(8.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (tabIndex == 0) {
                    // Catalog layout
                    var showDropdown by remember { mutableStateOf(false) }
                    val selectedProd = if (selectedProductIndex in products.indices) products[selectedProductIndex] else null

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Select Catalog Product", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .clickable { showDropdown = true }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedProd?.let { "${it.sku} - ${it.name}" } ?: "Choose product...",
                                    fontSize = 14.sp,
                                    color = if (selectedProd != null) MaterialTheme.colorScheme.onSurface else Color.Gray
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }

                            DropdownMenu(
                                expanded = showDropdown,
                                onDismissRequest = { showDropdown = false },
                                modifier = Modifier.fillMaxWidth(0.8f).heightIn(max = 250.dp)
                            ) {
                                products.forEachIndexed { index, prod ->
                                    DropdownMenuItem(
                                        text = { Text("${prod.sku} - ${prod.name} (৳${prod.sellingPrice})", fontSize = 13.sp) },
                                        onClick = {
                                            selectedProductIndex = index
                                            selectedSize = "" // Reset size selection
                                            val sizes = prod.getSizesAndStocksMap().keys
                                            if (sizes.isNotEmpty()) {
                                                selectedSize = sizes.first()
                                            }
                                            showDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (selectedProd != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        val sizesMap = selectedProd.getSizesAndStocksMap()

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Select Size (Stock available)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                sizesMap.forEach { (sz, stock) ->
                                    val isSelected = selectedSize == sz
                                    Box(
                                        modifier = Modifier
                                            .clickable { selectedSize = sz }
                                            .background(
                                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "$sz ($stock pcs)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = catalogQtyInput,
                            onValueChange = { catalogQtyInput = it },
                            label = { Text("Quantity to checkout") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    // Custom Line Item inputs
                    OutlinedTextField(
                        value = customSku,
                        onValueChange = { customSku = it },
                        label = { Text("Custom Catalog/SKU Tag") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("custom_item_sku_input")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Item Name / Description") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("custom_item_name_input")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customSize,
                            onValueChange = { customSize = it },
                            label = { Text("Weaved Size") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("custom_item_size_input")
                        )

                        OutlinedTextField(
                            value = customQty,
                            onValueChange = { customQty = it },
                            label = { Text("Quantity") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("custom_item_qty_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = customPrice,
                        onValueChange = { customPrice = it },
                        label = { Text("Price (৳ BDT)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("custom_item_price_input")
                    )
                }
            }
        }
    )
}
