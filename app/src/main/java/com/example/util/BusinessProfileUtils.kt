package com.example.util

import java.util.Locale

/**
 * Utilitários para gestão de tipos de negócio, localização em Moçambique,
 * unidades de medida e cálculo de vendas por peso (kg / g).
 */
object BusinessProfileUtils {

    // 17 Categorias padronizadas de tipos de negócio exigidas no Requisito 1.B
    val BUSINESS_ACTIVITY_TYPES = listOf(
        "Mercearia / Mini mercado",
        "Lanchonete",
        "Restaurante",
        "Venda de roupa",
        "Loja de calçado",
        "Salão de beleza",
        "Barbearia",
        "Loja de cosméticos",
        "Loja de electrónica",
        "Papelaria",
        "Bar",
        "Padaria",
        "Talho",
        "Loja de bebidas",
        "Comércio geral",
        "Prestação de serviços",
        "Outro"
    )

    // Províncias oficiais de Moçambique
    val MOZAMBIQUE_PROVINCES = listOf(
        "Maputo Cidade",
        "Maputo Província",
        "Gaza",
        "Inhambane",
        "Sofala",
        "Manica",
        "Tete",
        "Zambézia",
        "Nampula",
        "Cabo Delgado",
        "Niassa"
    )

    // Unidades de medida disponíveis
    val MEASURE_UNITS = listOf(
        "kg" to "Quilograma (kg)",
        "g" to "Grama (g)",
        "L" to "Litro (L)",
        "ml" to "Mililitro (ml)",
        "un" to "Unidade (un)",
        "pct" to "Pacote (pct)",
        "cx" to "Caixa (cx)",
        "par" to "Par (par)",
        "pç" to "Peça (pç)",
        "outro" to "Outra unidade"
    )

    /**
     * Verifica se o negócio normalmente vende produtos por peso/volume
     */
    fun isWeightRelevantBusiness(activityType: String): Boolean {
        val t = activityType.lowercase(Locale.ROOT)
        return t.contains("mercearia") ||
                t.contains("mini mercado") ||
                t.contains("padaria") ||
                t.contains("talho") ||
                t.contains("bebidas") ||
                t.contains("comércio geral")
    }

    /**
     * Verifica se o negócio é do ramo de vestuário / calçado
     */
    fun isApparelBusiness(activityType: String): Boolean {
        val t = activityType.lowercase(Locale.ROOT)
        return t.contains("roupa") || t.contains("calçado") || t.contains("boutique") || t.contains("moda")
    }

    /**
     * Verifica se o negócio é salão de beleza ou barbearia
     */
    fun isSalonOrBarber(activityType: String): Boolean {
        val t = activityType.lowercase(Locale.ROOT)
        return t.contains("salão") || t.contains("beleza") || t.contains("barbearia") || t.contains("estética")
    }

    /**
     * Verifica se é restaurante, lanchonete ou bar
     */
    fun isFoodOrRestaurant(activityType: String): Boolean {
        val t = activityType.lowercase(Locale.ROOT)
        return t.contains("restaurante") || t.contains("lanchonete") || t.contains("bar")
    }

    /**
     * Verifica se o negócio presta serviços (Requisito 4: Salão de beleza, Barbearia, Oficina mecânica, Reparação,
     * Canalização, Electricidade, Construção civil, Limpeza, Fotografia, Design, Informática, Consultoria, Transporte,
     * Decoração de eventos, etc.)
     */
    fun isServiceBusiness(activityType: String): Boolean {
        val t = activityType.lowercase(Locale.ROOT)
        return t.contains("serviços") ||
                t.contains("salão") ||
                t.contains("barbearia") ||
                t.contains("oficina") ||
                t.contains("mecânic") ||
                t.contains("reparação") ||
                t.contains("informática") ||
                t.contains("consultoria") ||
                t.contains("fotografia") ||
                t.contains("filmagem") ||
                t.contains("design") ||
                t.contains("limpeza") ||
                t.contains("canalização") ||
                t.contains("electricidade") ||
                t.contains("eletricidade") ||
                t.contains("construção") ||
                t.contains("transporte") ||
                t.contains("evento") ||
                t.contains("estética")
    }

    /**
     * Verifica se uma unidade é baseada em peso (kg ou g)
     */
    fun isWeightUnit(unit: String): Boolean {
        val u = unit.trim().lowercase(Locale.ROOT)
        return u == "kg" || u == "g" || u == "quilograma" || u == "grama" || u == "quilogramas" || u == "gramas"
    }

    /**
     * Verifica se uma unidade é líquida (L ou ml)
     */
    fun isLiquidUnit(unit: String): Boolean {
        val u = unit.trim().lowercase(Locale.ROOT)
        return u == "l" || u == "ml" || u == "litro" || u == "mililitro" || u == "litros"
    }

