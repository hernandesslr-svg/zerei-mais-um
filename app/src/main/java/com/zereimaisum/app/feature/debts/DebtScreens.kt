package com.zereimaisum.app.feature.debts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zereimaisum.app.core.Dates
import com.zereimaisum.app.core.Money
import com.zereimaisum.app.core.Validators
import com.zereimaisum.app.data.db.DebtEntity
import com.zereimaisum.app.data.repo.toView
import com.zereimaisum.app.domain.DebtRules
import com.zereimaisum.app.domain.DebtStatus
import com.zereimaisum.app.ui.components.ConfirmDialog
import com.zereimaisum.app.ui.components.DateField
import com.zereimaisum.app.ui.components.EmptyState
import com.zereimaisum.app.ui.components.MoneyField
import com.zereimaisum.app.ui.components.MoneyRow
import com.zereimaisum.app.ui.components.PrimaryButton
import com.zereimaisum.app.ui.components.ProgressBar
import com.zereimaisum.app.ui.components.SecondaryButton
import com.zereimaisum.app.ui.components.SectionTitle
import com.zereimaisum.app.ui.components.ZmTextField
import com.zereimaisum.app.ui.navigation.appContainer
import com.zereimaisum.app.ui.theme.ZmColors
import kotlinx.coroutines.launch

