package com.zereimaisum.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        DebtEntity::class,
        DebtPaymentEntity::class,
        BoxEntity::class,
        BoxMovementEntity::class,
        GoalEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun users(): UserDao
    abstract fun categories(): CategoryDao
    abstract fun transactions(): TransactionDao
    abstract fun debts(): DebtDao
    abstract fun boxes(): BoxDao
    abstract fun goals(): GoalDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "zerei_mais_um.db")
                // Novas versões do banco devem trazer Migrations explícitas;
                // nunca usar fallbackToDestructiveMigration com dados reais de usuários.
                .build()
    }
}
