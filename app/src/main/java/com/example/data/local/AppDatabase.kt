package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        BusinessEntity::class,
        CategoryEntity::class,
        ProductEntity::class,
        StockMovementEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        FinancialTransactionEntity::class,
        DebtEntity::class,
        DebtPaymentEntity::class,
        FeedbackEntity::class,
        ReservationEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun businessDao(): BusinessDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun financeDao(): FinanceDao
    abstract fun feedbackDao(): FeedbackDao
    abstract fun reservationDao(): ReservationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "negocio_na_pratica.db"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedDatabase(database)
                    }
                }
            }
        }

        private suspend fun seedDatabase(database: AppDatabase) {
            val userDao = database.userDao()
            val businessDao = database.businessDao()
            val categoryDao = database.categoryDao()
            val productDao = database.productDao()
            val financeDao = database.financeDao()
            val feedbackDao = database.feedbackDao()

            // 1. Admin User
            val adminUser = UserEntity(
                id = "admin-user-01",
                email = "admin@negocionapratica.com",
                fullName = "Administrador Geral",
                passwordHash = "admin123",
                phone = "+244 923 000 111",
                role = "ADMIN"
            )
            userDao.insertUser(adminUser)

            // 2. Default Owner User
            val ownerUser = UserEntity(
                id = "owner-user-01",
                email = "proprietario@exemplo.com",
                fullName = "Manuel dos Santos",
                passwordHash = "123456",
                phone = "+258 84 555 7777",
                role = "OWNER"
            )
            userDao.insertUser(ownerUser)

            // 3. Default Business
            val defaultBusiness = BusinessEntity(
                id = "business-01",
                ownerId = ownerUser.id,
                name = "Cantina & Mercearia Central",
                activityType = "Mercearia / Mini mercado",
                currencySymbol = "MT",
                defaultUnit = "un",
                phone = "+258 84 123 4567",
                address = "Av. Eduardo Mondlane, nº 1240",
                city = "Maputo",
                country = "Moçambique",
                province = "Maputo Cidade",
                neighborhood = "Central B",
                referencePoint = "Próximo à Praça dos Trabalhadores",
                taxNumber = "400128930"
            )
            businessDao.insertBusiness(defaultBusiness)

            // 4. Default Categories
            val catAlimentos = CategoryEntity(id = "cat-01", businessId = defaultBusiness.id, name = "Alimentos Básicos")
            val catBebidas = CategoryEntity(id = "cat-02", businessId = defaultBusiness.id, name = "Bebidas & Refrigerantes")
            val catHigiene = CategoryEntity(id = "cat-03", businessId = defaultBusiness.id, name = "Higiene & Limpeza")
            categoryDao.insertCategory(catAlimentos)
            categoryDao.insertCategory(catBebidas)
            categoryDao.insertCategory(catHigiene)

            // 5. Initial Products - Incluindo produto vendido por peso (Açúcar a 90 MT/kg)
            val pAcucar = ProductEntity(
                id = "prod-00",
                businessId = defaultBusiness.id,
                categoryId = catAlimentos.id,
                name = "Açúcar Castanho",
                sku = "ALIM-ACUCAR",
                purchasePrice = 70.0,
                sellingPrice = 90.0, // 90 MT / kg
                currentStock = 50.0, // 50 kg disponíveis
                minStockAlert = 10.0,
                unit = "kg",
                isSoldByWeight = true,
                weightUnit = "kg"
            )
            val pArroz = ProductEntity(
                id = "prod-01",
                businessId = defaultBusiness.id,
                categoryId = catAlimentos.id,
                name = "Arroz Agulha",
                sku = "ALIM-001",
                purchasePrice = 60.0,
                sellingPrice = 85.0,
                currentStock = 45.0,
                minStockAlert = 10.0,
                unit = "kg",
                isSoldByWeight = true,
                weightUnit = "kg"
            )
            productDao.insertProduct(pAcucar)
            val pOleo = ProductEntity(
                id = "prod-02",
                businessId = defaultBusiness.id,
                categoryId = catAlimentos.id,
                name = "Óleo Vegetal 1L",
                sku = "ALIM-002",
                purchasePrice = 1200.0,
                sellingPrice = 1650.0,
                currentStock = 8.0, // Alert low stock!
                minStockAlert = 12.0,
                unit = "litro"
            )
            val pLeite = ProductEntity(
                id = "prod-03",
                businessId = defaultBusiness.id,
                categoryId = catAlimentos.id,
                name = "Leite Condensado 395g",
                sku = "ALIM-003",
                purchasePrice = 600.0,
                sellingPrice = 900.0,
                currentStock = 28.0,
                minStockAlert = 10.0,
                unit = "un"
            )
            val pAgua = ProductEntity(
                id = "prod-04",
                businessId = defaultBusiness.id,
                categoryId = catBebidas.id,
                name = "Água Mineral 500ml",
                sku = "BEB-001",
                purchasePrice = 150.0,
                sellingPrice = 300.0,
                currentStock = 60.0,
                minStockAlert = 20.0,
                unit = "un"
            )
            val pSabao = ProductEntity(
                id = "prod-05",
                businessId = defaultBusiness.id,
                categoryId = catHigiene.id,
                name = "Sabão em Barra Azul",
                sku = "HIG-001",
                purchasePrice = 350.0,
                sellingPrice = 500.0,
                currentStock = 3.0, // Alert low stock!
                minStockAlert = 15.0,
                unit = "un"
            )
            productDao.insertProduct(pArroz)
            productDao.insertProduct(pOleo)
            productDao.insertProduct(pLeite)
            productDao.insertProduct(pAgua)
            productDao.insertProduct(pSabao)

            // Initial capital injection / cash balance
            financeDao.insertTransaction(
                FinancialTransactionEntity(
                    businessId = defaultBusiness.id,
                    type = "INCOME_OTHER",
                    category = "Capital Inicial",
                    amount = 50000.0,
                    description = "Fundo de maneio e saldo inicial de caixa",
                    timestamp = System.currentTimeMillis() - 86400000L * 5
                )
            )

            // Sample feedback for admin module
            feedbackDao.insertFeedback(
                FeedbackEntity(
                    id = "feed-01",
                    userId = ownerUser.id,
                    userEmail = ownerUser.email,
                    businessName = defaultBusiness.name,
                    type = "REVIEW",
                    rating = 5,
                    title = "Excelente controlo de stock",
                    description = "A aplicação permitiu-me organizar as contas da cantina e controlar as vendas a crédito sem perder dinheiro."
                )
            )
            feedbackDao.insertFeedback(
                FeedbackEntity(
                    id = "feed-02",
                    userId = ownerUser.id,
                    userEmail = ownerUser.email,
                    businessName = defaultBusiness.name,
                    type = "COMPLAINT",
                    rating = 3,
                    category = "Sistema",
                    title = "Impressão de recibo bluetooth",
                    description = "Gostaria de poder conectar uma mini-impressora bluetooth para imprimir os recibos no balcão.",
                    status = "EM_ANALISE"
                )
            )
        }
    }
}
