package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY id DESC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Int): Product?

    @Query("SELECT * FROM products WHERE sku = :sku LIMIT 1")
    suspend fun getProductBySku(sku: String): Product?

    @Query("SELECT COUNT(*) FROM products")
    suspend fun countProducts(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("DELETE FROM products")
    suspend fun clearAll()
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE orderId = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: String): Order?

    @Query("SELECT count(*) FROM orders")
    suspend fun getOrdersCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order)

    @Update
    suspend fun updateOrder(order: Order)

    @Delete
    suspend fun deleteOrder(order: Order)
}

@Dao
interface ReturnDao {
    @Query("SELECT * FROM returns ORDER BY createdAt DESC")
    fun getAllReturns(): Flow<List<ReturnItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturn(returnItem: ReturnItem)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLog)
}

@Dao
interface TeamInviteDao {
    @Query("SELECT * FROM team_invites ORDER BY timestamp DESC")
    fun getAllInvites(): Flow<List<TeamInvite>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvite(invite: TeamInvite)

    @Update
    suspend fun updateInvite(invite: TeamInvite)

    @Query("DELETE FROM team_invites WHERE email = :email")
    suspend fun deleteInvite(email: String)
}

@Dao
interface SyncStatusDao {
    @Query("SELECT * FROM sync_status WHERE id = 1 LIMIT 1")
    fun getSyncStatus(): Flow<SyncStatus?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncStatus(status: SyncStatus)
}

@Database(
    entities = [
        Product::class,
        Order::class,
        ReturnItem::class,
        AuditLog::class,
        TeamInvite::class,
        SyncStatus::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ThriftSixDatabase : RoomDatabase() {
    abstract val productDao: ProductDao
    abstract val orderDao: OrderDao
    abstract val returnDao: ReturnDao
    abstract val auditLogDao: AuditLogDao
    abstract val teamInviteDao: TeamInviteDao
    abstract val syncStatusDao: SyncStatusDao

    companion object {
        @Volatile
        private var INSTANCE: ThriftSixDatabase? = null

        fun getDatabase(context: Context): ThriftSixDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ThriftSixDatabase::class.java,
                    "thriftsix_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