@Composable
fun DebtsScreen(userId: Long, onAdd: () -> Unit, onOpen: (Long) -> Unit) {
    val c = appContainer()
    val debts by remember(userId) { c.debts.observe(userId) }.collectAsState(initial = emptyList())
    val views = debts.map { it.toView() }
    val open = views.filter { it.status != DebtStatus.PAID }
    val priority = DebtRules.prioritize(views).firstOrNull()

    Scaffold(floatingActionButton = {
        FloatingActionButton(onClick = onAdd) { Icon(Icons.Filled.Add, "Nova dívida") }
    }) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize()) {
            item {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Dívidas", style = MaterialTheme.typography.headlineMedium)
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(Modifier.padding(16.dp).fillMaxWidth()) {
                            Text("Saldo devedor total", style = MaterialTheme.typography.bodyMedium)
                            Text(Money.format(open.sumOf { it.balanceCents }), style = MaterialTheme.typography.headlineMedium)
                            Text(DebtRules.statusMessage(views), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    if (priority != null && open.size > 1) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Sugestão de prioridade", style = MaterialTheme.typography.titleMedium)
                                Text("${priority.debt.creditor}: ${priority.reason}")
                                Text("É uma orientação baseada nos seus registros, não uma garantia de resultado.",
                                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            if (debts.isEmpty()) {
                item { EmptyState("Nenhuma dívida cadastrada", "Se você tem dívidas, cadastre-as para acompanhar a evolução. Se não tem, ótimo!", "Cadastrar dívida", onAdd) }
            }
            items(debts, key = { it.id }) { d ->
                val v = d.toView()
                Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    MoneyRow(
                        icon = if (v.status == DebtStatus.PAID) Icons.Filled.EmojiEvents else Icons.Filled.MoneyOff,
                        title = d.creditor,
                        subtitle = "${v.status.label} · ${DebtRules.progressPercent(v)}% pago",
                        amount = Money.format(d.balanceCents),
                        amountColor = if (v.status == DebtStatus.PAID) ZmColors.Income else MaterialTheme.colorScheme.onSurface,
                        onClick = { onOpen(d.id) },
                    )
                    ProgressBar(DebtRules.progressPercent(v))
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtDetailScreen(userId: Long, debtId: Long, onBack: () -> Unit, onEdit: () -> Unit) {
    val c = appContainer()
    val scope = rememberCoroutineScope()
    val debt by remember(debtId) { c.debts.observeOne(userId, debtId) }.collectAsState(initial = null)
    val payments by remember(debtId) { c.debts.observePayments(debtId) }.collectAsState(initial = emptyList())
    var showPay by remember { mutableStateOf(false) }
    var showAgreement by remember { mutableStateOf(false) }
    var askDelete by remember { mutableStateOf(false) }
    var celebrate by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(debt?.creditor ?: "Dívida") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
            actions = { IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "Editar") } },
        )
    }) { pad ->
        val d = debt ?: return@Scaffold
        val v = d.toView()
        LazyColumn(Modifier.padding(pad).fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card {
                    Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Saldo devedor", style = MaterialTheme.typography.bodyMedium)
                        Text(Money.format(d.balanceCents), style = MaterialTheme.typography.headlineMedium)
                        ProgressBar(DebtRules.progressPercent(v))
                        Text("${DebtRules.progressPercent(v)}% de ${Money.format(d.originalCents)} já pago")
                        Text("Situação: ${v.status.label}")
                        if (d.monthlyInterestBp > 0) Text("Juros: ${DebtRules.formatRate(d.monthlyInterestBp)} ao mês")
                        d.installmentsTotal?.let { Text("Parcelas: ${d.installmentsPaid} de $it") }
                        d.dueDay?.let { Text("Vencimento: todo dia $it") }
                        d.agreementNote?.let { Text("Acordo: $it") }
                    }
                }
            }
            if (v.status != DebtStatus.PAID) {
                item { PrimaryButton("Registrar pagamento", { showPay = true }) }
                item { SecondaryButton("Registrar acordo", { showAgreement = true }) }
            } else {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                        Text("Zerei mais um! Esta dívida está quitada nos seus registros.", Modifier.padding(16.dp))
                    }
                }
            }
            item { SectionTitle("Histórico de pagamentos") }
            if (payments.isEmpty()) item { Text("Nenhum pagamento registrado.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(payments, key = { it.id }) { p ->
                MoneyRow(Icons.Filled.Payments, Money.format(p.amountCents), Dates.format(p.date) + (p.note?.let { " · $it" } ?: ""), "", ZmColors.Income)
                HorizontalDivider()
            }
            item { TextButton(onClick = { askDelete = true }) { Text("Excluir dívida", color = MaterialTheme.colorScheme.error) } }
        }
    }

    if (showPay) PaymentDialog(
        max = debt?.balanceCents ?: 0,
        onDismiss = { showPay = false },
        onConfirm = { amount, date, note ->
            scope.launch {
                c.debts.registerPayment(userId, debtId, amount, date, note)
                showPay = false
                if (c.debts.byId(userId, debtId)?.status == DebtStatus.PAID.name) celebrate = true
            }
        },
    )
    if (showAgreement) AgreementDialog(
        current = debt?.balanceCents ?: 0,
        onDismiss = { showAgreement = false },
        onConfirm = { balance, parcels, note ->
            scope.launch { c.debts.registerAgreement(userId, debtId, balance, parcels, note); showAgreement = false }
        },
    )
    if (askDelete) ConfirmDialog("Excluir dívida?", "O histórico de pagamentos desta dívida também será removido. As despesas já lançadas continuam no extrato.", "Excluir",
        onConfirm = { scope.launch { c.debts.delete(userId, debtId); onBack() } }, onDismiss = { askDelete = false })
    if (celebrate) AlertDialog(
        onDismissRequest = { celebrate = false },
        icon = { Icon(Icons.Filled.EmojiEvents, null, tint = ZmColors.Green) },
        title = { Text("Zerei mais um!") },
        text = { Text("Parabéns! Você quitou esta dívida. Cada passo conta na sua jornada.") },
        confirmButton = { TextButton(onClick = { celebrate = false }) { Text("Comemorar") } },
    )
}

