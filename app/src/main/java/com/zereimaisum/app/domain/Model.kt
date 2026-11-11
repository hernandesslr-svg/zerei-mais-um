package com.zereimaisum.app.domain

/*
 * Tipos de domínio em Kotlin puro (sem dependências Android).
 * As entidades do banco guardam esses enums pelo nome (TEXT).
 */

enum class TxKind { INCOME, EXPENSE, TRANSFER }

enum class Recurrence(val label: String) {
    NONE("Não repete"),
    WEEKLY("Semanal"),
    MONTHLY("Mensal"),
    YEARLY("Anual");
}

enum class DebtStatus(val label: String) {
    OPEN("Em aberto"),
    AGREEMENT("Em acordo"),
    PAID("Quitada");
}

enum class BoxMoveKind(val label: String) {
    DEPOSIT("Adição"),
    WITHDRAW("Retirada"),
    TRANSFER_IN("Transferência recebida"),
    TRANSFER_OUT("Transferência enviada");
}

/** Visão mínima de uma movimentação usada pelos cálculos. */
data class TxView(
    val kind: TxKind,
    val amountCents: Long,
    val epochDay: Long,
    val paid: Boolean,
    val description: String = "",
    val id: Long = 0,
)

/** Visão mínima de uma dívida usada pelos cálculos. */
data class DebtView(
    val id: Long,
    val creditor: String,
    val originalCents: Long,
    val balanceCents: Long,
    /** Juros mensais em pontos-base: 199 = 1,99% a.m. */
    val monthlyInterestBp: Int,
    val dueDay: Int?,
    val status: DebtStatus,
)
