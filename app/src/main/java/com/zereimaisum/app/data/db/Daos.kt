package com.zereimaisum.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/*
 * Toda consulta de dados recebe userId: é isso que garante o isolamento
 * entre contas diferentes no mesmo aparelho.
 */

@Dao
interface UserDao {
    @Insert suspend fun insert(user: UserEntity): Long
    @Update suspend fun update(user: UserEntity)
    @Query("SELECT * FROM users WHERE id = :id") suspend fun byId(id: Long): UserEntity?
    @Query("SELECT * FROM users WHERE id = :id") fun observe(id: Long): Flow<UserEntity?>
    @Query("SELECT * FROM users WHERE email = :email COLLATE NOCASE LIMIT 1")
    suspend fun byEmail(email: String): UserEntity?
    @Query("DELETE FROM users WHERE id = :id") suspend fun delete(id: Long)
}

@Dao
interface CategoryDao {
    @Insert suspend fun insertAll(items: List<CategoryEntity>)
    @Insert suspend fun insert(item: CategoryEntity): Long
    @Query("SELECT * FROM categories WHERE user_id = :userId ORDER BY name")
    fun observe(userId: Long): Flow<List<CategoryEntity>>
    @Query("SELECT * FROM categories WHERE user_id = :userId")
    suspend fun all(userId: Long): List<CategoryEntity>
    @Query("SELECT * FROM categories WHERE user_id = :userId AND name = :name AND kind = :kind LIMIT 1")
    suspend fun find(userId: Long, name: String, kind: String): CategoryEntity?
}

@Dao
interface TransactionDao {
    @Insert suspend fun insert(item: TransactionEntity): Long
    @Insert suspend fun insertAll(items: List<TransactionEntity>)
    @Update suspend fun update(item: TransactionEntity)
    @Query("SELECT * FROM transactions WHERE user_id = :userId ORDER BY date DESC, id DESC")
    fun observe(userId: Long): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM transactions WHERE user_id = :userId")
    suspend fun all(userId: Long): List<TransactionEntity>
    @Query("SELECT * FROM transactions WHERE user_id = :userId AND id = :id")
    suspend fun byId(userId: Long, id: Long): TransactionEntity?
    @Query("DELETE FROM transactions WHERE user_id = :userId AND id = :id")
    suspend fun delete(userId: Long, id: Long)
    /** Exclui esta e as próximas ocorrências da série (passado é preservado). */
    @Query("DELETE FROM transactions WHERE user_id = :userId AND series_id = :seriesId AND date >= :fromDate")
    suspend fun deleteSeriesFrom(userId: Long, seriesId: String, fromDate: Long)
    @Query("SELECT * FROM transactions WHERE user_id = :userId AND series_id = :seriesId ORDER BY date DESC LIMIT 1")
    suspend fun lastOfSeries(userId: Long, seriesId: String): TransactionEntity?
    @Query("SELECT * FROM transactions WHERE user_id = :userId AND series_id = :seriesId ORDER BY date ASC LIMIT 1")
    suspend fun firstOfSeries(userId: Long, seriesId: String): TransactionEntity?
    /** Encerra a recorrência: as ocorrências restantes deixam de gerar novas. */
    @Query("UPDATE transactions SET recurrence = 'NONE' WHERE user_id = :userId AND series_id = :seriesId")
    suspend fun endSeries(userId: Long, seriesId: String)
    @Query("SELECT DISTINCT series_id FROM transactions WHERE user_id = :userId AND recurrence != 'NONE' AND series_id IS NOT NULL")
    suspend fun recurringSeriesIds(userId: Long): List<String>
}

@Dao
interface DebtDao {
    @Insert suspend fun insert(item: DebtEntity): Long
    @Update suspend fun update(item: DebtEntity)
    @Query("SELECT * FROM debts WHERE user_id = :userId ORDER BY status = 'PAID', balance_cents DESC")
    fun observe(userId: Long): Flow<List<DebtEntity>>
    @Query("SELECT * FROM debts WHERE user_id = :userId")
    suspend fun all(userId: Long): List<DebtEntity>
    @Query("SELECT * FROM debts WHERE user_id = :userId AND id = :id")
    suspend fun byId(userId: Long, id: Long): DebtEntity?
    @Query("SELECT * FROM debts WHERE user_id = :userId AND id = :id")
    fun observeOne(userId: Long, id: Long): Flow<DebtEntity?>
    @Query("DELETE FROM debts WHERE user_id = :userId AND id = :id")
    suspend fun delete(userId: Long, id: Long)

    @Insert suspend fun insertPayment(item: DebtPaymentEntity): Long
    @Query("SELECT * FROM debt_payments WHERE debt_id = :debtId ORDER BY date DESC, id DESC")
    fun observePayments(debtId: Long): Flow<List<DebtPaymentEntity>>
    @Query("SELECT p.* FROM debt_payments p JOIN debts d ON d.id = p.debt_id WHERE d.user_id = :userId")
    suspend fun allPayments(userId: Long): List<DebtPaymentEntity>
}

@Dao
interface BoxDao {
    @Insert suspend fun insert(item: BoxEntity): Long
    @Insert suspend fun insertAll(items: List<BoxEntity>)
    @Update suspend fun update(item: BoxEntity)
    @Query("SELECT * FROM boxes WHERE user_id = :userId ORDER BY is_default DESC, id")
    fun observe(userId: Long): Flow<List<BoxEntity>>
    @Query("SELECT * FROM boxes WHERE user_id = :userId")
    suspend fun all(userId: Long): List<BoxEntity>
    @Query("SELECT * FROM boxes WHERE user_id = :userId AND id = :id")
    suspend fun byId(userId: Long, id: Long): BoxEntity?
    @Query("SELECT * FROM boxes WHERE user_id = :userId AND id = :id")
    fun observeOne(userId: Long, id: Long): Flow<BoxEntity?>
    @Query("DELETE FROM boxes WHERE user_id = :userId AND id = :id")
    suspend fun delete(userId: Long, id: Long)

    @Insert suspend fun insertMovement(item: BoxMovementEntity): Long
    @Query("SELECT * FROM box_movements WHERE box_id = :boxId ORDER BY date DESC, id DESC")
    fun observeMovements(boxId: Long): Flow<List<BoxMovementEntity>>
    @Query("SELECT m.* FROM box_movements m JOIN boxes b ON b.id = m.box_id WHERE b.user_id = :userId")
    suspend fun allMovements(userId: Long): List<BoxMovementEntity>
}

@Dao
interface GoalDao {
    @Insert suspend fun insert(item: GoalEntity): Long
    @Update suspend fun update(item: GoalEntity)
    @Query("SELECT * FROM goals WHERE user_id = :userId ORDER BY completed_at IS NOT NULL, id")
    fun observe(userId: Long): Flow<List<GoalEntity>>
    @Query("SELECT * FROM goals WHERE user_id = :userId")
    suspend fun all(userId: Long): List<GoalEntity>
    @Query("SELECT * FROM goals WHERE user_id = :userId AND id = :id")
    suspend fun byId(userId: Long, id: Long): GoalEntity?
    @Query("DELETE FROM goals WHERE user_id = :userId AND id = :id")
    suspend fun delete(userId: Long, id: Long)
}
