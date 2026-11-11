package com.zereimaisum.app.feature.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zereimaisum.app.core.Dates
import com.zereimaisum.app.core.Money
import com.zereimaisum.app.core.Validators
import com.zereimaisum.app.data.db.CategoryEntity
import com.zereimaisum.app.data.db.TransactionEntity
import com.zereimaisum.app.data.repo.NewTransaction
import com.zereimaisum.app.data.repo.TransactionRepository
import com.zereimaisum.app.domain.Recurrence
import com.zereimaisum.app.domain.TxKind
import com.zereimaisum.app.ui.components.ChipSelector
import com.zereimaisum.app.ui.components.ConfirmDialog
import com.zereimaisum.app.ui.components.DateField
import com.zereimaisum.app.ui.components.EmptyState
import com.zereimaisum.app.ui.components.MoneyField
import com.zereimaisum.app.ui.components.MoneyRow
import com.zereimaisum.app.ui.components.PrimaryButton
import com.zereimaisum.app.ui.components.ProgressBar
import com.zereimaisum.app.ui.components.ZmIcons
import com.zereimaisum.app.ui.components.ZmTextField
import com.zereimaisum.app.ui.navigation.appContainer
import com.zereimaisum.app.ui.navigation.zmViewModel
import com.zereimaisum.app.ui.theme.ZmColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

enum class TxFilter(val label: String) { ALL("Todas"), INCOME("Receitas"), EXPENSE("Despesas"), PENDING("A pagar") }

data class CategoryTotal(val name: String, val icon: String, val cents: Long, val limit: Long?)

data class TxListState(
    val month: YearMonth = YearMonth.now(),
    val query: String = "",
    val filter: TxFilter = TxFilter.ALL,
    val items: List<TransactionEntity> = emptyList(),
    val categories: Map<Long, CategoryEntity> = emptyMap(),
    val income: Long = 0,
    val expense: Long = 0,
    val byCategory: List<CategoryTotal> = emptyList(),
)

class TransactionsViewModel(private val userId: Long, private val repo: TransactionRepository) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.now())
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(TxFilter.ALL)

    val state = combine(repo.observe(userId), repo.observeCategories(userId), month, query, filter) { txs, cats, m, q, f ->
        val catMap = cats.associateBy { it.id }
        val range = Dates.monthRange(m)
        val inMonth = txs.filter { it.date in range }
        val filtered = inMonth.filter { t ->
            val kindOk = when (f) {
                TxFilter.ALL -> true
                TxFilter.INCOME -> t.kind == TxKind.INCOME.name
                TxFilter.EXPENSE -> t.kind == TxKind.EXPENSE.name
                TxFilter.PENDING -> !t.paid && t.kind == TxKind.EXPENSE.name
            }
            val text = q.trim()
            val queryOk = text.isEmpty() || t.description.contains(text, ignoreCase = true) ||
                (t.categoryId?.let { catMap[it]?.name?.contains(text, ignoreCase = true) } ?: false)
            kindOk && queryOk
        }
        val expenses = inMonth.filter { it.kind == TxKind.EXPENSE.name }
        TxListState(
            month = m, query = q, filter = f, items = filtered, categories = catMap,
            income = inMonth.filter { it.kind == TxKind.INCOME.name }.sumOf { it.amountCents },
            expense = expenses.sumOf { it.amountCents },
            byCategory = expenses.groupBy { it.categoryId }.map { (cid, list) ->
                val c = cid?.let { catMap[it] }
                CategoryTotal(c?.name ?: "Sem categoria", c?.icon ?: "more", list.sumOf { it.amountCents }, c?.monthlyLimitCents)
            }.sortedByDescending { it.cents },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TxListState())

    fun prevMonth() { month.value = month.value.minusMonths(1) }
    fun nextMonth() { month.value = month.value.plusMonths(1) }
    fun setQuery(q: String) { query.value = q.take(60) }
    fun setFilter(f: TxFilter) { filter.value = f }
    fun togglePaid(t: TransactionEntity) = viewModelScope.launch { repo.setPaid(userId, t.id, !t.paid) }
}

