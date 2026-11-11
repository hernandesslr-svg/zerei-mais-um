package com.zereimaisum.app.ui.navigation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.zereimaisum.app.notifications.BillReminders
import kotlinx.coroutines.flow.first
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zereimaisum.app.data.db.UserEntity
import com.zereimaisum.app.domain.TxKind
import com.zereimaisum.app.feature.auth.AuthNavHost
import com.zereimaisum.app.feature.boxes.BoxDetailScreen
import com.zereimaisum.app.feature.boxes.BoxFormScreen
import com.zereimaisum.app.feature.boxes.BoxesScreen
import com.zereimaisum.app.feature.boxes.GoalFormScreen
import com.zereimaisum.app.feature.dashboard.DashboardScreen
import com.zereimaisum.app.feature.debts.DebtDetailScreen
import com.zereimaisum.app.feature.debts.DebtFormScreen
import com.zereimaisum.app.feature.debts.DebtsScreen
import com.zereimaisum.app.feature.onboarding.OnboardingScreen
import com.zereimaisum.app.feature.profile.ProfileScreen
import com.zereimaisum.app.feature.transactions.TransactionFormScreen
import com.zereimaisum.app.feature.transactions.TransactionsScreen
import com.zereimaisum.app.ui.components.ZmLogo
import kotlinx.coroutines.flow.map

private sealed interface UserLoad {
    data object Loading : UserLoad
    data class Loaded(val user: UserEntity?) : UserLoad
}

/** Decide entre: entrada (não logado), assistente inicial ou app principal. */
@Composable
fun AppRoot() {
    val c = appContainer()
    val userId by c.session.userId.collectAsState(initial = null)
    when (val id = userId) {
        null -> Splash()
        -1L -> AuthNavHost()
        else -> LoggedIn(id)
    }
}

@Composable
private fun Splash() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) { ZmLogo(96.dp) }
}

@Composable
private fun LoggedIn(userId: Long) {
    val c = appContainer()
    val load by remember(userId) { c.users.observe(userId).map<UserEntity?, UserLoad> { UserLoad.Loaded(it) } }
        .collectAsState(initial = UserLoad.Loading)
    LaunchedEffect(userId) { c.transactions.extendRecurrences(userId) }

    when (val l = load) {
        UserLoad.Loading -> Splash()
        is UserLoad.Loaded -> {
            val user = l.user
            if (user == null) {
                // Conta inexistente (ex.: excluída): encerra a sessão.
                LaunchedEffect(Unit) { c.auth.logout() }
                Splash()
            } else if (user.onboardingStep < 5) {
                OnboardingScreen(userId, user.onboardingStep, user.name.substringBefore(' '))
            } else {
                AskNotificationPermission()
                MainScaffold(userId)
            }
        }
    }
}

/** Android 13+: pede a permissão de notificações uma única vez, ao entrar no app. */
@Composable
private fun AskNotificationPermission() {
    if (Build.VERSION.SDK_INT < 33) return
    val c = appContainer()
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (!BillReminders.hasPermission(context) && !c.session.notificationPermissionAsked.first()) {
            c.session.markNotificationPermissionAsked()
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Início", Icons.Filled.Home),
    Tab("transactions", "Movimentações", Icons.AutoMirrored.Filled.ReceiptLong),
    Tab("debts", "Dívidas", Icons.Filled.MoneyOff),
    Tab("boxes", "Caixinhas", Icons.Filled.Savings),
    Tab("profile", "Perfil", Icons.Filled.Person),
)

private fun NavHostController.switchTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
private fun MainScaffold(userId: Long) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route
    val showBar = tabs.any { it.route == current }

    Scaffold(bottomBar = {
        if (showBar) NavigationBar {
            tabs.forEach { t ->
                NavigationBarItem(
                    selected = current == t.route,
                    onClick = { nav.switchTab(t.route) },
                    icon = { Icon(t.icon, contentDescription = t.label) },
                    label = { Text(t.label, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
    }) { pad ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(pad)) {
            composable("home") {
                DashboardScreen(
                    userId,
                    onAddTx = { k -> nav.navigate("tx_form?kind=${k.name}") },
                    onOpenDebts = { nav.switchTab("debts") },
                    onOpenBoxes = { nav.switchTab("boxes") },
                    onOpenGoal = { id -> nav.navigate("goal_form?id=$id") },
                    onOpenTransactions = { nav.switchTab("transactions") },
                )
            }
            composable("transactions") {
                TransactionsScreen(userId, onAdd = { nav.navigate("tx_form") }, onEdit = { id -> nav.navigate("tx_form?id=$id") })
            }
            composable(
                "tx_form?id={id}&kind={kind}",
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("kind") { type = NavType.StringType; defaultValue = TxKind.EXPENSE.name },
                ),
            ) { e ->
                val id = e.arguments?.getLong("id")?.takeIf { it > 0 }
                val kind = runCatching { TxKind.valueOf(e.arguments?.getString("kind") ?: "") }.getOrDefault(TxKind.EXPENSE)
                TransactionFormScreen(userId, id, kind, onBack = { nav.popBackStack() })
            }
            composable("debts") {
                DebtsScreen(userId, onAdd = { nav.navigate("debt_form") }, onOpen = { id -> nav.navigate("debt/$id") })
            }
            composable("debt/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                val id = e.arguments!!.getLong("id")
                DebtDetailScreen(userId, id, onBack = { nav.popBackStack() }, onEdit = { nav.navigate("debt_form?id=$id") })
            }
            composable("debt_form?id={id}", arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L })) { e ->
                DebtFormScreen(userId, e.arguments?.getLong("id")?.takeIf { it > 0 }, onBack = { nav.popBackStack() })
            }
            composable("boxes") {
                BoxesScreen(
                    userId,
                    onOpenBox = { id -> nav.navigate("box/$id") },
                    onNewBox = { nav.navigate("box_form") },
                    onOpenGoal = { id -> nav.navigate(if (id == null) "goal_form" else "goal_form?id=$id") },
                )
            }
            composable("box/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                val id = e.arguments!!.getLong("id")
                BoxDetailScreen(userId, id, onBack = { nav.popBackStack() }, onEdit = { nav.navigate("box_form?id=$id") })
            }
            composable("box_form?id={id}", arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L })) { e ->
                BoxFormScreen(userId, e.arguments?.getLong("id")?.takeIf { it > 0 }, onBack = { nav.popBackStack() })
            }
            composable("goal_form?id={id}", arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L })) { e ->
                GoalFormScreen(userId, e.arguments?.getLong("id")?.takeIf { it > 0 }, onBack = { nav.popBackStack() })
            }
            composable("profile") { ProfileScreen(userId) }
        }
    }
}
