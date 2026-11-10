package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.util.PhoneUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

data class DreReportData(
    val grossRevenue: Double,
    val costOfGoodsSold: Double,
    val grossProfit: Double,
    val grossMarginPercentage: Double,
    val operationalExpenses: Double,
    val netProfit: Double,
    val totalCashReceived: Double,
    val totalCashPaid: Double,
    val currentCashBalance: Double,
    val pendingReceivables: Double,
    val pendingPayables: Double,
    val salesCount: Int,
    val topSellingProducts: List<TopProductSummary>
)

data class TopProductSummary(
    val productId: String,
    val productName: String,
    val totalQuantity: Double,
    val totalRevenue: Double
)

class AppRepository(val database: AppDatabase) {
    val userDao = database.userDao()
    val businessDao = database.businessDao()
    val categoryDao = database.categoryDao()
    val productDao = database.productDao()
    val saleDao = database.saleDao()
    val financeDao = database.financeDao()
    val feedbackDao = database.feedbackDao()
    val reservationDao = database.reservationDao()

    // Authentication
    suspend fun authenticate(identifier: String, passwordHash: String): UserEntity? {
        val trimmed = identifier.trim()
        val normalizedPhone = PhoneUtils.normalizeMozambiquePhone(trimmed)
        val cleanDigits = trimmed.filter { it.isDigit() }

        val user = userDao.getUserByEmail(trimmed.lowercase())
            ?: (if (normalizedPhone != null) userDao.getUserByPhone(normalizedPhone) else null)
            ?: userDao.getUserByPhone(trimmed)
            ?: (if (cleanDigits.length >= 8) userDao.getUserByPhone("+258$cleanDigits") else null)
            ?: userDao.getUserByPhone(trimmed.replace(" ", "").replace("-", ""))
            ?: return null
        return if (user.passwordHash == passwordHash) user else null
    }

    suspend fun registerUser(
        fullName: String,
        emailOrPhone: String,
        passwordHash: String,
        phoneInput: String,
        isPhoneVerified: Boolean = false
    ): UserEntity {
        val trimmedInput = emailOrPhone.trim()
        val isInputPhone = !trimmedInput.contains("@") && trimmedInput.any { it.isDigit() }

        val rawPhone = if (phoneInput.isNotBlank()) phoneInput.trim() else if (isInputPhone) trimmedInput else ""
        val normalizedPhone = PhoneUtils.normalizeMozambiquePhone(rawPhone) ?: rawPhone.replace(" ", "").replace("-", "")

        val cleanEmail = if (trimmedInput.contains("@")) {
            trimmedInput.lowercase()
        } else if (normalizedPhone.isNotBlank()) {
            "${normalizedPhone.replace("+", "")}@telefone.negocionapratica.com"
        } else {
            trimmedInput.lowercase()
        }

        // Verificação estrita de duplicados por email
        val existingByEmail = userDao.getUserByEmail(cleanEmail)
        if (existingByEmail != null) {
            throw IllegalArgumentException("Já existe uma conta registada com este endereço de e-mail.")
        }

        // Verificação estrita de duplicados por telefone normalizado
        if (normalizedPhone.isNotBlank()) {
            val existingByPhone = userDao.getUserByPhone(normalizedPhone)
                ?: userDao.getUserByPhone(rawPhone)
                ?: userDao.getUserByPhone(rawPhone.replace(" ", "").replace("-", ""))
            if (existingByPhone != null) {
                val formatted = PhoneUtils.formatDisplay(normalizedPhone)
                throw IllegalArgumentException("O número de telefone $formatted já se encontra associado a uma conta existente. Por favor, inicie sessão ou recupere o seu acesso.")
            }
        }

        val newUser = UserEntity(
            email = cleanEmail,
            fullName = fullName.trim(),
            passwordHash = passwordHash,
            phone = normalizedPhone,
            isPhoneVerified = isPhoneVerified,
            role = "OWNER"
        )
        userDao.insertUser(newUser)
        return newUser
    }

    suspend fun markPhoneVerified(userId: String) {
        userDao.updatePhoneVerification(userId, true)
    }

