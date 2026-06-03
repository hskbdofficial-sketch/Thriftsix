package com.example.data

import android.os.Handler
import android.os.Looper
import android.util.Base64
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.nio.charset.StandardCharsets
import java.text.DecimalFormat

class ThriftSixRepository(private val db: ThriftSixDatabase) {

    // Crypto Engine for Field level encryption at rest (PII)
    private val encryptionKey = "NASIF_HIMADRI_THRIFT_KEY"

    private fun encryptText(plainText: String): String {
        if (plainText.isEmpty()) return ""
        return try {
            val bytes = plainText.toByteArray(StandardCharsets.UTF_8)
            val encryptedBytes = ByteArray(bytes.size)
            for (i in bytes.indices) {
                val keyChar = encryptionKey[i % encryptionKey.length]
                encryptedBytes[i] = (bytes[i].toInt() xor keyChar.code).toByte()
            }
            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            plainText
        }
    }

    private fun decryptText(encryptedText: String): String {
        if (encryptedText.isEmpty()) return ""
        return try {
            val encryptedBytes = Base64.decode(encryptedText, Base64.NO_WRAP)
            val decryptedBytes = ByteArray(encryptedBytes.size)
            for (i in encryptedBytes.indices) {
                val keyChar = encryptionKey[i % encryptionKey.length]
                decryptedBytes[i] = (encryptedBytes[i].toInt() xor keyChar.code).toByte()
            }
            String(decryptedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            encryptedText
        }
    }

    // --- Product Repository Operations ---
    val allProducts: Flow<List<Product>> = db.productDao.getAllProducts()

    suspend fun getProductBySku(sku: String): Product? = db.productDao.getProductBySku(sku)
    suspend fun getProductById(id: Int): Product? = db.productDao.getProductById(id)

    suspend fun insertProduct(product: Product, user: String) {
        db.productDao.insertProduct(product)
        logAudit("Product Added/Updated", "Product SKU ${product.sku} (${product.name}) created/updated with initial sizes. Stock: ${product.quantity}", user)
        triggerMockSync()
        FirebaseSyncHelper.syncProductToCloud(product)
    }

    suspend fun updateProduct(product: Product, user: String) {
        db.productDao.updateProduct(product)
        logAudit("Product Edited", "Product SKU ${product.sku} stock altered manually or updated.", user)
        triggerMockSync()
        FirebaseSyncHelper.syncProductToCloud(product)
    }

    suspend fun deleteProduct(product: Product, user: String) {
        db.productDao.deleteProduct(product)
        logAudit("Product Deleted", "Product SKU ${product.sku} deleted from catalog.", user)
        triggerMockSync()
    }

    // --- Order Repository Operations (with PII Encryption/Decryption) ---
    val allOrders: Flow<List<Order>> = db.orderDao.getAllOrders().map { list ->
        list.map { decryptOrder(it) }
    }

    suspend fun getOrderById(orderId: String): Order? {
        val raw = db.orderDao.getOrderById(orderId) ?: return null
        return decryptOrder(raw)
    }

    private fun encryptOrder(order: Order): Order {
        return order.copy(
            customerName = encryptText(order.customerName),
            customerAddress = encryptText(order.customerAddress),
            customerEmail = encryptText(order.customerEmail),
            customerPhone = encryptText(order.customerPhone)
        )
    }

    private fun decryptOrder(order: Order): Order {
        return order.copy(
            customerName = decryptText(order.customerName),
            customerAddress = decryptText(order.customerAddress),
            customerEmail = decryptText(order.customerEmail),
            customerPhone = decryptText(order.customerPhone)
        )
    }

    suspend fun getNextInvoiceId(): String {
        val count = db.orderDao.getOrdersCount()
        val num = count + 1
        val df = DecimalFormat("0000")
        return "T6-INV-${df.format(num)}"
    }

    suspend fun insertOrder(order: Order, user: String) {
        val encrypted = encryptOrder(order)
        db.orderDao.insertOrder(encrypted)

        // Process inventory reduction for each ordered item
        order.getItemsList().forEach { item ->
            val product = db.productDao.getProductBySku(item.sku)
            if (product != null) {
                val stockMap = product.getSizesAndStocksMap().toMutableMap()
                val currentStock = stockMap[item.size] ?: 0
                val newStock = (currentStock - item.qty).coerceAtLeast(0)
                stockMap[item.size] = newStock
                val totalQty = stockMap.values.sum()
                val updatedProduct = product.copy(
                    sizesAndStocksJson = Product.mapToJson(stockMap),
                    quantity = totalQty
                )
                db.productDao.updateProduct(updatedProduct)
                FirebaseSyncHelper.syncProductToCloud(updatedProduct)
            }
        }

        logAudit("Order Created", "Order ${order.orderId} processed for customer. Total: BDT ${order.totalAmount}", user)
        triggerMockSync()
        FirebaseSyncHelper.syncOrderToCloud(order)
    }

    suspend fun updateOrder(order: Order, user: String) {
        val encrypted = encryptOrder(order)
        db.orderDao.updateOrder(encrypted)
        logAudit("Order Updated", "Order ${order.orderId} status modified to: ${order.status}", user)
        triggerMockSync()
        FirebaseSyncHelper.syncOrderToCloud(order)
    }

    // --- Return Operations ---
    val allReturns: Flow<List<ReturnItem>> = db.returnDao.getAllReturns()

    suspend fun processReturn(returnItem: ReturnItem, user: String) {
        db.returnDao.insertReturn(returnItem)

        // Return stock back to product
        val product = db.productDao.getProductBySku(returnItem.sku)
        if (product != null) {
            val stockMap = product.getSizesAndStocksMap().toMutableMap()
            val currentStock = stockMap[returnItem.size] ?: 0
            val newStock = currentStock + returnItem.quantity
            stockMap[returnItem.size] = newStock
            val totalQty = stockMap.values.sum()
            val updatedProduct = product.copy(
                sizesAndStocksJson = Product.mapToJson(stockMap),
                quantity = totalQty
            )
            db.productDao.updateProduct(updatedProduct)
            FirebaseSyncHelper.syncProductToCloud(updatedProduct)
        }

        logAudit("Return Processed", "Return recorded for order ${returnItem.orderId}. Item ${returnItem.sku}, size ${returnItem.size} returned. Reason: ${returnItem.reason}", user)
        triggerMockSync()
        FirebaseSyncHelper.syncReturnToCloud(returnItem)
    }

    // --- Team Invitation operations ---
    val allInvites: Flow<List<TeamInvite>> = db.teamInviteDao.getAllInvites()

    suspend fun insertInvite(invite: TeamInvite) {
        db.teamInviteDao.insertInvite(invite)
        logAudit("Team Invitation", "Sent role ${invite.role} invite to ${invite.email}", "Admin")
        triggerMockSync()
        FirebaseSyncHelper.syncInviteToCloud(invite)
    }

    suspend fun updateInvite(invite: TeamInvite) {
        db.teamInviteDao.updateInvite(invite)
        logAudit("Invite Response", "${invite.email} response: ${invite.status}", "Admin")
        triggerMockSync()
        FirebaseSyncHelper.syncInviteToCloud(invite)
    }

    suspend fun deleteInvite(email: String) {
        db.teamInviteDao.deleteInvite(email)
        triggerMockSync()
    }

    // --- Audit Logs ---
    val allAuditLogs: Flow<List<AuditLog>> = db.auditLogDao.getAllLogs()

    private suspend fun logAudit(action: String, details: String, userEmail: String) {
        db.auditLogDao.insertLog(
            AuditLog(
                action = action,
                details = details,
                userEmail = userEmail
            )
        )
    }

    // --- Sync Operations ---
    val syncStatus: Flow<SyncStatus?> = db.syncStatusDao.getSyncStatus()

    suspend fun manualSync() {
        db.syncStatusDao.insertSyncStatus(SyncStatus(syncPending = true))
        // Simulate remote firebase synchronization delay
        kotlinx.coroutines.delay(1200)
        db.syncStatusDao.insertSyncStatus(SyncStatus(lastSyncTime = System.currentTimeMillis(), syncPending = false))
    }

    private suspend fun triggerMockSync() {
        db.syncStatusDao.insertSyncStatus(SyncStatus(syncPending = true))
    }

    // Load initial sample mock products if database is empty to provide exceptional out-of-the-box user experience
    suspend fun prepopulateIfEmpty() {
        // We'll run a quick count or check products
        val size = db.productDao.countProducts()
        if (size == 0) {
            val sampleItems = listOf(
                Product(
                    sku = "T6-JKT-01",
                    name = "VINTAGE BARBOUR JACKET",
                    image = null,
                    color = "Olive Drab",
                    description = "Rare vintage heritage waxed canvas jacket with signature corduroy collar, lined with classic tartan plaid check.",
                    batchNumber = "B-0022",
                    costPrice = 1200.0,
                    sellingPrice = 3500.0,
                    quantity = 15,
                    category = "Outerwear",
                    sizesAndStocksJson = Product.mapToJson(mapOf("M" to 5, "L" to 7, "XL" to 3))
                ),
                Product(
                    sku = "T6-HD-04",
                    name = "STEVE & BARRY'S SWEATSHIRT",
                    image = null,
                    color = "Navy Blue",
                    description = "Chunky heavyweight fleece vintage 90s wash collegiate sweatshirt with embroidered applique patches.",
                    batchNumber = "B-0024",
                    costPrice = 850.0,
                    sellingPrice = 1850.0,
                    quantity = 21,
                    category = "Sweatshirts",
                    sizesAndStocksJson = Product.mapToJson(mapOf("S" to 4, "M" to 10, "L" to 7))
                ),
                Product(
                    sku = "T6-DNM-12",
                    name = "LEVI'S 501 REDLINE SELVEDGE",
                    image = null,
                    color = "Indigo Wash",
                    description = "1980s original straight cut, redline selvedge denim. Beautiful authentic honeycombs & whiskering fades.",
                    batchNumber = "B-0025",
                    costPrice = 1500.0,
                    sellingPrice = 4500.0,
                    quantity = 8,
                    category = "Jeans",
                    sizesAndStocksJson = Product.mapToJson(mapOf("30" to 2, "32" to 4, "34" to 2))
                )
            )
            sampleItems.forEach {
                db.productDao.insertProduct(it)
            }

            val sampleOrders = listOf(
                Order(
                    orderId = "T6-INV-0001",
                    customerName = encryptText("Ahad Hossain"),
                    customerAddress = encryptText("Sector 4, Uttara, Dhaka"),
                    customerEmail = encryptText("ahad50502p@gmail.com"),
                    customerPhone = encryptText("+8801712345678"),
                    orderNotes = "Please pack in THRIFTSIX premium custom box branding.",
                    itemsJson = Order.listToJson(listOf(
                        OrderItemSpec("T6-JKT-01", "VINTAGE BARBOUR JACKET", "L", 1, 3500.0)
                    )),
                    totalAmount = 3500.0,
                    status = "Shipped",
                    createdAt = System.currentTimeMillis() - 72000000 // 20 hours ago
                ),
                Order(
                    orderId = "T6-INV-0002",
                    customerName = encryptText("Nasif Himadri"),
                    customerAddress = encryptText("Dhanmondi 27, Dhaka"),
                    customerEmail = encryptText("nasif.himadri@thriftsix.com"),
                    customerPhone = encryptText("+8801999888777"),
                    orderNotes = "Store pick-up. Check size fit.",
                    itemsJson = Order.listToJson(listOf(
                        OrderItemSpec("T6-HD-04", "STEVE & BARRY'S SWEATSHIRT", "M", 2, 1850.0)
                    )),
                    totalAmount = 3700.0,
                    status = "Finished",
                    createdAt = System.currentTimeMillis() - 36000000 // 10 hours ago
                )
            )
            sampleOrders.forEach {
                db.orderDao.insertOrder(it)
            }

            // Pop team invites
            listOf(
                TeamInvite("nasif@thriftsix.com", "Admin", "Accepted", System.currentTimeMillis()),
                TeamInvite("editor.ahad@thriftsix.com", "Editor", "Pending", System.currentTimeMillis()),
                TeamInvite("viewer.demo@thriftsix.com", "Viewer", "Accepted", System.currentTimeMillis())
            ).forEach {
                db.teamInviteDao.insertInvite(it)
            }

            db.syncStatusDao.insertSyncStatus(SyncStatus(lastSyncTime = System.currentTimeMillis(), syncPending = false))
        }
    }
}
