package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val email: String,
    val fullName: String,
    val passwordHash: String,
    val phone: String = "",
    val isPhoneVerified: Boolean = false,
    val role: String = "OWNER", // "OWNER" or "ADMIN"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "businesses")
data class BusinessEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val ownerId: String,
    val name: String,
    val activityType: String = "Mercearia / Mini mercado", // Mercearia / Mini mercado, Lanchonete, Restaurante, Venda de roupa, etc.
    val currencySymbol: String = "MT", // MT (Metical de Moçambique), Kz, R$, €, $
    val defaultUnit: String = "un", // un, kg, litro, metro, caixa
    val phone: String = "",
    val address: String = "",
    val city: String = "Maputo", // Cidade (ex: Maputo, Matola, Beira, Nampula)
    val country: String = "Moçambique",
    val province: String = "Maputo Cidade",
    val neighborhood: String = "", // Bairro
    val referencePoint: String = "", // Ponto de referência opcional
    val latitude: Double? = null,
    val longitude: Double? = null,
    val taxNumber: String = "",
    val isSubscriptionPaid: Boolean = true, // Se pagou a taxa de adesão/mensalidade
    val subscriptionAmount: Double = 500.0, // Valor da taxa paga
    val isActive: Boolean = true, // Se o negócio está ativo ou desativado pelo administrador
    val ownerEmail: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val businessId: String,
    val name: String,
    val iconName: String = "Category"
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val businessId: String,
    val categoryId: String = "",
    val name: String,
    val sku: String = "",
    val purchasePrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val currentStock: Double = 0.0,
    val minStockAlert: Double = 5.0,
    val unit: String = "un",
    val sizes: String = "", // e.g. "S, M, L, XL" for clothing shops
    val colors: String = "",
    val isPaidToSupplier: Boolean = true, // Se a mercadoria já foi paga ao fornecedor
    val supplierDebtAmount: Double = 0.0, // Valor em dívida / não pago ao fornecedor
    val supplierName: String = "", // Nome ou contacto do fornecedor
    val isSoldByWeight: Boolean = false, // Se o produto é vendido por peso (kg / g)
    val weightUnit: String = "kg", // "kg", "g", "L", "ml"
    val isActive: Boolean = true
)

@Entity(tableName = "stock_movements")
data class StockMovementEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val businessId: String,
    val productId: String,
    val productName: String,
    val type: String, // "PURCHASE", "SALE", "ADJUSTMENT_LOSS", "ADJUSTMENT_GAIN", "RETURN"
    val quantity: Double,
    val previousStock: Double,
    val newStock: Double,
    val unitCost: Double = 0.0,
    val reason: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val businessId: String,
    val customerName: String = "Cliente Geral",
    val totalAmount: Double,
    val discount: Double = 0.0,
    val paymentStatus: String = "PAID", // "PAID", "CREDIT"
    val paymentMethod: String = "CASH", // "CASH", "MULTICAIXA_CARD", "TRANSFER"
    val amountPaid: Double = 0.0,
    val amountDue: Double = 0.0,
    val dueDate: Long? = null,
    val notes: String = "",
    val isCancelled: Boolean = false,
    val returnedAmount: Double = 0.0, // Valor devolvido em devoluções parciais
    val returnStatus: String = "NONE", // "NONE", "PARTIAL", "FULL"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "sale_items")
data class SaleItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val saleId: String,
    val productId: String,
    val productName: String,
    val quantity: Double,
    val unitPrice: Double,
    val unitCost: Double,
    val subtotal: Double,
    val unit: String = "un",
    val originalQuantityDisplay: String = "" // Ex: "750 g", "250 g", "1.5 kg"
)

@Entity(tableName = "financial_transactions")
data class FinancialTransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val businessId: String,
    val type: String, // "INCOME_SALE", "INCOME_DEBT_PAYMENT", "INCOME_OTHER", "EXPENSE_PURCHASE", "EXPENSE_OPERATIONAL", "WITHDRAWAL_PRO_LABORE"
    val category: String, // "Vendas", "Recebimento de Dívida", "Aluguer", "Salários", "Energia & Água", "Transporte", "Fornecedores", "Pró-Labore", "Outros"
    val amount: Double,
    val description: String,
    val referenceId: String = "", // SaleId or PurchaseId
    val paymentMethod: String = "CASH",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val businessId: String,
    val type: String, // "RECEIVABLE" (Clientes a dever), "PAYABLE" (A pagar a fornecedores)
    val partyName: String, // Customer name or Supplier name
    val phone: String = "", // Contacto telefónico (opcional)
    val description: String = "", // Descrição da origem da dívida
    val initialAmount: Double,
    val remainingAmount: Double,
    val dueDate: Long? = null,
    val status: String = "PENDING", // "PENDING", "PARTIAL", "PAID"
    val referenceId: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "debt_payments")
data class DebtPaymentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val debtId: String,
    val businessId: String,
    val amount: Double,
    val paymentDate: Long = System.currentTimeMillis(),
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "feedbacks")
data class FeedbackEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val userEmail: String,
    val businessName: String,
    val type: String, // "REVIEW", "COMPLAINT", "SUGGESTION"
    val rating: Int = 5, // 1 to 5
    val category: String = "Geral", // Sistema, Pagamentos, Stock, Usabilidade, Ideia
    val title: String,
    val description: String,
    val status: String = "RECEBIDA", // "RECEBIDA", "EM_ANALISE", "EM_RESOLUCAO", "RESOLVIDA"
    val adminNotes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reservations")
data class ReservationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val businessId: String,
    val customerName: String,
    val customerPhone: String = "",
    val reservationDate: String, // e.g. "2026-10-21"
    val reservationTime: String, // e.g. "19:30"
    val partySize: Int = 2, // Pessoas (Restaurante) ou Vagas (Salão)
    val serviceOrTable: String = "Mesa 1", // Mesa (Restaurante) ou Serviço/Profissional (Salão)
    val status: String = "CONFIRMED", // "PENDING", "CONFIRMED", "COMPLETED", "CANCELLED"
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

