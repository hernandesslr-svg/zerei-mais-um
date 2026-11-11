package com.zereimaisum.app.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "session")

enum class ThemeMode(val label: String) { SYSTEM("Igual ao sistema"), LIGHT("Claro"), DARK("Escuro") }

/** Guarda apenas o id do usuário logado e preferências de aparência. Nunca senhas. */
class SessionStore(private val context: Context) {
    private val userIdKey = longPreferencesKey("user_id")
    private val themeKey = stringPreferencesKey("theme_mode")

    /** -1 = ninguém logado. */
    val userId: Flow<Long> = context.dataStore.data.map { it[userIdKey] ?: -1L }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        prefs[themeKey]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    suspend fun setUser(id: Long) = context.dataStore.edit { it[userIdKey] = id }
    suspend fun clear() = context.dataStore.edit { it.remove(userIdKey) }
    suspend fun setTheme(mode: ThemeMode) = context.dataStore.edit { it[themeKey] = mode.name }
}
