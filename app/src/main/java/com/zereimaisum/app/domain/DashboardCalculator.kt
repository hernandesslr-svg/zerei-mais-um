package com.zereimaisum.app.domain

import com.zereimaisum.app.core.Dates
import java.time.YearMonth

data class MonthSummary(
    val incomeCents: Long,
    val expenseCents: Long,
    /** Receitas - despesas do mês (competência). */
    val monthBalanceCents: Long,
    /** Receitas recebidas - despesas pagas, acumulado até hoje. */
    val accumulatedCents: Long,
    /** Valor reservado nas caixinhas (virtual). */
    val reservedCents: Long,
    /** Acumulado menos o reservado: quanto está livre segundo os registros. */
    val availableCents: Long,
)

object DashboardCalculator {

    /**
     * Transferências (ex.: pagamento de fatura, movimentos internos) nunca entram
     * como receita nem como despesa, evitando contagem em dobro.
     */
    fun summarize(
        txs: List<TxView>,
        month: YearMonth,
        reservedCents: Long,
        today: Long,
    ): MonthSummary {
        val range = Dates.monthRange(month)
        val inMonth = txs.filter { it.epochDay in range }
        val income = inMonth.filter { it.kind == TxKind.INCOME }.sumOf { it.amountCents }
        val expense = inMonth.filter { it.kind == TxKind.EXPENSE }.sumOf { it.amountCents }

        val settled = txs.filter { it.paid && it.epochDay <= today }
        val accumulated = settled.filter { it.kind == TxKind.INCOME }.sumOf { it.amountCents } -
            settled.filter { it.kind == TxKind.EXPENSE }.sumOf { it.amountCents }

        return MonthSummary(
            incomeCents = income,
            expenseCents = expense,
            monthBalanceCents = income - expense,
            accumulatedCents = accumulated,
            reservedCents = reservedCents,
            availableCents = accumulated - reservedCents,
        )
    }

    /** Despesas não pagas vencidas ou que vencem nos próximos [days] dias, da mais urgente para a menos. */
    fun upcomingBills(txs: List<TxView>, today: Long, days: Int = 7): List<TxView> =
        txs.filter { it.kind == TxKind.EXPENSE && !it.paid && it.epochDay <= today + days }
            .sortedBy { it.epochDay }

    /** Saudação conforme a hora do dia. */
    fun greeting(hour: Int, firstName: String): String {
        val period = when (hour) {
            in 5..11 -> "Bom dia"
            in 12..17 -> "Boa tarde"
            else -> "Boa noite"
        }
        return if (firstName.isBlank()) "$period!" else "$period, $firstName!"
    }
}