@Composable
private fun PaymentDialog(max: Long, onDismiss: () -> Unit, onConfirm: (Long, Long, String?) -> Unit) {
    var amount by remember { mutableLongStateOf(0L) }
    var date by remember { mutableLongStateOf(Dates.today()) }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar pagamento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MoneyField(amount, { amount = it }, error = error)
                if (amount > max) Text("Valor acima do saldo: será registrado ${Money.format(max)}.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                DateField(date, { it?.let { d -> date = d } })
                ZmTextField(note, { note = it.take(Validators.MAX_NOTE) }, "Observação (opcional)")
                Text("O pagamento também entra como despesa no extrato, uma única vez.", style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = Validators.positiveAmount(amount)
                if (error == null) onConfirm(amount, date, note.trim().ifEmpty { null })
            }) { Text("Registrar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun AgreementDialog(current: Long, onDismiss: () -> Unit, onConfirm: (Long, Int?, String) -> Unit) {
    var balance by remember { mutableLongStateOf(current) }
    var parcels by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar acordo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MoneyField(balance, { balance = it }, "Novo saldo negociado", error)
                ZmTextField(parcels, { parcels = it.filter(Char::isDigit).take(3) }, "Nº de parcelas (opcional)", keyboardType = KeyboardType.Number)
                ZmTextField(note, { note = it.take(Validators.MAX_NOTE) }, "Condições do acordo", singleLine = false)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = Validators.positiveAmount(balance)
                if (error == null) onConfirm(balance, parcels.toIntOrNull()?.takeIf { it > 0 }, note)
            }) { Text("Salvar acordo") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

/** Formulário de dívida reutilizado no assistente inicial. */
@Composable
fun DebtForm(userId: Long, editId: Long?, onDone: () -> Unit, submitLabel: String = "Salvar") {
    val c = appContainer()
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf<DebtEntity?>(null) }
    var creditor by rememberSaveable { mutableStateOf("") }
    var original by rememberSaveable { mutableLongStateOf(0L) }
    var balance by rememberSaveable { mutableLongStateOf(0L) }
    var rate by rememberSaveable { mutableStateOf("") }
    var parcels by rememberSaveable { mutableStateOf("") }
    var dueDay by rememberSaveable { mutableStateOf("") }
    var errors by remember { mutableStateOf(mapOf<String, String>()) }

    LaunchedEffect(editId) {
        if (editId != null) c.debts.byId(userId, editId)?.let { d ->
            loaded = d; creditor = d.creditor; original = d.originalCents; balance = d.balanceCents
            rate = if (d.monthlyInterestBp > 0) DebtRules.formatRate(d.monthlyInterestBp).removeSuffix("%") else ""
            parcels = d.installmentsTotal?.toString() ?: ""; dueDay = d.dueDay?.toString() ?: ""
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ZmTextField(creditor, { creditor = it }, "Credor (banco, loja, pessoa)", error = errors["creditor"])
        MoneyField(original, { original = it; if (loaded == null && balance == 0L) balance = it }, "Valor original", errors["original"])
        MoneyField(balance, { balance = it }, "Saldo devedor atual", errors["balance"])
        ZmTextField(rate, { rate = it.take(6) }, "Juros ao mês em % (opcional)", error = errors["rate"], keyboardType = KeyboardType.Decimal, placeholder = "Ex.: 2,99")
        ZmTextField(parcels, { parcels = it.filter(Char::isDigit).take(3) }, "Total de parcelas (opcional)", keyboardType = KeyboardType.Number)
        ZmTextField(dueDay, { dueDay = it.filter(Char::isDigit).take(2) }, "Dia do vencimento (opcional)", error = errors["due"], keyboardType = KeyboardType.Number)
        PrimaryButton(submitLabel, {
            val e = mutableMapOf<String, String>()
            Validators.requiredText(creditor, "o credor")?.let { e["creditor"] = it }
            Validators.positiveAmount(original)?.let { e["original"] = it }
            Validators.nonNegativeAmount(balance)?.let { e["balance"] = it }
            val bp = DebtRules.parseRate(rate)
            if (bp == null) e["rate"] = "Taxa inválida (ex.: 2,99)."
            val day = dueDay.toIntOrNull()
            if (dueDay.isNotEmpty() && (day == null || day !in 1..31)) e["due"] = "Dia entre 1 e 31."
            errors = e
            if (e.isEmpty()) scope.launch {
                val base = loaded ?: DebtEntity(userId = userId, creditor = "", originalCents = 0, balanceCents = 0, monthlyInterestBp = 0, installmentsTotal = null, dueDay = null, createdAt = 0)
                val status = when {
                    balance == 0L -> DebtStatus.PAID.name
                    base.status == DebtStatus.PAID.name -> DebtStatus.OPEN.name
                    else -> base.status
                }
                c.debts.save(userId, base.copy(
                    creditor = creditor.trim(), originalCents = original, balanceCents = balance,
                    monthlyInterestBp = bp ?: 0, installmentsTotal = parcels.toIntOrNull()?.takeIf { it > 0 },
                    dueDay = day, status = status,
                ))
                onDone()
            }
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtFormScreen(userId: Long, editId: Long?, onBack: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (editId == null) "Nova dívida" else "Editar dívida") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
        )
    }) { pad ->
        Column(Modifier.padding(pad).imePadding().verticalScroll(rememberScrollState()).padding(16.dp)) {
            DebtForm(userId, editId, onBack)
        }
    }
}
