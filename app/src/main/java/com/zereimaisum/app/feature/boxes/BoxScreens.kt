package com.zereimaisum.app.feature.boxes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zereimaisum.app.core.Dates
import com.zereimaisum.app.core.Money
import com.zereimaisum.app.core.Validators
import com.zereimaisum.app.data.db.BoxEntity
import com.zereimaisum.app.data.db.GoalEntity
import com.zereimaisum.app.domain.BoxMoveKind
import com.zereimaisum.app.domain.BoxRules
import com.zereimaisum.app.ui.components.ChipSelector
import com.zereimaisum.app.ui.components.ConfirmDialog
import com.zereimaisum.app.ui.components.DateField
import com.zereimaisum.app.ui.components.EmptyState
import com.zereimaisum.app.ui.components.MoneyField
import com.zereimaisum.app.ui.components.MoneyRow
import com.zereimaisum.app.ui.components.PrimaryButton
import com.zereimaisum.app.ui.components.ProgressBar
import com.zereimaisum.app.ui.components.ProgressCard
import com.zereimaisum.app.ui.components.SectionTitle
import com.zereimaisum.app.ui.components.ZmIcons
import com.zereimaisum.app.ui.components.ZmTextField
import com.zereimaisum.app.ui.navigation.appContainer
import com.zereimaisum.app.ui.theme.ZmColors
import kotlinx.coroutines.launch

