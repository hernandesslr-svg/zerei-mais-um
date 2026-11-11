package com.zereimaisum.app.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zereimaisum.app.data.repo.AuthRepository
import com.zereimaisum.app.data.repo.AuthResult
import com.zereimaisum.app.ui.components.PasswordField
import com.zereimaisum.app.ui.components.PrimaryButton
import com.zereimaisum.app.ui.components.SecondaryButton
import com.zereimaisum.app.ui.components.ZmLogo
import com.zereimaisum.app.ui.components.ZmTextField
import com.zereimaisum.app.ui.navigation.zmViewModel
import com.zereimaisum.app.ui.theme.ZmColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    /** Código de recuperação a exibir uma única vez, junto do usuário criado. */
    val pendingCode: Pair<Long, String>? = null,
)

class AuthViewModel(private val repo: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state = _state.asStateFlow()

    private fun run(block: suspend () -> AuthResult) {
        _state.value = AuthUiState(loading = true)
        viewModelScope.launch {
            _state.value = when (val r = block()) {
                is AuthResult.Error -> AuthUiState(error = r.message)
                is AuthResult.Success -> AuthUiState(pendingCode = r.recoveryCode?.let { r.userId to it })
            }
        }
    }

    fun signUp(name: String, email: String, password: String, phone: String) = run { repo.signUp(name, email, password, phone) }
    fun login(email: String, password: String) = run { repo.login(email, password) }
    fun reset(email: String, code: String, newPassword: String) = run { repo.resetPassword(email, code, newPassword) }

    fun confirmCodeSaved() {
        val userId = _state.value.pendingCode?.first ?: return
        viewModelScope.launch { repo.startSession(userId) }
    }

    fun clearError() { _state.value = _state.value.copy(error = null) }
}

/** Grafo de telas para quem não está logado. */
@Composable
fun AuthNavHost() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "welcome") {
        composable("welcome") {
            WelcomeScreen(onStart = { nav.navigate("signup") }, onLogin = { nav.navigate("login") })
        }
        composable("signup") { SignUpScreen(onBack = { nav.popBackStack() }) }
        composable("login") {
            LoginScreen(onBack = { nav.popBackStack() }, onForgot = { nav.navigate("recovery") })
        }
        composable("recovery") { RecoveryScreen(onBack = { nav.popBackStack() }) }
    }
}

@Composable
fun WelcomeScreen(onStart: () -> Unit, onLogin: () -> Unit) {
    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(ZmColors.Navy, ZmColors.Blue)))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        ZmLogo(120.dp)
        Spacer(Modifier.height(24.dp))
        Text("Zerei Mais Um", style = MaterialTheme.typography.headlineLarge, color = ZmColors.White)
        Text("Controle Financeiro", style = MaterialTheme.typography.titleMedium, color = ZmColors.LightBlue)
        Spacer(Modifier.height(12.dp))
        Text("Organize. Planeje. Conquiste.", style = MaterialTheme.typography.bodyLarge, color = ZmColors.Green)
        Spacer(Modifier.height(56.dp))
        PrimaryButton("Começar", onStart)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onLogin) { Text("Já tenho conta — Entrar", color = ZmColors.White) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text(title) }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") }
        })
    }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { content() }
    }
}

@Composable
fun SignUpScreen(onBack: () -> Unit) {
    val vm = zmViewModel { AuthViewModel(it.auth) }
    val state by vm.state.collectAsState()
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    AuthScaffold("Criar conta", onBack) {
        Text("Seus dados ficam guardados com segurança neste aparelho.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        ZmTextField(name, { name = it }, "Nome completo")
        ZmTextField(email, { email = it }, "E-mail", keyboardType = KeyboardType.Email)
        ZmTextField(phone, { phone = it }, "Telefone (opcional)", keyboardType = KeyboardType.Phone)
        PasswordField(password, { password = it }, "Senha (8+ caracteres, letras e números)")
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        PrimaryButton("Criar minha conta", { vm.signUp(name, email, password, phone) }, loading = state.loading)
        Text(
            "Não pedimos CPF nem senhas bancárias. O app não movimenta dinheiro real.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    state.pendingCode?.let { (_, code) -> RecoveryCodeDialog(code, vm::confirmCodeSaved) }
}

@Composable
fun LoginScreen(onBack: () -> Unit, onForgot: () -> Unit) {
    val vm = zmViewModel { AuthViewModel(it.auth) }
    val state by vm.state.collectAsState()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    AuthScaffold("Entrar", onBack) {
        ZmTextField(email, { email = it }, "E-mail", keyboardType = KeyboardType.Email)
        PasswordField(password, { password = it })
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        PrimaryButton("Entrar", { vm.login(email, password) }, loading = state.loading)
        TextButton(onClick = onForgot) { Text("Esqueci minha senha") }
    }
}

@Composable
fun RecoveryScreen(onBack: () -> Unit) {
    val vm = zmViewModel { AuthViewModel(it.auth) }
    val state by vm.state.collectAsState()
    var email by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    AuthScaffold("Recuperar senha", onBack) {
        Text("Use o código de recuperação que você anotou ao criar a conta.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        ZmTextField(email, { email = it }, "E-mail", keyboardType = KeyboardType.Email)
        ZmTextField(code, { code = it }, "Código de recuperação", placeholder = "XXXX-XXXX-XXXX")
        PasswordField(password, { password = it }, "Nova senha")
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        PrimaryButton("Redefinir senha", { vm.reset(email, code, password) }, loading = state.loading)
        SecondaryButton("Voltar", onBack)
    }
    state.pendingCode?.let { (_, newCode) -> RecoveryCodeDialog(newCode, vm::confirmCodeSaved, isNew = true) }
}

@Composable
private fun RecoveryCodeDialog(code: String, onConfirm: () -> Unit, isNew: Boolean = false) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(if (isNew) "Seu novo código de recuperação" else "Guarde seu código de recuperação") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Anote este código em um lugar seguro. Ele é a única forma de recuperar sua senha e não será mostrado de novo.")
                SelectionContainer {
                    Text(code, fontFamily = FontFamily.Monospace, fontSize = 24.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(8.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Já anotei, continuar") } },
    )
}
