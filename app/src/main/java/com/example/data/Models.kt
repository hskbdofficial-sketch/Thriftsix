package com.example.data

import androidx.room.*

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sku: String,
    val name: String,
    val image: String?,
    val color: String,
    val description: String,
    val batchNumber: String,
    val costPrice: Double,
    val sellingPrice: Double,
    val quantity: Int, // Cached total quantity
    val category: String,
    val sizesAndStocksJson: String // Serialized Map<String, Int> e.g. {"S":12, "M":15}
) {
    // Helper to get total stock
    fun getStockForSize(size: String): Int {
        return try {
            val map = getSizesAndStocksMap()
            map[size] ?: 0
        } catch (e: Exception) {
            0
        }
    }

    fun getSizesAndStocksMap(): Map<String, Int> {
        if (sizesAndStocksJson.isBlank()) return emptyMap()
        return try {
            // Simple manual parser to avoid dependency issue during preview compilation
            // Expected format: {"S":12, "M":15}
            val clean = sizesAndStocksJson.trim().removeSurrounding("{", "}")
            if (clean.isBlank()) return emptyMap()
            clean.split(",").associate {
                val parts = it.split(":")
                val key = parts[0].trim().removeSurrounding("\"")
                val value = parts[1].trim().toInt()
                key to value
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    companion object {
        fun mapToJson(map: Map<String, Int>): String {
            return map.entries.joinToString(prefix = "{", postfix = "}") {
                "\"${it.key}\":${it.value}"
            }
        }
    }
}

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey val orderId: String, // T6-INV-XXXX
    val customerName: String,
    val customerAddress: String,
    val customerEmail: String,
    val customerPhone: String,
    val orderNotes: String,
    val itemsJson: String, // List of {sku, name, size, quantity, price}
    val totalAmount: Double,
    val discount: Double = 0.0,
    val expense: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String // Pending, Processing, Shipped, Returned
) {
    fun getItemsList(): List<OrderItemSpec> {
        if (itemsJson.isBlank()) return emptyList()
        return try {
            // Format: [{"sku":"S1","name":"N1","size":"M","qty":2,"price":10.0}]
            val clean = itemsJson.trim().removeSurrounding("[", "]")
            if (clean.isBlank()) return emptyList()
            clean.split("},").map {
                val cleanedObj = it.removeSurrounding("{", "}").trim()
                val pairs = cleanedObj.split(",")
                var sku = ""
                var name = ""
                var size = ""
                var qty = 0
                var price = 0.0
                var image: String? = null
                pairs.forEach { pair ->
                    val pts = pair.split(":")
                    if (pts.size >= 2) {
                        val key = pts[0].trim().removeSurrounding("\"")
                        val value = pts.subList(1, pts.size).joinToString(":").trim().removeSurrounding("\"")
                        when (key) {
                            "sku" -> sku = value
                            "name" -> name = value
                            "size" -> size = value
                            "qty" -> qty = value.toIntOrNull() ?: 0
                            "price" -> price = value.toDoubleOrNull() ?: 0.0
                            "image" -> if (value.isNotBlank()) image = value
                        }
                    }
                }
                OrderItemSpec(sku, name, size, qty, price, image)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        fun listToJson(list: List<OrderItemSpec>): String {
            return list.joinToString(prefix = "[", postfix = "]") {
                "{\"sku\":\"${it.sku}\",\"name\":\"${it.name}\",\"size\":\"${it.size}\",\"qty\":${it.qty},\"price\":${it.price},\"image\":\"${it.image ?: ""}\"}"
            }
        }
    }
}

data class OrderItemSpec(
    val sku: String,
    val name: String,
    val size: String,
    val qty: Int,
    val price: Double,
    val image: String? = null
)

@Entity(tableName = "returns")
data class ReturnItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderId: String,
    val sku: String,
    val size: String,
    val quantity: Int,
    val reason: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val action: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis(),
    val userEmail: String
)

@Entity(tableName = "team_invites")
data class TeamInvite(
    @PrimaryKey val email: String,
    val role: String, // Admin, Editor, Viewer
    val status: String, // Pending, Accepted, Declined
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "sync_status")
data class SyncStatus(
    @PrimaryKey val id: Int = 1,
    val lastSyncTime: Long = System.currentTimeMillis(),
    val syncPending: Boolean = false
)
