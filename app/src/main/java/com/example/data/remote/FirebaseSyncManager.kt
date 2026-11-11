package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class SyncStatus(
    val inProgress: Boolean = false,
    val lastSyncTime: Long? = null,
    val itemsSynced: Int = 0,
    val errorMessage: String? = null
)

class FirebaseSyncManager(val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    suspend fun syncAll(userId: String, localDb: AppDatabase): SyncStatus = withContext(Dispatchers.IO) {
        try {
            var count = 0

            // 1. Sync Businesses
            val localBusinesses = localDb.businessDao().getAllBusinessesDirect()
            for (biz in localBusinesses) {
                val data = hashMapOf<String, Any>(
                    "userId" to userId,
                    "name" to biz.name,
                    "activityType" to biz.activityType,
                    "currencySymbol" to biz.currencySymbol,
                    "defaultUnit" to biz.defaultUnit,
                    "phone" to biz.phone,
                    "address" to biz.address,
                    "taxNumber" to biz.taxNumber,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                db.collection("businesses").document(biz.id).set(data, SetOptions.merge()).await()
                count++
            }

            // Pull remote businesses
            val remoteBusinesses = db.collection("businesses")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            for (doc in remoteBusinesses.documents) {
                val name = doc.getString("name") ?: continue
                val existing = localDb.businessDao().getBusinessById(doc.id)
                if (existing == null) {
                    localDb.businessDao().insertBusiness(
                        BusinessEntity(
                            id = doc.id,
                            ownerId = userId,
                            name = name,
                            activityType = doc.getString("activityType") ?: "Mercearia / Retalho",
                            currencySymbol = doc.getString("currencySymbol") ?: "MT",
                            defaultUnit = doc.getString("defaultUnit") ?: "un",
                            phone = doc.getString("phone") ?: "",
                            address = doc.getString("address") ?: "",
                            taxNumber = doc.getString("taxNumber") ?: "",
                            createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis()
                        )
                    )
                }
            }

            // 2. Sync Products
            val localProducts = localDb.productDao().getAllProductsDirect()
            for (prod in localProducts) {
                val data = hashMapOf<String, Any>(
                    "userId" to userId,
                    "businessId" to prod.businessId,
                    "categoryId" to prod.categoryId,
                    "name" to prod.name,
                    "sku" to prod.sku,
                    "purchasePrice" to prod.purchasePrice,
                    "sellingPrice" to prod.sellingPrice,
                    "currentStock" to prod.currentStock,
                    "minStockAlert" to prod.minStockAlert,
                    "unit" to prod.unit,
                    "sizes" to prod.sizes,
                    "colors" to prod.colors,
                    "isActive" to prod.isActive,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                db.collection("products").document(prod.id).set(data, SetOptions.merge()).await()
                count++
            }

            // Pull remote products
            val remoteProducts = db.collection("products")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            for (doc in remoteProducts.documents) {
                val name = doc.getString("name") ?: continue
                val existing = localDb.productDao().getProductById(doc.id)
                if (existing == null) {
                    localDb.productDao().insertProduct(
                        ProductEntity(
                            id = doc.id,
                            businessId = doc.getString("businessId") ?: "",
                            categoryId = doc.getString("categoryId") ?: "",
                            name = name,
                            sku = doc.getString("sku") ?: "",
                            purchasePrice = (doc.get("purchasePrice") as? Number)?.toDouble() ?: 0.0,
                            sellingPrice = (doc.get("sellingPrice") as? Number)?.toDouble() ?: 0.0,
                            currentStock = (doc.get("currentStock") as? Number)?.toDouble() ?: 0.0,
                            minStockAlert = (doc.get("minStockAlert") as? Number)?.toDouble() ?: 5.0,
                            unit = doc.getString("unit") ?: "un",
                            sizes = doc.getString("sizes") ?: "",
                            colors = doc.getString("colors") ?: "",
                            isActive = doc.getBoolean("isActive") ?: true
                        )
                    )
                }
            }

            // 3. Sync Sales
            val localSales = localDb.saleDao().getAllSalesDirect()
            for (sale in localSales) {
                val data = hashMapOf<String, Any>(
                    "userId" to userId,
                    "businessId" to sale.businessId,
                    "customerName" to sale.customerName,
                    "totalAmount" to sale.totalAmount,
                    "discount" to sale.discount,
                    "paymentStatus" to sale.paymentStatus,
                    "paymentMethod" to sale.paymentMethod,
                    "amountPaid" to sale.amountPaid,
                    "amountDue" to sale.amountDue,
                    "notes" to sale.notes,
                    "isCancelled" to sale.isCancelled,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                db.collection("sales").document(sale.id).set(data, SetOptions.merge()).await()
                count++
            }

            // 4. Sync Financial Transactions
            val localTx = localDb.financeDao().getAllTransactionsDirect()
            for (tx in localTx) {
                val data = hashMapOf<String, Any>(
                    "userId" to userId,
                    "businessId" to tx.businessId,
                    "type" to tx.type,
                    "category" to tx.category,
                    "amount" to tx.amount,
                    "description" to tx.description,
                    "referenceId" to tx.referenceId,
                    "paymentMethod" to tx.paymentMethod,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                db.collection("financial_transactions").document(tx.id).set(data, SetOptions.merge()).await()
                count++
            }

            // 5. Sync Debts
            val localDebts = localDb.financeDao().getAllDebtsDirect()
            for (debt in localDebts) {
                val data = hashMapOf<String, Any>(
                    "userId" to userId,
                    "businessId" to debt.businessId,
                    "type" to debt.type,
                    "partyName" to debt.partyName,
                    "initialAmount" to debt.initialAmount,
                    "remainingAmount" to debt.remainingAmount,
                    "status" to debt.status,
                    "referenceId" to debt.referenceId,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                db.collection("debts").document(debt.id).set(data, SetOptions.merge()).await()
                count++
            }

            // 6. Sync Feedbacks
            val localFeedbacks = localDb.feedbackDao().getAllFeedbacksDirect()
            for (fb in localFeedbacks) {
                val data = hashMapOf<String, Any>(
                    "userId" to userId,
                    "userEmail" to fb.userEmail,
                    "businessName" to fb.businessName,
                    "type" to fb.type,
                    "rating" to fb.rating,
                    "category" to fb.category,
                    "title" to fb.title,
                    "description" to fb.description,
                    "status" to fb.status,
                    "adminNotes" to fb.adminNotes,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                db.collection("feedbacks").document(fb.id).set(data, SetOptions.merge()).await()
                count++
            }

            // 7. Sync Services (Requisito 8 e 9)
            val localServices = localDb.serviceDao().getAllServicesDirect()
            for (srv in localServices) {
                val data = hashMapOf<String, Any>(
                    "userId" to userId,
                    "businessId" to srv.businessId,
                    "serviceCatalogId" to srv.serviceCatalogId,
                    "title" to srv.title,
                    "category" to srv.category,
                    "customerName" to srv.customerName,
                    "customerPhone" to srv.customerPhone,
                    "description" to srv.description,
                    "status" to srv.status,
                    "scheduledDate" to srv.scheduledDate,
                    "scheduledTime" to srv.scheduledTime,
                    "completedDate" to srv.completedDate,
                    "location" to srv.location,
                    "agreedPrice" to srv.agreedPrice,
                    "amountReceived" to srv.amountReceived,
                    "estimatedExpenses" to srv.estimatedExpenses,
                    "notes" to srv.notes,
                    "targetEquipmentOrVehicle" to srv.targetEquipmentOrVehicle,
                    "isPastService" to srv.isPastService,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                db.collection("services").document(srv.id).set(data, SetOptions.merge()).await()
                count++
            }

            // Pull remote services
            val remoteServices = db.collection("services")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            for (doc in remoteServices.documents) {
                val title = doc.getString("title") ?: continue
                val existing = localDb.serviceDao().getServiceById(doc.id)
                if (existing == null) {
                    localDb.serviceDao().insertService(
                        ServiceEntity(
                            id = doc.id,
                            businessId = doc.getString("businessId") ?: "",
                            serviceCatalogId = doc.getString("serviceCatalogId") ?: "",
                            title = title,
                            category = doc.getString("category") ?: "Geral",
                            customerName = doc.getString("customerName") ?: "Cliente Geral",
                            customerPhone = doc.getString("customerPhone") ?: "",
                            description = doc.getString("description") ?: "",
                            status = doc.getString("status") ?: "AGENDADO",
                            scheduledDate = doc.getString("scheduledDate") ?: "",
                            scheduledTime = doc.getString("scheduledTime") ?: "",
                            completedDate = doc.getString("completedDate") ?: "",
                            location = doc.getString("location") ?: "",
                            agreedPrice = (doc.get("agreedPrice") as? Number)?.toDouble() ?: 0.0,
                            amountReceived = (doc.get("amountReceived") as? Number)?.toDouble() ?: 0.0,
                            estimatedExpenses = (doc.get("estimatedExpenses") as? Number)?.toDouble() ?: 0.0,
                            notes = doc.getString("notes") ?: "",
                            targetEquipmentOrVehicle = doc.getString("targetEquipmentOrVehicle") ?: "",
                            isPastService = doc.getBoolean("isPastService") ?: false
                        )
                    )
                }
            }

            // 8. Sync Service Catalog
            val localCatalog = localDb.serviceCatalogDao().getAllCatalogDirect()
            for (item in localCatalog) {
                val data = hashMapOf<String, Any>(
                    "userId" to userId,
                    "businessId" to item.businessId,
                    "name" to item.name,
                    "category" to item.category,
                    "description" to item.description,
                    "standardPrice" to item.standardPrice,
                    "estimatedDurationMinutes" to item.estimatedDurationMinutes,
                    "notes" to item.notes,
                    "isActive" to item.isActive,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                db.collection("service_catalog").document(item.id).set(data, SetOptions.merge()).await()
                count++
            }

            SyncStatus(
                inProgress = false,
                lastSyncTime = System.currentTimeMillis(),
                itemsSynced = count,
                errorMessage = null
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "sync_all")
            SyncStatus(
                inProgress = false,
                lastSyncTime = null,
                itemsSynced = 0,
                errorMessage = e.localizedMessage ?: "Erro na sincronização em nuvem."
            )
        }
    }
}
