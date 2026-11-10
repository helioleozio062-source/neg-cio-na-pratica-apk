package com.example.util

/**
 * Utilitário de validação, normalização e formatação de números de telefone de Moçambique (+258).
 * Suporta operadores nacionais: Tmcel (82, 83), Vodacom (84, 85), Movitel (86, 87) e linhas fixas (2X).
 */
object PhoneUtils {

    const val COUNTRY_CODE = "+258"
    const val ADMIN_PHONE = "+258864180161"
    const val ADMIN_EMAIL = "helioleozio062@gmail.com"

    /**
     * Normaliza qualquer entrada de número moçambicano para o formato internacional padrão E.164 (+258XXXXXXXXX).
     * Aceita formatos: 84XXXXXXX, 85XXXXXXX, 86XXXXXXX, 87XXXXXXX, 82XXXXXXX, 83XXXXXXX,
     * +25884XXXXXXX, +258 84 123 4567, 0025884XXXXXXX, 25884XXXXXXX, etc.
     * Retorna o número normalizado ou null se não for um número válido.
     */
    fun normalizeMozambiquePhone(rawInput: String?): String? {
        if (rawInput.isNullOrBlank()) return null

        // Remove espaços, traços, pontos, parênteses e caracteres não numéricos exceto o '+' inicial
        val trimmed = rawInput.trim()
        val hasPlus = trimmed.startsWith("+")
        val digitsOnly = trimmed.filter { it.isDigit() }

        if (digitsOnly.isEmpty()) return null

        // Caso 1: Começa com indicativo internacional 258 (ex: +258841234567, 00258841234567, 258841234567)
        val nationalDigits = when {
            digitsOnly.startsWith("00258") && digitsOnly.length == 14 -> digitsOnly.removePrefix("00258")
            digitsOnly.startsWith("258") && digitsOnly.length == 12 -> digitsOnly.removePrefix("258")
            digitsOnly.length == 9 -> digitsOnly
            else -> return null
        }

        // Valida se os 9 dígitos nacionais correspondem a um número moçambicano válido
        // Móvel: 82, 83, 84, 85, 86, 87; Fixo: 21, 23, 24, 25, 26, 27, 28, 29
        val isValidPrefix = nationalDigits.startsWith("82") ||
                nationalDigits.startsWith("83") ||
                nationalDigits.startsWith("84") ||
                nationalDigits.startsWith("85") ||
                nationalDigits.startsWith("86") ||
                nationalDigits.startsWith("87") ||
                nationalDigits.startsWith("2")

        if (!isValidPrefix || nationalDigits.length != 9) {
            return null
        }

        return "$COUNTRY_CODE$nationalDigits"
    }

    /**
     * Verifica se o número moçambicano é válido.
     */
    fun isValidMozambiquePhone(rawInput: String?): Boolean {
        return normalizeMozambiquePhone(rawInput) != null
    }

    /**
     * Formata para exibição amigável: +258 84 123 4567
     */
    fun formatDisplay(rawInput: String?): String {
        val normalized = normalizeMozambiquePhone(rawInput) ?: return rawInput?.trim() ?: ""
        val digits = normalized.removePrefix(COUNTRY_CODE)
        return if (digits.length == 9) {
            "$COUNTRY_CODE ${digits.substring(0, 2)} ${digits.substring(2, 5)} ${digits.substring(5)}"
        } else {
            normalized
        }
    }

    /**
     * Retorna a operadora correspondente ao número para informação do comerciante.
     */
    fun getOperatorName(rawInput: String?): String {
        val normalized = normalizeMozambiquePhone(rawInput) ?: return "Número Desconhecido"
        val prefix = normalized.removePrefix(COUNTRY_CODE).take(2)
        return when (prefix) {
            "84", "85" -> "Vodacom Moçambique"
            "86", "87" -> "Movitel Moçambique"
            "82", "83" -> "Tmcel Moçambique"
            else -> if (prefix.startsWith("2")) "Linha Fixa Moçambique" else "Moçambique"
        }
    }
}
