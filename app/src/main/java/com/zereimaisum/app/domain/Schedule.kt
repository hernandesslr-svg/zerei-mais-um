package com.zereimaisum.app.domain

import java.time.LocalDate

/** Geração de datas para lançamentos recorrentes e parcelados. */
object Schedule {

    /** Quantas ocorrências futuras uma recorrência mantém geradas. */
    const val RECURRENCE_HORIZON = 12

    const val MAX_INSTALLMENTS = 120

    /**
     * Datas de [count] ocorrências a partir de [startEpochDay].
     * Mensal preserva o dia original (31/01 -> 28/02 -> 31/03), sem "escorregar".
     */
    fun occurrences(startEpochDay: Long, recurrence: Recurrence, count: Int): List<Long> {
        require(count >= 1) { "count deve ser >= 1" }
        val start = LocalDate.ofEpochDay(startEpochDay)
        return (0 until count).map { i ->
            when (recurrence) {
                Recurrence.NONE -> if (i == 0) start else error("NONE só tem uma ocorrência")
                Recurrence.WEEKLY -> start.plusWeeks(i.toLong())
                Recurrence.MONTHLY -> start.plusMonths(i.toLong())
                Recurrence.YEARLY -> start.plusYears(i.toLong())
            }.toEpochDay()
        }
    }

    /**
     * Divide [totalCents] em [n] parcelas. A soma é sempre exatamente o total;
     * a diferença de centavos fica na primeira parcela.
     */
    fun splitInstallments(totalCents: Long, n: Int): List<Long> {
        require(n in 1..MAX_INSTALLMENTS) { "Parcelas devem estar entre 1 e $MAX_INSTALLMENTS" }
        require(totalCents > 0) { "Total deve ser positivo" }
        val base = totalCents / n
        val remainder = totalCents - base * n
        return List(n) { i -> if (i == 0) base + remainder else base }
    }
}
