package com.example.viewmodels

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.DecimalFormat

class ThriftSixViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = ThriftSixDatabase.getDatabase(context)
    val repository = ThriftSixRepository(database)

    // --- Authentication & Role-Based States ---
    var isLoggedIn = mutableStateOf(false)
    var isVerifyingEmail = mutableStateOf(false)
    var authEmail = mutableStateOf("")
    var authPassword = mutableStateOf("")
    var registerMode = mutableStateOf(false)
    var errorState = mutableStateOf("")

    var currentUserEmail = mutableStateOf("nasif.himadri@thriftsix.com")
    var currentUserRole = mutableStateOf("Admin") // Admin, Editor, Viewer

    var rememberMe = mutableStateOf(false)
    private val sharedPrefs = context.getSharedPreferences("thriftsix_prefs", android.content.Context.MODE_PRIVATE)

    // --- Dynamic Shop Branding States ---
    var shopName = mutableStateOf("THRIFTSIX")
    var shopOwner = mutableStateOf("Nasif Himadri Store Spec")
    var shopAddress = mutableStateOf("Dhanmondi Rd 27, Dhaka")
    var shopPhone = mutableStateOf("+8801999888777")
    var shopFacebook = mutableStateOf("facebook.com/thriftsix")

    // Session activity tracker (Inactivity Timeout Simulation)
    var lastActiveTime = mutableStateOf(System.currentTimeMillis())

    // --- Inventory & Showcase States ---
    val allProducts = repository.allProducts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    var searchQuery = mutableStateOf("")
    var selectedCategory = mutableStateOf("All")
    var sortOption = mutableStateOf("SKU Asc") // "SKU Asc", "Stock Low-High", "Stock High-Low", "Price Dec"

    // --- Order Form & Invoicing States ---
    var orderCart = mutableStateListOf<OrderItemSpec>()
    var orderCustName = mutableStateOf("")
    var orderCustAddress = mutableStateOf("")
    var orderCustEmail = mutableStateOf("client@test.com")
    var orderCustPhone = mutableStateOf("")
    var orderNotes = mutableStateOf("")
    var orderDiscount = mutableStateOf("0.0")
    var orderExpense = mutableStateOf("0.0")

    val allOrders = repository.allOrders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    var selectedOrderForInvoice = mutableStateOf<Order?>(null)

    // --- Scan & QR Returns States ---
    var scanResultProduct = mutableStateOf<Product?>(null)
    var scannerSearchSku = mutableStateOf("")
    var returnReason = mutableStateOf("Size mismatch")
    var returnSelectedSize = mutableStateOf("")
    var returnQty = mutableStateOf("1")
    var returnOrderId = mutableStateOf("")

    // --- Team invitation states ---
    val allInvites = repository.allInvites.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    var inviteEmailInput = mutableStateOf("")
    var inviteRoleInput = mutableStateOf("Editor") // Editor, Viewer

    val allReturns = repository.allReturns.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- Audit & Sync Status ---
    val allAuditLogs = repository.allAuditLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val syncStatus = repository.syncStatus.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SyncStatus()
    )

    init {
        // Programmatically check and initialize Firebase at startup
        val firebaseActive = FirebaseSyncHelper.checkAndInitialize(context)
        Log.d("ThriftSixViewModel", "Firebase programmatic status checked. Active: $firebaseActive")

        // Load Shop Custom preferences
        shopName.value = sharedPrefs.getString("shop_name", "THRIFTSIX") ?: "THRIFTSIX"
        shopOwner.value = sharedPrefs.getString("shop_owner", "Nasif Himadri Store Spec") ?: "Nasif Himadri Store Spec"
        shopAddress.value = sharedPrefs.getString("shop_address", "Dhanmondi Rd 27, Dhaka") ?: "Dhanmondi Rd 27, Dhaka"
        shopPhone.value = sharedPrefs.getString("shop_phone", "+8801999888777") ?: "+8801999888777"
        shopFacebook.value = sharedPrefs.getString("shop_facebook", "facebook.com/thriftsix") ?: "facebook.com/thriftsix"

        // Load remembered credentials or restore current Firebase Auth user session
        val isRemembered = sharedPrefs.getBoolean("remember_me", false)
        rememberMe.value = isRemembered

        if (firebaseActive) {
            try {
                val currentFirebaseUser = FirebaseAuth.getInstance().currentUser
                if (currentFirebaseUser != null) {
                    val email = currentFirebaseUser.email ?: ""
                    currentUserEmail.value = email
                    authEmail.value = email

                    val defaultRole = if (email.contains("admin") || email == "nasif.himadri@thriftsix.com" || email == "ahad50502p@gmail.com") "Admin" else if (email.contains("viewer")) "Viewer" else "Editor"
                    currentUserRole.value = defaultRole
                    isLoggedIn.value = true
                    updateSessionActivity()

                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(currentFirebaseUser.uid)
                        .get()
                        .addOnSuccessListener { doc ->
                            val cloudRole = doc.getString("role")
                            if (!cloudRole.isNullOrEmpty()) {
                                currentUserRole.value = cloudRole
                            }
                            showToast("Logged in: $email (Role: ${currentUserRole.value})")
                        }
                        .addOnFailureListener {
                            showToast("Logged in with default role: ${currentUserRole.value}")
                        }
                } else if (isRemembered) {
                    val savedEmail = sharedPrefs.getString("saved_email", "") ?: ""
                    val savedPassword = sharedPrefs.getString("saved_password", "") ?: ""
                    authEmail.value = savedEmail
                    authPassword.value = savedPassword
                }
            } catch (e: Exception) {
                Log.e("ThriftSixViewModel", "Firebase getCurrentUser error: ${e.message}")
            }
        } else if (isRemembered) {
            val savedEmail = sharedPrefs.getString("saved_email", "") ?: ""
            val savedPassword = sharedPrefs.getString("saved_password", "") ?: ""
            authEmail.value = savedEmail
            authPassword.value = savedPassword
            if (savedEmail.isNotEmpty() && savedPassword.isNotEmpty()) {
                currentUserEmail.value = savedEmail
                currentUserRole.value = if (savedEmail.contains("admin") || savedEmail == "nasif.himadri@thriftsix.com" || savedEmail == "ahad50502p@gmail.com") "Admin" else if (savedEmail.contains("viewer")) "Viewer" else "Editor"
                isLoggedIn.value = true
                showToast("Welcome back! Role: ${currentUserRole.value}")
                updateSessionActivity()
            }
        }

        viewModelScope.launch {
            // Populate database with lovely sample thrift products if default is empty
            repository.prepopulateIfEmpty()
        }
    }

    // --- Safe Thread-Safe Toast Dispatcher ---
    fun showToast(message: String, isLong: Boolean = false) {
        try {
            Handler(Looper.getMainLooper()).post {
                try {
                    Toast.makeText(context, message, if (isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("ThriftSixViewModel", "Failed to show toast: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("ThriftSixViewModel", "Main looper post failed: ${e.message}")
        }
    }

    // --- Interaction Timers & Security Session Timeout ---
    fun updateSessionActivity() {
        lastActiveTime.value = System.currentTimeMillis()
    }

    fun checkSessionTimeout() {
        // Enforce 5 minutes (300,000 ms) inactivity timeout
        val elapsed = System.currentTimeMillis() - lastActiveTime.value
        if (isLoggedIn.value && elapsed > 300000) {
            isLoggedIn.value = false
            errorState.value = "Session timed out due to 5 minutes of inactivity."
            showToast("Session timed out for security.", isLong = true)
        }
    }

    // --- Authenticators ---
    fun validatePassword(password: String): Boolean {
        // Enforce: min 8 char, uppercase, digit, special symbol
        if (password.length < 8) return false
        val hasUpper = password.any { it.isUpperCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }
        return hasUpper && hasDigit && hasSpecial
    }

    fun authenticate() {
        updateSessionActivity()
        val email = authEmail.value.trim()
        val password = authPassword.value

        if (email.isEmpty() || password.isEmpty()) {
            errorState.value = "Email and Password cannot be empty."
            return
        }

        if (!validatePassword(password)) {
            errorState.value = "Password must be at least 8 characters, include an uppercase letter, a digit, and a special character."
            return
        }

        if (FirebaseSyncHelper.isFirebaseAvailable()) {
            val auth = FirebaseAuth.getInstance()
            if (registerMode.value) {
                // Real register
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { authResult ->
                        val user = authResult.user
                        if (user != null) {
                            user.sendEmailVerification()
                                .addOnSuccessListener {
                                    isVerifyingEmail.value = true
                                    errorState.value = ""
                                    showToast("Verification email sent to $email!", isLong = true)

                                    // Save default role in Firestore
                                    val defaultRole = if (email.contains("admin") || email == "nasif.himadri@thriftsix.com" || email == "ahad50502p@gmail.com") "Admin" else "Editor"
                                    val userMap = mapOf(
                                        "email" to email,
                                        "role" to defaultRole
                                    )
                                    FirebaseFirestore.getInstance().collection("users")
                                        .document(user.uid)
                                        .set(userMap)
                                        .addOnSuccessListener {
                                            Log.d("ThriftSixViewModel", "Saved user role to Firestore.")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("ThriftSixViewModel", "Error saving role to Firestore: ${e.message}")
                                        }
                                }
                                .addOnFailureListener { e ->
                                    errorState.value = "Failed to send verification email: ${e.localizedMessage}"
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        errorState.value = e.localizedMessage ?: "Registration failed."
                    }
            } else {
                // Real login
                auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener { authResult ->
                        val user = authResult.user
                        if (user != null) {
                            currentUserEmail.value = email

                            // Caching password logic
                            if (rememberMe.value) {
                                sharedPrefs.edit().apply {
                                    putBoolean("remember_me", true)
                                    putString("saved_email", email)
                                    putString("saved_password", password)
                                    apply()
                                }
                            } else {
                                sharedPrefs.edit().apply {
                                    putBoolean("remember_me", false)
                                    remove("saved_email")
                                    remove("saved_password")
                                    apply()
                                }
                            }

                            val defaultRole = if (email.contains("admin") || email == "nasif.himadri@thriftsix.com" || email == "ahad50502p@gmail.com") "Admin" else if (email.contains("viewer")) "Viewer" else "Editor"
                            currentUserRole.value = defaultRole
                            isLoggedIn.value = true
                            errorState.value = ""

                            // Load user role from Firestore
                            FirebaseFirestore.getInstance().collection("users")
                                .document(user.uid)
                                .get()
                                .addOnSuccessListener { doc ->
                                    val cloudRole = doc.getString("role")
                                    if (!cloudRole.isNullOrEmpty()) {
                                        currentUserRole.value = cloudRole
                                    }
                                    showToast("Welcome! Role: ${currentUserRole.value}")
                                }
                                .addOnFailureListener {
                                    showToast("Welcome! Role defaults to ${currentUserRole.value}")
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        errorState.value = e.localizedMessage ?: "Login failed. Check your coordinates."
                    }
            }
        } else {
            // Offline/Simulation Fallback
            if (registerMode.value) {
                isVerifyingEmail.value = true
                errorState.value = ""
                showToast("Verification email sent to $email! (Simulation Mode)", isLong = true)
            } else {
                currentUserEmail.value = email
                if (rememberMe.value) {
                    sharedPrefs.edit().apply {
                        putBoolean("remember_me", true)
                        putString("saved_email", email)
                        putString("saved_password", password)
                        apply()
                    }
                } else {
                    sharedPrefs.edit().apply {
                        putBoolean("remember_me", false)
                        remove("saved_email")
                        remove("saved_password")
                        apply()
                    }
                }

                if (email.contains("admin") || email == "nasif.himadri@thriftsix.com" || email == "ahad50502p@gmail.com") {
                    currentUserRole.value = "Admin"
                } else if (email.contains("viewer")) {
                    currentUserRole.value = "Viewer"
                } else {
                    currentUserRole.value = "Editor"
                }
                isLoggedIn.value = true
                errorState.value = ""
                showToast("Welcome back! Role: ${currentUserRole.value} (Simulation Mode)")
            }
        }
    }

    fun confirmEmailVerified() {
        val email = authEmail.value.trim()
        val password = authPassword.value

        if (FirebaseSyncHelper.isFirebaseAvailable()) {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                user.reload()
                    .addOnSuccessListener {
                        if (user.isEmailVerified) {
                            currentUserEmail.value = email
                            
                            val defaultRole = if (email.contains("admin") || email == "nasif.himadri@thriftsix.com" || email == "ahad50502p@gmail.com") "Admin" else "Editor"
                            currentUserRole.value = defaultRole
                            isLoggedIn.value = true
                            isVerifyingEmail.value = false
                            errorState.value = ""

                            if (rememberMe.value && email.isNotEmpty() && password.isNotEmpty()) {
                                sharedPrefs.edit().apply {
                                    putBoolean("remember_me", true)
                                    putString("saved_email", email)
                                    putString("saved_password", password)
                                    apply()
                                }
                            }

                            // Fetch cloud role if any
                            FirebaseFirestore.getInstance().collection("users")
                                .document(user.uid)
                                .get()
                                .addOnSuccessListener { doc ->
                                    val cloudRole = doc.getString("role")
                                    if (!cloudRole.isNullOrEmpty()) {
                                        currentUserRole.value = cloudRole
                                    }
                                }

                            showToast("Email verified successfully!")
                        } else {
                            showToast("Email registration pending corporate link verification check.", isLong = true)
                        }
                    }
                    .addOnFailureListener { e ->
                        showToast("Reload failed: ${e.localizedMessage}")
                    }
            } else {
                showToast("No active user configuration context.")
            }
        } else {
            // Offline/Simulation confirm
            currentUserEmail.value = email
            currentUserRole.value = "Admin"
            isLoggedIn.value = true
            isVerifyingEmail.value = false
            errorState.value = ""

            if (rememberMe.value && email.isNotEmpty() && password.isNotEmpty()) {
                sharedPrefs.edit().apply {
                    putBoolean("remember_me", true)
                    putString("saved_email", email)
                    putString("saved_password", password)
                    apply()
                }
            }

            showToast("Email verified successfully! (Simulation Mode)")
        }
    }

    fun logout() {
        isLoggedIn.value = false
        authPassword.value = ""
        registerMode.value = false
        isVerifyingEmail.value = false

        try {
            if (FirebaseSyncHelper.isFirebaseAvailable()) {
                FirebaseAuth.getInstance().signOut()
            }
        } catch (e: Exception) {
            Log.e("ThriftSixViewModel", "Firebase sign out error: ${e.message}")
        }

        // Clear credentials on manual logout for data privacy & security
        sharedPrefs.edit().apply {
            putBoolean("remember_me", false)
            remove("saved_email")
            remove("saved_password")
            apply()
        }

        showToast("Logged out securely.")
    }

    // --- Inventory Operations ---
    fun addOrUpdateProduct(sku: String, name: String, category: String, color: String, desc: String, cost: Double, sell: Double, sizes: Map<String, Int>, isEdit: Boolean, existingId: Int = 0, image: String? = null) {
        updateSessionActivity()
        if (currentUserRole.value == "Viewer") {
            showToast("Permission Denied: Viewer role is Read-Only.", isLong = true)
            return
        }

        val totalStock = sizes.values.sum()
        val product = Product(
            id = if (isEdit) existingId else 0,
            sku = sku.uppercase().trim(),
            name = name.trim(),
            image = image,
            color = color.trim(),
            description = desc.trim(),
            batchNumber = if (isEdit) "B-${(1000..9999).random()}" else "B-${(1000..9999).random()}",
            costPrice = cost,
            sellingPrice = sell,
            quantity = totalStock,
            category = category.trim(),
            sizesAndStocksJson = Product.mapToJson(sizes)
        )

        viewModelScope.launch {
            if (isEdit) {
                repository.updateProduct(product, currentUserEmail.value)
                showToast("Product inventory updated!")
            } else {
                repository.insertProduct(product, currentUserEmail.value)
                showToast("Product item added successfully!")
            }
        }
    }

    fun deleteItem(product: Product) {
        updateSessionActivity()
        if (currentUserRole.value != "Admin") {
            showToast("Permission Denied: Only Admin can delete catalogue items.", isLong = true)
            return
        }

        viewModelScope.launch {
            repository.deleteProduct(product, currentUserEmail.value)
            showToast("Product catalog entry deleted.")
        }
    }

    // --- Order Form cart actions ---
    fun addToCart(product: Product, size: String, qty: Int) {
        updateSessionActivity()
        val maxAvailable = product.getStockForSize(size)
        if (qty > maxAvailable) {
            showToast("Only $maxAvailable items level left in size $size!", isLong = true)
            return
        }

        // Check if already in cart
        val idx = orderCart.indexOfFirst { it.sku == product.sku && it.size == size }
        if (idx != -1) {
            val combinedQty = orderCart[idx].qty + qty
            if (combinedQty > maxAvailable) {
                showToast("Cannot exceed available sizing stock ($maxAvailable)", isLong = true)
                return
            }
            orderCart[idx] = orderCart[idx].copy(qty = combinedQty)
        } else {
            orderCart.add(OrderItemSpec(product.sku, product.name, size, qty, product.sellingPrice, product.image))
        }
        showToast("${product.name} Added to Order Draft")
    }

    fun removeFromCart(item: OrderItemSpec) {
        orderCart.remove(item)
    }

    fun checkoutOrder() {
        updateSessionActivity()
        if (currentUserRole.value == "Viewer") {
            showToast("Permission Denied: Viewer is read-only.", isLong = true)
            return
        }
        if (orderCart.isEmpty()) {
            showToast("Draft order cart is empty!")
            return
        }
        if (orderCustName.value.isBlank() || orderCustPhone.value.isBlank()) {
            showToast("Please input Customer Name and Phone Number.")
            return
        }

        viewModelScope.launch {
            val invoiceId = repository.getNextInvoiceId()
            val total = orderCart.sumOf { it.qty * it.price }
            val discountVal = orderDiscount.value.toDoubleOrNull() ?: 0.0
            val expenseVal = orderExpense.value.toDoubleOrNull() ?: 0.0

            val order = Order(
                orderId = invoiceId,
                customerName = orderCustName.value.trim(),
                customerAddress = orderCustAddress.value.trim(),
                customerEmail = orderCustEmail.value.trim(),
                customerPhone = orderCustPhone.value.trim(),
                orderNotes = orderNotes.value.trim(),
                itemsJson = Order.listToJson(orderCart.toList()),
                totalAmount = (total - discountVal).coerceAtLeast(0.0),
                discount = discountVal,
                expense = expenseVal,
                createdAt = System.currentTimeMillis(),
                status = "Processing"
            )

            repository.insertOrder(order, currentUserEmail.value)

            // Clear draft cart
            orderCart.clear()
            orderCustName.value = ""
            orderCustAddress.value = ""
            orderCustEmail.value = "client@test.com"
            orderCustPhone.value = ""
            orderNotes.value = ""
            orderDiscount.value = "0.0"
            orderExpense.value = "0.0"

            selectedOrderForInvoice.value = order
            showToast("Invoice $invoiceId processed successfully!", isLong = true)
        }
    }

    fun sendInvoiceEmailDirect(order: Order) {
        updateSessionActivity()
        
        val itemsList = order.getItemsList()
        val itemsText = itemsList.joinToString("\n") { "- ${it.name} (${it.sku}) Size ${it.size} x ${it.qty} @ ৳${it.price}" }
        
        val emailBody = """
            Dear ${order.customerName},
            
            Thank you for shopping with THRIFTSIX! Here are the details of your official invoice:
            
            Invoice Reference ID: ${order.orderId}
            Date Generated: ${java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(order.createdAt))}
            
            ------------------------------------------
            Apparel Sizing Specifics:
            $itemsText
            ------------------------------------------
            
            Subtotal Sizing Sum: BDT ${itemsList.sumOf { it.qty * it.price }}
            Promo Campaign Discount: -BDT ${order.discount}
            Delivery Courier Surcharge: +BDT ${order.expense}
            ==========================================
            Total Net BDT Payable: BDT ${order.totalAmount}
            ==========================================
            
            Delivery Destination:
            ${order.customerAddress.ifBlank { "N/A" }}
            
            Order Remarks & Notes:
            ${order.orderNotes.ifBlank { "No special remarks." }}
            
            Ledger system authentic verified.
            Need further support? WhatsApp us anytime: +8801999888777
            
            Kind Regards,
            Nasif Himadri
            Executive Officer, THRIFTSIX Head Office
        """.trimIndent()

        // Create standard mail prefill intent
        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("mailto:")
            putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(order.customerEmail))
            putExtra(android.content.Intent.EXTRA_SUBJECT, "THRIFTSIX Official Invoice Details - ${order.orderId}")
            putExtra(android.content.Intent.EXTRA_TEXT, emailBody)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        try {
            context.startActivity(intent)
            showToast("Redirecting to your preferred mail client...")
        } catch (e: Exception) {
            // Fallback: system application share-sheet if mailto doesn't resolve readily
            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(order.customerEmail))
                putExtra(android.content.Intent.EXTRA_SUBJECT, "THRIFTSIX Official Invoice Details - ${order.orderId}")
                putExtra(android.content.Intent.EXTRA_TEXT, emailBody)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                val chooser = android.content.Intent.createChooser(shareIntent, "Share Branded Invoice Details").apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } catch (ex: Exception) {
                // Clipboard copy if all else fails
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("ThriftSix Invoice", emailBody)
                clipboard.setPrimaryClip(clip)
                showToast("Invoice details copied to clipboard! (Please paste manually)", isLong = true)
            }
        }

        viewModelScope.launch {
            repository.manualSync() // Fire sync status and alert log
        }
    }

    // --- Scanning & Process Return Operations ---
    fun lookupProductForScanner(sku: String) {
        updateSessionActivity()
        viewModelScope.launch {
            val prod = repository.getProductBySku(sku.uppercase().trim())
            if (prod != null) {
                scanResultProduct.value = prod
                val sizesMap = prod.getSizesAndStocksMap()
                if (sizesMap.keys.isNotEmpty()) {
                    returnSelectedSize.value = sizesMap.keys.first()
                }
            } else {
                scanResultProduct.value = null
                showToast("Barcode or SKU $sku not found in catalouge.", isLong = true)
            }
        }
    }

    fun executeManualScanAdjustment(productId: Int, size: String, newQty: Int) {
        updateSessionActivity()
        if (currentUserRole.value == "Viewer") {
            showToast("Viewer can't adjust stocks.")
            return
        }

        viewModelScope.launch {
            val prod = repository.getProductById(productId)
            if (prod != null) {
                val stockMap = prod.getSizesAndStocksMap().toMutableMap()
                stockMap[size] = newQty
                val totalQty = stockMap.values.sum()
                val updated = prod.copy(
                    sizesAndStocksJson = Product.mapToJson(stockMap),
                    quantity = totalQty
                )
                repository.updateProduct(updated, currentUserEmail.value)
                scanResultProduct.value = updated
                showToast("Manual Stock adjusted!")
            }
        }
    }

    fun submitReturnItem() {
        updateSessionActivity()
        val orderId = returnOrderId.value.uppercase().trim()
        val sku = scannerSearchSku.value.uppercase().trim()
        val size = returnSelectedSize.value
        val qty = returnQty.value.toIntOrNull() ?: 1

        if (orderId.isEmpty() || sku.isEmpty() || size.isEmpty()) {
            showToast("Specify Order ID, SKU, and sizing parameters.")
            return
        }

        viewModelScope.launch {
            val returnItem = ReturnItem(
                orderId = orderId,
                sku = sku,
                size = size,
                quantity = qty,
                reason = returnReason.value,
                createdAt = System.currentTimeMillis()
            )
            repository.processReturn(returnItem, currentUserEmail.value)
            // Re-fetch product to update UI
            lookupProductForScanner(sku)
            showToast("Return processed! Inventory replenished.", isLong = true)

            // reset return forms
            returnOrderId.value = ""
        }
    }

    // --- Team invitation flow ---
    fun sendInvite() {
        updateSessionActivity()
        if (currentUserRole.value != "Admin") {
            showToast("Only Admin accounts can invite team members.")
            return
        }

        val email = inviteEmailInput.value.trim()
        if (email.isEmpty() || !email.contains("@")) {
            showToast("Please input a valid email.")
            return
        }

        viewModelScope.launch {
            val invite = TeamInvite(email, inviteRoleInput.value, "Pending", System.currentTimeMillis())
            repository.insertInvite(invite)
            inviteEmailInput.value = ""
            showToast("Invite sent to $email!")

            // Simulation of team response accepting or declining in 4 seconds
            kotlinx.coroutines.delay(4000)
            val responseStatus = if (email.startsWith("reject")) "Declined" else "Accepted"
            repository.updateInvite(invite.copy(status = responseStatus))
        }
    }

    fun removeTeamMember(email: String) {
        updateSessionActivity()
        if (currentUserRole.value != "Admin") {
            showToast("Only Admin accounts can remove team members.")
            return
        }
        viewModelScope.launch {
            repository.deleteInvite(email)
            showToast("Removed $email from team.")
        }
    }

    fun updateTeamMemberRole(email: String, newRole: String) {
        updateSessionActivity()
        if (currentUserRole.value != "Admin") {
            showToast("Only Admin accounts can modify roles.")
            return
        }
        viewModelScope.launch {
            val existing = allInvites.value.find { it.email == email }
            if (existing != null) {
                repository.updateInvite(existing.copy(role = newRole))
                showToast("Updated $email role to $newRole")
            }
        }
    }

    fun triggerSyncNow() {
        viewModelScope.launch {
            repository.manualSync()
            showToast("Offline changes synced immediately to Firebase Firestore!")
        }
    }

    fun updateShopInfo(name: String, owner: String, address: String, phone: String, facebook: String) {
        shopName.value = name.trim()
        shopOwner.value = owner.trim()
        shopAddress.value = address.trim()
        shopPhone.value = phone.trim()
        shopFacebook.value = facebook.trim()

        sharedPrefs.edit().apply {
            putString("shop_name", name.trim())
            putString("shop_owner", owner.trim())
            putString("shop_address", address.trim())
            putString("shop_phone", phone.trim())
            putString("shop_facebook", facebook.trim())
            apply()
        }
        showToast("Shop Branding Information Updated!")
    }

    fun updateOrderStatus(order: Order, newStatus: String) {
        updateSessionActivity()
        viewModelScope.launch {
            val updated = order.copy(status = newStatus)
            repository.updateOrder(updated, currentUserEmail.value)
            showToast("Order ${order.orderId} Delivery Status is now: $newStatus")
        }
    }

    fun saveInvoiceCopyToFile(order: Order) {
        updateSessionActivity()
        val itemsList = order.getItemsList()
        val itemsText = itemsList.joinToString("\n") { 
            "  * ${it.name} [SKU: ${it.sku}] - Size: ${it.size} x ${it.qty} @ BDT ${it.price}" 
        }
        
        val dateString = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(order.createdAt))
        val subtotal = itemsList.sumOf { it.qty * it.price }

        val formatText = """
            ==================================================
                        OFFICIAL INVOICE - COPY
            ==================================================
            Store: ${shopName.value}
            Spec: ${shopOwner.value}
            Address: ${shopAddress.value}
            Phone: ${shopPhone.value}
            Socials: ${shopFacebook.value}
            --------------------------------------------------
            Invoice Reference ID: ${order.orderId}
            Generation Date: $dateString
            Delivery Status: ${order.status}
            ==================================================
            RECIPIENT CUSTOMER INFO
            --------------------------------------------------
            Name: ${order.customerName}
            Phone/WhatsApp: ${order.customerPhone}
            Email Address: ${order.customerEmail}
            Delivery Destination: ${order.customerAddress.ifBlank { "N/A" }}
            ==================================================
            BILLING SPECIFICATIONS & ITEMS
            --------------------------------------------------
            $itemsText
            --------------------------------------------------
            Subtotal: BDT $subtotal
            Discounts: -BDT ${order.discount}
            Courier Charges: +BDT ${order.expense}
            --------------------------------------------------
            NET TOTAL PAYABLE VALUE: BDT ${order.totalAmount}
            ==================================================
            Custom Note: ${order.orderNotes.ifBlank { "No special remarks." }}
            ==================================================
            Verified by: ${currentUserEmail.value}
            Thank you for shopping with ${shopName.value}!
            --------------------------------------------------
            Verification QR Copy: https://${shopName.value.lowercase().replace(" ", "")}.com/verify/${order.orderId}
        """.trimIndent()

        try {
            // Write to local cache or app accessible directory
            val directory = java.io.File(context.getExternalFilesDir(null), "Invoices")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val file = java.io.File(directory, "Invoice_${order.orderId}.txt")
            file.writeText(formatText)

            // Copy to Clipboard
            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("ThriftSix Invoice", formatText)
            clipboard.setPrimaryClip(clip)

            showToast("Invoice Copy saved locally & copied to clipboard!\nPath: ${file.absolutePath}", isLong = true)
        } catch (e: Exception) {
            Log.e("ThriftSixViewModel", "Save file failed: ${e.message}")
            showToast("Saved to clipboard! (File write error: ${e.localizedMessage})", isLong = true)
        }
    }
}
