package com.zereimaisum.app.core

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Hash de senhas com PBKDF2-HMAC-SHA256 + salt aleatório por usuário.
 * A senha em texto nunca é salva nem registrada em log.
 */
object PasswordHasher {
    const val ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16
    private val random = SecureRandom()

    // Alfabeto sem caracteres ambíguos (0/O, 1/I/L)
    private const val CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"

    fun newSalt(): String {
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        return Base64.getEncoder().encodeToString(salt)
    }

    fun hash(secret: String, saltB64: String, iterations: Int = ITERATIONS): String {
        val salt = Base64.getDecoder().decode(saltB64)
        val spec = PBEKeySpec(secret.toCharArray(), salt, iterations, KEY_BITS)
        try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            return Base64.getEncoder().encodeToString(factory.generateSecret(spec).encoded)
        } finally {
            spec.clearPassword()
        }
    }

    /** Comparação em tempo constante para evitar ataques de temporização. */
    fun verify(secret: String, saltB64: String, expectedHashB64: String): Boolean {
        val actual = Base64.getDecoder().decode(hash(secret, saltB64))
        val expected = Base64.getDecoder().decode(expectedHashB64)
        return MessageDigest.isEqual(actual, expected)
    }

    /** Código de recuperação no formato XXXX-XXXX-XXXX, mostrado uma única vez ao usuário. */
    fun newRecoveryCode(): String {
        val chars = (1..12).map { CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)] }
        return chars.chunked(4).joinToString("-") { it.joinToString("") }
    }

    /** Normaliza o código digitado: ignora hífens, espaços e maiúsculas/minúsculas. */
    fun normalizeRecoveryCode(input: String): String =
        input.uppercase().filter { it.isLetterOrDigit() }
}