private val boxColors = listOf(0xFF0879E8, 0xFF43D5F2, 0xFF8DEB35, 0xFFF2A541, 0xFF9B6DFF, 0xFFE8638B, 0xFF09295B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxesScreen(userId: Long, onOpenBox: (Long) -> Unit, onNewBox: () -> Unit, onOpenGoal: (Long?) -> Unit) {
    val c = appContainer()
    val boxes by remember(userId) { c.boxes.observe(userId) }.collectAsState(initial = emptyList())
    val goals by remember(userId) { c.goals.observe(userId) }.collectAsState(initial = emptyList())
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(floatingActionButton = {
        FloatingActionButton(onClick = { if (tab == 0) onNewBox() else onOpenGoal(null) }) {
            Icon(Icons.Filled.Add, if (tab == 0) "Nova caixinha" else "Nova meta")
        }
    }) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (tab == 0) "Minhas caixinhas" else "Metas", style = MaterialTheme.typography.headlineMedium)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        listOf("Caixinhas", "Metas").forEachIndexed { i, l ->
                            SegmentedButton(selected = tab == i, onClick = { tab = i }, shape = SegmentedButtonDefaults.itemShape(i, 2)) { Text(l) }
                        }
                    }
                }
            }
            if (tab == 0) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(Modifier.padding(16.dp).fillMaxWidth()) {
                            Text("Total reservado", style = MaterialTheme.typography.bodyMedium)
                            Text(Money.format(boxes.sumOf { it.reservedCents }), style = MaterialTheme.typography.headlineMedium)
                            Text("Reserva virtual conforme seus registros. Sem rendimento e sem bloqueio bancário.",
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                items(boxes, key = { "b${it.id}" }) { b ->
                    ProgressCard(
                        title = b.name, current = b.reservedCents, target = b.targetCents,
                        icon = ZmIcons.of(b.icon), color = Color(b.color),
                        subtitle = b.plannedWithdrawalDate?.let { "Retirada planejada: ${Dates.format(it)}" },
                        onClick = { onOpenBox(b.id) },
                    )
                }
            } else {
                if (goals.isEmpty()) item { EmptyState("Defina sua primeira meta", "Metas ajudam a transformar planos em conquistas.", "Criar meta") { onOpenGoal(null) } }
                items(goals, key = { "g${it.id}" }) { g ->
                    ProgressCard(
                        title = g.title, current = g.currentCents, target = g.targetCents,
                        icon = if (g.completedAt != null) Icons.Filled.EmojiEvents else Icons.Filled.Flag,
                        color = if (g.completedAt != null) ZmColors.Green else MaterialTheme.colorScheme.primary,
                        subtitle = when {
                            g.completedAt != null -> "Meta alcançada! Parabéns!"
                            else -> BoxRules.monthlyNeeded(g.currentCents, g.targetCents, Dates.today(), g.deadline)
                                ?.let { "Guarde ~${Money.format(it)}/mês até ${Dates.format(g.deadline!!)}" }
                        },
                        onClick = { onOpenGoal(g.id) },
                    )
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

private enum class MoveDialog { DEPOSIT, WITHDRAW, TRANSFER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxDetailScreen(userId: Long, boxId: Long, onBack: () -> Unit, onEdit: () -> Unit) {
    val c = appContainer()
    val scope = rememberCoroutineScope()
    val box by remember(boxId) { c.boxes.observeOne(userId, boxId) }.collectAsState(initial = null)
    val all by remember(userId) { c.boxes.observe(userId) }.collectAsState(initial = emptyList())
    val moves by remember(boxId) { c.boxes.observeMovements(boxId) }.collectAsState(initial = emptyList())
    var dialog by remember { mutableStateOf<MoveDialog?>(null) }
    var askDelete by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(box?.name ?: "Caixinha") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
            actions = { IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, "Editar") } },
        )
    }) { pad ->
        val b = box ?: return@Scaffold
        val pct = BoxRules.progressPercent(b.reservedCents, b.targetCents)
        LazyColumn(Modifier.padding(pad).fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card {
                    Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Reservado", style = MaterialTheme.typography.bodyMedium)
                        Text(Money.format(b.reservedCents), style = MaterialTheme.typography.headlineMedium)
                        if (b.targetCents > 0) {
                            ProgressBar(pct, Color(b.color))
                            Text("$pct% da meta de ${Money.format(b.targetCents)}")
                            BoxRules.monthlyNeeded(b.reservedCents, b.targetCents, Dates.today(), b.targetDate)?.let {
                                Text("Para chegar até ${Dates.format(b.targetDate!!)}, guarde ~${Money.format(it)} por mês.")
                            }
                        } else Text("Defina um valor de meta em Editar para acompanhar o progresso.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { dialog = MoveDialog.DEPOSIT }, Modifier.weight(1f)) { Icon(Icons.Filled.ArrowDownward, null); Text(" Adicionar") }
                    OutlinedButton(onClick = { dialog = MoveDialog.WITHDRAW }, Modifier.weight(1f)) { Icon(Icons.Filled.ArrowUpward, null); Text(" Retirar") }
                }
            }
            item { OutlinedButton(onClick = { dialog = MoveDialog.TRANSFER }, Modifier.fillMaxWidth()) { Icon(Icons.Filled.SwapHoriz, null); Text(" Transferir para outra caixinha") } }
            item { Column {
                SectionTitle("Retirada planejada")
                Text("Um compromisso com você mesmo: o app lembra a data, mas não bloqueia dinheiro real.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                ChipSelector(BoxRules.PLANNED_WITHDRAWAL_MONTHS, null, { if (it == 1) "1 mês" else "$it meses" }) { m ->
                    scope.launch { c.boxes.setPlannedWithdrawal(userId, boxId, BoxRules.plannedWithdrawalDate(Dates.today(), m)) }
                }
                Spacer(Modifier.height(8.dp))
                DateField(b.plannedWithdrawalDate, { d -> scope.launch { c.boxes.setPlannedWithdrawal(userId, boxId, d) } }, "Data personalizada", allowClear = true)
            } }
            item { SectionTitle("Histórico") }
            if (moves.isEmpty()) item { Text("Nenhuma movimentação ainda.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(moves, key = { it.id }) { m ->
                val kind = BoxMoveKind.valueOf(m.kind)
                val inbound = kind == BoxMoveKind.DEPOSIT || kind == BoxMoveKind.TRANSFER_IN
                MoneyRow(
                    if (inbound) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                    kind.label, Dates.format(m.date) + (m.note?.let { " · $it" } ?: ""),
                    (if (inbound) "+" else "-") + Money.format(m.amountCents),
                    if (inbound) ZmColors.Income else ZmColors.Expense,
                )
                HorizontalDivider()
            }
            item { TextButton(onClick = { askDelete = true }) { Text("Excluir caixinha", color = MaterialTheme.colorScheme.error) } }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    dialog?.let { kind ->
        MoveAmountDialog(
            title = when (kind) { MoveDialog.DEPOSIT -> "Adicionar à caixinha"; MoveDialog.WITHDRAW -> "Retirar da caixinha"; MoveDialog.TRANSFER -> "Transferir" },
            destinations = if (kind == MoveDialog.TRANSFER) all.filter { it.id != boxId } else emptyList(),
            onDismiss = { dialog = null },
            onConfirm = { amount, toId, note, setError ->
                scope.launch {
                    val today = Dates.today()
                    val err = when (kind) {
                        MoveDialog.DEPOSIT -> c.boxes.deposit(userId, boxId, amount, today, note)
                        MoveDialog.WITHDRAW -> c.boxes.withdraw(userId, boxId, amount, today, note)
                        MoveDialog.TRANSFER -> if (toId == null) "Escolha a caixinha de destino." else c.boxes.transfer(userId, boxId, toId, amount, today)
                    }
                    if (err == null) dialog = null else setError(err)
                }
            },
        )
    }
    if (askDelete) ConfirmDialog("Excluir caixinha?", "O histórico desta caixinha será apagado. O valor reservado deixa de ser contado.", "Excluir",
        onConfirm = { scope.launch { c.boxes.delete(userId, boxId); onBack() } }, onDismiss = { askDelete = false })
}

@Composable
private fun MoveAmountDialog(
    title: String,
    destinations: List<BoxEntity>,
    onDismiss: () -> Unit,
    onConfirm: (amount: Long, toId: Long?, note: String?, setError: (String) -> Unit) -> Unit,
) {
    var amount by remember { mutableLongStateOf(0L) }
    var to by remember { mutableStateOf<BoxEntity?>(null) }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MoneyField(amount, { amount = it; error = null })
                if (destinations.isNotEmpty()) {
                    Text("Destino")
                    ChipSelector(destinations, to, { it.name }) { to = it }
                } else {
                    ZmTextField(note, { note = it.take(Validators.MAX_NOTE) }, "Observação (opcional)")
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val e = Validators.positiveAmount(amount)
                if (e != null) error = e else onConfirm(amount, to?.id, note.trim().ifEmpty { null }) { error = it }
            }) { Text("Confirmar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxFormScreen(userId: Long, editId: Long?, onBack: () -> Unit) {
    val c = appContainer()
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf<BoxEntity?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var icon by rememberSaveable { mutableStateOf("savings") }
    var color by rememberSaveable { mutableLongStateOf(boxColors.first()) }
    var target by rememberSaveable { mutableLongStateOf(0L) }
    var targetDate by rememberSaveable { mutableStateOf<Long?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(editId) {
        if (editId != null) c.boxes.byId(userId, editId)?.let { b ->
            loaded = b; name = b.name; icon = b.icon; color = b.color; target = b.targetCents; targetDate = b.targetDate
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (editId == null) "Nova caixinha" else "Editar caixinha") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
        )
    }) { pad ->
        Column(Modifier.padding(pad).imePadding().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ZmTextField(name, { name = it }, "Nome da caixinha", error = error)
            Text("Ícone", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ZmIcons.boxChoices.forEach { key ->
                    Box(
                        Modifier.size(48.dp).clip(CircleShape)
                            .background(if (icon == key) Color(color).copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { icon = key },
                        contentAlignment = Alignment.Center,
                    ) { Icon(ZmIcons.of(key), contentDescription = key, tint = Color(color)) }
                }
            }
            Text("Cor", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                boxColors.forEach { hex ->
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(Color(hex))
                            .then(if (hex == color) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                            .clickable { color = hex }
                    )
                }
            }
            MoneyField(target, { target = it }, "Valor da meta")
            DateField(targetDate, { targetDate = it }, "Data prevista (opcional)", allowClear = true)
            PrimaryButton("Salvar", {
                error = Validators.requiredText(name, "o nome")
                if (error == null) scope.launch {
                    val base = loaded ?: BoxEntity(userId = userId, name = "", icon = "", color = 0, targetCents = 0, createdAt = 0)
                    c.boxes.save(userId, base.copy(name = name.trim(), icon = icon, color = color, targetCents = target, targetDate = targetDate))
                    onBack()
                }
            })
        }
    }
}

/** Formulário de meta, reutilizado no assistente inicial. */
@Composable
fun GoalForm(userId: Long, editId: Long?, onDone: () -> Unit, submitLabel: String = "Salvar") {
    val c = appContainer()
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf<GoalEntity?>(null) }
    var title by rememberSaveable { mutableStateOf("") }
    var target by rememberSaveable { mutableLongStateOf(0L) }
    var current by rememberSaveable { mutableLongStateOf(0L) }
    var deadline by rememberSaveable { mutableStateOf<Long?>(null) }
    var progress by remember { mutableLongStateOf(0L) }
    var errors by remember { mutableStateOf(mapOf<String, String>()) }
    var askDelete by remember { mutableStateOf(false) }
    var celebrate by remember { mutableStateOf(false) }

    LaunchedEffect(editId) {
        if (editId != null) c.goals.byId(userId, editId)?.let { g ->
            loaded = g; title = g.title; target = g.targetCents; current = g.currentCents; deadline = g.deadline
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ZmTextField(title, { title = it }, "Qual é a meta?", error = errors["title"], placeholder = "Ex.: Trocar de celular à vista")
        MoneyField(target, { target = it }, "Valor da meta", errors["target"])
        if (loaded == null) MoneyField(current, { current = it }, "Quanto já tenho (opcional)")
        DateField(deadline, { deadline = it }, "Prazo (opcional)", allowClear = true)
        PrimaryButton(submitLabel, {
            val e = mutableMapOf<String, String>()
            Validators.requiredText(title, "a meta")?.let { e["title"] = it }
            Validators.positiveAmount(target)?.let { e["target"] = it }
            errors = e
            if (e.isEmpty()) scope.launch {
                val base = loaded ?: GoalEntity(userId = userId, title = "", targetCents = 0, currentCents = current, createdAt = 0)
                c.goals.save(userId, base.copy(title = title.trim(), targetCents = target, deadline = deadline))
                onDone()
            }
        })
        val g = loaded
        if (g != null) {
            HorizontalDivider()
            Text("Progresso: ${Money.format(g.currentCents)} de ${Money.format(g.targetCents)}", style = MaterialTheme.typography.titleMedium)
            ProgressBar(Money.percent(g.currentCents, g.targetCents))
            MoneyField(progress, { progress = it }, "Registrar valor guardado")
            OutlinedButton(onClick = {
                if (progress > 0) scope.launch {
                    val done = c.goals.addProgress(userId, g.id, progress)
                    loaded = c.goals.byId(userId, g.id); progress = 0
                    if (done) celebrate = true
                }
            }, Modifier.fillMaxWidth()) { Text("Adicionar progresso") }
            TextButton(onClick = { askDelete = true }) { Text("Excluir meta", color = MaterialTheme.colorScheme.error) }
        }
    }
    if (askDelete) ConfirmDialog("Excluir meta?", "Esta ação não pode ser desfeita.", "Excluir",
        onConfirm = { scope.launch { loaded?.let { c.goals.delete(userId, it.id) }; onDone() } }, onDismiss = { askDelete = false })
    if (celebrate) AlertDialog(
        onDismissRequest = { celebrate = false },
        icon = { Icon(Icons.Filled.EmojiEvents, null, tint = ZmColors.Green) },
        title = { Text("Meta alcançada!") },
        text = { Text("Você chegou lá. Organize. Planeje. Conquiste!") },
        confirmButton = { TextButton(onClick = { celebrate = false }) { Text("Uhul!") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalFormScreen(userId: Long, editId: Long?, onBack: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (editId == null) "Nova meta" else "Meta") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
        )
    }) { pad ->
        Column(Modifier.padding(pad).imePadding().verticalScroll(rememberScrollState()).padding(16.dp)) {
            GoalForm(userId, editId, onBack)
        }
    }
}