    suspend fun resetPassword(identifier: String, newPasswordHash: String): Boolean {
        val trimmed = identifier.trim()
        val normalizedPhone = PhoneUtils.normalizeMozambiquePhone(trimmed)
        val user = userDao.getUserByEmail(trimmed.lowercase())
            ?: (if (normalizedPhone != null) userDao.getUserByPhone(normalizedPhone) else null)
            ?: userDao.getUserByPhone(trimmed)
            ?: userDao.getUserByPhone(trimmed.replace(" ", "").replace("-", ""))
            ?: return false
        val updated = user.copy(passwordHash = newPasswordHash)
        userDao.insertUser(updated)
        return true
    }

    // Business
    fun getBusinessesForOwner(ownerId: String): Flow<List<BusinessEntity>> {
        return businessDao.getBusinessesForOwner(ownerId)
    }

    suspend fun createBusiness(
        ownerId: String,
        name: String,
        activityType: String,
        currencySymbol: String = "MT",
        defaultUnit: String = "un",
        phone: String = "",
        address: String = "",
        city: String = "Maputo",
        country: String = "Moçambique",
        province: String = "Maputo Cidade",
        neighborhood: String = "",
        referencePoint: String = "",
        latitude: Double? = null,
        longitude: Double? = null,
        taxNumber: String = "",
        isSubscriptionPaid: Boolean = true,
        subscriptionAmount: Double = 500.0,
        isActive: Boolean = true,
        ownerEmail: String = ""
    ): BusinessEntity {
        val business = BusinessEntity(
            ownerId = ownerId,
            name = name.trim(),
            activityType = activityType,
            currencySymbol = currencySymbol,
            defaultUnit = defaultUnit,
            phone = phone.trim(),
            address = address.trim(),
            city = if (city.isNotBlank()) city.trim() else "Maputo",
            country = if (country.isNotBlank()) country.trim() else "Moçambique",
            province = if (province.isNotBlank()) province.trim() else "Maputo Cidade",
            neighborhood = neighborhood.trim(),
            referencePoint = referencePoint.trim(),
            latitude = latitude,
            longitude = longitude,
            taxNumber = taxNumber.trim(),
            isSubscriptionPaid = isSubscriptionPaid,
            subscriptionAmount = subscriptionAmount,
            isActive = isActive,
            ownerEmail = ownerEmail.trim()
        )
        businessDao.insertBusiness(business)

        // Criar categorias padrão inteligentes adaptadas aos 17 tipos de negócio
        val categories = when {
            activityType.contains("Mercearia", true) || activityType.contains("Mini mercado", true) ->
                listOf("Alimentos Básicos (kg/un)", "Bebidas & Refrigerantes", "Limpeza & Higiene", "Enlatados & Cereais", "Hortícolas")
            activityType.contains("Lanchonete", true) ->
                listOf("Lanches & Salgados", "Bebidas & Sumos", "Doces & Bolos", "Cafetaria", "Combos")
            activityType.contains("Restaurante", true) ->
                listOf("Pratos Principais", "Acompanhamentos", "Bebidas & Vinhos", "Sobremesas", "Entradas")
            activityType.contains("Venda de roupa", true) ->
                listOf("Moda Masculina", "Moda Feminina", "Infantil", "Acessórios", "Promoções")
            activityType.contains("Loja de calçado", true) ->
                listOf("Sapatilhas", "Sapatos Sociais", "Chinelos & Sandálias", "Calçado Infantil", "Meias & Acessórios")
            activityType.contains("Salão de beleza", true) ->
                listOf("Penteados & Tranças", "Corte & Escova", "Manicure & Pedicure", "Tratamentos Faciais", "Produtos Capilares")
            activityType.contains("Barbearia", true) ->
                listOf("Corte Masculino", "Barba & Bigode", "Corte Infantil", "Pigmentação", "Produtos para Barba")
            activityType.contains("Loja de cosméticos", true) ->
                listOf("Perfumaria", "Maquilhagem", "Cuidados com a Pele", "Cabelo", "Higiene Pessoal")
            activityType.contains("Loja de electrónica", true) ->
                listOf("Smartphones", "Acessórios & Cabos", "Áudio & Som", "Computadores", "Eletrodomésticos")
            activityType.contains("Papelaria", true) ->
                listOf("Material Escolar", "Material de Escritório", "Cópias & Impressões", "Livros & Cadernos", "Artesanato")
            activityType.contains("Bar", true) ->
                listOf("Cervejas & Sidras", "Bebidas Espirituosas", "Coquetéis", "Petiscos", "Refrigerantes")
            activityType.contains("Padaria", true) ->
                listOf("Pães Frescos (kg/un)", "Bolos & Tortas", "Pastelaria", "Leite & Laticínios", "Farinhas & Fermentos")
            activityType.contains("Talho", true) ->
                listOf("Carne de Vaca (kg)", "Frango & Aves (kg)", "Carne de Porco (kg)", "Peixe & Mariscos (kg)", "Enchidos & Miudezas")
            activityType.contains("Loja de bebidas", true) ->
                listOf("Cervejas Nacionais", "Vinhos Importados", "Whisky & Licores", "Sumos & Águas", "Embalagens Fechadas")
            activityType.contains("Prestação de serviços", true) ->
                listOf("Mão de Obra", "Consultoria / Diagnóstico", "Manutenção", "Instalação", "Serviços Avulsos")
            else -> listOf("Produtos Gerais", "Bebidas", "Serviços", "Diversos")
        }
        for (cat in categories) {
            categoryDao.insertCategory(CategoryEntity(businessId = business.id, name = cat))
        }

        return business
    }

