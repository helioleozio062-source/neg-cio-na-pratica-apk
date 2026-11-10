package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context verifies app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Negócio na Prática", appName)
    }

    @Test
    fun `test financial dre calculation logic`() {
        // Accounting validation test
        val grossRevenue = 150000.0
        val costOfGoodsSold = 85000.0 // CMV: custo das mercadorias vendidas
        val grossProfit = grossRevenue - costOfGoodsSold
        val marginPercentage = (grossProfit / grossRevenue) * 100.0

        val operationalExpenses = 25000.0 // Aluguer, energia, salários
        val proLabore = 15000.0 // Retirada pessoal
        val totalExpenses = operationalExpenses + proLabore

        val netProfit = grossProfit - totalExpenses

        assertEquals(65000.0, grossProfit, 0.001)
        assertEquals(43.333, marginPercentage, 0.01)
        assertEquals(25000.0, netProfit, 0.001)
        assertTrue(netProfit > 0)
    }

    @Test
    fun `test credit sale separates revenue from cash in hand`() {
        val totalSaleAmount = 50000.0
        val downPayment = 15000.0
        val creditDue = totalSaleAmount - downPayment

        assertEquals(35000.0, creditDue, 0.001)
        // Cash in hand only receives the downPayment, not the total sale!
        assertEquals(15000.0, downPayment, 0.001)
    }

    @Test
    fun `test dark mode state toggle`() {
        var isDark = false
        // User activates dark mode
        isDark = !isDark
        assertTrue(isDark)
        // User deactivates dark mode
        isDark = !isDark
        assertFalse(isDark)
    }

    @Test
    fun `test stock replenishment calculation`() {
        val currentStock = 8.0
        val incomingStock = 24.0
        val updatedStock = currentStock + incomingStock
        assertEquals(32.0, updatedStock, 0.001)

        val minStockAlert = 12.0
        val isLowStockBefore = currentStock <= minStockAlert
        val isLowStockAfter = updatedStock <= minStockAlert

        assertTrue(isLowStockBefore)
        assertFalse(isLowStockAfter)
    }

    @Test
    fun `test mozambique phone normalization handles all valid formats`() {
        val n1 = com.example.util.PhoneUtils.normalizeMozambiquePhone("841234567")
        val n2 = com.example.util.PhoneUtils.normalizeMozambiquePhone("+258 84 123 4567")
        val n3 = com.example.util.PhoneUtils.normalizeMozambiquePhone("864180161") // Admin number
        val n4 = com.example.util.PhoneUtils.normalizeMozambiquePhone("85-987-6543")
        val n5 = com.example.util.PhoneUtils.normalizeMozambiquePhone("87 000 1122")
        val n6 = com.example.util.PhoneUtils.normalizeMozambiquePhone("821112233")
        val invalid = com.example.util.PhoneUtils.normalizeMozambiquePhone("12345")

        assertEquals("+258841234567", n1)
        assertEquals("+258841234567", n2)
        assertEquals("+258864180161", n3)
        assertEquals("+258859876543", n4)
        assertEquals("+258870001122", n5)
        assertEquals("+258821112233", n6)
        assertEquals(null, invalid)
    }

    @Test
    fun `test debt remaining calculation and status progression`() {
        val initialAmount = 10000.0
        var remainingAmount = initialAmount

        // 1. Initially Pending
        var status = if (remainingAmount == initialAmount) "PENDING" else "PARTIAL"
        assertEquals("PENDING", status)

        // 2. Partial payment of 4000 MT
        val partialPayment = 4000.0
        remainingAmount -= partialPayment
        status = if (remainingAmount <= 0.0) "PAID" else "PARTIAL"
        assertEquals(6000.0, remainingAmount, 0.001)
        assertEquals("PARTIAL", status)

        // 3. Final payment of 6000 MT
        val finalPayment = 6000.0
        remainingAmount -= finalPayment
        status = if (remainingAmount <= 0.0) "PAID" else "PARTIAL"
        assertEquals(0.0, remainingAmount, 0.001)
        assertEquals("PAID", status)
    }

    @Test
    fun `test weight based price calculation formula exact scenarios`() {
        val pricePerKg = 90.0

        // Exact scenarios required:
        // 100 g -> (100 / 1000) * 90 = 9.00 MT
        assertEquals(9.00, com.example.util.BusinessProfileUtils.calculateWeightPrice(100.0, pricePerKg), 0.001)

        // 250 g -> (250 / 1000) * 90 = 22.50 MT
        assertEquals(22.50, com.example.util.BusinessProfileUtils.calculateWeightPrice(250.0, pricePerKg), 0.001)

        // 350 g -> (350 / 1000) * 90 = 31.50 MT
        assertEquals(31.50, com.example.util.BusinessProfileUtils.calculateWeightPrice(350.0, pricePerKg), 0.001)

        // 500 g -> (500 / 1000) * 90 = 45.00 MT
        assertEquals(45.00, com.example.util.BusinessProfileUtils.calculateWeightPrice(500.0, pricePerKg), 0.001)

        // 750 g -> (750 / 1000) * 90 = 67.50 MT
        assertEquals(67.50, com.example.util.BusinessProfileUtils.calculateWeightPrice(750.0, pricePerKg), 0.001)

        // 1000 g (1 kg) -> 90.00 MT
        assertEquals(90.00, com.example.util.BusinessProfileUtils.calculateWeightPrice(1000.0, pricePerKg), 0.001)

        // 1250 g (1.25 kg) -> (1250 / 1000) * 90 = 112.50 MT
        assertEquals(112.50, com.example.util.BusinessProfileUtils.calculateWeightPrice(1250.0, pricePerKg), 0.001)

        // 1500 g (1.5 kg) -> (1500 / 1000) * 90 = 135.00 MT
        assertEquals(135.00, com.example.util.BusinessProfileUtils.calculateWeightPrice(1500.0, pricePerKg), 0.001)

        // 2000 g (2 kg) -> 180.00 MT
        assertEquals(180.00, com.example.util.BusinessProfileUtils.calculateWeightPrice(2000.0, pricePerKg), 0.001)
    }

    @Test
    fun `test weight unit conversions and stock decrement`() {
        val initialStockKg = 50.0

        // Customer buys 750 grams of sugar
        val saleInGrams = 750.0
        val saleInKg = com.example.util.BusinessProfileUtils.gramsToKg(saleInGrams)
        assertEquals(0.75, saleInKg, 0.0001)

        val remainingStockAfter750g = initialStockKg - saleInKg
        assertEquals(49.25, remainingStockAfter750g, 0.0001)

        // Customer buys another 250 grams
        val secondSaleInKg = com.example.util.BusinessProfileUtils.gramsToKg(250.0)
        assertEquals(0.25, secondSaleInKg, 0.0001)

        val remainingStockAfter250g = remainingStockAfter750g - secondSaleInKg
        assertEquals(49.00, remainingStockAfter250g, 0.0001)

        // Customer buys 1.5 kg
        val remainingStockAfter1_5kg = remainingStockAfter250g - 1.5
        assertEquals(47.50, remainingStockAfter1_5kg, 0.0001)
    }

    @Test
    fun `test stock protection prevents sale when quantity exceeds available stock`() {
        val availableStockKg = 50.0
        val requestedWeightGrams = 55000.0 // 55 kg requested
        val requestedWeightKg = com.example.util.BusinessProfileUtils.gramsToKg(requestedWeightGrams)

        val canSell = requestedWeightKg <= availableStockKg
        assertFalse("Sale exceeding available stock must be blocked", canSell)
    }

    @Test
    fun `test business category identification helpers`() {
        // Grocery & Weight relevant
        assertTrue(com.example.util.BusinessProfileUtils.isWeightRelevantBusiness("Mercearia / Mini mercado"))
        assertTrue(com.example.util.BusinessProfileUtils.isWeightRelevantBusiness("Padaria"))
        assertTrue(com.example.util.BusinessProfileUtils.isWeightRelevantBusiness("Talho"))
        assertFalse(com.example.util.BusinessProfileUtils.isWeightRelevantBusiness("Venda de roupa"))

        // Apparel & Footwear
        assertTrue(com.example.util.BusinessProfileUtils.isApparelBusiness("Venda de roupa"))
        assertTrue(com.example.util.BusinessProfileUtils.isApparelBusiness("Loja de calçado"))
        assertFalse(com.example.util.BusinessProfileUtils.isApparelBusiness("Mercearia / Mini mercado"))

        // Beauty & Services
        assertTrue(com.example.util.BusinessProfileUtils.isSalonOrBarber("Salão de beleza"))
        assertTrue(com.example.util.BusinessProfileUtils.isSalonOrBarber("Barbearia"))
        assertTrue(com.example.util.BusinessProfileUtils.isServiceBusiness("Prestação de serviços"))

        // Hospitality
        assertTrue(com.example.util.BusinessProfileUtils.isFoodOrRestaurant("Restaurante"))
        assertTrue(com.example.util.BusinessProfileUtils.isFoodOrRestaurant("Lanchonete"))
        assertTrue(com.example.util.BusinessProfileUtils.isFoodOrRestaurant("Bar"))
    }

    @Test
    fun `test receipt weight item formatting`() {
        val formatted = com.example.util.BusinessProfileUtils.formatWeightSaleHistoryItem(
            productName = "Açúcar",
            quantityKg = 0.75,
            pricePerKg = 90.0,
            totalPrice = 67.50,
            currencySymbol = "MT"
        )
        assertTrue(formatted.contains("Açúcar"))
        assertTrue(formatted.contains("750 g"))
        assertTrue(formatted.contains("90"))
        assertTrue(formatted.contains("67.50 MT"))
    }

    @Test
    fun `test 17 business activity types and 11 mozambique provinces lists`() {
        val categories = com.example.util.BusinessProfileUtils.BUSINESS_ACTIVITY_TYPES
        assertEquals(17, categories.size)
        assertTrue(categories.contains("Mercearia / Mini mercado"))
        assertTrue(categories.contains("Lanchonete"))
        assertTrue(categories.contains("Restaurante"))
        assertTrue(categories.contains("Venda de roupa"))
        assertTrue(categories.contains("Loja de calçado"))
        assertTrue(categories.contains("Salão de beleza"))
        assertTrue(categories.contains("Barbearia"))
        assertTrue(categories.contains("Loja de cosméticos"))
        assertTrue(categories.contains("Loja de electrónica"))
        assertTrue(categories.contains("Papelaria"))
        assertTrue(categories.contains("Bar"))
        assertTrue(categories.contains("Padaria"))
        assertTrue(categories.contains("Talho"))
        assertTrue(categories.contains("Loja de bebidas"))
        assertTrue(categories.contains("Comércio geral"))
        assertTrue(categories.contains("Prestação de serviços"))
        assertTrue(categories.contains("Outro"))

        val provinces = com.example.util.BusinessProfileUtils.MOZAMBIQUE_PROVINCES
        assertEquals(11, provinces.size)
        assertTrue(provinces.contains("Maputo Cidade"))
        assertTrue(provinces.contains("Maputo Província"))
        assertTrue(provinces.contains("Gaza"))
        assertTrue(provinces.contains("Inhambane"))
        assertTrue(provinces.contains("Sofala"))
        assertTrue(provinces.contains("Manica"))
        assertTrue(provinces.contains("Tete"))
        assertTrue(provinces.contains("Zambézia"))
        assertTrue(provinces.contains("Nampula"))
        assertTrue(provinces.contains("Cabo Delgado"))
        assertTrue(provinces.contains("Niassa"))
    }

    @Test
    fun `test business registration form validation logic`() {
        val validName = "Mini Mercado Leozio"
        val validActivity = "Mercearia / Mini mercado"
        val validCity = "Maputo"
        val validAddress = "Av. Eduardo Mondlane, Mercado Central, Banca 14"

        val isValid = validName.isNotBlank() && validActivity.isNotBlank() && validCity.isNotBlank() && validAddress.isNotBlank()
        assertTrue(isValid)

        // Missing name fails
        assertFalse("".isNotBlank() && validActivity.isNotBlank() && validCity.isNotBlank() && validAddress.isNotBlank())
        // Missing city fails
        assertFalse(validName.isNotBlank() && validActivity.isNotBlank() && "".isNotBlank() && validAddress.isNotBlank())
        // Missing address fails
        assertFalse(validName.isNotBlank() && validActivity.isNotBlank() && validCity.isNotBlank() && "".isNotBlank())
    }
}
