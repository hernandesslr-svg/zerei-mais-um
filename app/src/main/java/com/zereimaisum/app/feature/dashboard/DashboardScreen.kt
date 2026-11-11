package com.zereimaisum.app.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zereimaisum.app.core.Dates
import com.zereimaisum.app.core.Money
import com.zereimaisum.app.data.db.GoalEntity
import com.zereimaisum.app.data.db.UserEntity
import com.zereimaisum.app.data.repo.toView
import com.zereimaisum.app.di.AppContainer
import com.zereimaisum.app.domain.DashboardCalculator
import com.zereimaisum.app.domain.DebtRules
import com.zereimaisum.app.domain.DebtStatus
import com.zereimaisum.app.domain.MonthSummary
import com.zereimaisum.app.domain.TxKind
import com.zereimaisum.app.domain.TxView
import com.zereimaisum.app.ui.components.MoneyRow
import com.zereimaisum.app.ui.components.ProgressCard
import com.zereimaisum.app.ui.components.SectionTitle
import com.zereimaisum.app.ui.components.SummaryCard
import com.zereimaisum.app.ui.navigation.zmViewModel
import com.zereimaisum.app.ui.theme.ZmColors
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.YearMonth

data class DashboardState(
    val user: UserEntity? = null,
    val summary: MonthSummary? = null,
    val openDebtsCents: Long = 0,
    val debtMessage: String = "",
    val upcoming: List<TxView> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
    val tips: List<String> = emptyList(),
)

