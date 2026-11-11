package com.zereimaisum.app.domain

import com.zereimaisum.app.core.Dates
import com.zereimaisum.app.core.Money

/** Caixinha padrão criada no cadastro. [icon] e [colorHex] são chaves interpretadas pela UI. */
data class DefaultBox(val name: String, val icon: String, val colorHex: Long)

/**
 * Caixinhas são reservas VIRTUAIS: representam dinheiro que o usuário separou
 * nos seus registros. Não há rendimento nem bloqueio bancário real.
 */
object BoxRules {

    val DEFAULT_BOXES = listOf(
        DefaultBox("Emergência", "shield", 0xFF0879E8),
        DefaultBox("Viagem", "flight", 0xFF43D5F2),
        DefaultBox("Reforma", "home", 0xFFF2A541),
        DefaultBox("Lazer", "celebration", 0xFF8DEB35),
        DefaultBox("Objetivos pessoais", "star", 0xFF9B6DFF),
    )

    /** Opções de retirada planejada, em meses. */
    val PLANNED_WITHDRAWAL_MONTHS = listOf(1, 3, 6, 7, 12)

    fun plannedWithdrawalDate(fromEpochDay: Long, months: Int): Long {
        require(months in PLANNED_WITHDRAWAL_MONTHS) { "Opção de meses inválida" }
        return Dates.plusMonths(fromEpochDay, months.toLong())
    }

    /** Valida uma retirada: retorna mensagem de erro ou null se permitida. */
    fun validateWithdraw(reservedCents: Long, amountCents: Long): String? = when {
        amountCents <= 0 -> "O valor precisa ser maior que zero."
        amountCents > reservedCents ->
            "Você só tem ${Money.format(reservedCents)} reservado nesta caixinha."
        else -> null
    }

    fun validateTransfer(fromId: Long, toId: Long, fromReserved: Long, amountCents: Long): String? = when {
        fromId == toId -> "Escolha uma caixinha de destino diferente."
        else -> validateWithdraw(fromReserved, amountCents)
    }

    fun progressPercent(reservedCents: Long, targetCents: Long): Int =
        Money.percent(reservedCents, targetCents)

    /**
     * Quanto guardar por mês para atingir a meta até a data prevista.
     * Retorna null se não houver data, se já atingiu ou se a data já passou.
     */
    fun monthlyNeeded(reservedCents: Long, targetCents: Long, today: Long, targetEpochDay: Long?): Long? {
        if (targetEpochDay == null) return null
        val remaining = targetCents - reservedCents
        if (remaining <= 0 || targetEpochDay <= today) return null
        val months = java.time.temporal.ChronoUnit.MONTHS.between(
            java.time.LocalDate.ofEpochDay(today),
            java.time.LocalDate.ofEpochDay(targetEpochDay),
        ).coerceAtLeast(1)
        return (remaining + months - 1) / months // arredonda para cima
    }
}

object Projections {
    /**
     * Economia ESTIMADA (não é dinheiro reservado): ex. R$ 200/mês por 12 meses = R$ 2.400.
     */
    fun estimatedSavings(monthlyCents: Long, months: Int): Long {
        require(months >= 0)
        return monthlyCents * months
    }
}