    fun getAllBusinesses(): Flow<List<BusinessEntity>> =
        businessDao.getAllBusinessesFlow()

    suspend fun setBusinessActiveStatus(businessId: String, isActive: Boolean) {
        val b = businessDao.getBusinessById(businessId) ?: return
        businessDao.updateBusiness(b.copy(isActive = isActive))
    }

    suspend fun setBusinessSubscriptionPaid(businessId: String, isPaid: Boolean, amount: Double) {
        val b = businessDao.getBusinessById(businessId) ?: return
        businessDao.updateBusiness(b.copy(isSubscriptionPaid = isPaid, subscriptionAmount = amount))
    }

    suspend fun updateBusiness(business: BusinessEntity) {
        businessDao.updateBusiness(business)
    }

    // Products & Stock
    fun getProducts(businessId: String): Flow<List<ProductEntity>> =
        productDao.getProductsForBusiness(businessId)

    fun getCategories(businessId: String): Flow<List<CategoryEntity>> =
        categoryDao.getCategoriesForBusiness(businessId)

    fun getStockMovements(businessId: String): Flow<List<StockMovementEntity>> =
        productDao.getStockMovementsForBusiness(businessId)

    suspend fun addProduct(product: ProductEntity) {
        productDao.insertProduct(product)
        if (product.currentStock > 0) {
            productDao.insertStockMovement(
                StockMovementEntity(
                    businessId = product.businessId,
                    productId = product.id,
                    productName = product.name,
                    type = "PURCHASE",
                    quantity = product.currentStock,
                    previousStock = 0.0,
                    newStock = product.currentStock,
                    unitCost = product.purchasePrice,
                    reason = "Stock inicial registado"
                )
            )
        }

        // Automatic Supplier Debt Registration if merchandise was not paid or has debt
        if (!product.isPaidToSupplier && product.supplierDebtAmount > 0) {
            val supplier = if (product.supplierName.isNotBlank()) product.supplierName else "Fornecedor da Mercadoria"
            financeDao.insertDebt(
                DebtEntity(
                    businessId = product.businessId,
                    type = "PAYABLE",
                    partyName = supplier,
                    initialAmount = product.supplierDebtAmount,
                    remainingAmount = product.supplierDebtAmount,
                    referenceId = product.id,
                    status = "PENDING"
                )
            )
            financeDao.insertTransaction(
                FinancialTransactionEntity(
                    businessId = product.businessId,
                    type = "EXPENSE_PURCHASE",
                    category = "Fornecedores",
                    amount = product.supplierDebtAmount,
                    description = "Dívida registada de mercadoria: ${product.name} ($supplier)",
                    referenceId = product.id,
                    paymentMethod = "A_PRAZO"
                )
            )
        }
    }

