package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseSyncHelper {
    private const val TAG = "FirebaseSyncHelper"
    private var initialized = false

    /**
     * Attempts to check and initialize the Firebase Application programmatically.
     * Catches and ignores exceptions if google-services.json is absent.
     */
    fun checkAndInitialize(context: Context): Boolean {
        if (initialized) return true
        return try {
            val app = FirebaseApp.initializeApp(context)
            initialized = (app != null)
            initialized
        } catch (e: Throwable) {
            Log.e(TAG, "Firebase not initialized (likely missing google-services.json): ${e.message}")
            initialized = false
            false
        }
    }

    fun isFirebaseAvailable(): Boolean = initialized

    /**
     * Synchronizes a product to Firebase Firestore if initialized
     */
    fun syncProductToCloud(product: Product) {
        if (!initialized) return
        try {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("products")
                .document(product.sku)
                .set(product)
                .addOnSuccessListener {
                    Log.d(TAG, "Successfully synced product ${product.sku} to cloud.")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed syncing product to cloud: ${e.message}")
                }
        } catch (e: Throwable) {
            Log.e(TAG, "Error during Firestore operation: ${e.message}")
        }
    }

    /**
     * Synchronizes an order/invoice to Firebase Firestore if initialized
     */
    fun syncOrderToCloud(order: Order) {
        if (!initialized) return
        try {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("orders")
                .document(order.orderId)
                .set(order)
                .addOnSuccessListener {
                    Log.d(TAG, "Successfully synced order ${order.orderId} to cloud.")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed syncing order to cloud: ${e.message}")
                }
        } catch (e: Throwable) {
            Log.e(TAG, "Error during Firestore operation: ${e.message}")
        }
    }

    /**
     * Synchronizes a return entry to Firebase Firestore if initialized
     */
    fun syncReturnToCloud(returnItem: ReturnItem) {
        if (!initialized) return
        try {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("returns")
                .document("${returnItem.orderId}_${returnItem.sku}")
                .set(returnItem)
                .addOnSuccessListener {
                    Log.d(TAG, "Successfully synced return to cloud.")
                }
        } catch (e: Throwable) {
            Log.e(TAG, "Error: ${e.message}")
        }
    }

    /**
     * Synchronizes a team invitation to Firebase Firestore if initialized
     */
    fun syncInviteToCloud(invite: TeamInvite) {
        if (!initialized) return
        try {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("invites")
                .document(invite.email)
                .set(invite)
                .addOnSuccessListener {
                    Log.d(TAG, "Successfully synced invite to cloud.")
                }
        } catch (e: Throwable) {
            Log.e(TAG, "Error: ${e.message}")
        }
    }
}
