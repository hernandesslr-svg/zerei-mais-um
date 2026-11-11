package com.zereimaisum.app.di

import android.content.Context
import com.zereimaisum.app.data.db.AppDatabase
import com.zereimaisum.app.data.repo.AuthRepository
import com.zereimaisum.app.data.repo.BoxRepository
import com.zereimaisum.app.data.repo.DebtRepository
import com.zereimaisum.app.data.repo.GoalRepository
import com.zereimaisum.app.data.repo.TransactionRepository
import com.zereimaisum.app.data.repo.UserRepository
import com.zereimaisum.app.data.session.SessionStore

/** Injeção de dependências manual: um único lugar cria e compartilha os objetos. */
class AppContainer(context: Context) {
    val db: AppDatabase = AppDatabase.build(context)
    val session = SessionStore(context)
    val auth = AuthRepository(db, session)
    val users = UserRepository(db)
    val transactions = TransactionRepository(db)
    val debts = DebtRepository(db)
    val boxes = BoxRepository(db)
    val goals = GoalRepository(db)
}
