package com.zereimaisum.app.data.repo

import androidx.room.withTransaction
import com.zereimaisum.app.core.Dates
import com.zereimaisum.app.data.db.AppDatabase
import com.zereimaisum.app.data.db.CategoryEntity
import com.zereimaisum.app.data.db.TransactionEntity
import com.zereimaisum.app.domain.Recurrence
import com.zereimaisum.app.domain.Schedule
import com.zereimaisum.app.domain.TxKind
import com.zereimaisum.app.domain.TxView
import kotlinx.coroutines.flow.Flow
import java.util.UUID

data class NewTransaction(
    val kind: TxKind,
    val description: String,
    val amountCents: Long,
    val date: Long,
    val categoryId: Long?,
    val paid: Boolean,
    val recurrence: Recurrence = Recurrence.NONE,
    /** 1 = à vista. >1 = valor total dividido em parcelas mensais. */
    val installments: Int = 1,
    val note: String? = null,
)

fun TransactionEntity.toView() = TxView(
    kind = TxKind.valueOf(kind), amountCents = amountCents, epochDay = date,
    paid = paid, description = description, id = id,
)

class TransactionRepository(
    private val db: AppDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    fun observe(userId: Long): Flow<List<TransactionEntity>> = db.transactions().observe(userId)
    fun observeCategories(userId: Long): Flow<List<CategoryEntity>> = db.categories().observe(userId)
    suspend fun byId(userId: Long, id: Long) = db.transactions().byId(userId, id)

    /**
     * Cria o lançamento. Parcelado: gera N lançamentos mensais com a soma exata do total.
     * Recorrente: gera as próximas [Schedule.RECURRENCE_HORIZON] ocorrências; as futuras
     * nascem como "não pagas".
     */
    suspend fun add(userId: Long, tx: NewTransaction) {
        require(tx.amountCents > 0)
        val now = clock()
        val items: List<TransactionEntity> = when {
            tx.installments > 1 -> {
                val series = UUID.randomUUID().toString()
                val parts = Schedule.splitInstallments(tx.amountCents, tx.installments)
                val dates = Schedule.occurrences(tx.date, Recurrence.MONTHLY, tx.installments)
                parts.indices.map { i ->
                    TransactionEntity(
                        userId = userId, kind = tx.kind.name,
                        description = "${tx.description.trim()} (${i + 1}/${tx.installments})",
                        amountCents = parts[i], date = dates[i], categoryId = tx.categoryId,
                        paid = if (i == 0) tx.paid else false, seriesId = series,
                        installmentN = i + 1, installmentTotal = tx.installments,
                        note = tx.note, createdAt = now,
                    )
                }
            }
            tx.recurrence != Recurrence.NONE -> {
                val series = UUID.randomUUID().toString()
                Schedule.occurrences(tx.date, tx.recurrence, Schedule.RECURRENCE_HORIZON).mapIndexed { i, date ->
                    TransactionEntity(
                        userId = userId, kind = tx.kind.name, description = tx.description.trim(),
                        amountCents = tx.amountCents, date = date, categoryId = tx.categoryId,
                        paid = if (i == 0) tx.paid else false, recurrence = tx.recurrence.name,
                        seriesId = series, note = tx.note, createdAt = now,
                    )
                }
            }
            else -> listOf(
                TransactionEntity(
                    userId = userId, kind = tx.kind.name, description = tx.description.trim(),
                    amountCents = tx.amountCents, date = tx.date, categoryId = tx.categoryId,
                    paid = tx.paid, note = tx.note, createdAt = now,
                )
            )
        }
        db.transactions().insertAll(items)
    }

    suspend fun update(userId: Long, entity: TransactionEntity) {
        require(entity.userId == userId)
        db.transactions().update(entity)
    }

    suspend fun setPaid(userId: Long, id: Long, paid: Boolean) {
        val tx = db.transactions().byId(userId, id) ?: return
        db.transactions().update(tx.copy(paid = paid))
    }

    /** [wholeSeriesFromHere] = exclui esta e as próximas ocorrências e encerra a recorrência. */
    suspend fun delete(userId: Long, id: Long, wholeSeriesFromHere: Boolean) {
        val tx = db.transactions().byId(userId, id) ?: return
        db.withTransaction {
            if (wholeSeriesFromHere && tx.seriesId != null) {
                db.transactions().deleteSeriesFrom(userId, tx.seriesId, tx.date)
                db.transactions().endSeries(userId, tx.seriesId)
            } else {
                db.transactions().delete(userId, id)
            }
        }
    }

    /**
     * Mantém as recorrências ativas sempre com ~12 meses à frente.
     * Chamado na abertura do app. As datas partem da primeira ocorrência para
     * preservar o dia original (ex.: dia 31).
     */
    suspend fun extendRecurrences(userId: Long, today: Long = Dates.today()) {
        val horizon = Dates.plusMonths(today, 12)
        val newItems = mutableListOf<TransactionEntity>()
        for (series in db.transactions().recurringSeriesIds(userId)) {
            val first = db.transactions().firstOfSeries(userId, series) ?: continue
            val last = db.transactions().lastOfSeries(userId, series) ?: continue
            val rec = runCatching { Recurrence.valueOf(first.recurrence) }.getOrNull() ?: continue
            if (rec == Recurrence.NONE || last.date >= Dates.plusMonths(today, 10)) continue
            var count = Schedule.RECURRENCE_HORIZON
            var dates = Schedule.occurrences(first.date, rec, count)
            while (dates.last() < horizon && count < 1000) {
                count += Schedule.RECURRENCE_HORIZON
                dates = Schedule.occurrences(first.date, rec, count)
            }
            dates.filter { it > last.date && it <= horizon }.forEach { date ->
                newItems += last.copy(id = 0, date = date, paid = false, createdAt = clock())
            }
        }
        if (newItems.isNotEmpty()) db.transactions().insertAll(newItems)
    }
}