    suspend fun updateProduct(product: ProductEntity) {
        productDao.updateProduct(product)
    }

    suspend fun registerStockEntry(
        businessId: String,
        productId: String,
        quantity: Double,
        unitCost: Double,
        supplierName: String,
        paidImmediately: Boolean
    ) {
        val product = productDao.getProductById(productId) ?: return
        val prev = product.currentStock
        val next = prev + quantity

        // Update purchase price if provided
        val updatedProduct = product.copy(
            currentStock = next,
            purchasePrice = if (unitCost > 0) unitCost else product.purchasePrice
        )
        productDao.updateProduct(updatedProduct)

        // Record stock movement
        productDao.insertStockMovement(
            StockMovementEntity(
                businessId = businessId,
                productId = productId,
                productName = product.name,
                type = "PURCHASE",
                quantity = quantity,
                previousStock = prev,
                newStock = next,
                unitCost = unitCost,
                reason = "Compra de mercadoria ($supplierName)"
            )
        )

        val totalCost = quantity * (if (unitCost > 0) unitCost else product.purchasePrice)
        if (paidImmediately) {
            financeDao.insertTransaction(
                FinancialTransactionEntity(
                    businessId = businessId,
                    type = "EXPENSE_PURCHASE",
                    category = "Fornecedores",
                    amount = totalCost,
                    description = "Compra de ${product.name} (Qt: $quantity) - $supplierName"
                )
            )
        } else {
            financeDao.insertDebt(
                DebtEntity(
                    businessId = businessId,
                    type = "PAYABLE",
                    partyName = supplierName.ifBlank { "Fornecedor de ${product.name}" },
                    initialAmount = totalCost,
                    remainingAmount = totalCost,
                    status = "PENDING",
                    referenceId = productId
                )
            )
        }
    }

    suspend fun registerStockAdjustment(
        businessId: String,
        productId: String,
        quantityDiff: Double,
        reason: String
    ) {
        val product = productDao.getProductById(productId) ?: return
        val prev = product.currentStock
        val next = (prev + quantityDiff).coerceAtLeast(0.0)

        productDao.updateProduct(product.copy(currentStock = next))
        productDao.insertStockMovement(
            StockMovementEntity(
                businessId = businessId,
                productId = productId,
                productName = product.name,
                type = if (quantityDiff < 0) "ADJUSTMENT_LOSS" else "ADJUSTMENT_GAIN",
                quantity = kotlin.math.abs(quantityDiff),
                previousStock = prev,
                newStock = next,
                unitCost = product.purchasePrice,
                reason = reason.ifBlank { "Ajuste manual de inventário" }
            )
        )
    }

    // Sales & Point of Sale (POS)
    fun getSales(businessId: String): Flow<List<SaleEntity>> =
        saleDao.getSalesForBusiness(businessId)

