package com.example.ui

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodels.ThriftSixViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(viewModel: ThriftSixViewModel) {
    val products by viewModel.allProducts.collectAsState()
    val scanResult = viewModel.scanResultProduct.value
    val userRole = viewModel.currentUserRole.value

    var skuInput by viewModel.scannerSearchSku

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("scanner_screen_root")
    ) {
        // --- TITLE ---
        Text(
            text = "QR/Barcode & Returns Center",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Generate order tags, audit inventory quantities, & restock returns",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(14.dp))

        val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

        // --- SCANNER VIEWFINDER AND HARDWARE CAMERA DRIVER ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (cameraPermissionState.status.isGranted) "Live Hardware Camera Viewfinder" else "Click Viewfinder HUD to Activate Camera",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Frame Viewport Canvas Holder
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .background(Color.Black, RoundedCornerShape(12.dp))
                        .border(
                            2.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (cameraPermissionState.status.isGranted) {
                        // ACTUAL CAMERA PREVIEW RUNNING ON DEVICE
                        val lifecycleOwner = LocalLifecycleOwner.current
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx).apply {
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                }
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                val executor = ContextCompat.getMainExecutor(ctx)
                                cameraProviderFuture.addListener({
                                    try {
                                        val cameraProvider = cameraProviderFuture.get()
                                        val preview = Preview.Builder().build().also {
                                            it.setSurfaceProvider(previewView.surfaceProvider)
                                        }
                                        val barcodeScanner = BarcodeScanning.getClient()
                                        val imageAnalysis = ImageAnalysis.Builder()
                                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                            .build()
                                        imageAnalysis.setAnalyzer(executor) { imageProxy ->
                                            val mediaImage = imageProxy.image
                                            if (mediaImage != null) {
                                                try {
                                                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                                    barcodeScanner.process(image)
                                                        .addOnSuccessListener { barcodes ->
                                                            for (barcode in barcodes) {
                                                                val rawValue = barcode.rawValue
                                                                if (!rawValue.isNullOrBlank()) {
                                                                    skuInput = rawValue
                                                                    viewModel.lookupProductForScanner(rawValue)
                                                                    break
                                                                }
                                                            }
                                                        }
                                                        .addOnFailureListener {
                                                            imageProxy.close()
                                                        }
                                                        .addOnCompleteListener {
                                                            imageProxy.close()
                                                        }
                                                } catch (e: Exception) {
                                                    imageProxy.close()
                                                }
                                            } else {
                                                imageProxy.close()
                                            }
                                        }
                                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            cameraSelector,
                                            preview,
                                            imageAnalysis
                                        )
                                    } catch (t: Throwable) {
                                        android.util.Log.e("ScannerScreen", "CameraX/MLKit initialization failed gracefully: ", t)
                                    }
                                }, executor)
                                previewView
                            },
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        // OFFLINE TAP TO AUTHORIZE CAMERA BUTTON WITH PSEUDO SCAN FALLBACK
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { cameraPermissionState.launchPermissionRequest() }
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Camera hardware",
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "TAP TO ENABLE CAMERA",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "(Natively decodes SKU tags)",
                                fontSize = 8.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Floating Corner Reticles HUD (Rendered as overlay in all states!)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val thickness = 4.dp.toPx()
                        val len = 20.dp.toPx()
                        val w = size.width
                        val h = size.height
                        val clr = Color(0xFFFF5740) // Coral red color spec

                        // Top Left corner
                        drawLine(clr, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(len, 0f), thickness)
                        drawLine(clr, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(0f, len), thickness)

                        // Top Right
                        drawLine(clr, androidx.compose.ui.geometry.Offset(w, 0f), androidx.compose.ui.geometry.Offset(w - len, 0f), thickness)
                        drawLine(clr, androidx.compose.ui.geometry.Offset(w,  0f), androidx.compose.ui.geometry.Offset(w, len), thickness)

                        // Bottom Left
                        drawLine(clr, androidx.compose.ui.geometry.Offset(0f, h), androidx.compose.ui.geometry.Offset(len, h), thickness)
                        drawLine(clr, androidx.compose.ui.geometry.Offset(0f, h), androidx.compose.ui.geometry.Offset(0f, h - len), thickness)

                        // Bottom Right
                        drawLine(clr, androidx.compose.ui.geometry.Offset(w, h), androidx.compose.ui.geometry.Offset(w - len, h), thickness)
                        drawLine(clr, androidx.compose.ui.geometry.Offset(w, h), androidx.compose.ui.geometry.Offset(w, h - len), thickness)
                    }

                    // Pseudo-Laser scanner light line (Rendered as overlay in all states!)
                    var stateProgress by remember { mutableStateOf(0f) }
                    LaunchedEffect(Unit) {
                        while(true) {
                            stateProgress += 0.05f
                            if (stateProgress > 1f) stateProgress = 0f
                            kotlinx.coroutines.delay(80)
                        }
                    }

                    HorizontalDivider(
                        color = Color.Red,
                        thickness = 2.dp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (stateProgress * 160).dp)
                            .padding(horizontal = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Select a SKU or type a customized order code to simulate instant scanning events:",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick item options selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    products.forEach { prod ->
                        val selected = skuInput == prod.sku
                        Box(
                            modifier = Modifier
                                .clickable {
                                    skuInput = prod.sku
                                    viewModel.lookupProductForScanner(prod.sku)
                                }
                                .background(
                                    color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(text = prod.sku, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom typing search field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = skuInput,
                        onValueChange = { skuInput = it },
                        placeholder = { Text("Input SKU / Tag Barcode Number", fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("scanner_sku_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Button(
                        onClick = { viewModel.lookupProductForScanner(skuInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("scanner_simulate_btn")
                    ) {
                        Text("Aim", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SCAN RESULT DETAILS PANELS ---
        AnimatedVisibility(
            visible = scanResult != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            val prod = scanResult!!
            val sizesMap = prod.getSizesAndStocksMap()

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Scanned Item Physical Properties",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = prod.name,
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = prod.sku,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Batch Code: ${prod.batchNumber}  |  Category: ${prod.category}  |  Color: ${prod.color}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = prod.description,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            lineHeight = 16.sp
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        // Sizes stock ledger control panel
                        Text("Current Sizing Quantities Inventory Ledger:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        sizesMap.entries.forEach { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Size ${entry.key}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.DarkGray
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "${entry.value} items",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = if (entry.value <= 3) Color.Red else MaterialTheme.colorScheme.onSurface
                                    )

                                    if (userRole != "Viewer") {
                                        // Plus/Minus Stock modifier
                                        IconButton(
                                            onClick = {
                                                viewModel.executeManualScanAdjustment(prod.id, entry.key, (entry.value - 1).coerceAtLeast(0))
                                            },
                                            modifier = Modifier.size(28.dp).testTag("decrease_size_${entry.key}")
                                        ) {
                                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease", tint = Color.Red, modifier = Modifier.size(20.dp))
                                        }

                                        IconButton(
                                            onClick = {
                                                viewModel.executeManualScanAdjustment(prod.id, entry.key, entry.value + 1)
                                            },
                                            modifier = Modifier.size(28.dp).testTag("increase_size_${entry.key}")
                                        ) {
                                            Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase", tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- RETURN PROCESSING ACTION SYSTEM ---
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Replenish Returns & Refund Ledger Form",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Order ID reference
                            OutlinedTextField(
                                value = viewModel.returnOrderId.value,
                                onValueChange = { viewModel.returnOrderId.value = it },
                                label = { Text("Original Invoice ID (e.g. T6-INV-0001)", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("returns_invoice_id_field")
                            )

                            // Return Sizing dropdown toggle
                            Column(modifier = Modifier.width(90.dp)) {
                                Text("Size Select", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White, RoundedCornerShape(8.dp))
                                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                        .clickable {
                                            // Quick select cycle items size list helper
                                            val currentIdx = sizesMap.keys.toList().indexOf(viewModel.returnSelectedSize.value)
                                            if (currentIdx != -1 && currentIdx < sizesMap.keys.size - 1) {
                                                viewModel.returnSelectedSize.value = sizesMap.keys.toList()[currentIdx + 1]
                                            } else {
                                                viewModel.returnSelectedSize.value = sizesMap.keys.first()
                                            }
                                        }
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = viewModel.returnSelectedSize.value, fontWeight = FontWeight.Bold, color = Color.Black)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = viewModel.returnQty.value,
                                onValueChange = { viewModel.returnQty.value = it },
                                label = { Text("Qty to Restock") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(0.8f).testTag("returns_qty_field")
                            )

                            OutlinedTextField(
                                value = viewModel.returnReason.value,
                                onValueChange = { viewModel.returnReason.value = it },
                                label = { Text("Repatriation Audit Reason") },
                                singleLine = true,
                                modifier = Modifier.weight(1.5f).testTag("returns_reason_field")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { viewModel.submitReturnItem() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("returns_submit_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.AssignmentReturn, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Process Restock Return & Log Audit", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
