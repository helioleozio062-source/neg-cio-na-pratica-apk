package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.remote.*
import com.example.data.repository.*
import com.example.util.PhoneUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

enum class AppScreen {
    LOGIN,
    REGISTER,
    BUSINESS_SETUP,
    DASHBOARD,
    SERVICES, // Módulo de Gestão de Serviços (Requisito 1)
    PRODUCTS_STOCK,
    POS_SALES,
    DEBTS,
    FINANCE,
    REPORTS_DRE,
    FEEDBACK,
    ADMIN_PANEL,
    SETTINGS
}

enum class ReportPeriod {
    TODAY,
    WEEK,
    MONTH,
    ALL
}

data class PlatformAdminStats(
    val totalUsers: Int = 0,
    val totalBusinesses: Int = 0,
    val totalSalesVolume: Double = 0.0,
    val pendingComplaints: Int = 0,
    val resolvedComplaints: Int = 0,
    val totalSuggestions: Int = 0,
    val averageRating: Double = 5.0
)

class MainViewModel(
    private val repository: AppRepository,
    private val syncManager: FirebaseSyncManager? = null
) : ViewModel() {

    private val _currentScreen = MutableStateFlow(AppScreen.LOGIN)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _activeBusiness = MutableStateFlow<BusinessEntity?>(null)
    val activeBusiness: StateFlow<BusinessEntity?> = _activeBusiness.asStateFlow()

    private val _businesses = MutableStateFlow<List<BusinessEntity>>(emptyList())
    val businesses: StateFlow<List<BusinessEntity>> = _businesses.asStateFlow()

    private val _products = MutableStateFlow<List<ProductEntity>>(emptyList())
    val products: StateFlow<List<ProductEntity>> = _products.asStateFlow()

    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories.asStateFlow()

    private val _stockMovements = MutableStateFlow<List<StockMovementEntity>>(emptyList())
    val stockMovements: StateFlow<List<StockMovementEntity>> = _stockMovements.asStateFlow()

    private val _sales = MutableStateFlow<List<SaleEntity>>(emptyList())
    val sales: StateFlow<List<SaleEntity>> = _sales.asStateFlow()

    private val _transactions = MutableStateFlow<List<FinancialTransactionEntity>>(emptyList())
    val transactions: StateFlow<List<FinancialTransactionEntity>> = _transactions.asStateFlow()

    private val _debts = MutableStateFlow<List<DebtEntity>>(emptyList())
    val debts: StateFlow<List<DebtEntity>> = _debts.asStateFlow()

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _reportPeriod = MutableStateFlow(ReportPeriod.MONTH)
    val reportPeriod: StateFlow<ReportPeriod> = _reportPeriod.asStateFlow()

    private val _dreReport = MutableStateFlow<DreReportData?>(null)
    val dreReport: StateFlow<DreReportData?> = _dreReport.asStateFlow()

    private val _feedbacks = MutableStateFlow<List<FeedbackEntity>>(emptyList())
    val feedbacks: StateFlow<List<FeedbackEntity>> = _feedbacks.asStateFlow()

    private val _adminStats = MutableStateFlow(PlatformAdminStats())
    val adminStats: StateFlow<PlatformAdminStats> = _adminStats.asStateFlow()

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    private val _reservations = MutableStateFlow<List<ReservationEntity>>(emptyList())
    val reservations: StateFlow<List<ReservationEntity>> = _reservations.asStateFlow()

    // Gestão de Serviços & Catálogo (Requisitos 1, 2, 3)
    private val _services = MutableStateFlow<List<ServiceEntity>>(emptyList())
    val services: StateFlow<List<ServiceEntity>> = _services.asStateFlow()

    private val _serviceCatalog = MutableStateFlow<List<ServiceCatalogEntity>>(emptyList())
    val serviceCatalog: StateFlow<List<ServiceCatalogEntity>> = _serviceCatalog.asStateFlow()

    private val _allPlatformBusinesses = MutableStateFlow<List<BusinessEntity>>(emptyList())
    val allPlatformBusinesses: StateFlow<List<BusinessEntity>> = _allPlatformBusinesses.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _prefilledBusinessName = MutableStateFlow("")
    val prefilledBusinessName: StateFlow<String> = _prefilledBusinessName.asStateFlow()

    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _feedbackSelectedTab = MutableStateFlow(0) // 0: Avaliações, 1: Reclamações, 2: Sugestões
    val feedbackSelectedTab: StateFlow<Int> = _feedbackSelectedTab.asStateFlow()

    fun setFeedbackSelectedTab(tabIndex: Int) {
        _feedbackSelectedTab.value = tabIndex
    }

    fun navigateToComplaints() {
        _feedbackSelectedTab.value = 1
        _currentScreen.value = AppScreen.FEEDBACK
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
    }

    fun syncWithCloud() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            _syncStatus.value = _syncStatus.value.copy(inProgress = true, errorMessage = null)
            try {
                val result = syncManager?.syncAll(user.id, repository.database)
                if (result != null) {
                    _syncStatus.value = result
                    _uiMessage.value = if (result.errorMessage != null) {
                        "Sincronização: ${result.errorMessage}"
                    } else {
                        "Nuvem sincronizada com sucesso! (${result.itemsSynced} registos actualizados)"
                    }
                    loadBusinessesForUser(user.id)
                } else {
                    _syncStatus.value = _syncStatus.value.copy(inProgress = false, lastSyncTime = System.currentTimeMillis())
                    _uiMessage.value = "Modo local: os dados estão salvos com segurança no dispositivo."
                }
            } catch (e: Exception) {
                _syncStatus.value = _syncStatus.value.copy(inProgress = false, errorMessage = e.localizedMessage)
                _uiMessage.value = "Erro na sincronização: ${e.message}"
            }
        }
    }

    fun loginWithGoogle(email: String, displayName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                var user = repository.userDao.getUserByEmail(email.trim().lowercase())
                if (user == null) {
                    user = UserEntity(
                        id = UUID.randomUUID().toString(),
                        email = email.trim().lowercase(),
                        fullName = displayName.ifBlank { "Utilizador Google" },
                        passwordHash = "GOOGLE_AUTH",
                        role = if (email.equals("admin@negocionapratica.com", ignoreCase = true) || email.equals("helioleozio062@gmail.com", ignoreCase = true)) "ADMIN" else "OWNER"
                    )
                    repository.userDao.insertUser(user)
                }
                _currentUser.value = user
                loadBusinessesForUser(user.id)
                _uiMessage.value = "Sessão iniciada com Google! Bem-vindo, ${user.fullName}."
                if (user.role == "ADMIN") {
                    _currentScreen.value = AppScreen.ADMIN_PANEL
                } else if (_activeBusiness.value != null) {
                    _currentScreen.value = AppScreen.DASHBOARD
                } else {
                    _currentScreen.value = AppScreen.BUSINESS_SETUP
                }
                syncWithCloud()
            } catch (e: Exception) {
                _uiMessage.value = "Erro no login Google: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun getSaleItems(saleId: String): List<SaleItemEntity> {
        return repository.saleDao.getItemsForSale(saleId)
    }

    init {
        // Collect feedback & platform admin metrics
        viewModelScope.launch {
            repository.getAllFeedbacks().collect { list ->
                _feedbacks.value = list
                updateAdminStats()
            }
        }
        viewModelScope.launch {
            repository.getAllBusinesses().collect { list ->
                _allPlatformBusinesses.value = list
                updateAdminStats()
            }
        }
    }

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    fun navigateTo(screen: AppScreen) {
        if (screen == AppScreen.ADMIN_PANEL) {
            val user = _currentUser.value
            if (user == null || user.role != "ADMIN") {
                _uiMessage.value = "Acesso Negado: A área de administração é restrita exclusivamente ao Administrador Geral autenticado."
                _currentScreen.value = if (user != null) AppScreen.DASHBOARD else AppScreen.LOGIN
                return
            }
        }
        _currentScreen.value = screen
    }

    // Auth
    fun login(emailOrPhone: String, pass: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val user = repository.authenticate(emailOrPhone, pass)
                if (user != null) {
                    _currentUser.value = user
                    loadBusinessesForUser(user.id)
                    _uiMessage.value = "Sessão iniciada com sucesso. Bem-vindo, ${user.fullName}!"
                    if (user.role == "ADMIN") {
                        _currentScreen.value = AppScreen.ADMIN_PANEL
                    }
                } else {
                    _uiMessage.value = "Credenciais inválidas. Verifique o telefone/e-mail e a palavra-passe."
                }
            } catch (e: Exception) {
                _uiMessage.value = "Erro ao iniciar sessão: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(fullName: String, emailOrPhone: String, pass: String, phone: String, initialBusinessName: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val user = repository.registerUser(fullName, emailOrPhone, pass, phone)
                _currentUser.value = user
                _prefilledBusinessName.value = initialBusinessName?.trim() ?: ""
                _currentScreen.value = AppScreen.BUSINESS_SETUP
                _uiMessage.value = "Conta criada com sucesso! Conclua o registo do seu negócio com a localização e ramo comercial."
            } catch (e: Exception) {
                _uiMessage.value = e.message ?: "Erro no registo de conta."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun changePassword(oldPass: String, newPass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val user = _currentUser.value
            if (user == null) {
                onResult(false, "Nenhum utilizador com sessão ativa.")
                return@launch
            }
            if (user.passwordHash != "GOOGLE_AUTH" && user.passwordHash != oldPass.trim()) {
                onResult(false, "O código ou palavra-passe atual está incorreto.")
                return@launch
            }
            if (newPass.trim().length < 4) {
                onResult(false, "O novo código deve conter pelo menos 4 caracteres.")
                return@launch
            }
            val updated = user.copy(passwordHash = newPass.trim())
            repository.userDao.insertUser(updated)
            _currentUser.value = updated
            _uiMessage.value = "Código de acesso alterado com sucesso! Guarde o seu novo código."
            onResult(true, "Código alterado com sucesso!")
        }
    }

    fun recoverPassword(identifier: String, newPass: String) {
        viewModelScope.launch {
            if (identifier.trim().equals("admin@negocionapratica.com", ignoreCase = true)) {
                _uiMessage.value = "Por razões de segurança, o código de Administrador Geral não pode ser redefinido por este formulário."
                return@launch
            }
            val ok = repository.resetPassword(identifier, newPass)
            if (ok) {
                _uiMessage.value = "Palavra-passe redefinida com sucesso. Faça login agora."
            } else {
                _uiMessage.value = "Telefone ou e-mail não encontrado no sistema."
            }
        }
    }

    fun switchToAdmin(codeOrPassword: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                var admin = repository.userDao.getUserByEmail("admin@negocionapratica.com")
                if (admin == null) {
                    admin = com.example.data.model.UserEntity(
                        id = "admin-user-01",
                        email = "admin@negocionapratica.com",
                        fullName = "Administrador Geral",
                        passwordHash = "admin123",
                        phone = "+258840000000",
                        role = "ADMIN"
                    )
                    repository.userDao.insertUser(admin)
                }

                if (admin.passwordHash == codeOrPassword.trim()) {
                    _currentUser.value = admin
                    loadBusinessesForUser(admin.id)
                    _currentScreen.value = AppScreen.ADMIN_PANEL
                    _uiMessage.value = "Sessão de Administrador Geral iniciada com sucesso!"
                    onResult(true, "Acesso concedido!")
                } else {
                    onResult(false, "Código de Administrador incorreto. Verifique a palavra-passe.")
                }
            } catch (e: Exception) {
                onResult(false, "Erro ao validar: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Phone verification (OTP) service
    val phoneVerificationService = PhoneVerificationService()
    private val _otpState = MutableStateFlow<OtpRequestResult?>(null)
    val otpState: StateFlow<OtpRequestResult?> = _otpState.asStateFlow()

    fun sendPhoneVerificationCode(phone: String) {
        val result = phoneVerificationService.sendVerificationCode(phone)
        _otpState.value = result
        when (result) {
            is OtpRequestResult.CodeSent -> {
                _uiMessage.value = "Código de confirmação gerado para ${result.formattedDisplay}."
            }
            is OtpRequestResult.RateLimited -> {
                _uiMessage.value = "Aguarde ${result.waitSecondsRemaining} segundos antes de solicitar um novo código."
            }
            is OtpRequestResult.Error -> {
                _uiMessage.value = result.message
            }
        }
    }

    fun verifyPhoneCode(phone: String, code: String, onSuccess: () -> Unit) {
        when (val result = phoneVerificationService.verifyCode(phone, code)) {
            is OtpVerifyResult.Success -> {
                _uiMessage.value = "Número de telefone confirmado com sucesso!"
                val user = _currentUser.value
                if (user != null) {
                    viewModelScope.launch {
                        repository.markPhoneVerified(user.id)
                        _currentUser.value = user.copy(isPhoneVerified = true)
                    }
                }
                onSuccess()
            }
            is OtpVerifyResult.InvalidCode -> {
                _uiMessage.value = "Código incorreto. Tentativas restantes: ${result.attemptsRemaining}."
            }
            is OtpVerifyResult.Expired -> {
                _uiMessage.value = "O código expirou. Solicite um novo código."
            }
            is OtpVerifyResult.MaxAttemptsExceeded -> {
                _uiMessage.value = "Limite de tentativas excedido. Solicite um novo código."
            }
            is OtpVerifyResult.NotFound -> {
                _uiMessage.value = "Não foi encontrado pedido de confirmação ativo para este número."
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _activeBusiness.value = null
        _cartItems.value = emptyList()
        _currentScreen.value = AppScreen.LOGIN
        _uiMessage.value = "Sessão encerrada."
    }

    private fun loadBusinessesForUser(userId: String) {
        viewModelScope.launch {
            repository.getBusinessesForOwner(userId).collect { list ->
                _businesses.value = list
                if (_activeBusiness.value == null && list.isNotEmpty()) {
                    selectBusiness(list.first())
                } else if (list.isEmpty() && _currentUser.value?.role != "ADMIN") {
                    _currentScreen.value = AppScreen.BUSINESS_SETUP
                }
            }
        }
    }

    fun selectBusiness(business: BusinessEntity) {
        _activeBusiness.value = business
        _currentScreen.value = AppScreen.DASHBOARD
        loadBusinessData(business.id)
    }

    fun createBusiness(
        name: String,
        activityType: String,
        currencySymbol: String = "MT",
        defaultUnit: String = "un",
        phone: String = "",
        city: String = "Maputo",
        country: String = "Moçambique",
        province: String = "Maputo Cidade",
        neighborhood: String = "",
        referencePoint: String = "",
        latitude: Double? = null,
        longitude: Double? = null,
        address: String = "",
        taxNumber: String = ""
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val b = repository.createBusiness(
                    ownerId = user.id,
                    name = name,
                    activityType = activityType,
                    currencySymbol = currencySymbol,
                    defaultUnit = defaultUnit,
                    phone = phone,
                    city = city,
                    country = country,
                    province = province,
                    neighborhood = neighborhood,
                    referencePoint = referencePoint,
                    latitude = latitude,
                    longitude = longitude,
                    address = address,
                    taxNumber = taxNumber,
                    ownerEmail = user.email
                )
                selectBusiness(b)
                _uiMessage.value = "Negócio '${b.name}' configurado e pronto a operar!"
            } catch (e: Exception) {
                _uiMessage.value = "Erro ao configurar negócio: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateBusinessDetails(business: BusinessEntity) {
        viewModelScope.launch {
            repository.updateBusiness(business)
            _activeBusiness.value = business
            _uiMessage.value = "Dados do negócio atualizados com sucesso."
        }
    }

    private fun loadBusinessData(businessId: String) {
        viewModelScope.launch {
            repository.getProducts(businessId).collect { _products.value = it }
        }
        viewModelScope.launch {
            repository.getCategories(businessId).collect { _categories.value = it }
        }
        viewModelScope.launch {
            repository.getStockMovements(businessId).collect { _stockMovements.value = it }
        }
        viewModelScope.launch {
            repository.getSales(businessId).collect { _sales.value = it; refreshDreReport() }
        }
        viewModelScope.launch {
            repository.getTransactions(businessId).collect { _transactions.value = it; refreshDreReport() }
        }
        viewModelScope.launch {
            repository.getDebts(businessId).collect { _debts.value = it; refreshDreReport() }
        }
        viewModelScope.launch {
            repository.getReservations(businessId).collect { _reservations.value = it }
        }
        viewModelScope.launch {
            repository.getServices(businessId).collect { _services.value = it; refreshDreReport() }
        }
        viewModelScope.launch {
            repository.getServiceCatalog(businessId).collect { _serviceCatalog.value = it }
        }
    }

    // Products & Stock Actions
    fun addProduct(
        name: String,
        categoryId: String,
        sku: String,
        purchasePrice: Double,
        sellingPrice: Double,
        initialStock: Double,
        minStockAlert: Double,
        unit: String,
        sizes: String = "",
        colors: String = "",
        isPaidToSupplier: Boolean = true,
        supplierDebtAmount: Double = 0.0,
        supplierName: String = "",
        isSoldByWeight: Boolean = false,
        weightUnit: String = "kg"
    ) {
        val b = _activeBusiness.value ?: return
        viewModelScope.launch {
            try {
                val product = ProductEntity(
                    businessId = b.id,
                    categoryId = categoryId,
                    name = name.trim(),
                    sku = sku.trim(),
                    purchasePrice = purchasePrice,
                    sellingPrice = sellingPrice,
                    currentStock = initialStock,
                    minStockAlert = minStockAlert,
                    unit = unit,
                    sizes = sizes,
                    colors = colors,
                    isPaidToSupplier = isPaidToSupplier,
                    supplierDebtAmount = supplierDebtAmount,
                    supplierName = supplierName.trim(),
                    isSoldByWeight = isSoldByWeight,
                    weightUnit = weightUnit
                )
                repository.addProduct(product)
                _uiMessage.value = "Produto '${product.name}' registado com sucesso."
            } catch (e: Exception) {
                _uiMessage.value = "Erro ao adicionar produto: ${e.message}"
            }
        }
    }

    fun updateProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.updateProduct(product)
            _uiMessage.value = "Produto '${product.name}' atualizado."
        }
    }

    fun registerStockPurchase(
        productId: String,
        quantity: Double,
        unitCost: Double,
        supplierName: String,
        paidImmediately: Boolean
    ) {
        val b = _activeBusiness.value ?: return
        viewModelScope.launch {
            try {
                repository.registerStockEntry(b.id, productId, quantity, unitCost, supplierName, paidImmediately)
                _uiMessage.value = "Entrada de stock e compra registadas com sucesso."
                refreshDreReport()
            } catch (e: Exception) {
                _uiMessage.value = "Erro na entrada de stock: ${e.message}"
            }
        }
    }

    fun registerStockAdjustment(productId: String, diff: Double, reason: String) {
        val b = _activeBusiness.value ?: return
        viewModelScope.launch {
            try {
                repository.registerStockAdjustment(b.id, productId, diff, reason)
                _uiMessage.value = "Ajuste de inventário efetuado com sucesso."
            } catch (e: Exception) {
                _uiMessage.value = "Erro no ajuste de stock: ${e.message}"
            }
        }
    }

    // POS Cart & Sales
    fun addToCart(product: ProductEntity) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val item = current[index]
            current[index] = item.copy(quantity = item.quantity + 1.0)
        } else {
            current.add(CartItem(product = product, quantity = 1.0))
        }
        _cartItems.value = current
    }

    fun addToCartWithQuantity(product: ProductEntity, quantity: Double, customUnitDisplay: String = "") {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val item = current[index]
            current[index] = item.copy(
                quantity = item.quantity + quantity,
                customUnitDisplay = if (customUnitDisplay.isNotBlank()) customUnitDisplay else item.customUnitDisplay
            )
        } else {
            current.add(CartItem(product = product, quantity = quantity, customUnitDisplay = customUnitDisplay))
        }
        _cartItems.value = current
    }

    fun updateCartItemQuantity(productId: String, newQty: Double) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            if (newQty <= 0.0) {
                current.removeAt(index)
            } else {
                current[index] = current[index].copy(quantity = newQty)
            }
            _cartItems.value = current
        }
    }

    fun removeFromCart(productId: String) {
        _cartItems.value = _cartItems.value.filter { it.product.id != productId }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    fun finalizeSale(
        customerName: String,
        discount: Double,
        paymentStatus: String, // "PAID" or "CREDIT"
        paymentMethod: String,
        amountPaid: Double,
        dueDate: Long?,
        notes: String
    ) {
        val b = _activeBusiness.value ?: return
        val items = _cartItems.value
        if (items.isEmpty()) {
            _uiMessage.value = "Adicione produtos ao carrinho antes de finalizar a venda."
            return
        }

        viewModelScope.launch {
            try {
                val sale = repository.registerSale(
                    businessId = b.id,
                    customerName = customerName,
                    items = items,
                    discount = discount,
                    paymentStatus = paymentStatus,
                    paymentMethod = paymentMethod,
                    amountPaid = amountPaid,
                    dueDate = dueDate,
                    notes = notes
                )
                clearCart()
                _uiMessage.value = "Venda #${sale.id.takeLast(6)} registada com sucesso!"
                refreshDreReport()
            } catch (e: Exception) {
                _uiMessage.value = "Erro ao registar venda: ${e.message}"
            }
        }
    }

    fun cancelSale(saleId: String) {
        viewModelScope.launch {
            try {
                repository.cancelSale(saleId)
                _uiMessage.value = "Venda cancelada e stock reposto com sucesso."
                refreshDreReport()
            } catch (e: Exception) {
                _uiMessage.value = "Erro ao cancelar venda: ${e.message}"
            }
        }
    }

    fun processPartialReturn(saleId: String, returnedQuantities: Map<String, Double>, reason: String) {
        viewModelScope.launch {
            try {
                repository.processPartialReturn(saleId, returnedQuantities, reason)
                _uiMessage.value = "Devolução registada com sucesso! O stock dos itens devolvidos foi restaurado e o recibo atualizado."
                refreshDreReport()
            } catch (e: Exception) {
                _uiMessage.value = "Erro ao processar devolução: ${e.message}"
            }
        }
    }

    // Finance Actions
    fun addExpense(category: String, amount: Double, description: String, paymentMethod: String) {
        val b = _activeBusiness.value ?: return
        viewModelScope.launch {
            try {
                repository.addOperationalExpense(b.id, category, amount, description, paymentMethod)
                _uiMessage.value = "Despesa de $amount ${b.currencySymbol} registada."
                refreshDreReport()
            } catch (e: Exception) {
                _uiMessage.value = "Erro ao registar despesa: ${e.message}"
            }
        }
    }

    fun addProLabore(amount: Double, notes: String) {
        val b = _activeBusiness.value ?: return
        viewModelScope.launch {
            try {
                repository.addProLabore(b.id, amount, notes)
                _uiMessage.value = "Retirada pessoal (Pró-labore) de $amount ${b.currencySymbol} registada."
                refreshDreReport()
            } catch (e: Exception) {
                _uiMessage.value = "Erro ao registar retirada: ${e.message}"
            }
        }
    }

    fun createDebt(
        type: String, // "RECEIVABLE" ou "PAYABLE"
        partyName: String,
        phone: String = "",
        description: String = "",
        amount: Double,
        dueDate: Long? = null,
        notes: String = ""
    ) {
        val b = _activeBusiness.value ?: return
        viewModelScope.launch {
            try {
                val newDebt = DebtEntity(
                    businessId = b.id,
                    type = type,
                    partyName = partyName.trim(),
                    phone = PhoneUtils.normalizeMozambiquePhone(phone) ?: phone.trim(),
                    description = description.trim(),
                    initialAmount = amount,
                    remainingAmount = amount,
                    dueDate = dueDate,
                    notes = notes.trim(),
                    status = "PENDING"
                )
                repository.createDebt(newDebt)
                _uiMessage.value = if (type == "RECEIVABLE") {
                    "Dívida a receber de $partyName ($amount ${b.currencySymbol}) registada com sucesso."
                } else {
                    "Dívida a pagar a $partyName ($amount ${b.currencySymbol}) registada com sucesso."
                }
                refreshDreReport()
            } catch (e: Exception) {
                _uiMessage.value = "Erro ao registar dívida: ${e.message}"
            }
        }
    }

    fun payDebt(
        debtId: String,
        amount: Double,
        notes: String = "",
        paymentDate: Long = System.currentTimeMillis()
    ) {
        val b = _activeBusiness.value ?: return
        viewModelScope.launch {
            try {
                repository.payDebt(debtId, amount, notes, paymentDate)
                _uiMessage.value = "Pagamento de $amount ${b.currencySymbol} registado e integrado no caixa."
                refreshDreReport()
            } catch (e: Exception) {
                _uiMessage.value = "Erro ao liquidar dívida: ${e.message}"
            }
        }
    }

    suspend fun getPaymentsForDebt(debtId: String): List<DebtPaymentEntity> {
        return repository.getPaymentsForDebtDirect(debtId)
    }

    fun deleteDebt(debt: DebtEntity) {
        viewModelScope.launch {
            try {
                repository.deleteDebt(debt)
                _uiMessage.value = "Registo de dívida removido com sucesso."
                refreshDreReport()
            } catch (e: Exception) {
                _uiMessage.value = "Erro ao eliminar dívida: ${e.message}"
            }
        }
    }

    // Reports / DRE
    fun setReportPeriod(period: ReportPeriod) {
        _reportPeriod.value = period
        refreshDreReport()
    }

    fun refreshDreReport() {
        val b = _activeBusiness.value ?: return
        val (startTime, endTime) = calculateTimeRange(_reportPeriod.value)
        viewModelScope.launch {
            val report = repository.calculateDre(b.id, startTime, endTime)
            _dreReport.value = report
        }
    }

    private fun calculateTimeRange(period: ReportPeriod): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        return when (period) {
            ReportPeriod.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                Pair(cal.timeInMillis, now)
            }
            ReportPeriod.WEEK -> {
                val sevenDaysAgo = now - (7L * 24 * 3600 * 1000)
                Pair(sevenDaysAgo, now)
            }
            ReportPeriod.MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                Pair(cal.timeInMillis, now)
            }
            ReportPeriod.ALL -> {
                Pair(0L, now)
            }
        }
    }

    // Feedback & Platform Support
    fun submitFeedback(type: String, rating: Int, category: String, title: String, description: String) {
        val user = _currentUser.value ?: return
        val bName = _activeBusiness.value?.name ?: "Negócio Geral"
        viewModelScope.launch {
            try {
                repository.submitFeedback(user.id, user.email, bName, type, rating, category, title, description)
                _uiMessage.value = "O seu envio foi registado. A equipa do Negócio na Prática agradece!"
            } catch (e: Exception) {
                _uiMessage.value = "Erro ao enviar feedback: ${e.message}"
            }
        }
    }

    fun updateFeedbackStatus(feedbackId: String, newStatus: String, adminNotes: String) {
        viewModelScope.launch {
            try {
                repository.updateFeedbackStatus(feedbackId, newStatus, adminNotes)
                _uiMessage.value = "Estado da reclamação atualizado para '$newStatus'."
            } catch (e: Exception) {
                _uiMessage.value = "Erro ao atualizar estado: ${e.message}"
            }
        }
    }

    private fun updateAdminStats() {
        viewModelScope.launch {
            val allSales = repository.saleDao.getAllValidSales()
            val totalVol = allSales.sumOf { it.totalAmount }
            val list = _feedbacks.value
            val complaints = list.filter { it.type == "COMPLAINT" }
            val reviews = list.filter { it.type == "REVIEW" }
            val suggestions = list.filter { it.type == "SUGGESTION" }

            val avgRating = if (reviews.isNotEmpty()) reviews.map { it.rating }.average() else 5.0

            _adminStats.value = PlatformAdminStats(
                totalUsers = repository.userDao.getUserByEmail("admin@negocionapratica.com")?.let { 2 } ?: 1,
                totalBusinesses = _allPlatformBusinesses.value.size.coerceAtLeast(1),
                totalSalesVolume = totalVol,
                pendingComplaints = complaints.count { it.status != "RESOLVIDA" },
                resolvedComplaints = complaints.count { it.status == "RESOLVIDA" },
                totalSuggestions = suggestions.size,
                averageRating = avgRating
            )
        }
    }

    // Reservations (Restaurante e Salão de Beleza / Estética)
    fun createReservation(
        customerName: String,
        customerPhone: String,
        reservationDate: String,
        reservationTime: String,
        partySize: Int,
        serviceOrTable: String,
        notes: String
    ) {
        val b = _activeBusiness.value ?: return
        viewModelScope.launch {
            try {
                val res = ReservationEntity(
                    businessId = b.id,
                    customerName = customerName.trim(),
                    customerPhone = customerPhone.trim(),
                    reservationDate = reservationDate.trim(),
                    reservationTime = reservationTime.trim(),
                    partySize = partySize,
                    serviceOrTable = serviceOrTable.trim().ifBlank { if (b.activityType.contains("Salão", ignoreCase = true)) "Serviço de Salão" else "Mesa 1" },
                    notes = notes.trim(),
                    status = "CONFIRMED"
                )
                repository.createReservation(res)
                _uiMessage.value = "Reserva registada para '$customerName' com sucesso!"
            } catch (e: Exception) {
                _uiMessage.value = "Erro ao registar reserva: ${e.message}"
            }
        }
    }

    fun updateReservationStatus(id: String, status: String) {
        viewModelScope.launch {
            try {
                repository.updateReservationStatus(id, status)
                _uiMessage.value = "Estado da reserva atualizado para $status."
            } catch (e: Exception) {
                _uiMessage.value = "Erro ao atualizar reserva: ${e.message}"
            }
        }
    }

    fun deleteReservation(reservation: ReservationEntity) {
        viewModelScope.launch {
            try {
                repository.deleteReservation(reservation)
                _uiMessage.value = "Reserva de ${reservation.customerName} removida."
            } catch (e: Exception) {
                _uiMessage.value = "Erro ao remover reserva: ${e.message}"
            }
        }
    }

    // Admin Autonomy: Businesses, Fees, Cities, Activation & Requests
    fun adminToggleBusinessActive(businessId: String, isActive: Boolean) {
        if (_currentUser.value?.role != "ADMIN") {
            _uiMessage.value = "Acesso Negado: Apenas o Administrador Geral tem autorização para gerir o estado de negócios."
            return
        }
        viewModelScope.launch {
            try {
                repository.setBusinessActiveStatus(businessId, isActive)
                _uiMessage.value = if (isActive) "Negócio ATIVADO com sucesso!" else "Negócio DESATIVADO pelo administrador."
            } catch (e: Exception) {
                _uiMessage.value = "Erro ao alterar estado do negócio: ${e.message}"
            }
        }
    }

    fun adminToggleBusinessFee(businessId: String, isPaid: Boolean, amount: Double = 500.0) {
        if (_currentUser.value?.role != "ADMIN") {
            _uiMessage.value = "Acesso Negado: Apenas o Administrador Geral tem autorização para gerir taxas."
            return
        }
        viewModelScope.launch {
            try {
                repository.setBusinessSubscriptionPaid(businessId, isPaid, amount)
                val formattedAmt = String.format(java.util.Locale.US, "%.2f MT", amount)
                _uiMessage.value = if (isPaid) "Taxa marcada como PAGA ($formattedAmt)." else "Taxa marcada como PENDENTE."
            } catch (e: Exception) {
                _uiMessage.value = "Erro ao alterar taxa: ${e.message}"
            }
        }
    }

    fun adminCreateBusiness(
        name: String,
        ownerEmail: String,
        activityType: String,
        city: String,
        currency: String = "MT",
        isPaid: Boolean = true,
        feeAmount: Double = 500.0,
        phone: String = ""
    ) {
        if (_currentUser.value?.role != "ADMIN") {
            _uiMessage.value = "Acesso Negado: Apenas o Administrador Geral pode registar negócios nesta secção."
            return
        }
        viewModelScope.launch {
            try {
                val b = BusinessEntity(
                    ownerId = UUID.randomUUID().toString(),
                    ownerEmail = ownerEmail.trim().lowercase(),
                    name = name.trim(),
                    activityType = activityType,
                    city = city.trim().ifBlank { "Maputo" },
                    currencySymbol = currency.trim().ifBlank { "MT" },
                    phone = phone.trim(),
                    isSubscriptionPaid = isPaid,
                    subscriptionAmount = feeAmount,
                    isActive = true
                )
                repository.adminCreateBusiness(b)
                _uiMessage.value = "Novo negócio '${b.name}' adicionado pelo Administrador na cidade de ${b.city}!"
            } catch (e: Exception) {
                _uiMessage.value = "Erro ao adicionar negócio: ${e.message}"
            }
        }
    }

    fun submitBusinessRequest(
        applicantName: String,
        phone: String,
        businessName: String,
        activityType: String,
        city: String,
        feePaid: Boolean,
        paymentReference: String,
        notes: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val desc = buildString {
                    appendLine("Solicitante: $applicantName")
                    appendLine("Contacto / WhatsApp: $phone")
                    appendLine("Negócio Solicitado: $businessName")
                    appendLine("Ramo de Atividade: $activityType")
                    appendLine("Cidade: ${city.ifBlank { "Maputo" }}")
                    appendLine("Taxa de Adesão: ${if (feePaid) "PAGA" else "PENDENTE"}")
                    if (paymentReference.isNotBlank()) {
                        appendLine("Referência / Comprovativo: $paymentReference")
                    }
                    if (notes.isNotBlank()) {
                        appendLine("Observações: $notes")
                    }
                }
                repository.submitFeedback(
                    userId = _currentUser.value?.id ?: "guest-${UUID.randomUUID().toString().take(6)}",
                    userEmail = _currentUser.value?.email ?: phone,
                    businessName = businessName.trim(),
                    type = "BUSINESS_REQUEST",
                    rating = 5,
                    category = "Novo Negócio",
                    title = "Pedido de Novo Negócio: $businessName ($city)",
                    description = desc
                )
                _uiMessage.value = "Pedido de novo negócio enviado ao Administrador! Aguarde a ativação."
                onSuccess()
            } catch (e: Exception) {
                _uiMessage.value = "Erro ao enviar pedido de novo negócio: ${e.message}"
            }
        }
    }

    // =========================================================================
    // MÓDULO DE GESTÃO DE SERVIÇOS & CATÁLOGO (Requisitos 1, 2, 3, 4, 5, 7)
    // =========================================================================

    fun registerService(
        serviceCatalogId: String = "",
        title: String,
        category: String = "Geral",
        customerName: String,
        customerPhone: String = "",
        description: String = "",
        status: String = "AGENDADO",
        scheduledDate: String = "",
        scheduledTime: String = "",
        completedDate: String = "",
        location: String = "",
        agreedPrice: Double,
        initialPayment: Double = 0.0,
        estimatedExpenses: Double = 0.0,
        paymentMethod: String = "CASH",
        notes: String = "",
        targetEquipmentOrVehicle: String = "",
        isPastService: Boolean = false,
        onSuccess: () -> Unit = {}
    ) {
        val b = _activeBusiness.value ?: return
        viewModelScope.launch {
            try {
                repository.registerService(
                    businessId = b.id,
                    serviceCatalogId = serviceCatalogId,
                    title = title,
                    category = category,
                    customerName = customerName,
                    customerPhone = customerPhone,
                    description = description,
                    status = status,
                    scheduledDate = scheduledDate,
                    scheduledTime = scheduledTime,
                    completedDate = completedDate,
                    location = location,
                    agreedPrice = agreedPrice,
                    initialPayment = initialPayment,
                    estimatedExpenses = estimatedExpenses,
                    paymentMethod = paymentMethod,
                    notes = notes,
                    targetEquipmentOrVehicle = targetEquipmentOrVehicle,
                    isPastService = isPastService
                )
                _uiMessage.value = if (isPastService) {
                    "Serviço anterior '$title' registado com sucesso no histórico!"
                } else {
                    "Serviço '$title' registado com sucesso para $customerName!"
                }
                refreshDreReport()
                onSuccess()
            } catch (e: Exception) {
                _uiMessage.value = "Erro ao registar serviço: ${e.message}"
            }
        }
    }

    fun updateServiceStatus(serviceId: String, newStatus: String, completedDate: String = "") {
        viewModelScope.launch {
            try {
                repository.updateServiceStatus(serviceId, newStatus, completedDate)
                _uiMessage.value = "Estado do serviço atualizado para '$newStatus'!"
                refreshDreReport()
            } catch (e: Exception) {
                _uiMessage.value = "Erro ao atualizar estado: ${e.message}"
            }
        }
    }

    fun addServicePayment(
        serviceId: String,
        amount: Double,
        paymentMethod: String = "CASH",
        notes: String = "",
        onSuccess: () -> Unit = {}
    ) {
        val b = _activeBusiness.value ?: return
        viewModelScope.launch {
            try {
                repository.addServicePayment(serviceId, amount, paymentMethod, notes)
                _uiMessage.value = "Pagamento de $amount ${b.currencySymbol} registado com sucesso no caixa!"
                refreshDreReport()
                onSuccess()
            } catch (e: Exception) {
                _uiMessage.value = "Erro ao registar pagamento: ${e.message}"
            }
        }
    }

    fun addServiceExpense(
        serviceId: String,
        description: String,
        amount: Double,
        isPaidNow: Boolean = true,
        paymentMethod: String = "CASH",
        notes: String = "",
        onSuccess: () -> Unit = {}
    ) {
        val b = _activeBusiness.value ?: return
        viewModelScope.launch {
            try {
                repository.addServiceExpense(serviceId, description, amount, isPaidNow, paymentMethod, notes)
                _uiMessage.value = "Despesa de $amount ${b.currencySymbol} associada ao serviço!"
                refreshDreReport()
                onSuccess()
            } catch (e: Exception) {
                _uiMessage.value = "Erro ao associar despesa: ${e.message}"
            }
        }
    }

    suspend fun getPaymentsForService(serviceId: String): List<ServicePaymentEntity> {
        return repository.getPaymentsForService(serviceId)
    }

    suspend fun getExpensesForService(serviceId: String): List<ServiceExpenseEntity> {
        return repository.getExpensesForService(serviceId)
    }

    fun deleteService(service: ServiceEntity) {
        viewModelScope.launch {
            try {
                repository.deleteService(service)
                _uiMessage.value = "Serviço '${service.title}' removido."
                refreshDreReport()
            } catch (e: Exception) {
                _uiMessage.value = "Erro ao remover serviço: ${e.message}"
            }
        }
    }

    // Catálogo de Serviços Habitual
    fun addServiceCatalogItem(
        name: String,
        category: String,
        standardPrice: Double,
        estimatedDurationMinutes: Int = 60,
        description: String = "",
        notes: String = ""
    ) {
        val b = _activeBusiness.value ?: return
        viewModelScope.launch {
            try {
                repository.addServiceCatalogItem(
                    ServiceCatalogEntity(
                        businessId = b.id,
                        name = name.trim(),
                        category = category.trim().ifBlank { "Geral" },
                        standardPrice = standardPrice,
                        estimatedDurationMinutes = estimatedDurationMinutes,
                        description = description.trim(),
                        notes = notes.trim()
                    )
                )
                _uiMessage.value = "Serviço '$name' adicionado ao catálogo com sucesso!"
            } catch (e: Exception) {
                _uiMessage.value = "Erro ao adicionar ao catálogo: ${e.message}"
            }
        }
    }

    fun updateServiceCatalogItem(item: ServiceCatalogEntity) {
        viewModelScope.launch {
            try {
                repository.updateServiceCatalogItem(item)
                _uiMessage.value = "Catálogo de serviço atualizado!"
            } catch (e: Exception) {
                _uiMessage.value = "Erro ao atualizar catálogo: ${e.message}"
            }
        }
    }

    fun deleteServiceCatalogItem(item: ServiceCatalogEntity) {
        viewModelScope.launch {
            try {
                repository.deleteServiceCatalogItem(item)
                _uiMessage.value = "Item removido do catálogo."
            } catch (e: Exception) {
                _uiMessage.value = "Erro ao remover do catálogo: ${e.message}"
            }
        }
    }
}