    suspend fun registerSale(
        businessId: String,
        customerName: String,
        items: List<CartItem>,
        discount: Double,
        paymentStatus: String, // "PAID" or "CREDIT"
        paymentMethod: String,
        amountPaid: Double,
        dueDate: Long?,
        notes: String
    ): SaleEntity {
        val totalGross = items.sumOf { it.quantity * it.product.sellingPrice }
        val totalNet = (totalGross - discount).coerceAtLeast(0.0)
        val finalAmountPaid = if (paymentStatus == "PAID") totalNet else amountPaid.coerceAtMost(totalNet)
        val amountDue = (totalNet - finalAmountPaid).coerceAtLeast(0.0)

        val sale = SaleEntity(
            businessId = businessId,
            customerName = customerName.ifBlank { "Cliente Geral" },
            totalAmount = totalNet,
            discount = discount,
            paymentStatus = paymentStatus,
            paymentMethod = paymentMethod,
            amountPaid = finalAmountPaid,
            amountDue = amountDue,
            dueDate = dueDate,
            notes = notes
        )
        saleDao.insertSale(sale)

        val saleItems = items.map { item ->
            val isWeight = com.example.util.BusinessProfileUtils.isWeightUnit(item.product.unit) || item.product.isSoldByWeight
            val qtyDisplay = if (item.customUnitDisplay.isNotBlank()) {
                item.customUnitDisplay
            } else if (isWeight) {
                com.example.util.BusinessProfileUtils.formatWeightDisplay(item.quantity)
            } else {
                "${if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()} ${item.product.unit}"
            }
            SaleItemEntity(
                saleId = sale.id,
                productId = item.product.id,
                productName = item.product.name,
                quantity = item.quantity,
                unitPrice = item.product.sellingPrice,
                unitCost = item.product.purchasePrice,
                subtotal = item.quantity * item.product.sellingPrice,
                unit = item.product.unit,
                originalQuantityDisplay = qtyDisplay
            )
        }
        saleDao.insertSaleItems(saleItems)

        // Atomically decrement stock & record movements
        for (item in items) {
            val prev = item.product.currentStock
            val next = (prev - item.quantity).coerceAtLeast(0.0)
            productDao.adjustProductStock(item.product.id, -item.quantity)
            productDao.insertStockMovement(
                StockMovementEntity(
                    businessId = businessId,
                    productId = item.product.id,
                    productName = item.product.name,
                    type = "SALE",
                    quantity = item.quantity,
                    previousStock = prev,
                    newStock = next,
                    unitCost = item.product.purchasePrice,
                    reason = "Venda #${sale.id.takeLast(6)}"
                )
            )
        }

        // Financial cash inflow
        if (finalAmountPaid > 0) {
            financeDao.insertTransaction(
                FinancialTransactionEntity(
                    businessId = businessId,
                    type = "INCOME_SALE",
                    category = "Vendas",
                    amount = finalAmountPaid,
                    description = "Venda a ${sale.customerName}" + if (paymentStatus == "CREDIT") " (Entrada parcial)" else "",
                    referenceId = sale.id,
                    paymentMethod = paymentMethod
                )
            )
        }

        // If credit sale with balance due, create receivable debt
        if (amountDue > 0) {
            financeDao.insertDebt(
                DebtEntity(
                    businessId = businessId,
                    type = "RECEIVABLE",
                    partyName = sale.customerName,
                    initialAmount = amountDue,
                    remainingAmount = amountDue,
                    dueDate = dueDate,
                    status = "PENDING",
                    referenceId = sale.id
                )
            )
        }

        return sale
    }

    suspend fun cancelSale(saleId: String) {
        val sale = saleDao.getSaleById(saleId) ?: return
        if (sale.isCancelled) return

        saleDao.updateSale(sale.copy(isCancelled = true))
        val items = saleDao.getItemsForSale(saleId)

        // Restore stock
        for (item in items) {
            productDao.adjustProductStock(item.productId, item.quantity)
            val product = productDao.getProductById(item.productId)
            val current = product?.currentStock ?: item.quantity
            productDao.insertStockMovement(
                StockMovementEntity(
                    businessId = sale.businessId,
                    productId = item.productId,
                    productName = item.productName,
                    type = "RETURN",
                    quantity = item.quantity,
                    previousStock = (current - item.quantity).coerceAtLeast(0.0),
                    newStock = current,
                    unitCost = item.unitCost,
                    reason = "Anulação da Venda #${sale.id.takeLast(6)}"
                )
            )
        }

        // Reverse cash if it was paid
        if (sale.amountPaid > 0) {
            financeDao.insertTransaction(
                FinancialTransactionEntity(
                    businessId = sale.businessId,
                    type = "EXPENSE_OPERATIONAL",
                    category = "Estorno de Venda",
                    amount = sale.amountPaid,
                    description = "Reembolso por anulação de venda #${sale.id.takeLast(6)}",
                    referenceId = sale.id
                )
            )
        }
    }

