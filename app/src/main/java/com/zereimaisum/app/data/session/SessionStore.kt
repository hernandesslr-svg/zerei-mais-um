package com.zereimaisum.app.data.session

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
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
    private val remindersKey = booleanPreferencesKey("bill_reminders")
    private val notifAskedKey = booleanPreferencesKey("notif_permission_asked")

    /** -1 = ninguém logado. */
    val userId: Flow<Long> = context.dataStore.data.map { it[userIdKey] ?: -1L }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        prefs[themeKey]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    /** Lembretes de contas a vencer (ligados por padrão; o usuário pode desligar no Perfil). */
    val remindersEnabled: Flow<Boolean> = context.dataStore.data.map { it[remindersKey] ?: true }

    /** Se o app já pediu a permissão de notificações (evita pedir a cada abertura). */
    val notificationPermissionAsked: Flow<Boolean> = context.dataStore.data.map { it[notifAskedKey] ?: false }

    suspend fun setRemindersEnabled(enabled: Boolean) = context.dataStore.edit { it[remindersKey] = enabled }
    suspend fun markNotificationPermissionAsked() = context.dataStore.edit { it[notifAskedKey] = true }

    suspend fun setUser(id: Long) = context.dataStore.edit { it[userIdKey] = id }
    suspend fun clear() = context.dataStore.edit { it.remove(userIdKey) }
    suspend fun setTheme(mode: ThemeMode) = context.dataStore.edit { it[themeKey] = mode.name }
}
