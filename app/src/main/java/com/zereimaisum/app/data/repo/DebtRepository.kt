package com.zereimaisum.app.data.repo

import androidx.room.withTransaction
import com.zereimaisum.app.data.db.AppDatabase
import com.zereimaisum.app.data.db.DebtEntity
import com.zereimaisum.app.data.db.DebtPaymentEntity
import com.zereimaisum.app.data.db.TransactionEntity
import com.zereimaisum.app.domain.DebtRules
import com.zereimaisum.app.domain.DebtStatus
import com.zereimaisum.app.domain.DebtView
import com.zereimaisum.app.domain.TxKind
import kotlinx.coroutines.flow.Flow

fun DebtEntity.toView() = DebtView(
    id = id, creditor = creditor, originalCents = originalCents, balanceCents = balanceCents,
    monthlyInterestBp = monthlyInterestBp, dueDay = dueDay, status = DebtStatus.valueOf(status),
)

class DebtRepository(
    private val db: AppDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    fun observe(userId: Long): Flow<List<DebtEntity>> = db.debts().observe(userId)
    fun observeOne(userId: Long, id: Long): Flow<DebtEntity?> = db.debts().observeOne(userId, id)
    fun observePayments(debtId: Long): Flow<List<DebtPaymentEntity>> = db.debts().observePayments(debtId)
    suspend fun byId(userId: Long, id: Long) = db.debts().byId(userId, id)

    suspend fun save(userId: Long, debt: DebtEntity): Long {
        require(debt.userId == userId)
        require(debt.balanceCents >= 0 && debt.originalCents > 0)
        return if (debt.id == 0L) {
            db.debts().insert(debt.copy(createdAt = clock()))
        } else {
            db.debts().update(debt); debt.id
        }
    }

    suspend fun delete(userId: Long, id: Long) = db.debts().delete(userId, id)

    /**
     * Registra um pagamento numa única transação de banco:
     * pagamento + atualização do saldo + despesa vinculada (debt_id) no extrato.
     * A despesa vinculada é a única representação do pagamento, então não há duplicidade.
     */
    suspend fun registerPayment(userId: Long, debtId: Long, amountCents: Long, date: Long, note: String?): String? {
        val debt = db.debts().byId(userId, debtId) ?: return "Dívida não encontrada."
        if (debt.status == DebtStatus.PAID.name) return "Esta dívida já está quitada."
        if (amountCents <= 0) return "O valor precisa ser maior que zero."
        val result = DebtRules.applyPayment(debt.toView(), amountCents)
        val debtCategory = db.categories().find(userId, "Dívidas", "EXPENSE")
        db.withTransaction {
            db.debts().insertPayment(
                DebtPaymentEntity(debtId = debtId, amountCents = result.appliedCents, date = date, note = note)
            )
            db.debts().update(
                debt.copy(
                    balanceCents = result.newBalanceCents,
                    status = result.status.name,
                    installmentsPaid = debt.installmentsPaid + 1,
                    paidAt = if (result.status == DebtStatus.PAID) clock() else null,
                )
            )
            db.transactions().insert(
                TransactionEntity(
                    userId = userId, kind = TxKind.EXPENSE.name,
                    description = "Pagamento: ${debt.creditor}", amountCents = result.appliedCents,
                    date = date, categoryId = debtCategory?.id, paid = true, debtId = debtId,
                    note = note, createdAt = clock(),
                )
            )
        }
        return null
    }

    suspend fun registerAgreement(userId: Long, debtId: Long, newBalanceCents: Long, installments: Int?, note: String) {
        val debt = db.debts().byId(userId, debtId) ?: return
        db.debts().update(
            debt.copy(
                balanceCents = newBalanceCents, installmentsTotal = installments,
                installmentsPaid = 0, agreementNote = note.trim(), status = DebtStatus.AGREEMENT.name,
            )
        )
    }
}