    suspend fun processPartialReturn(
        saleId: String,
        returnedQuantities: Map<String, Double>, // saleItemId to quantityToReturn
        reason: String
    ) {
        val sale = saleDao.getSaleById(saleId) ?: return
        if (sale.isCancelled) return

        val items = saleDao.getItemsForSale(saleId)
        var totalRefundAmount = 0.0
        var allItemsFullyReturned = true

        for (item in items) {
            val returnQty = returnedQuantities[item.id] ?: 0.0
            if (returnQty > 0) {
                val qtyToReturn = returnQty.coerceAtMost(item.quantity)
                val itemRefund = qtyToReturn * item.unitPrice
                totalRefundAmount += itemRefund

                // Restore stock for this returned quantity
                productDao.adjustProductStock(item.productId, qtyToReturn)
                val product = productDao.getProductById(item.productId)
                val current = product?.currentStock ?: qtyToReturn

                productDao.insertStockMovement(
                    StockMovementEntity(
                        businessId = sale.businessId,
                        productId = item.productId,
                        productName = item.productName,
                        type = "RETURN",
                        quantity = qtyToReturn,
                        previousStock = (current - qtyToReturn).coerceAtLeast(0.0),
                        newStock = current,
                        unitCost = item.unitCost,
                        reason = if (reason.isNotBlank()) reason else "Devolução parcial da Venda #${sale.id.takeLast(6)}"
                    )
                )

                if (qtyToReturn < item.quantity) {
                    allItemsFullyReturned = false
                }
            } else {
                allItemsFullyReturned = false
            }
        }

        if (totalRefundAmount > 0) {
            val newReturnedTotal = sale.returnedAmount + totalRefundAmount
            val isFullCancel = allItemsFullyReturned || newReturnedTotal >= (sale.totalAmount - sale.discount)

            val updatedSale = sale.copy(
                returnedAmount = newReturnedTotal,
                returnStatus = if (isFullCancel) "FULL" else "PARTIAL",
                isCancelled = isFullCancel
            )
            saleDao.updateSale(updatedSale)

            // Register financial refund expense
            financeDao.insertTransaction(
                FinancialTransactionEntity(
                    businessId = sale.businessId,
                    type = "EXPENSE_OPERATIONAL",
                    category = "Estorno de Venda",
                    amount = totalRefundAmount,
                    description = "Reembolso por devolução ${if (isFullCancel) "total" else "parcial"} da venda #${sale.id.takeLast(6)}",
                    referenceId = sale.id
                )
            )
        }
    }

    suspend fun getItemsForSale(saleId: String): List<SaleItemEntity> =
        saleDao.getItemsForSale(saleId)

    suspend fun adminCreateBusiness(business: BusinessEntity) {
        businessDao.insertBusiness(business)
    }

    // Reservations (Restaurantes, Salões, etc.)
    fun getReservations(businessId: String): Flow<List<ReservationEntity>> =
        reservationDao.getReservationsForBusiness(businessId)

    suspend fun createReservation(reservation: ReservationEntity) {
        reservationDao.insertReservation(reservation)
    }

    suspend fun updateReservationStatus(id: String, newStatus: String) {
        val res = reservationDao.getReservationById(id) ?: return
        reservationDao.updateReservation(res.copy(status = newStatus))
    }

    suspend fun deleteReservation(reservation: ReservationEntity) {
        reservationDao.deleteReservation(reservation)
    }

    // Finances & Debts
    fun getTransactions(businessId: String): Flow<List<FinancialTransactionEntity>> =
        financeDao.getTransactionsForBusiness(businessId)

    fun getDebts(businessId: String): Flow<List<DebtEntity>> =
        financeDao.getDebtsForBusiness(businessId)

    suspend fun addOperationalExpense(
        businessId: String,
        category: String,
        amount: Double,
        description: String,
        paymentMethod: String
    ) {
        financeDao.insertTransaction(
            FinancialTransactionEntity(
                businessId = businessId,
                type = "EXPENSE_OPERATIONAL",
                category = category,
                amount = amount,
                description = description.ifBlank { "Despesa ($category)" },
                paymentMethod = paymentMethod
            )
        )
    }

