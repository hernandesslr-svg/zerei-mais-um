package com.zereimaisum.app.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zereimaisum.app.core.Money
import com.zereimaisum.app.domain.TxKind
import com.zereimaisum.app.feature.boxes.GoalForm
import com.zereimaisum.app.feature.debts.DebtForm
import com.zereimaisum.app.feature.transactions.TransactionForm
import com.zereimaisum.app.ui.components.PrimaryButton
import com.zereimaisum.app.ui.components.StepIndicator
import com.zereimaisum.app.ui.navigation.appContainer
import com.zereimaisum.app.ui.theme.ZmColors
import kotlinx.coroutines.launch

private val stepLabels = listOf("Receitas", "Despesas", "Dívidas", "Patrimônio", "Objetivos")

/**
 * Assistente de configuração inicial em 5 etapas.
 * Cada item é salvo na hora; o progresso da etapa fica no perfil, então dá para
 * fechar o app e continuar depois. Todas as etapas podem ser puladas e editadas mais tarde.
 */
@Composable
fun OnboardingScreen(userId: Long, startStep: Int, firstName: String, onFinished: () -> Unit = {}) {
    val c = appContainer()
    val scope = rememberCoroutineScope()
    var step by rememberSaveable { mutableIntStateOf(startStep.coerceIn(0, 4)) }
    var formKey by remember { mutableIntStateOf(0) }
    val txs by remember(userId) { c.transactions.observe(userId) }.collectAsState(initial = emptyList())
    val debts by remember(userId) { c.debts.observe(userId) }.collectAsState(initial = emptyList())
    val goals by remember(userId) { c.goals.observe(userId) }.collectAsState(initial = emptyList())

    fun goTo(next: Int) {
        scope.launch {
            c.users.setOnboardingStep(userId, next)
            if (next >= 5) onFinished() else { step = next; formKey++ }
        }
    }

    Scaffold { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(if (step == 0) "Olá, $firstName! Vamos organizar tudo?" else stepLabels[step], style = MaterialTheme.typography.headlineMedium)
            StepIndicator(step, 5, stepLabels)

            val added: List<String> = when (step) {
                // Recorrências e parcelas aparecem uma única vez (agrupadas pela série).
                0, 1 -> txs.filter { it.kind == (if (step == 0) TxKind.INCOME else TxKind.EXPENSE).name }
                    .distinctBy { it.seriesId ?: "id-${it.id}" }
                    .map { "${it.description.substringBefore(" (")} · ${Money.format(it.amountCents)}" }
                2 -> debts.map { "${it.creditor} · ${Money.format(it.balanceCents)}" }
                4 -> goals.map { "${it.title} · ${Money.format(it.targetCents)}" }
                else -> emptyList()
            }
            if (added.isNotEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Column(Modifier.padding(12.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        added.distinct().take(8).forEach {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, null, tint = ZmColors.Green)
                                Text("  $it")
                            }
                        }
                    }
                }
            }

            key(step, formKey) {
                when (step) {
                    0 -> {
                        Text("Cadastre salário, benefícios, comissões, trabalho autônomo e outras rendas. Use a frequência para rendas que se repetem.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TransactionForm(userId, null, TxKind.INCOME, onDone = { formKey++ }, lockKind = true, submitLabel = "Adicionar receita")
                    }
                    1 -> {
                        Text("Aluguel, água, energia, internet, mercado, transporte... Contas fixas podem ser mensais.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TransactionForm(userId, null, TxKind.EXPENSE, onDone = { formKey++ }, lockKind = true, submitLabel = "Adicionar despesa")
                    }
                    2 -> {
                        Text("Cadastre dívidas para acompanhar a evolução. Não tem dívidas? Ótimo, é só pular esta etapa.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        DebtForm(userId, null, onDone = { formKey++ }, submitLabel = "Adicionar dívida")
                    }
                    3 -> {
                        Card {
                            Text(
                                "O cadastro de bens, imóveis, veículos e valores guardados chega na próxima versão (Fase 2). " +
                                    "Por enquanto, registre o que você tem guardado nas caixinhas, como a Emergência.",
                                Modifier.padding(16.dp),
                            )
                        }
                    }
                    4 -> {
                        Text("Defina metas. Suas 5 caixinhas iniciais (Emergência, Viagem, Reforma, Lazer e Objetivos pessoais) já foram criadas.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        GoalForm(userId, null, onDone = { formKey++ }, submitLabel = "Adicionar meta")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            PrimaryButton(if (step == 4) "Concluir e ir para o painel" else "Próxima etapa", { goTo(step + 1) })
            Row(Modifier.fillMaxWidth()) {
                if (step > 0) TextButton(onClick = { step -= 1; formKey++ }) { Text("Voltar") }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { goTo(5) }) { Text("Pular e configurar depois") }
            }
        }
    }
}
