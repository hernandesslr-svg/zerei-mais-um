package com.zereimaisum.app.core

/**
 * Valores monetários são sempre representados em centavos (Long).
 * Nunca use Float/Double para dinheiro.
 */
object Money {

    /** Limite de segurança: R$ 10 bilhões. Evita overflow e entradas absurdas. */
    const val MAX_CENTS: Long = 1_000_000_000_000L

    /** 123456 -> "R$ 1.234,56"; -500 -> "-R$ 5,00" */
    fun format(cents: Long): String {
        val sign = if (cents < 0) "-" else ""
        return "${sign}R$ ${formatPlain(kotlin.math.abs(cents))}"
    }

    /** 123456 -> "1.234,56" (sem símbolo, sem sinal) */
    fun formatPlain(cents: Long): String {
        val abs = kotlin.math.abs(cents)
        val reais = abs / 100
        val centavos = (abs % 100).toInt()
        val grouped = reais.toString().reversed().chunked(3).joinToString(".").reversed()
        return "$grouped,${centavos.toString().padStart(2, '0')}"
    }

    /**
     * Converte texto digitado pelo usuário em centavos.
     * Aceita: "1.234,56", "1234,56", "1234.56", "R$ 10", "10".
     * Retorna null se inválido, negativo ou acima do limite.
     */
    fun parse(input: String): Long? {
        var s = input.trim().replace("R$", "").replace(" ", "").replace(" ", "")
        if (s.isEmpty() || s.startsWith("-")) return null
        if (!s.all { it.isDigit() || it == '.' || it == ',' }) return null

        val intPart: String
        val decPart: String
        if (s.contains(',')) {
            if (s.count { it == ',' } > 1) return null
            val idx = s.indexOf(',')
            intPart = s.substring(0, idx).replace(".", "")
            decPart = s.substring(idx + 1)
        } else if (s.contains('.')) {
            val dots = s.count { it == '.' }
            val last = s.substring(s.lastIndexOf('.') + 1)
            if (dots == 1 && last.length in 1..2) {
                intPart = s.substring(0, s.lastIndexOf('.'))
                decPart = last
            } else {
                // pontos como separador de milhar: "1.234.567"
                if (s.split('.').drop(1).any { it.length != 3 }) return null
                intPart = s.replace(".", "")
                decPart = ""
            }
        } else {
            intPart = s
            decPart = ""
        }
        if (decPart.length > 2 || !decPart.all { it.isDigit() }) return null
        if (intPart.isEmpty() && decPart.isEmpty()) return null
        if (intPart.length > 13) return null
        val reais = if (intPart.isEmpty()) 0L else intPart.toLongOrNull() ?: return null
        val cents = decPart.padEnd(2, '0').ifEmpty { "00" }.toLong()
        val total = reais * 100 + cents
        return if (total > MAX_CENTS) null else total
    }

    /**
     * Usado pelo campo de valor que digita "da direita para a esquerda":
     * "123456" -> 123456 centavos (R$ 1.234,56).
     */
    fun fromDigits(digits: String): Long {
        val only = digits.filter { it.isDigit() }.trimStart('0').take(13)
        if (only.isEmpty()) return 0L
        return only.toLong().coerceAtMost(MAX_CENTS)
    }

    /** Percentual inteiro 0..100 de [part] em relação a [total]. */
    fun percent(part: Long, total: Long): Int {
        if (total <= 0) return 0
        return ((part.coerceAtLeast(0) * 100) / total).toInt().coerceIn(0, 100)
    }
}
