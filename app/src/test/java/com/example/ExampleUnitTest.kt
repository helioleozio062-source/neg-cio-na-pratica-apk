package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testServiceFinancialCalculation() {
    // Requisito 2:
    // Serviço: Reparação de equipamento.
    // Valor acordado: 5.000 MT.
    // Valor recebido: 2.000 MT.
    // Valor pendente: 3.000 MT.
    // Despesas: 1.000 MT.
    // Resultado estimado: 4.000 MT.
    val agreedPrice = 5000.0
    val initialReceived = 2000.0
    val expenses = 1000.0

    val service = com.example.data.model.ServiceEntity(
      businessId = "biz-123",
      title = "Reparação de equipamento",
      customerName = "Sr. Alberto Matusse",
      agreedPrice = agreedPrice,
      amountReceived = initialReceived,
      estimatedExpenses = expenses,
      status = "EM_EXECUCAO"
    )

    assertEquals(3000.0, service.amountPending, 0.001)
    assertEquals(4000.0, service.estimatedNetResult, 0.001)

    // Pagamento restante de 3.000 MT
    val updatedService = service.copy(amountReceived = service.amountReceived + 3000.0, status = "CONCLUIDO")
    assertEquals(0.0, updatedService.amountPending, 0.001)
    assertEquals(5000.0, updatedService.amountReceived, 0.001)
  }

  @Test
  fun testBusinessActivityAdaptations() {
    // Reconhecimento de prestadores de serviços e atividades híbridas
    assertTrue(com.example.util.BusinessProfileUtils.isSalonOrBarber("Salão de beleza"))
    assertTrue(com.example.util.BusinessProfileUtils.isServiceBusiness("Salão de beleza"))
    assertTrue(com.example.util.BusinessProfileUtils.isServiceBusiness("Oficina mecânica"))
    assertTrue(com.example.util.BusinessProfileUtils.isServiceBusiness("Barbearia"))
    assertTrue(com.example.util.BusinessProfileUtils.isServiceBusiness("Prestação de serviços"))
    assertTrue(com.example.util.BusinessProfileUtils.isServiceBusiness("Informática e redes"))
    assertTrue(com.example.util.BusinessProfileUtils.isServiceBusiness("Fotografia e filmagem"))

    // Vendas por peso continuam ativas para mercearias e comércio alimentar
    assertTrue(com.example.util.BusinessProfileUtils.isWeightRelevantBusiness("Mercearia / Mini mercado"))
    assertTrue(com.example.util.BusinessProfileUtils.isWeightRelevantBusiness("Talho"))
  }
}
