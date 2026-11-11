package com.zereimaisum.app.feature.profile

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Switch
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.zereimaisum.app.notifications.BillReminders
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zereimaisum.app.core.Validators
import com.zereimaisum.app.data.repo.AuthResult
import com.zereimaisum.app.data.session.ThemeMode
import com.zereimaisum.app.ui.components.ChipSelector
import com.zereimaisum.app.ui.components.PasswordField
import com.zereimaisum.app.ui.components.PrimaryButton
import com.zereimaisum.app.ui.components.SectionTitle
import com.zereimaisum.app.ui.components.ZmTextField
import com.zereimaisum.app.ui.navigation.appContainer
import com.zereimaisum.app.ui.theme.ZmColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProfileScreen(userId: Long) {
    val c = appContainer()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user by remember(userId) { c.users.observe(userId) }.collectAsState(initial = null)
    val theme by c.session.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var askDelete by remember { mutableStateOf(false) }
    var photo by remember { mutableStateOf<Bitmap?>(null) }
    val reminders by c.session.remindersEnabled.collectAsState(initial = true)
    var notifGranted by remember { mutableStateOf(BillReminders.hasPermission(context)) }
    // Atualiza ao voltar das configurações do sistema.
    LifecycleResumeEffect(Unit) {
        notifGranted = BillReminders.hasPermission(context)
        onPauseOrDispose { }
    }

    LaunchedEffect(user?.id) { user?.let { name = it.name; phone = it.phone ?: "" } }
    LaunchedEffect(user?.photoUri) {
        photo = user?.photoUri?.let { uri -> withContext(Dispatchers.IO) { loadBitmap(context, Uri.parse(uri)) } }
    }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            scope.launch { c.users.updateProfile(userId, user?.name ?: name, user?.phone, uri.toString()) }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val json = c.users.exportJson(userId)
            val ok = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) } }.isSuccess
            }
            message = if (ok) "Dados exportados com sucesso." else "Não foi possível salvar o arquivo."
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Perfil", style = MaterialTheme.typography.headlineMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                contentAlignment = Alignment.Center,
            ) {
                val bmp = photo
                if (bmp != null) Image(bmp.asImageBitmap(), "Foto de perfil", contentScale = ContentScale.Crop, modifier = Modifier.size(80.dp))
                else Icon(Icons.Filled.Person, "Adicionar foto", Modifier.size(40.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(user?.name ?: "", style = MaterialTheme.typography.titleLarge)
                Text(user?.email ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Toque na foto para alterar", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Card {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.MilitaryTech, null, tint = ZmColors.Green, modifier = Modifier.size(40.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    val done = (user?.onboardingStep ?: 0) >= 5
                    Text(if (done) "Nível 1 — Primeiro Passo" else "Jornada começando", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (done) "Você organizou suas informações financeiras." else "Conclua a configuração inicial para alcançar o Nível 1.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text("Níveis 2 a 5, medalhas e moedas ZM chegam na Fase 3.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        SectionTitle("Dados pessoais")
        ZmTextField(name, { name = it }, "Nome completo", error = nameError)
        ZmTextField(phone, { phone = it }, "Telefone (opcional)", keyboardType = KeyboardType.Phone, error = Validators.phone(phone))
        PrimaryButton("Salvar dados", {
            nameError = Validators.name(name)
            if (nameError == null && Validators.phone(phone) == null) scope.launch {
                c.users.updateProfile(userId, name, phone, user?.photoUri); message = "Dados atualizados."
            }
        })

        SectionTitle("Aparência")
        ChipSelector(ThemeMode.entries, theme, { it.label }) { scope.launch { c.session.setTheme(it) } }

        SectionTitle("Notificações")
        ListItem(
            headlineContent = { Text("Lembretes de contas a vencer") },
            supportingContent = {
                Text(
                    if (notifGranted) "Aviso diário, por volta das 9h, de contas vencidas ou que vencem até amanhã"
                    else "As notificações estão bloqueadas. Toque aqui para permitir nas configurações do Android."
                )
            },
            leadingContent = { Icon(Icons.Filled.Notifications, null) },
            trailingContent = {
                Switch(checked = reminders && notifGranted, onCheckedChange = { on ->
                    if (on && !notifGranted) openNotificationSettings(context)
                    else scope.launch { c.session.setRemindersEnabled(on) }
                })
            },
            modifier = Modifier.clickable { if (!notifGranted) openNotificationSettings(context) },
        )

        SectionTitle("Organização")
        ListItem(
            headlineContent = { Text("Rever configuração inicial") },
            supportingContent = { Text("Volte ao assistente para adicionar receitas, despesas, dívidas e metas") },
            leadingContent = { Icon(Icons.Filled.Replay, null) },
            modifier = Modifier.clickable { scope.launch { c.users.setOnboardingStep(userId, 0) } },
        )

        SectionTitle("Privacidade e segurança")
        ListItem(
            headlineContent = { Text("Seus dados ficam neste aparelho") },
            supportingContent = { Text("Senha protegida por hash (PBKDF2). Não pedimos CPF nem senhas bancárias. O app não movimenta dinheiro real.") },
            leadingContent = { Icon(Icons.Filled.Security, null) },
        )
        ListItem(
            headlineContent = { Text("Exportar meus dados") },
            supportingContent = { Text("Arquivo JSON com todas as suas informações") },
            leadingContent = { Icon(Icons.Filled.Download, null) },
            modifier = Modifier.clickable { exportLauncher.launch("zerei-mais-um-dados.json") },
        )
        HorizontalDivider()
        ListItem(
            headlineContent = { Text("Sair") },
            leadingContent = { Icon(Icons.AutoMirrored.Filled.Logout, null) },
            modifier = Modifier.clickable { scope.launch { c.auth.logout() } },
        )
        ListItem(
            headlineContent = { Text("Excluir conta", color = MaterialTheme.colorScheme.error) },
            supportingContent = { Text("Apaga definitivamente todos os seus dados") },
            leadingContent = { Icon(Icons.Filled.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            modifier = Modifier.clickable { askDelete = true },
        )
        Spacer(Modifier.height(8.dp))
        Text("Zerei Mais Um — Organize. Planeje. Conquiste.  Versão 1.0", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    message?.let {
        AlertDialog(onDismissRequest = { message = null }, text = { Text(it) },
            confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } })
    }
    if (askDelete) DeleteAccountDialog(
        onDismiss = { askDelete = false },
        onConfirm = { password, setError ->
            scope.launch {
                when (val r = c.auth.deleteAccount(userId, password)) {
                    is AuthResult.Error -> setError(r.message)
                    is AuthResult.Success -> askDelete = false
                }
            }
        },
    )
}

@Composable
private fun DeleteAccountDialog(onDismiss: () -> Unit, onConfirm: (String, (String) -> Unit) -> Unit) {
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Excluir conta definitivamente?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Todas as movimentações, dívidas, caixinhas e metas serão apagadas. Exporte seus dados antes, se quiser guardá-los.")
                PasswordField(password, { password = it }, "Confirme sua senha", error)
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(password) { error = it } }) { Text("Excluir", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

private fun openNotificationSettings(context: android.content.Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

private fun loadBitmap(context: android.content.Context, uri: Uri): Bitmap? = runCatching {
    if (Build.VERSION.SDK_INT >= 28) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            val max = maxOf(info.size.width, info.size.height)
            if (max > 512) decoder.setTargetSampleSize(max / 512)
        }
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }
}.getOrNull()
