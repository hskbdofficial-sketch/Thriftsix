package com.example.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Order
import com.example.viewmodels.ThriftSixViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ShopSettingsDialog(viewModel: ThriftSixViewModel, onDismiss: () -> Unit) {
    var nameByState by remember { mutableStateOf(viewModel.shopName.value) }
    var ownerByState by remember { mutableStateOf(viewModel.shopOwner.value) }
    var addressByState by remember { mutableStateOf(viewModel.shopAddress.value) }
    var phoneByState by remember { mutableStateOf(viewModel.shopPhone.value) }
    var fbByState by remember { mutableStateOf(viewModel.shopFacebook.value) }

    val orders by viewModel.allOrders.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Shop Configuration", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.updateShopInfo(
                        name = nameByState,
                        owner = ownerByState,
                        address = addressByState,
                        phone = phoneByState,
                        facebook = fbByState
                    )
                    onDismiss()
                },
                modifier = Modifier.testTag("save_shop_info_btn")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp) // Maintain ideal density and size limit
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Branding Header
                Text(
                    text = "STORE BRANDING FOR INVOICES",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )

                OutlinedTextField(
                    value = nameByState,
                    onValueChange = { nameByState = it },
                    label = { Text("Shop Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("settings_shop_name")
                )

                OutlinedTextField(
                    value = ownerByState,
                    onValueChange = { ownerByState = it },
                    label = { Text("Owner Spec / Tagline") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("settings_shop_owner")
                )

                OutlinedTextField(
                    value = addressByState,
                    onValueChange = { addressByState = it },
                    label = { Text("Physical Store Address") },
                    minLines = 1,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("settings_shop_address")
                )

                OutlinedTextField(
                    value = phoneByState,
                    onValueChange = { phoneByState = it },
                    label = { Text("WhatsApp Contact No.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("settings_shop_phone")
                )

                OutlinedTextField(
                    value = fbByState,
                    onValueChange = { fbByState = it },
                    label = { Text("Facebook URL/Handle") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("settings_shop_facebook")
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), thickness = 1.dp)

                // Delivery Status Manager
                Text(
                    text = "ORDER DELIVERY STATUS UPDATER",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Update shipment and completion pipelines directly",
                    fontSize = 10.sp,
                    color = Color.Gray
                )

                if (orders.isEmpty()) {
                    Text(
                        text = "No invoices registered to edit delivery status.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    orders.take(15).forEach { ord ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = ord.orderId,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(ord.createdAt)),
                                        fontSize = 9.sp,
                                        color = Color.Gray
                                    )
                                }

                                Text(
                                    text = "Customer: ${ord.customerName} | Total: ৳${ord.totalAmount}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Status controls
                                val statuses = listOf("Processing", "Shipped", "Finished", "Returned")
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    statuses.forEach { status ->
                                        val isSelected = ord.status == status
                                        val buttonColor = when (status) {
                                            "Finished" -> Color(0xFF10B981)
                                            "Processing" -> MaterialTheme.colorScheme.secondary
                                            "Shipped" -> MaterialTheme.colorScheme.primary
                                            else -> Color.Red
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clickable {
                                                    viewModel.updateOrderStatus(ord, status)
                                                }
                                                .background(
                                                    color = if (isSelected) buttonColor else Color.Transparent,
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .border(
                                                    0.5.dp,
                                                    if (isSelected) buttonColor else Color.Gray.copy(alpha = 0.5f),
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = status,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}
