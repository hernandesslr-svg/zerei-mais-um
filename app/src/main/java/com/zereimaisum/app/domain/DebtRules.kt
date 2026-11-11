package com.zereimaisum.app.domain

import com.zereimaisum.app.core.Money

data class PaymentResult(val newBalanceCents: Long, val appliedCents: Long, val status: DebtStatus)

data class DebtPriorityItem(val debt: DebtView, val reason: String)

object DebtRules {

    /** Aplica um pagamento: nunca deixa o saldo negativo; saldo zero = quitada. */
    fun applyPayment(debt: DebtView, amountCents: Long): PaymentResult {
        require(amountCents > 0) { "Pagamento deve ser positivo" }
        require(debt.status != DebtStatus.PAID) { "Dívida já quitada" }
        val applied = minOf(amountCents, debt.balanceCents)
        val newBalance = debt.balanceCents - applied
        val status = if (newBalance == 0L) DebtStatus.PAID else debt.status
        return PaymentResult(newBalance, applied, status)
    }

    /** Percentual já pago (0..100). */
    fun progressPercent(debt: DebtView): Int =
        Money.percent(debt.originalCents - debt.balanceCents, debt.originalCents)

    /**
     * Sugestão de prioridade (não é garantia de resultado):
     * 1) maior juros mensal primeiro (avalanche), pois custa mais caro manter;
     * 2) em empate, menor saldo primeiro (bola de neve), para uma vitória rápida.
     */
    fun prioritize(debts: List<DebtView>): List<DebtPriorityItem> =
        debts.filter { it.status != DebtStatus.PAID && it.balanceCents > 0 }
            .sortedWith(compareByDescending<DebtView> { it.monthlyInterestBp }.thenBy { it.balanceCents })
            .map { d ->
                val reason = when {
                    d.monthlyInterestBp >= 500 ->
                        "Juros altos (${formatRate(d.monthlyInterestBp)} ao mês): cada mês custa caro."
                    d.monthlyInterestBp > 0 ->
                        "Juros de ${formatRate(d.monthlyInterestBp)} ao mês."
                    else -> "Sem juros cadastrados; saldo de ${Money.format(d.balanceCents)}."
                }
                DebtPriorityItem(d, reason)
            }

    /**
     * Mensagem de status. Importante: o app só conhece as dívidas cadastradas
     * pelo usuário, então NUNCA afirma que o CPF está limpo.
     */
    fun statusMessage(debts: List<DebtView>): String {
        if (debts.isEmpty()) return "Nenhuma dívida cadastrada."
        val open = debts.count { it.status != DebtStatus.PAID }
        return if (open == 0) {
            "Parabéns! Você quitou todas as dívidas cadastradas no app."
        } else {
            "$open de ${debts.size} dívidas cadastradas ainda em aberto."
        }
    }

    /** 199 -> "1,99%" */
    fun formatRate(bp: Int): String = "${bp / 100},${(bp % 100).toString().padStart(2, '0')}%"

    /** "1,99" ou "1.99" -> 199 pontos-base. Aceita até 2 casas decimais; máximo 100% a.m. */
    fun parseRate(text: String): Int? {
        val t = text.trim().replace("%", "").replace(',', '.')
        if (t.isEmpty()) return 0
        val v = t.toBigDecimalOrNull() ?: return null
        if (v.signum() < 0 || v.scale() > 2 || v > java.math.BigDecimal(100)) return null
        return v.movePointRight(2).toInt()
    }
}