    suspend fun addProLabore(businessId: String, amount: Double, notes: String) {
        financeDao.insertTransaction(
            FinancialTransactionEntity(
                businessId = businessId,
                type = "WITHDRAWAL_PRO_LABORE",
                category = "Pró-Labore",
                amount = amount,
                description = notes.ifBlank { "Retirada pessoal do proprietário" }
            )
        )
    }

    suspend fun createDebt(debt: DebtEntity) {
        financeDao.insertDebt(debt)
    }

    suspend fun deleteDebt(debt: DebtEntity) {
        financeDao.deleteDebt(debt)
    }

    fun getPaymentsForDebt(debtId: String): Flow<List<DebtPaymentEntity>> =
        financeDao.getPaymentsForDebtFlow(debtId)

    suspend fun getPaymentsForDebtDirect(debtId: String): List<DebtPaymentEntity> =
        financeDao.getPaymentsForDebtDirect(debtId)

    fun getAllDebtPayments(businessId: String): Flow<List<DebtPaymentEntity>> =
        financeDao.getPaymentsForBusinessFlow(businessId)

    suspend fun payDebt(
        debtId: String,
        amountPaid: Double,
        notes: String = "",
        paymentDate: Long = System.currentTimeMillis()
    ) {
        val debt = financeDao.getDebtById(debtId) ?: return
        val newRemaining = (debt.remainingAmount - amountPaid).coerceAtLeast(0.0)
        val newStatus = if (newRemaining <= 0.0) "PAID" else "PARTIAL"

        financeDao.updateDebt(debt.copy(remainingAmount = newRemaining, status = newStatus))

        // Grava registo individual no histórico de pagamentos de dívida
        financeDao.insertDebtPayment(
            DebtPaymentEntity(
                debtId = debt.id,
                businessId = debt.businessId,
                amount = amountPaid,
                paymentDate = paymentDate,
                notes = notes,
                timestamp = paymentDate
            )
        )

        // Integração financeira com caixa real:
        // Clientes a pagar -> entrada de caixa (INCOME_DEBT_PAYMENT)
        // Negócio a pagar a fornecedor -> saída de caixa (EXPENSE_PURCHASE)
        if (debt.type == "RECEIVABLE") {
            financeDao.insertTransaction(
                FinancialTransactionEntity(
                    businessId = debt.businessId,
                    type = "INCOME_DEBT_PAYMENT",
                    category = "Recebimento de Dívida",
                    amount = amountPaid,
                    description = "Recebimento de dívida: ${debt.partyName}" + if (notes.isNotBlank()) " ($notes)" else "",
                    referenceId = debt.id,
                    timestamp = paymentDate
                )
            )
        } else {
            financeDao.insertTransaction(
                FinancialTransactionEntity(
                    businessId = debt.businessId,
                    type = "EXPENSE_PURCHASE",
                    category = "Fornecedores",
                    amount = amountPaid,
                    description = "Pagamento a fornecedor: ${debt.partyName}" + if (notes.isNotBlank()) " ($notes)" else "",
                    referenceId = debt.id,
                    timestamp = paymentDate
                )
            )
        }
    }

