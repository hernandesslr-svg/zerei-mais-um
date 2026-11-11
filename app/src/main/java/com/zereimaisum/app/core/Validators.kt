package com.zereimaisum.app.core

/** Cada função retorna null quando o valor é válido, ou a mensagem de erro para exibir. */
object Validators {
    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    const val MAX_TEXT = 80
    const val MAX_NOTE = 300

    fun name(value: String): String? {
        val v = value.trim()
        return when {
            v.length < 3 -> "Informe seu nome completo."
            !v.contains(' ') -> "Informe nome e sobrenome."
            v.length > MAX_TEXT -> "Nome muito longo."
            else -> null
        }
    }

    fun email(value: String): String? {
        val v = value.trim()
        return when {
            v.isEmpty() -> "Informe seu e-mail."
            v.length > 120 || !emailRegex.matches(v) -> "E-mail inválido."
            else -> null
        }
    }

    fun password(value: String): String? = when {
        value.length < 8 -> "A senha precisa ter pelo menos 8 caracteres."
        value.length > 128 -> "Senha muito longa."
        value.none { it.isLetter() } || value.none { it.isDigit() } ->
            "Use letras e números na senha."
        else -> null
    }

    /** Telefone é opcional; se informado, aceita 10 ou 11 dígitos (DDD + número). */
    fun phone(value: String): String? {
        val digits = value.filter { it.isDigit() }
        return when {
            value.isBlank() -> null
            digits.length !in 10..11 -> "Telefone inválido. Use DDD + número."
            else -> null
        }
    }

    fun requiredText(value: String, field: String, max: Int = MAX_TEXT): String? {
        val v = value.trim()
        return when {
            v.isEmpty() -> "Informe $field."
            v.length > max -> "Texto muito longo (máx. $max caracteres)."
            else -> null
        }
    }

    fun positiveAmount(cents: Long?): String? = when {
        cents == null -> "Valor inválido."
        cents <= 0 -> "O valor precisa ser maior que zero."
        cents > Money.MAX_CENTS -> "Valor acima do permitido."
        else -> null
    }

    fun nonNegativeAmount(cents: Long?): String? = when {
        cents == null -> "Valor inválido."
        cents < 0 -> "O valor não pode ser negativo."
        cents > Money.MAX_CENTS -> "Valor acima do permitido."
        else -> null
    }
}
