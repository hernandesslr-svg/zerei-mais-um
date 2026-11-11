package com.zereimaisum.app.data.repo

import androidx.room.withTransaction
import com.zereimaisum.app.data.db.AppDatabase
import com.zereimaisum.app.data.db.BoxEntity
import com.zereimaisum.app.data.db.BoxMovementEntity
import com.zereimaisum.app.data.db.GoalEntity
import com.zereimaisum.app.domain.BoxMoveKind
import com.zereimaisum.app.domain.BoxRules
import kotlinx.coroutines.flow.Flow

/** Caixinhas: reservas virtuais. Nenhuma operação aqui movimenta dinheiro real. */
class BoxRepository(
    private val db: AppDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    fun observe(userId: Long): Flow<List<BoxEntity>> = db.boxes().observe(userId)
    fun observeOne(userId: Long, id: Long): Flow<BoxEntity?> = db.boxes().observeOne(userId, id)
    fun observeMovements(boxId: Long): Flow<List<BoxMovementEntity>> = db.boxes().observeMovements(boxId)
    suspend fun byId(userId: Long, id: Long) = db.boxes().byId(userId, id)

    suspend fun save(userId: Long, box: BoxEntity): Long {
        require(box.userId == userId)
        return if (box.id == 0L) db.boxes().insert(box.copy(createdAt = clock()))
        else { db.boxes().update(box); box.id }
    }

    suspend fun delete(userId: Long, id: Long) = db.boxes().delete(userId, id)

    suspend fun deposit(userId: Long, boxId: Long, amountCents: Long, date: Long, note: String?): String? {
        if (amountCents <= 0) return "O valor precisa ser maior que zero."
        val box = db.boxes().byId(userId, boxId) ?: return "Caixinha não encontrada."
        db.withTransaction {
            db.boxes().update(box.copy(reservedCents = box.reservedCents + amountCents))
            db.boxes().insertMovement(BoxMovementEntity(boxId = boxId, kind = BoxMoveKind.DEPOSIT.name, amountCents = amountCents, date = date, note = note))
        }
        return null
    }

    suspend fun withdraw(userId: Long, boxId: Long, amountCents: Long, date: Long, note: String?): String? {
        val box = db.boxes().byId(userId, boxId) ?: return "Caixinha não encontrada."
        BoxRules.validateWithdraw(box.reservedCents, amountCents)?.let { return it }
        db.withTransaction {
            db.boxes().update(box.copy(reservedCents = box.reservedCents - amountCents))
            db.boxes().insertMovement(BoxMovementEntity(boxId = boxId, kind = BoxMoveKind.WITHDRAW.name, amountCents = amountCents, date = date, note = note))
        }
        return null
    }

    suspend fun transfer(userId: Long, fromId: Long, toId: Long, amountCents: Long, date: Long): String? {
        val from = db.boxes().byId(userId, fromId) ?: return "Caixinha de origem não encontrada."
        val to = db.boxes().byId(userId, toId) ?: return "Caixinha de destino não encontrada."
        BoxRules.validateTransfer(fromId, toId, from.reservedCents, amountCents)?.let { return it }
        db.withTransaction {
            db.boxes().update(from.copy(reservedCents = from.reservedCents - amountCents))
            db.boxes().update(to.copy(reservedCents = to.reservedCents + amountCents))
            db.boxes().insertMovement(BoxMovementEntity(boxId = fromId, kind = BoxMoveKind.TRANSFER_OUT.name, amountCents = amountCents, date = date, note = "Para ${to.name}", relatedBoxId = toId))
            db.boxes().insertMovement(BoxMovementEntity(boxId = toId, kind = BoxMoveKind.TRANSFER_IN.name, amountCents = amountCents, date = date, note = "De ${from.name}", relatedBoxId = fromId))
        }
        return null
    }

    /** Compromisso de planejamento: não há bloqueio real do dinheiro. */
    suspend fun setPlannedWithdrawal(userId: Long, boxId: Long, date: Long?) {
        val box = db.boxes().byId(userId, boxId) ?: return
        db.boxes().update(box.copy(plannedWithdrawalDate = date))
    }
}

class GoalRepository(
    private val db: AppDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    fun observe(userId: Long): Flow<List<GoalEntity>> = db.goals().observe(userId)
    suspend fun byId(userId: Long, id: Long) = db.goals().byId(userId, id)

    suspend fun save(userId: Long, goal: GoalEntity): Long {
        require(goal.userId == userId)
        val completed = if (goal.currentCents >= goal.targetCents && goal.targetCents > 0) goal.completedAt ?: clock() else null
        val g = goal.copy(completedAt = completed)
        return if (g.id == 0L) db.goals().insert(g.copy(createdAt = clock())) else { db.goals().update(g); g.id }
    }

    /** Registra progresso na meta; retorna true se a meta foi concluída agora. */
    suspend fun addProgress(userId: Long, id: Long, amountCents: Long): Boolean {
        val goal = db.goals().byId(userId, id) ?: return false
        val wasDone = goal.completedAt != null
        val newCurrent = (goal.currentCents + amountCents).coerceAtLeast(0)
        val done = newCurrent >= goal.targetCents
        db.goals().update(goal.copy(currentCents = newCurrent, completedAt = if (done) goal.completedAt ?: clock() else null))
        return done && !wasDone
    }

    suspend fun delete(userId: Long, id: Long) = db.goals().delete(userId, id)
}