    // Reports & DRE Calculations
    suspend fun calculateDre(businessId: String, startTime: Long, endTime: Long): DreReportData {
        val allSales = saleDao.getSalesForBusiness(businessId).first()
            .filter { !it.isCancelled && it.timestamp in startTime..endTime }
        val allTransactions = financeDao.getTransactionsListForBusiness(businessId)
            .filter { it.timestamp in startTime..endTime }
        val allBusinessTransactions = financeDao.getTransactionsListForBusiness(businessId)
        val allDebts = financeDao.getDebtsForBusiness(businessId).first()

        val grossRevenue = allSales.sumOf { it.totalAmount }

        // Real Cost of Goods Sold (CMV) computed from actual items sold
        val saleIds = allSales.map { it.id }.toSet()
        val allSaleItems = saleDao.getAllSaleItems().filter { it.saleId in saleIds }
        val costOfGoodsSold = allSaleItems.sumOf { it.quantity * it.unitCost }
        val grossProfit = grossRevenue - costOfGoodsSold
        val grossMarginPercentage = if (grossRevenue > 0) (grossProfit / grossRevenue) * 100.0 else 0.0

        val operationalExpenses = allTransactions
            .filter { it.type == "EXPENSE_OPERATIONAL" || it.type == "WITHDRAWAL_PRO_LABORE" }
            .sumOf { it.amount }

        val netProfit = grossProfit - operationalExpenses

        // Cash flow in the period
        val cashIn = allTransactions
            .filter { it.type.startsWith("INCOME_") }
            .sumOf { it.amount }
        val cashOut = allTransactions
            .filter { it.type.startsWith("EXPENSE_") || it.type.startsWith("WITHDRAWAL_") }
            .sumOf { it.amount }

        // Lifetime Cash balance for the business
        val lifetimeIn = allBusinessTransactions
            .filter { it.type.startsWith("INCOME_") }
            .sumOf { it.amount }
        val lifetimeOut = allBusinessTransactions
            .filter { it.type.startsWith("EXPENSE_") || it.type.startsWith("WITHDRAWAL_") }
            .sumOf { it.amount }
        val currentCashBalance = lifetimeIn - lifetimeOut

        val pendingReceivables = allDebts
            .filter { it.type == "RECEIVABLE" && it.status != "PAID" }
            .sumOf { it.remainingAmount }
        val pendingPayables = allDebts
            .filter { it.type == "PAYABLE" && it.status != "PAID" }
            .sumOf { it.remainingAmount }

        // Top selling products summary
        val topProducts = allSaleItems
            .groupBy { it.productId }
            .map { (prodId, itemsList) ->
                TopProductSummary(
                    productId = prodId,
                    productName = itemsList.firstOrNull()?.productName ?: "Produto",
                    totalQuantity = itemsList.sumOf { it.quantity },
                    totalRevenue = itemsList.sumOf { it.subtotal }
                )
            }
            .sortedByDescending { it.totalRevenue }
            .take(6)

        return DreReportData(
            grossRevenue = grossRevenue,
            costOfGoodsSold = costOfGoodsSold,
            grossProfit = grossProfit,
            grossMarginPercentage = grossMarginPercentage,
            operationalExpenses = operationalExpenses,
            netProfit = netProfit,
            totalCashReceived = cashIn,
            totalCashPaid = cashOut,
            currentCashBalance = currentCashBalance,
            pendingReceivables = pendingReceivables,
            pendingPayables = pendingPayables,
            salesCount = allSales.size,
            topSellingProducts = topProducts
        )
    }

    // Feedback & Platform Admin
    fun getAllFeedbacks(): Flow<List<FeedbackEntity>> = feedbackDao.getAllFeedbacksFlow()

    fun getFeedbacksForUser(userId: String): Flow<List<FeedbackEntity>> =
        feedbackDao.getFeedbacksForUserFlow(userId)

    suspend fun submitFeedback(
        userId: String,
        userEmail: String,
        businessName: String,
        type: String,
        rating: Int,
        category: String,
        title: String,
        description: String
    ) {
        feedbackDao.insertFeedback(
            FeedbackEntity(
                userId = userId,
                userEmail = userEmail,
                businessName = businessName,
                type = type,
                rating = rating,
                category = category,
                title = title.trim(),
                description = description.trim()
            )
        )
    }

    suspend fun updateFeedbackStatus(feedbackId: String, newStatus: String, adminNotes: String) {
        val feedbacks = feedbackDao.getAllFeedbacksFlow().first()
        val target = feedbacks.firstOrNull { it.id == feedbackId } ?: return
        feedbackDao.updateFeedback(
            target.copy(
                status = newStatus,
                adminNotes = adminNotes.trim(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}

data class CartItem(
    val product: ProductEntity,
    var quantity: Double = 1.0,
    var customUnitDisplay: String = ""
)
