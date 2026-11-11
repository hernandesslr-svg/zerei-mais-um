package com.zereimaisum.app.data.repo

import com.zereimaisum.app.data.db.AppDatabase
import com.zereimaisum.app.data.db.UserEntity
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class UserRepository(private val db: AppDatabase) {
    fun observe(userId: Long): Flow<UserEntity?> = db.users().observe(userId)

    suspend fun setOnboardingStep(userId: Long, step: Int) {
        val u = db.users().byId(userId) ?: return
        if (step > u.onboardingStep || step == 0) db.users().update(u.copy(onboardingStep = step.coerceIn(0, 5)))
    }

    suspend fun updateProfile(userId: Long, name: String, phone: String?, photoUri: String?) {
        val u = db.users().byId(userId) ?: return
        db.users().update(u.copy(name = name.trim(), phone = phone?.filter { it.isDigit() }?.ifEmpty { null }, photoUri = photoUri))
    }

    /**
     * Exporta todos os dados do usuário em JSON (direito de portabilidade, LGPD).
     * Credenciais (hash, salt, código de recuperação) nunca são exportadas.
     */
    suspend fun exportJson(userId: Long): String {
        val u = db.users().byId(userId) ?: return "{}"
        val root = JSONObject()
        root.put("app", "Zerei Mais Um")
        root.put("exported_at", System.currentTimeMillis())
        root.put("user", JSONObject().put("name", u.name).put("email", u.email).put("phone", u.phone ?: JSONObject.NULL))
        root.put("categories", JSONArray(db.categories().all(userId).map {
            JSONObject().put("id", it.id).put("name", it.name).put("kind", it.kind)
        }))
        root.put("transactions", JSONArray(db.transactions().all(userId).map {
            JSONObject().put("id", it.id).put("kind", it.kind).put("description", it.description)
                .put("amount_cents", it.amountCents).put("date_epoch_day", it.date)
                .put("category_id", it.categoryId ?: JSONObject.NULL).put("paid", it.paid)
                .put("recurrence", it.recurrence).put("installment", it.installmentN?.let { n -> "$n/${it.installmentTotal}" } ?: JSONObject.NULL)
        }))
        root.put("debts", JSONArray(db.debts().all(userId).map {
            JSONObject().put("id", it.id).put("creditor", it.creditor).put("original_cents", it.originalCents)
                .put("balance_cents", it.balanceCents).put("monthly_interest_bp", it.monthlyInterestBp)
                .put("status", it.status)
        }))
        root.put("debt_payments", JSONArray(db.debts().allPayments(userId).map {
            JSONObject().put("debt_id", it.debtId).put("amount_cents", it.amountCents).put("date_epoch_day", it.date)
        }))
        root.put("boxes", JSONArray(db.boxes().all(userId).map {
            JSONObject().put("id", it.id).put("name", it.name).put("target_cents", it.targetCents)
                .put("reserved_cents", it.reservedCents)
        }))
        root.put("box_movements", JSONArray(db.boxes().allMovements(userId).map {
            JSONObject().put("box_id", it.boxId).put("kind", it.kind).put("amount_cents", it.amountCents).put("date_epoch_day", it.date)
        }))
        root.put("goals", JSONArray(db.goals().all(userId).map {
            JSONObject().put("title", it.title).put("target_cents", it.targetCents).put("current_cents", it.currentCents)
        }))
        return root.toString(2)
    }
}