    /**
     * Fórmula oficial exigida pelo utilizador:
     * Preço da venda = (Quantidade em gramas ÷ 1.000) × Preço por quilograma
     * Ex: (250 ÷ 1.000) × 90 = 22,50 MT
     */
    fun calculateWeightPrice(grams: Double, pricePerKg: Double): Double {
        if (grams <= 0.0 || pricePerKg <= 0.0) return 0.0
        return (grams / 1000.0) * pricePerKg
    }

    /**
     * Converte gramas para quilogramas (ex: 750 g -> 0.75 kg)
     */
    fun gramsToKg(grams: Double): Double = grams / 1000.0

    /**
     * Converte quilogramas para gramas (ex: 0.75 kg -> 750 g)
     */
    fun kgToGrams(kg: Double): Double = kg * 1000.0

    /**
     * Formata quantidade de peso de forma legível para o comerciante:
     * Se for < 1 kg, exibe "750 g"
     * Se for inteiro, exibe "2 kg"
     * Se for decimal, exibe "1,5 kg" ou "2,35 kg"
     */
    fun formatWeightDisplay(kg: Double): String {
        return when {
            kg < 0.001 -> "0 g"
            kg < 1.0 -> {
                val grams = (kg * 1000.0).toInt()
                "$grams g"
            }
            kg % 1.0 == 0.0 -> "${kg.toInt()} kg"
            else -> {
                val formatted = String.format(Locale.US, "%.2f", kg).replace(".00", "")
                "$formatted kg"
            }
        }
    }

    /**
     * Formata o resumo do item no histórico de vendas conforme exigência:
     * Exemplo: "Açúcar — 750 g — 90 MT/kg — Total: 67,50 MT"
     */
    fun formatWeightSaleHistoryItem(
        productName: String,
        quantityKg: Double,
        pricePerKg: Double,
        totalPrice: Double,
        currencySymbol: String = "MT"
    ): String {
        val qtyDisplay = formatWeightDisplay(quantityKg)
        val priceKgDisplay = String.format(Locale.US, "%.2f %s/kg", pricePerKg, currencySymbol)
        val totalDisplay = String.format(Locale.US, "%.2f %s", totalPrice, currencySymbol)
        return "$productName — $qtyDisplay — $priceKgDisplay — Total: $totalDisplay"
    }

    /**
     * Converte o código de pagamento para a designação comum em Moçambique
     */
    fun formatPaymentMethod(code: String): String {
        return when (code.uppercase(Locale.ROOT)) {
            "CASH" -> "Dinheiro"
            "MPESA" -> "M-Pesa"
            "EMOLA" -> "e-Mola"
            "MKESH" -> "mKesh"
            "CARD", "MULTICAIXA_CARD" -> "Cartão / POS"
            "TRANSFER" -> "Transferência"
            else -> code
        }
    }

    /**
     * Retorna o ícone visual correspondente a cada tipo de negócio
     */
    fun getActivityIcon(type: String): String {
        val t = type.lowercase(Locale.ROOT)
        return when {
            t.contains("mercearia") || t.contains("mini mercado") -> "🛒"
            t.contains("lanchonete") -> "🍔"
            t.contains("restaurante") -> "🍽️"
            t.contains("roupa") -> "👗"
            t.contains("calçado") -> "👟"
            t.contains("salão") || t.contains("beleza") -> "💇‍♀️"
            t.contains("barbearia") -> "💈"
            t.contains("cosméticos") -> "💄"
            t.contains("electrónica") || t.contains("eletrônica") -> "📱"
            t.contains("papelaria") -> "📚"
            t.contains("bar") -> "🍺"
            t.contains("padaria") -> "🥖"
            t.contains("talho") -> "🥩"
            t.contains("bebidas") -> "🥤"
            t.contains("comércio geral") -> "🏪"
            t.contains("serviços") -> "💼"
            else -> "✨"
        }
    }

    /**
     * Retorna um resumo prático de como o sistema se adapta ao tipo de negócio
     */
    fun getActivityAdaptationSummary(type: String): String {
        return when {
            isWeightRelevantBusiness(type) -> "⚖️ Vendas por peso ativadas: Vendas por kg, gramas, litros e cálculo automático de qualquer fração."
            isApparelBusiness(type) -> "👗 Controlo de vestuário ativado: Gestão por peças, tamanhos, cores e pares."
            isSalonOrBarber(type) -> "💇‍♀️ Salão & Barbearia: Gestão de serviços prestados, agendamentos e produtos utilizados."
            isFoodOrRestaurant(type) -> "🍽️ Restauração: Vendas por refeições, pratos, bebidas e lanches."
            isServiceBusiness(type) -> "💼 Prestação de serviços: Controlo de serviços realizados, pagamentos e valores a receber."
            else -> "🏪 Comércio adaptado: Controlo de inventário, vendas e DRE financeiro."
        }
    }
}
