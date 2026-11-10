package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun getUserByPhone(phone: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT COUNT(*) FROM users")
    fun getTotalUsersCountFlow(): Flow<Int>

    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Query("UPDATE users SET isPhoneVerified = :verified WHERE id = :userId")
    suspend fun updatePhoneVerification(userId: String, verified: Boolean)
}

@Dao
interface BusinessDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBusiness(business: BusinessEntity)

    @Update
    suspend fun updateBusiness(business: BusinessEntity)

    @Query("SELECT * FROM businesses WHERE ownerId = :ownerId")
    fun getBusinessesForOwner(ownerId: String): Flow<List<BusinessEntity>>

    @Query("SELECT * FROM businesses WHERE id = :id LIMIT 1")
    suspend fun getBusinessById(id: String): BusinessEntity?

    @Query("SELECT * FROM businesses")
    suspend fun getAllBusinessesDirect(): List<BusinessEntity>

    @Query("SELECT COUNT(*) FROM businesses")
    fun getTotalBusinessesCountFlow(): Flow<Int>

    @Query("SELECT * FROM businesses ORDER BY createdAt DESC")
    fun getAllBusinessesFlow(): Flow<List<BusinessEntity>>

    @Delete
    suspend fun deleteBusiness(business: BusinessEntity)
}

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE businessId = :businessId")
    fun getCategoriesForBusiness(businessId: String): Flow<List<CategoryEntity>>
}

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("SELECT * FROM products WHERE businessId = :businessId AND isActive = 1 ORDER BY name ASC")
    fun getProductsForBusiness(businessId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): ProductEntity?

    @Query("SELECT * FROM products")
    suspend fun getAllProductsDirect(): List<ProductEntity>

    @Query("UPDATE products SET currentStock = currentStock + :amount WHERE id = :productId")
    suspend fun adjustProductStock(productId: String, amount: Double)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockMovement(movement: StockMovementEntity)

    @Query("SELECT * FROM stock_movements WHERE businessId = :businessId ORDER BY timestamp DESC")
    fun getStockMovementsForBusiness(businessId: String): Flow<List<StockMovementEntity>>
}

@Dao
interface SaleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity)

    @Update
    suspend fun updateSale(sale: SaleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItemEntity>)

    @Query("SELECT * FROM sales WHERE businessId = :businessId ORDER BY timestamp DESC")
    fun getSalesForBusiness(businessId: String): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE id = :saleId LIMIT 1")
    suspend fun getSaleById(saleId: String): SaleEntity?

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getItemsForSale(saleId: String): List<SaleItemEntity>

    @Query("SELECT * FROM sale_items")
    suspend fun getAllSaleItems(): List<SaleItemEntity>

    @Query("SELECT * FROM sales WHERE isCancelled = 0")
    suspend fun getAllValidSales(): List<SaleEntity>

    @Query("SELECT * FROM sales")
    suspend fun getAllSalesDirect(): List<SaleEntity>
}

@Dao
interface FinanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: FinancialTransactionEntity)

    @Query("SELECT * FROM financial_transactions WHERE businessId = :businessId ORDER BY timestamp DESC")
    fun getTransactionsForBusiness(businessId: String): Flow<List<FinancialTransactionEntity>>

    @Query("SELECT * FROM financial_transactions WHERE businessId = :businessId ORDER BY timestamp DESC")
    suspend fun getTransactionsListForBusiness(businessId: String): List<FinancialTransactionEntity>

    @Query("SELECT * FROM financial_transactions")
    suspend fun getAllTransactionsDirect(): List<FinancialTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtEntity)

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Query("SELECT * FROM debts WHERE businessId = :businessId ORDER BY timestamp DESC")
    fun getDebtsForBusiness(businessId: String): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE id = :debtId LIMIT 1")
    suspend fun getDebtById(debtId: String): DebtEntity?

    @Query("SELECT * FROM debts")
    suspend fun getAllDebtsDirect(): List<DebtEntity>

    @Delete
    suspend fun deleteDebt(debt: DebtEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebtPayment(payment: DebtPaymentEntity)

    @Query("SELECT * FROM debt_payments WHERE debtId = :debtId ORDER BY paymentDate DESC")
    fun getPaymentsForDebtFlow(debtId: String): Flow<List<DebtPaymentEntity>>

    @Query("SELECT * FROM debt_payments WHERE debtId = :debtId ORDER BY paymentDate DESC")
    suspend fun getPaymentsForDebtDirect(debtId: String): List<DebtPaymentEntity>

    @Query("SELECT * FROM debt_payments WHERE businessId = :businessId ORDER BY paymentDate DESC")
    fun getPaymentsForBusinessFlow(businessId: String): Flow<List<DebtPaymentEntity>>

    @Query("SELECT * FROM debt_payments WHERE businessId = :businessId")
    suspend fun getPaymentsForBusinessDirect(businessId: String): List<DebtPaymentEntity>
}

@Dao
interface FeedbackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: FeedbackEntity)

    @Update
    suspend fun updateFeedback(feedback: FeedbackEntity)

    @Query("SELECT * FROM feedbacks ORDER BY createdAt DESC")
    fun getAllFeedbacksFlow(): Flow<List<FeedbackEntity>>

    @Query("SELECT * FROM feedbacks")
    suspend fun getAllFeedbacksDirect(): List<FeedbackEntity>

    @Query("SELECT * FROM feedbacks WHERE userId = :userId ORDER BY createdAt DESC")
    fun getFeedbacksForUserFlow(userId: String): Flow<List<FeedbackEntity>>
}

@Dao
interface ReservationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReservation(reservation: ReservationEntity)

    @Update
    suspend fun updateReservation(reservation: ReservationEntity)

    @Delete
    suspend fun deleteReservation(reservation: ReservationEntity)

    @Query("SELECT * FROM reservations WHERE businessId = :businessId ORDER BY reservationDate ASC, reservationTime ASC")
    fun getReservationsForBusiness(businessId: String): Flow<List<ReservationEntity>>

    @Query("SELECT * FROM reservations WHERE id = :id LIMIT 1")
    suspend fun getReservationById(id: String): ReservationEntity?
}

