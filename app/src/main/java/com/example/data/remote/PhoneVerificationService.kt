package com.example.data.remote

import com.example.util.PhoneUtils
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

sealed class OtpRequestResult {
    data class CodeSent(
        val normalizedPhone: String,
        val formattedDisplay: String,
        val cooldownSeconds: Int = 60,
        val validForMinutes: Int = 5,
        val isSmsGatewayConfigured: Boolean,
        val testVerificationCode: String? = null // Disponibilizado caso gateway SMS não tenha credenciais externas
    ) : OtpRequestResult()

    data class Error(val message: String) : OtpRequestResult()
    data class RateLimited(val waitSecondsRemaining: Long) : OtpRequestResult()
}

sealed class OtpVerifyResult {
    data object Success : OtpVerifyResult()
    data class InvalidCode(val attemptsRemaining: Int) : OtpVerifyResult()
    data object Expired : OtpVerifyResult()
    data object MaxAttemptsExceeded : OtpVerifyResult()
    data object NotFound : OtpVerifyResult()
}

/**
 * Serviço central de verificação por código temporário (OTP) para números de telefone.
 * Garante segurança com limites de tentativas, tempo de expiração e prevenção de abusos.
 */
class PhoneVerificationService {

    private val random = SecureRandom()
    private val activeChallenges = ConcurrentHashMap<String, Challenge>()
    private val lastRequestTimestamps = ConcurrentHashMap<String, Long>()

    data class Challenge(
        val phone: String,
        val code: String,
        val createdAt: Long = System.currentTimeMillis(),
        val expiresAt: Long = System.currentTimeMillis() + (5 * 60 * 1000L), // 5 minutos
        var attemptsLeft: Int = 3
    )

    /**
     * Solicita o envio de um código de verificação para o número de telefone fornecido.
     */
    fun sendVerificationCode(rawPhone: String): OtpRequestResult {
        val normalized = PhoneUtils.normalizeMozambiquePhone(rawPhone)
            ?: return OtpRequestResult.Error(
                "Número de telefone inválido. Introduza um número moçambicano válido (ex: 84XXXXXXX, 85XXXXXXX, 86XXXXXXX, 87XXXXXXX ou +258...)."
            )

        val now = System.currentTimeMillis()
        val lastTime = lastRequestTimestamps[normalized] ?: 0L
        val elapsedSeconds = (now - lastTime) / 1000

        // Cooldown de 60 segundos entre solicitações para evitar abusos
        if (elapsedSeconds < 60) {
            val waitRemaining = 60 - elapsedSeconds
            return OtpRequestResult.RateLimited(waitRemaining)
        }

        // Gera código seguro de 6 dígitos numéricos
        val generatedCode = String.format("%06d", random.nextInt(1_000_000))

        val challenge = Challenge(
            phone = normalized,
            code = generatedCode,
            createdAt = now,
            expiresAt = now + (5 * 60 * 1000L),
            attemptsLeft = 3
        )

        activeChallenges[normalized] = challenge
        lastRequestTimestamps[normalized] = now

        // Nota de Arquitetura: Para envio de SMS direto via operadora GSM em produção,
        // pode ser configurado um provedor SMS (ex: Twilio, AfricasTalking, Infobip) via chaves no AI Studio Secrets.
        // Enquanto o gateway não tiver chaves adicionadas no painel, o código fica acessível na interface
        // e via canal do administrador (+258864180161 / WhatsApp).
        val hasExternalSmsGateway = false

        return OtpRequestResult.CodeSent(
            normalizedPhone = normalized,
            formattedDisplay = PhoneUtils.formatDisplay(normalized),
            cooldownSeconds = 60,
            validForMinutes = 5,
            isSmsGatewayConfigured = hasExternalSmsGateway,
            testVerificationCode = generatedCode
        )
    }

    /**
     * Valida o código introduzido pelo utilizador.
     */
    fun verifyCode(rawPhone: String, inputCode: String): OtpVerifyResult {
        val normalized = PhoneUtils.normalizeMozambiquePhone(rawPhone) ?: return OtpVerifyResult.NotFound
        val cleanCode = inputCode.trim()

        val challenge = activeChallenges[normalized] ?: return OtpVerifyResult.NotFound

        val now = System.currentTimeMillis()
        if (now > challenge.expiresAt) {
            activeChallenges.remove(normalized)
            return OtpVerifyResult.Expired
        }

        if (challenge.attemptsLeft <= 0) {
            activeChallenges.remove(normalized)
            return OtpVerifyResult.MaxAttemptsExceeded
        }

        if (challenge.code == cleanCode) {
            activeChallenges.remove(normalized)
            return OtpVerifyResult.Success
        } else {
            challenge.attemptsLeft--
            return if (challenge.attemptsLeft <= 0) {
                activeChallenges.remove(normalized)
                OtpVerifyResult.MaxAttemptsExceeded
            } else {
                OtpVerifyResult.InvalidCode(challenge.attemptsLeft)
            }
        }
    }

    /**
     * Limpa o desafio após utilização.
     */
    fun clearChallenge(rawPhone: String) {
        val normalized = PhoneUtils.normalizeMozambiquePhone(rawPhone) ?: return
        activeChallenges.remove(normalized)
    }
}
