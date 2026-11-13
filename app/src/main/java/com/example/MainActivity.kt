package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.model.CashbookType
import com.example.data.repository.HisabRepository
import com.example.ui.screens.AddCashbookEntryDialog
import com.example.ui.screens.AddEditWorkerDialog
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GallaCashbookScreen
import com.example.ui.screens.StaffAttendanceScreen
import com.example.ui.screens.WorkerDetailScreen
import com.example.ui.theme.LocalHisabColors
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.HisabUiState
import com.example.ui.viewmodel.HisabViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_STAFF = "staff"
const val ROUTE_GALLA = "galla"
const val ROUTE_WORKER_DETAIL = "worker_detail/{workerId}"

private data class NavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            HisabMainShell(
                currentUserId = user.uid,
                userDisplayName = user.displayName ?: user.email ?: "Shop Owner",
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HisabMainShell(
    currentUserId: String,
    userDisplayName: String,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val hisabColors = LocalHisabColors.current

    val viewModel: HisabViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                HisabViewModel(HisabRepository(db), currentUserId)
            }
        },
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    var showAddWorkerDialog by remember { mutableStateOf(false) }
    var cashbookDialogType by remember { mutableStateOf<CashbookType?>(null) }

    if (showAddWorkerDialog) {
        AddEditWorkerDialog(
            onDismiss = { showAddWorkerDialog = false },
            onSave = { name, phone, role, wage, joiningDate, _ ->
                viewModel.addWorker(name, phone, role, wage, joiningDate) {
                    showAddWorkerDialog = false
                }
            },
        )
    }

    cashbookDialogType?.let { initialType ->
        AddCashbookEntryDialog(
            initialType = initialType,
            onDismiss = { cashbookDialogType = null },
            onConfirm = { type, category, amount, paymentMode, date, note ->
                viewModel.addCashbookEntry(type, category, amount, paymentMode, date, note) {
                    cashbookDialogType = null
                }
            },
        )
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_DASHBOARD
    val isTopLevel = currentRoute in setOf(ROUTE_DASHBOARD, ROUTE_STAFF, ROUTE_GALLA)

    val tabs = remember {
        listOf(
            NavTab(
                route = ROUTE_DASHBOARD,
                label = "Home • Aaj",
                selectedIcon = Icons.Filled.Dashboard,
                unselectedIcon = Icons.Outlined.Dashboard,
                tag = "nav_dashboard",
            ),
            NavTab(
                route = ROUTE_STAFF,
                label = "Staff & Haziri",
                selectedIcon = Icons.Filled.Groups,
                unselectedIcon = Icons.Outlined.Groups,
                tag = "nav_staff",
            ),
            NavTab(
                route = ROUTE_GALLA,
                label = "Galla • Cash",
                selectedIcon = Icons.Filled.AccountBalanceWallet,
                unselectedIcon = Icons.Outlined.AccountBalanceWallet,
                tag = "nav_galla",
            ),
        )
    }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(ROUTE_DASHBOARD) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.navigationBars,
            topBar = {
                if (isTopLevel) {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                        ),
                        title = {
                            val firstName = userDisplayName.substringBefore(" ").ifBlank { "Owner" }
                            Column {
                                Text(
                                    text = "Namaste, $firstName",
                                    style = MaterialTheme.typography.titleLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "Shop & Labour Hisab",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        actions = {
                            val pending = (uiState as? HisabUiState.Ready)?.hasPendingWrites == true
                            Surface(
                                shape = RoundedCornerShape(99.dp),
                                color = if (pending) hisabColors.warningContainer else hisabColors.incomeContainer,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = if (pending) Icons.Filled.CloudSync else Icons.Filled.CloudDone,
                                        contentDescription = if (pending) "Syncing pending writes" else "Cloud synced",
                                        tint = if (pending) hisabColors.warning else hisabColors.income,
                                        modifier = Modifier.size(15.dp),
                                    )
                                    Text(
                                        text = if (pending) "Offline Queue" else "Synced",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (pending) hisabColors.onWarningContainer else hisabColors.onIncomeContainer,
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    signOutUser(credentialManager, scope)
                                },
                                modifier = Modifier.testTag("sign_out_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = "Sign out",
                                )
                            }
                        },
                    )
                }
            },
            bottomBar = {
                if (isTopLevel && !useRail) {
                    NavigationBar(windowInsets = WindowInsets.navigationBars) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.testTag(tab.tag),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (isTopLevel && useRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        Spacer(Modifier.weight(0.2f))
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.tag),
                            )
                        }
                        Spacer(Modifier.weight(1f))
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 840.dp)
                            .fillMaxSize(),
                    ) {
                        when (val state = uiState) {
                            is HisabUiState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        CircularProgressIndicator()
                                        Text(
                                            text = "Hisab load ho raha hai...",
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                }
                            }

                            is HisabUiState.Error -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        Text(
                                            text = "Sync Error",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                        Text(
                                            text = state.message,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                        Button(onClick = { signOutUser(credentialManager, scope) }) {
                                            Text("Sign out & retry")
                                        }
                                    }
                                }
                            }

                            is HisabUiState.Ready -> {
                                NavHost(
                                    navController = navController,
                                    startDestination = ROUTE_DASHBOARD,
                                ) {
                                    composable(ROUTE_DASHBOARD) {
                                        DashboardScreen(
                                            state = state,
                                            onMarkAttendanceClick = { navigateTopLevel(ROUTE_STAFF) },
                                            onAddCashbookClick = { cashbookDialogType = CashbookType.INCOME },
                                            onAddLabourClick = { showAddWorkerDialog = true },
                                            onOpenWorkerDetail = { workerId ->
                                                navController.navigate("worker_detail/$workerId")
                                            },
                                            onOpenGallaTab = { navigateTopLevel(ROUTE_GALLA) },
                                        )
                                    }
                                    composable(ROUTE_STAFF) {
                                        StaffAttendanceScreen(
                                            state = state,
                                            onSelectDate = viewModel::setAttendanceDate,
                                            onMarkAttendance = viewModel::markAttendance,
                                            onOpenWorkerDetail = { workerId ->
                                                navController.navigate("worker_detail/$workerId")
                                            },
                                            onAddWorkerClick = { showAddWorkerDialog = true },
                                        )
                                    }
                                    composable(ROUTE_GALLA) {
                                        GallaCashbookScreen(
                                            state = state,
                                            onSelectPeriod = viewModel::setCashbookPeriod,
                                            onAddEntryClick = { type -> cashbookDialogType = type },
                                            onDeleteEntry = viewModel::deleteCashbookEntry,
                                        )
                                    }
                                    composable(
                                        route = ROUTE_WORKER_DETAIL,
                                        arguments = listOf(navArgument("workerId") { type = NavType.StringType }),
                                    ) { entry ->
                                        val workerId = entry.arguments?.getString("workerId").orEmpty()
                                        WorkerDetailScreen(
                                            workerId = workerId,
                                            state = state,
                                            shopOwnerLabel = userDisplayName,
                                            onBack = { navController.popBackStack() },
                                            onUpdateWorker = { w, n, p, r, wage, join, active ->
                                                viewModel.updateWorker(w, n, p, r, wage, join, active)
                                            },
                                            onDeleteWorker = { w -> viewModel.deleteWorker(w) },
                                            onMarkAttendance = viewModel::markAttendance,
                                            onAddTransaction = { w, type, amt, date, mode, note ->
                                                viewModel.addLabourTransaction(w, type, amt, date, mode, note)
                                            },
                                            onDeleteTransaction = viewModel::deleteLabourTransaction,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