@Composable
fun TransactionsScreen(userId: Long, onAdd: () -> Unit, onEdit: (Long) -> Unit) {
    val vm = zmViewModel(key = "tx-$userId") { TransactionsViewModel(userId, it.transactions) }
    val s by vm.state.collectAsState()
    Scaffold(floatingActionButton = {
        FloatingActionButton(onClick = onAdd) { Icon(Icons.Filled.Add, contentDescription = "Nova movimentação") }
    }) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize()) {
            item {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Movimentações", style = MaterialTheme.typography.headlineMedium)
                    MonthSwitcher(s.month, vm::prevMonth, vm::nextMonth)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Totals("Receitas", s.income, ZmColors.Income, Modifier.weight(1f))
                        Totals("Despesas", s.expense, ZmColors.Expense, Modifier.weight(1f))
                    }
                    OutlinedTextField(
                        value = s.query, onValueChange = vm::setQuery, singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                        placeholder = { Text("Pesquisar descrição ou categoria") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ChipSelector(TxFilter.entries, s.filter, { it.label }, vm::setFilter)
                }
            }
            if (s.items.isEmpty()) {
                item { EmptyState("Nada por aqui ainda", "Registre receitas e despesas para acompanhar seu mês.", "Adicionar", onAdd) }
            }
            items(s.items, key = { it.id }) { t ->
                val cat = t.categoryId?.let { s.categories[it] }
                val income = t.kind == TxKind.INCOME.name
                MoneyRow(
                    icon = ZmIcons.of(cat?.icon ?: "more"),
                    title = t.description,
                    subtitle = buildString {
                        append(Dates.format(t.date)); cat?.let { append(" · ${it.name}") }
                        if (!t.paid) append(if (income) " · a receber" else " · a pagar")
                    },
                    amount = (if (income) "+" else "-") + Money.format(t.amountCents),
                    amountColor = if (income) ZmColors.Income else ZmColors.Expense,
                    onClick = { onEdit(t.id) },
                    trailing = { Checkbox(checked = t.paid, onCheckedChange = { vm.togglePaid(t) }) },
                )
                HorizontalDivider()
            }
            if (s.byCategory.isNotEmpty()) {
                item {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Despesas por categoria", style = MaterialTheme.typography.titleMedium)
                        val max = s.byCategory.first().cents
                        s.byCategory.forEach { c ->
                            Column {
                                Row { Text(c.name, Modifier.weight(1f)); Text(Money.format(c.cents)) }
                                ProgressBar(Money.percent(c.cents, max), if (c.limit != null && c.cents > c.limit) ZmColors.Expense else MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun MonthSwitcher(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrev) { Icon(Icons.Filled.ChevronLeft, "Mês anterior") }
        Text(Dates.monthLabel(month), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        IconButton(onClick = onNext) { Icon(Icons.Filled.ChevronRight, "Próximo mês") }
    }
}

@Composable
private fun Totals(label: String, cents: Long, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(Money.format(cents), style = MaterialTheme.typography.titleMedium, color = color)
        }
    }
}

/**
 * Formulário de receita/despesa. Usado tanto na aba Movimentações quanto no assistente inicial.
 * [editId] != null abre em modo edição (altera só esta ocorrência).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionForm(
    userId: Long,
    editId: Long?,
    initialKind: TxKind,
    onDone: () -> Unit,
    lockKind: Boolean = false,
    submitLabel: String = "Salvar",
) {
    val c = appContainer()
    val scope = rememberCoroutineScope()
    val categories by c.transactions.observeCategories(userId).collectAsState(initial = emptyList())
    var loaded by remember { mutableStateOf<TransactionEntity?>(null) }
    var kind by rememberSaveable { mutableStateOf(initialKind) }
    var description by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableLongStateOf(0L) }
    var date by rememberSaveable { mutableLongStateOf(Dates.today()) }
    var categoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var paid by rememberSaveable { mutableStateOf(true) }
    var recurrence by rememberSaveable { mutableStateOf(Recurrence.NONE) }
    var installments by rememberSaveable { mutableIntStateOf(1) }
    var errors by remember { mutableStateOf(mapOf<String, String>()) }
    var askDelete by remember { mutableStateOf(false) }

    LaunchedEffect(editId) {
        if (editId != null) c.transactions.byId(userId, editId)?.let { t ->
            loaded = t; kind = TxKind.valueOf(t.kind); description = t.description; amount = t.amountCents
            date = t.date; categoryId = t.categoryId; paid = t.paid
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!lockKind && editId == null) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(TxKind.INCOME to "Receita", TxKind.EXPENSE to "Despesa").forEachIndexed { i, (k, l) ->
                    SegmentedButton(selected = kind == k, onClick = { kind = k; categoryId = null }, shape = SegmentedButtonDefaults.itemShape(i, 2)) { Text(l) }
                }
            }
        }
        ZmTextField(description, { description = it }, "Descrição", error = errors["description"])
        MoneyField(amount, { amount = it }, if (installments > 1) "Valor total" else "Valor", errors["amount"])
        DateField(date, { it?.let { d -> date = d } }, if (kind == TxKind.INCOME) "Data de recebimento" else "Data / vencimento")
        Text("Categoria", style = MaterialTheme.typography.titleMedium)
        val cats = categories.filter { it.kind == kind.name }
        ChipSelector(cats, cats.firstOrNull { it.id == categoryId }, { it.name }) { categoryId = it.id }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (kind == TxKind.INCOME) "Já recebido" else "Já pago", Modifier.weight(1f))
            Switch(checked = paid, onCheckedChange = { paid = it })
        }
        if (editId == null) {
            Text("Frequência", style = MaterialTheme.typography.titleMedium)
            ChipSelector(Recurrence.entries, recurrence, { it.label }) { recurrence = it; if (it != Recurrence.NONE) installments = 1 }
            if (kind == TxKind.EXPENSE && recurrence == Recurrence.NONE) {
                Text("Parcelas", style = MaterialTheme.typography.titleMedium)
                ChipSelector(listOf(1, 2, 3, 4, 5, 6, 10, 12, 18, 24), installments, { if (it == 1) "À vista" else "${it}x" }) { installments = it }
                if (installments > 1 && amount > 0) {
                    Text("${installments}x de ~${Money.format(amount / installments)} (mensais)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else if (loaded?.seriesId != null) {
            Text("Esta movimentação faz parte de uma série; a edição altera só esta ocorrência.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        errors["general"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        PrimaryButton(submitLabel, {
            val e = mutableMapOf<String, String>()
            Validators.requiredText(description, "a descrição")?.let { e["description"] = it }
            Validators.positiveAmount(amount)?.let { e["amount"] = it }
            errors = e
            if (e.isEmpty()) scope.launch {
                val current = loaded
                if (current != null) {
                    c.transactions.update(userId, current.copy(description = description.trim(), amountCents = amount, date = date, categoryId = categoryId, paid = paid))
                } else {
                    c.transactions.add(userId, NewTransaction(kind, description, amount, date, categoryId, paid, recurrence, installments))
                }
                onDone()
            }
        })
        if (loaded != null) {
            TextButton(onClick = { askDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Delete, null); Spacer(Modifier.width(8.dp)); Text("Excluir", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    val current = loaded
    if (askDelete && current != null) {
        if (current.seriesId == null) {
            ConfirmDialog("Excluir movimentação?", "Esta ação não pode ser desfeita.", "Excluir",
                onConfirm = { scope.launch { c.transactions.delete(userId, current.id, false); onDone() } },
                onDismiss = { askDelete = false })
        } else {
            AlertDialog(
                onDismissRequest = { askDelete = false },
                title = { Text("Excluir da série") },
                text = { Text("Deseja excluir só esta ocorrência ou esta e todas as próximas?") },
                confirmButton = {
                    TextButton(onClick = { scope.launch { c.transactions.delete(userId, current.id, true); onDone() } }) { Text("Esta e as próximas") }
                },
                dismissButton = {
                    TextButton(onClick = { scope.launch { c.transactions.delete(userId, current.id, false); onDone() } }) { Text("Só esta") }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(userId: Long, editId: Long?, initialKind: TxKind, onBack: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (editId == null) "Nova movimentação" else "Editar movimentação") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
        )
    }) { pad ->
        Column(Modifier.padding(pad).imePadding().verticalScroll(rememberScrollState()).padding(16.dp)) {
            TransactionForm(userId, editId, initialKind, onDone = onBack)
        }
    }
}
