package com.example.ui

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.viewmodels.ThriftSixViewModel

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.util.Base64
import java.io.InputStream
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale

private fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
    return try {
        val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InventoryScreen(viewModel: ThriftSixViewModel) {
    val products by viewModel.allProducts.collectAsState()
    val userRole = viewModel.currentUserRole.value

    var showAddDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }

    // Search and filters
    var searchQuery by viewModel.searchQuery
    var selectedCategory by viewModel.selectedCategory
    var sortOption by viewModel.sortOption

    val categories = listOf("All", "Outerwear", "Sweatshirts", "Jeans", "T-Shirts", "Accessories")

    // Filter and sort products
    val filteredProducts = products.filter {
        val matchesSearch = it.name.contains(searchQuery, ignoreCase = true) ||
                it.sku.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == "All" || it.category == selectedCategory
        matchesSearch && matchesCategory
    }.sortedWith { a, b ->
        when (sortOption) {
            "SKU Asc" -> a.sku.compareTo(b.sku)
            "Stock Low-High" -> a.quantity.compareTo(b.quantity)
            "Stock High-Low" -> b.quantity.compareTo(a.quantity)
            "Price Desc" -> b.sellingPrice.compareTo(a.sellingPrice)
            else -> a.name.compareTo(b.name)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("inventory_screen_root")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // --- HEADER TITLE ---
            Text(
                text = "Showcase & Products Catalogue",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("inventory_title")
            )
            Text(
                text = "Manage styles, physical sizing batches & track security levels",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // --- SEARCH BAR ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by Style, Name or SKU...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("search_bar_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // --- FILTER CHIPS ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("category_chip_${cat.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- SORT DRAWER DROP CHIP ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredProducts.size} Items Listed",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )

                // Sort pills
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val sorts = listOf("SKU Asc", "Stock Low-High", "Price Desc")
                    sorts.forEach { opt ->
                        val isSel = sortOption == opt
                        Box(
                            modifier = Modifier
                                .clickable { sortOption = opt }
                                .background(
                                    color = if (isSel) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSel) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = when(opt) {
                                    "SKU Asc" -> "SKU"
                                    "Stock Low-High" -> "Low Stock"
                                    "Price Desc" -> "Price"
                                    else -> opt
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- PRODUCT GRID LIST ---
            if (filteredProducts.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Thrift Items Matches Search",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Try adjusting tags or add a brand-new SKU jacket.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("inventory_grid"),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredProducts) { prod ->
                        val hasLowStock = prod.getSizesAndStocksMap().values.any { it <= 3 }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(295.dp)
                                .clickable {
                                    if (userRole != "Viewer") {
                                        editingProduct = prod
                                        showAddDialog = true
                                    } else {
                                        viewModel.updateSessionActivity()
                                    }
                                }
                                .testTag("product_card_${prod.sku.lowercase()}"),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                1.dp,
                                if (hasLowStock) MaterialTheme.colorScheme.error.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            ),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                            ) {
                                // SKU Badge + Card Action Controls (Edit & Delete)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = prod.sku,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // Action buttons for Edit and Delete
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (userRole != "Viewer") {
                                            IconButton(
                                                onClick = {
                                                    editingProduct = prod
                                                    showAddDialog = true
                                                    viewModel.updateSessionActivity()
                                                },
                                                modifier = Modifier.size(28.dp).testTag("card_edit_btn_${prod.sku.lowercase()}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit Product SKU",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        if (userRole == "Admin") {
                                            IconButton(
                                                onClick = {
                                                    viewModel.deleteItem(prod)
                                                },
                                                modifier = Modifier.size(28.dp).testTag("card_delete_btn_${prod.sku.lowercase()}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Product SKU",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Image layout display slot
                                val productBitmap: Bitmap? = remember(prod.image) {
                                    prod.image?.let { decodeBase64ToBitmap(it) }
                                }
                                if (productBitmap != null) {
                                    Image(
                                        bitmap = productBitmap.asImageBitmap(),
                                        contentDescription = "Product Image",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(65.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(65.dp)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Product Title
                                Text(
                                    text = prod.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val colorParts = prod.color.split(Regex("[,/\\s]+")).map { it.trim() }.filter { it.isNotEmpty() }
                                    colorParts.take(3).forEach { colorName ->
                                        val indicatorColor = when (colorName.lowercase()) {
                                            "red" -> Color(0xFFEF4444)
                                            "green" -> Color(0xFF10B981)
                                            "blue" -> Color(0xFF3B82F6)
                                            "yellow" -> Color(0xFFFBBF24)
                                            "orange" -> Color(0xFFF97316)
                                            "pink" -> Color(0xFFEC4899)
                                            "purple" -> Color(0xFF8B5CF6)
                                            "black" -> Color(0xFF111827)
                                            "white" -> Color(0xFFF9FAFB)
                                            "gray", "grey" -> Color(0xFF9CA3AF)
                                            else -> Color.Transparent
                                        }

                                        if (indicatorColor != Color.Transparent) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .background(indicatorColor, androidx.compose.foundation.shape.CircleShape)
                                                    .border(0.5.dp, Color.Gray.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
                                            )
                                        } else if (colorName.lowercase() in listOf("multi", "multicolor", "multicolour", "rainbow")) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .background(
                                                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                                                            listOf(Color.Red, Color.Yellow, Color.Green, Color.Blue, Color.Magenta)
                                                        ),
                                                        androidx.compose.foundation.shape.CircleShape
                                                    )
                                                    .border(0.5.dp, Color.Gray.copy(alpha = 0.4f), androidx.compose.foundation.shape.CircleShape)
                                            )
                                        }
                                    }

                                    Text(
                                        text = (if (prod.color.isBlank()) "No Color" else prod.color) + " • " + prod.category + if (hasLowStock) " (⚠️ Low)" else "",
                                        fontSize = 11.sp,
                                        color = if (hasLowStock) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        fontWeight = if (hasLowStock) FontWeight.SemiBold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Size Inventory table preview
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(4.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    val sizeMap = prod.getSizesAndStocksMap()
                                    sizeMap.entries.take(4).forEach { entry ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = "Size ${entry.key}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                            Text(
                                                text = "${entry.value} pcs",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (entry.value <= 3) Color.Red else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Bottom Buying details & total units
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Column {
                                        Text(
                                            text = "৳" + prod.sellingPrice.toString(),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // Add sizing select cart drawer button
                                    Button(
                                        onClick = {
                                            val sizes = prod.getSizesAndStocksMap()
                                            if (sizes.isNotEmpty()) {
                                                val firstAvailableSize = sizes.keys.firstOrNull { sizes[it] ?: 0 > 0 } ?: sizes.keys.first()
                                                viewModel.addToCart(prod, firstAvailableSize, 1)
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        modifier = Modifier
                                            .height(28.dp)
                                            .testTag("add_to_cart_btn_${prod.sku.lowercase()}"),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                    ) {
                                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.onSecondary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("+Cart", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- FAB TO ADD GEMS (Only for Non-Viewer) ---
        if (userRole != "Viewer") {
            FloatingActionButton(
                onClick = {
                    editingProduct = null
                    showAddDialog = true
                    viewModel.updateSessionActivity()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 80.dp, end = 20.dp)
                    .testTag("add_product_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Style")
            }
        }
    }

    // --- DIALOG FOR ADDING & EDITING PRODUCTS WITH SEPARATE SIZE STOCK CHANNELS ---
    if (showAddDialog) {
        var nameInput by remember { mutableStateOf(editingProduct?.name ?: "") }
        var skuInput by remember { mutableStateOf(editingProduct?.sku ?: "") }
        var categoryInput by remember { mutableStateOf(editingProduct?.category ?: "Outerwear") }
        var colorInput by remember { mutableStateOf(editingProduct?.color ?: "") }
        var descInput by remember { mutableStateOf(editingProduct?.description ?: "") }
        var costInput by remember { mutableStateOf(editingProduct?.costPrice?.toString() ?: "") }
        var sellInput by remember { mutableStateOf(editingProduct?.sellingPrice?.toString() ?: "") }
        var imageInput by remember { mutableStateOf(editingProduct?.image ?: "") }

        val context = LocalContext.current
        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                try {
                    val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                    val bytes = inputStream?.readBytes()
                    if (bytes != null) {
                        val base64String = Base64.encodeToString(bytes, Base64.DEFAULT)
                        imageInput = base64String
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Sizing maps
        val sizes = listOf("S", "M", "L", "XL", "XXL", "30", "32", "34")
        val sizeStocks = remember {
            val map = editingProduct?.getSizesAndStocksMap() ?: emptyMap()
            val stateMap = mutableStateMapOf<String, String>()
            sizes.forEach { s ->
                stateMap[s] = map[s]?.toString() ?: if (s == "M" || s == "L") "5" else "0"
            }
            stateMap
        }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = if (editingProduct == null) "Add Brand-New SKU Style" else "Edit Inventory Sku details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cost = costInput.toDoubleOrNull() ?: 0.0
                        val sell = sellInput.toDoubleOrNull() ?: 0.0
                        val convertedSizes = sizeStocks.entries.associate {
                            it.key to (it.value.toIntOrNull() ?: 0)
                        }.filterValues { it > 0 }

                        if (skuInput.isBlank() || nameInput.isBlank() || convertedSizes.isEmpty()) {
                            // Validation alert
                            return@Button
                        }

                        viewModel.addOrUpdateProduct(
                            sku = skuInput,
                            name = nameInput,
                            category = categoryInput,
                            color = colorInput,
                            desc = descInput,
                            cost = cost,
                            sell = sell,
                            sizes = convertedSizes,
                            isEdit = editingProduct != null,
                            existingId = editingProduct?.id ?: 0,
                            image = imageInput
                        )
                        showAddDialog = false
                    },
                    modifier = Modifier.testTag("submit_product_button")
                ) {
                    Text("Save SKU")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Close")
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // --- PRODUCT IMAGE PICKER SLOT ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val selectedBitmap: Bitmap? = remember(imageInput) {
                            if (imageInput.isNotEmpty()) decodeBase64ToBitmap(imageInput) else null
                        }
                        if (selectedBitmap != null) {
                            Image(
                                bitmap = selectedBitmap.asImageBitmap(),
                                contentDescription = "Selected Product Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = { imagePickerLauncher.launch("image/*") },
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Change Image", fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = { imageInput = "" },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Remove Image", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                    .clickable { imagePickerLauncher.launch("image/*") }
                                    .testTag("upload_product_image_box"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = "Upload",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Upload Product Image", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                    Text("Supports local gallery pick", fontSize = 9.sp, color = Color.Gray)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = skuInput,
                        onValueChange = { skuInput = it },
                        label = { Text("SKU Barcode Reference (e.g. T6-SH-05)") },
                        singleLine = true,
                        enabled = editingProduct == null,
                        modifier = Modifier.fillMaxWidth().testTag("add_product_sku_field")
                    )

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Product Style Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_product_name_field")
                    )

                    // Category Selector
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Category Options", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val options = listOf("Outerwear", "Sweatshirts", "Jeans", "T-Shirts", "Accessories")
                            options.forEach { opt ->
                                val selected = categoryInput == opt
                                Box(
                                    modifier = Modifier
                                        .clickable { categoryInput = opt }
                                        .background(
                                            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(text = opt, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = colorInput,
                            onValueChange = { colorInput = it },
                            label = { Text("Colors (e.g. Red, Green, Multi-color)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("add_product_color_field")
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = costInput,
                            onValueChange = { costInput = it },
                            label = { Text("Buying Cost BDT") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("add_product_cost_field")
                        )
                        OutlinedTextField(
                            value = sellInput,
                            onValueChange = { sellInput = it },
                            label = { Text("Selling Price BDT") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("add_product_selling_field")
                        )
                    }

                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { descInput = it },
                        label = { Text("Style Story, Wear Details & Flaws") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider()

                    // Separate size stocks
                    Text(
                        text = "Separate Size-Stock Levels",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Add values to specify quantities per size batch.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        maxItemsInEachRow = 4,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        sizes.forEach { sizeKey ->
                            Row(
                                modifier = Modifier
                                    .width(62.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = sizeKey,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 2.dp)
                                )
                                BasicTextField(
                                    value = sizeStocks[sizeKey] ?: "0",
                                    onValueChange = { sizeStocks[sizeKey] = it },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier
                                        .width(36.dp)
                                        .background(Color.White, RoundedCornerShape(4.dp))
                                        .padding(2.dp)
                                        .testTag("size_stock_input_$sizeKey")
                                )
                            }
                        }
                    }

                    // Delete product option (if editing and role is admin)
                    if (editingProduct != null && userRole == "Admin") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                viewModel.deleteItem(editingProduct!!)
                                showAddDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("delete_product_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete product catalog entry")
                        }
                    }
                }
            }
        )
    }
}
