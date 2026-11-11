package com.zereimaisum.app.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * Convenções:
 * - Valores em centavos (Long).
 * - Datas em epoch day (Long); instantes em epoch millis.
 * - Enums de domínio guardados pelo nome (TEXT).
 * - Toda tabela de dados tem user_id com CASCADE: excluir a conta apaga tudo.
 */

@Entity(tableName = "users", indices = [Index(value = ["email"], unique = true)])
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String,
    val phone: String?,
    @ColumnInfo(name = "password_hash") val passwordHash: String,
    @ColumnInfo(name = "password_salt") val passwordSalt: String,
    @ColumnInfo(name = "recovery_hash") val recoveryHash: String,
    @ColumnInfo(name = "recovery_salt") val recoverySalt: String,
    @ColumnInfo(name = "photo_uri") val photoUri: String? = null,
    /** 0..5 = etapa atual do assistente; 5 = concluído. */
    @ColumnInfo(name = "onboarding_step") val onboardingStep: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(
    tableName = "categories",
    foreignKeys = [ForeignKey(UserEntity::class, ["id"], ["user_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("user_id")],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long,
    val name: String,
    /** INCOME ou EXPENSE */
    val kind: String,
    val icon: String,
    @ColumnInfo(name = "monthly_limit_cents") val monthlyLimitCents: Long? = null,
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(UserEntity::class, ["id"], ["user_id"], onDelete = ForeignKey.CASCADE),
        ForeignKey(CategoryEntity::class, ["id"], ["category_id"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("user_id"), Index("category_id"), Index("series_id"), Index(value = ["user_id", "date"])],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long,
    /** INCOME, EXPENSE ou TRANSFER (TRANSFER nunca entra nos totais). */
    val kind: String,
    val description: String,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    val date: Long,
    @ColumnInfo(name = "category_id") val categoryId: Long?,
    val paid: Boolean,
    /** NONE, WEEKLY, MONTHLY, YEARLY */
    val recurrence: String = "NONE",
    /** Agrupa recorrências e parcelas da mesma compra. */
    @ColumnInfo(name = "series_id") val seriesId: String? = null,
    @ColumnInfo(name = "installment_n") val installmentN: Int? = null,
    @ColumnInfo(name = "installment_total") val installmentTotal: Int? = null,
    /** Pagamento de dívida que gerou esta despesa (evita lançamento duplicado). */
    @ColumnInfo(name = "debt_id") val debtId: Long? = null,
    val note: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(
    tableName = "debts",
    foreignKeys = [ForeignKey(UserEntity::class, ["id"], ["user_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("user_id")],
)
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long,
    val creditor: String,
    @ColumnInfo(name = "original_cents") val originalCents: Long,
    @ColumnInfo(name = "balance_cents") val balanceCents: Long,
    @ColumnInfo(name = "monthly_interest_bp") val monthlyInterestBp: Int,
    @ColumnInfo(name = "installments_total") val installmentsTotal: Int?,
    @ColumnInfo(name = "installments_paid") val installmentsPaid: Int = 0,
    @ColumnInfo(name = "due_day") val dueDay: Int?,
    @ColumnInfo(name = "agreement_note") val agreementNote: String? = null,
    /** OPEN, AGREEMENT, PAID */
    val status: String = "OPEN",
    @ColumnInfo(name = "paid_at") val paidAt: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(
    tableName = "debt_payments",
    foreignKeys = [ForeignKey(DebtEntity::class, ["id"], ["debt_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("debt_id")],
)
data class DebtPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "debt_id") val debtId: Long,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    val date: Long,
    val note: String?,
)

@Entity(
    tableName = "boxes",
    foreignKeys = [ForeignKey(UserEntity::class, ["id"], ["user_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("user_id")],
)
data class BoxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long,
    val name: String,
    val icon: String,
    val color: Long,
    @ColumnInfo(name = "target_cents") val targetCents: Long,
    @ColumnInfo(name = "reserved_cents") val reservedCents: Long = 0,
    @ColumnInfo(name = "target_date") val targetDate: Long? = null,
    @ColumnInfo(name = "planned_withdrawal_date") val plannedWithdrawalDate: Long? = null,
    @ColumnInfo(name = "is_default") val isDefault: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(
    tableName = "box_movements",
    foreignKeys = [ForeignKey(BoxEntity::class, ["id"], ["box_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("box_id")],
)
data class BoxMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "box_id") val boxId: Long,
    /** DEPOSIT, WITHDRAW, TRANSFER_IN, TRANSFER_OUT */
    val kind: String,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    val date: Long,
    val note: String?,
    @ColumnInfo(name = "related_box_id") val relatedBoxId: Long? = null,
)

@Entity(
    tableName = "goals",
    foreignKeys = [ForeignKey(UserEntity::class, ["id"], ["user_id"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("user_id")],
)
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long,
    val title: String,
    @ColumnInfo(name = "target_cents") val targetCents: Long,
    @ColumnInfo(name = "current_cents") val currentCents: Long = 0,
    val deadline: Long? = null,
    @ColumnInfo(name = "completed_at") val completedAt: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