class DashboardViewModel(private val userId: Long, private val c: AppContainer) : ViewModel() {
    val state = combine(
        c.users.observe(userId), c.transactions.observe(userId), c.debts.observe(userId),
        c.boxes.observe(userId), c.goals.observe(userId),
    ) { user, txs, debts, boxes, goals ->
        val today = Dates.today()
        val views = txs.map { it.toView() }
        val summary = DashboardCalculator.summarize(views, YearMonth.now(), boxes.sumOf { it.reservedCents }, today)
        val debtViews = debts.map { it.toView() }
        val upcoming = DashboardCalculator.upcomingBills(views, today)
        DashboardState(
            user = user,
            summary = summary,
            openDebtsCents = debtViews.filter { it.status != DebtStatus.PAID }.sumOf { it.balanceCents },
            debtMessage = DebtRules.statusMessage(debtViews),
            upcoming = upcoming,
            goals = goals.filter { it.completedAt == null }.take(3),
            tips = buildTips(summary, upcoming, today),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    /** Alertas simples baseados em regras verificáveis (o motor completo chega na Fase 2). */
    private fun buildTips(s: MonthSummary, upcoming: List<TxView>, today: Long): List<String> = buildList {
        val overdue = upcoming.count { it.epochDay < today }
        if (overdue > 0) add("Opa! Você tem $overdue conta(s) vencida(s). Que tal regularizar ou marcar como paga?")
        if (s.incomeCents > 0 && s.expenseCents > s.incomeCents)
            add("Atenção: as despesas do mês já passam das receitas em ${Money.format(s.expenseCents - s.incomeCents)}.")
        if (s.incomeCents > 0 && s.expenseCents in 1 until s.incomeCents)
            add("Muito bem! Você está gastando menos do que ganha neste mês. Que tal reservar parte da sobra numa caixinha?")
        if (s.incomeCents == 0L && s.expenseCents == 0L)
            add("Registre suas receitas e despesas do mês para receber análises do seu orçamento.")
    }

    fun markPaid(id: Long) = viewModelScope.launch { c.transactions.setPaid(userId, id, true) }
}

@Composable
fun DashboardScreen(
    userId: Long,
    onAddTx: (TxKind) -> Unit,
    onOpenDebts: () -> Unit,
    onOpenBoxes: () -> Unit,
    onOpenGoal: (Long) -> Unit,
    onOpenTransactions: () -> Unit,
) {
    val vm = zmViewModel(key = "dash-$userId") { DashboardViewModel(userId, it) }
    val s by vm.state.collectAsState()
    var menu by remember { mutableStateOf(false) }
    val firstName = s.user?.name?.substringBefore(' ') ?: ""

    Scaffold(floatingActionButton = {
        Column(horizontalAlignment = Alignment.End) {
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Nova receita") }, leadingIcon = { Icon(Icons.Filled.ArrowDownward, null) }, onClick = { menu = false; onAddTx(TxKind.INCOME) })
                DropdownMenuItem(text = { Text("Nova despesa") }, leadingIcon = { Icon(Icons.Filled.ArrowUpward, null) }, onClick = { menu = false; onAddTx(TxKind.EXPENSE) })
            }
            ExtendedFloatingActionButton(onClick = { menu = true }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Lançar") })
        }
    }) { pad ->
        val sum = s.summary
        LazyColumn(Modifier.padding(pad).fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Column(Modifier.padding(top = 16.dp)) {
                    Text(DashboardCalculator.greeting(LocalTime.now().hour, firstName), style = MaterialTheme.typography.headlineMedium)
                    Text(Dates.monthLabel(YearMonth.now()), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(ZmColors.Navy, ZmColors.Blue))).padding(20.dp)
                ) {
                    Text("Saldo disponível (calculado)", color = ZmColors.LightBlue)
                    Text(Money.format(sum?.availableCents ?: 0), style = MaterialTheme.typography.headlineLarge, color = ZmColors.White)
                    Spacer(Modifier.height(6.dp))
                    Text("Recebido − pago até hoje, menos ${Money.format(sum?.reservedCents ?: 0)} reservados nas caixinhas.",
                        style = MaterialTheme.typography.bodyMedium, color = ZmColors.White.copy(alpha = 0.85f))
                    Text("Saldo do mês: ${Money.format(sum?.monthBalanceCents ?: 0)}", style = MaterialTheme.typography.bodyMedium, color = ZmColors.Green)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryCard("Receitas do mês", Money.format(sum?.incomeCents ?: 0), Icons.Filled.ArrowDownward, ZmColors.Income, Modifier.weight(1f), onClick = onOpenTransactions)
                    SummaryCard("Despesas do mês", Money.format(sum?.expenseCents ?: 0), Icons.Filled.ArrowUpward, ZmColors.Expense, Modifier.weight(1f), onClick = onOpenTransactions)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryCard("Dívidas pendentes", Money.format(s.openDebtsCents), Icons.Filled.MoneyOff, ZmColors.Warning, Modifier.weight(1f), subtitle = s.debtMessage, onClick = onOpenDebts)
                    SummaryCard("Valores reservados", Money.format(sum?.reservedCents ?: 0), Icons.Filled.Savings, ZmColors.Blue, Modifier.weight(1f), subtitle = "Nas suas caixinhas", onClick = onOpenBoxes)
                }
            }
            if (s.tips.isNotEmpty()) {
                item { SectionTitle("Alertas") }
                items(s.tips) { tip ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Lightbulb, null, tint = MaterialTheme.colorScheme.primary)
                            Text("  $tip", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            item { SectionTitle("Contas próximas do vencimento", "Ver todas", onOpenTransactions) }
            if (s.upcoming.isEmpty()) item { Text("Nenhuma conta a pagar nos próximos 7 dias.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(s.upcoming.take(5), key = { "u${it.id}" }) { t ->
                val overdue = t.epochDay < Dates.today()
                MoneyRow(
                    Icons.Filled.Schedule, t.description,
                    (if (overdue) "Venceu em " else "Vence em ") + Dates.format(t.epochDay),
                    Money.format(t.amountCents), if (overdue) ZmColors.Expense else MaterialTheme.colorScheme.onSurface,
                    trailing = { Checkbox(checked = false, onCheckedChange = { vm.markPaid(t.id) }) },
                )
            }
            item { SectionTitle("Progresso das metas", "Ver metas", onOpenBoxes) }
            if (s.goals.isEmpty()) item { Text("Nenhuma meta em andamento.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(s.goals, key = { "g${it.id}" }) { g ->
                ProgressCard(g.title, g.currentCents, g.targetCents, Icons.Filled.Flag, MaterialTheme.colorScheme.primary, onClick = { onOpenGoal(g.id) })
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.EmojiEvents, null, tint = ZmColors.Green)
                        Text("  Cada conta registrada é um passo. Organize. Planeje. Conquiste.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item { Spacer(Modifier.height(88.dp)) }
        }
    }
}
