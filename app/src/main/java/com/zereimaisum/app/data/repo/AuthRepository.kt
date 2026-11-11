package com.zereimaisum.app.data.repo

import androidx.room.withTransaction
import com.zereimaisum.app.core.PasswordHasher
import com.zereimaisum.app.core.Validators
import com.zereimaisum.app.data.db.AppDatabase
import com.zereimaisum.app.data.db.BoxEntity
import com.zereimaisum.app.data.db.CategoryEntity
import com.zereimaisum.app.data.db.UserEntity
import com.zereimaisum.app.data.session.SessionStore
import com.zereimaisum.app.domain.BoxRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class AuthResult {
    data class Success(val userId: Long, val recoveryCode: String? = null) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

/**
 * Autenticação local (V1). Na Fase 4 este repositório passa a delegar ao Supabase Auth,
 * mantendo a mesma interface para as telas.
 */
class AuthRepository(
    private val db: AppDatabase,
    private val session: SessionStore,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private var failedAttempts = 0
    private var lockedUntil = 0L

    suspend fun signUp(name: String, email: String, password: String, phone: String): AuthResult =
        withContext(Dispatchers.Default) {
            val error = Validators.name(name) ?: Validators.email(email)
                ?: Validators.password(password) ?: Validators.phone(phone)
            if (error != null) return@withContext AuthResult.Error(error)

            val normalizedEmail = email.trim().lowercase()
            if (db.users().byEmail(normalizedEmail) != null) {
                return@withContext AuthResult.Error("Já existe uma conta com este e-mail neste aparelho.")
            }
            val salt = PasswordHasher.newSalt()
            val recoveryCode = PasswordHasher.newRecoveryCode()
            val recoverySalt = PasswordHasher.newSalt()
            val now = clock()

            val userId = db.withTransaction {
                val id = db.users().insert(
                    UserEntity(
                        name = name.trim(),
                        email = normalizedEmail,
                        phone = phone.filter { it.isDigit() }.ifEmpty { null },
                        passwordHash = PasswordHasher.hash(password, salt),
                        passwordSalt = salt,
                        recoveryHash = PasswordHasher.hash(PasswordHasher.normalizeRecoveryCode(recoveryCode), recoverySalt),
                        recoverySalt = recoverySalt,
                        createdAt = now,
                    )
                )
                seedUserData(id, now)
                id
            }
            // A sessão só começa depois que o usuário confirmar que anotou o código.
            AuthResult.Success(userId, recoveryCode)
        }

    suspend fun login(email: String, password: String): AuthResult = withContext(Dispatchers.Default) {
        val now = clock()
        if (now < lockedUntil) {
            val seconds = (lockedUntil - now) / 1000 + 1
            return@withContext AuthResult.Error("Muitas tentativas. Aguarde $seconds segundos.")
        }
        val user = db.users().byEmail(email.trim().lowercase())
        // Mesma mensagem para e-mail inexistente e senha errada: não revela quem tem conta.
        if (user == null || !PasswordHasher.verify(password, user.passwordSalt, user.passwordHash)) {
            failedAttempts++
            if (failedAttempts >= 5) {
                lockedUntil = now + 30_000
                failedAttempts = 0
            }
            return@withContext AuthResult.Error("E-mail ou senha incorretos.")
        }
        failedAttempts = 0
        session.setUser(user.id)
        AuthResult.Success(user.id)
    }

    /** Redefine a senha com o código de recuperação e gera um novo código. */
    suspend fun resetPassword(email: String, code: String, newPassword: String): AuthResult =
        withContext(Dispatchers.Default) {
            Validators.password(newPassword)?.let { return@withContext AuthResult.Error(it) }
            val user = db.users().byEmail(email.trim().lowercase())
            val normalized = PasswordHasher.normalizeRecoveryCode(code)
            if (user == null || !PasswordHasher.verify(normalized, user.recoverySalt, user.recoveryHash)) {
                return@withContext AuthResult.Error("E-mail ou código de recuperação incorretos.")
            }
            val salt = PasswordHasher.newSalt()
            val newCode = PasswordHasher.newRecoveryCode()
            val newRecoverySalt = PasswordHasher.newSalt()
            db.users().update(
                user.copy(
                    passwordHash = PasswordHasher.hash(newPassword, salt),
                    passwordSalt = salt,
                    recoveryHash = PasswordHasher.hash(PasswordHasher.normalizeRecoveryCode(newCode), newRecoverySalt),
                    recoverySalt = newRecoverySalt,
                )
            )
            AuthResult.Success(user.id, newCode)
        }

    suspend fun startSession(userId: Long) = session.setUser(userId)

    suspend fun logout() = session.clear()

    /** Exclusão definitiva: o CASCADE do banco remove todos os dados do usuário. */
    suspend fun deleteAccount(userId: Long, password: String): AuthResult = withContext(Dispatchers.Default) {
        val user = db.users().byId(userId) ?: return@withContext AuthResult.Error("Conta não encontrada.")
        if (!PasswordHasher.verify(password, user.passwordSalt, user.passwordHash)) {
            return@withContext AuthResult.Error("Senha incorreta.")
        }
        db.users().delete(userId)
        session.clear()
        AuthResult.Success(userId)
    }

    private suspend fun seedUserData(userId: Long, now: Long) {
        val income = listOf(
            "Salário" to "work", "Benefícios" to "card", "Comissões" to "percent",
            "Trabalho autônomo" to "build", "Outras rendas" to "add",
        ).map { (n, i) -> CategoryEntity(userId = userId, name = n, kind = "INCOME", icon = i) }
        val expense = listOf(
            "Aluguel" to "home", "Água" to "water", "Energia" to "bolt", "Internet" to "wifi",
            "Telefone" to "phone", "Mercado" to "cart", "Transporte" to "car", "Educação" to "school",
            "Saúde" to "health", "Lazer" to "celebration", "Assinaturas" to "subscriptions",
            "Delivery" to "delivery", "Dívidas" to "debt", "Outras despesas" to "more",
        ).map { (n, i) -> CategoryEntity(userId = userId, name = n, kind = "EXPENSE", icon = i) }
        db.categories().insertAll(income + expense)

        db.boxes().insertAll(
            BoxRules.DEFAULT_BOXES.map {
                BoxEntity(
                    userId = userId, name = it.name, icon = it.icon, color = it.colorHex,
                    targetCents = 0, isDefault = true, createdAt = now,
                )
            }
        )
    }
}
