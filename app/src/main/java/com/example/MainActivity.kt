package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.remote.FirebaseSyncManager
import com.example.data.repository.AppRepository
import com.example.ui.screens.admin.AdminScreen
import com.example.ui.screens.auth.BusinessSetupScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.auth.RegisterScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.debts.DebtsScreen
import com.example.ui.screens.feedback.FeedbackScreen
import com.example.ui.screens.finance.FinanceScreen
import com.example.ui.screens.products.ProductsScreen
import com.example.ui.screens.reports.ReportsScreen
import com.example.ui.screens.sales.SalesScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SlateSurfaceDark
import com.example.ui.theme.SlateTextSecondaryDark
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val database = remember { AppDatabase.getDatabase(context) }
            val repository = remember { AppRepository(database) }
            val syncManager = remember { FirebaseSyncManager(context) }
            val viewModel: MainViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return MainViewModel(repository, syncManager) as T
                    }
                }
            )
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            MyApplicationTheme(darkTheme = isDarkMode) {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val activeBusiness by viewModel.activeBusiness.collectAsState()
    val uiMessage by viewModel.uiMessage.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUiMessage()
        }
    }

    // Handle back button behavior for sub-screens
    BackHandler(enabled = currentScreen != AppScreen.LOGIN && currentScreen != AppScreen.DASHBOARD) {
        when (currentScreen) {
            AppScreen.REGISTER -> viewModel.navigateTo(AppScreen.LOGIN)
            AppScreen.BUSINESS_SETUP -> {
                if (currentUser != null && activeBusiness != null) {
                    viewModel.navigateTo(AppScreen.DASHBOARD)
                } else {
                    viewModel.navigateTo(AppScreen.LOGIN)
                }
            }
            AppScreen.ADMIN_PANEL,
            AppScreen.FEEDBACK,
            AppScreen.SETTINGS,
            AppScreen.SERVICES,
            AppScreen.PRODUCTS_STOCK,
            AppScreen.POS_SALES,
            AppScreen.DEBTS,
            AppScreen.FINANCE,
            AppScreen.REPORTS_DRE -> viewModel.navigateTo(AppScreen.DASHBOARD)
            else -> {}
        }
    }

    val isAuthFlow = currentScreen == AppScreen.LOGIN ||
            currentScreen == AppScreen.REGISTER ||
            currentScreen == AppScreen.BUSINESS_SETUP

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 720.dp

        if (isAuthFlow) {
            // Auth flow is centered with quick dark mode toggle on top-right
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = { viewModel.toggleDarkMode() },
                            modifier = Modifier.testTag("toggle_dark_mode_auth")
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = if (isDarkMode) "Modo Claro" else "Modo Noturno",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    when (currentScreen) {
                        AppScreen.LOGIN -> LoginScreen(viewModel = viewModel)
                        AppScreen.REGISTER -> RegisterScreen(viewModel = viewModel)
                        AppScreen.BUSINESS_SETUP -> BusinessSetupScreen(viewModel = viewModel)
                        else -> {}
                    }
                }
            }
        } else if (isWideScreen) {
            // Desktop / Tablet Layout with Left Sidebar matching Stitch Desktop designs
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = SlateSurfaceDark,
                    contentColor = Color.White,
                    header = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Storefront, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Negócio na Prática",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
                    },
                    modifier = Modifier.fillMaxHeight().width(96.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    NavigationRailItem(
                        selected = currentScreen == AppScreen.DASHBOARD,
                        onClick = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                        label = { Text("Painel", fontSize = 10.sp) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            unselectedIconColor = SlateTextSecondaryDark,
                            unselectedTextColor = SlateTextSecondaryDark
                        )
                    )
                    NavigationRailItem(
                        selected = currentScreen == AppScreen.SERVICES,
                        onClick = { viewModel.navigateTo(AppScreen.SERVICES) },
                        icon = { Icon(Icons.Default.Handyman, contentDescription = null) },
                        label = { Text("Serviços", fontSize = 10.sp) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            unselectedIconColor = SlateTextSecondaryDark,
                            unselectedTextColor = SlateTextSecondaryDark
                        )
                    )
                    NavigationRailItem(
                        selected = currentScreen == AppScreen.PRODUCTS_STOCK,
                        onClick = { viewModel.navigateTo(AppScreen.PRODUCTS_STOCK) },
                        icon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                        label = { Text("Stock", fontSize = 10.sp) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            unselectedIconColor = SlateTextSecondaryDark,
                            unselectedTextColor = SlateTextSecondaryDark
                        )
                    )
                    NavigationRailItem(
                        selected = currentScreen == AppScreen.POS_SALES,
                        onClick = { viewModel.navigateTo(AppScreen.POS_SALES) },
                        icon = { Icon(Icons.Default.PointOfSale, contentDescription = null) },
                        label = { Text("Vender", fontSize = 10.sp) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            unselectedIconColor = SlateTextSecondaryDark,
                            unselectedTextColor = SlateTextSecondaryDark
                        )
                    )
                    NavigationRailItem(
                        selected = currentScreen == AppScreen.DEBTS,
                        onClick = { viewModel.navigateTo(AppScreen.DEBTS) },
                        icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                        label = { Text("Dívidas", fontSize = 10.sp) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            unselectedIconColor = SlateTextSecondaryDark,
                            unselectedTextColor = SlateTextSecondaryDark
                        )
                    )
                    NavigationRailItem(
                        selected = currentScreen == AppScreen.FINANCE,
                        onClick = { viewModel.navigateTo(AppScreen.FINANCE) },
                        icon = { Icon(Icons.Default.Payments, contentDescription = null) },
                        label = { Text("Finanças", fontSize = 10.sp) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            unselectedIconColor = SlateTextSecondaryDark,
                            unselectedTextColor = SlateTextSecondaryDark
                        )
                    )
                    NavigationRailItem(
                        selected = currentScreen == AppScreen.REPORTS_DRE,
                        onClick = { viewModel.navigateTo(AppScreen.REPORTS_DRE) },
                        icon = { Icon(Icons.Default.Assessment, contentDescription = null) },
                        label = { Text("DRE", fontSize = 10.sp) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            unselectedIconColor = SlateTextSecondaryDark,
                            unselectedTextColor = SlateTextSecondaryDark
                        )
                    )
                    NavigationRailItem(
                        selected = currentScreen == AppScreen.FEEDBACK,
                        onClick = { viewModel.navigateTo(AppScreen.FEEDBACK) },
                        icon = { Icon(Icons.Default.Feedback, contentDescription = null) },
                        label = { Text("Suporte", fontSize = 10.sp) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            unselectedIconColor = SlateTextSecondaryDark,
                            unselectedTextColor = SlateTextSecondaryDark
                        )
                    )
                    if (currentUser?.role == "ADMIN") {
                        NavigationRailItem(
                            selected = currentScreen == AppScreen.ADMIN_PANEL,
                            onClick = { viewModel.navigateTo(AppScreen.ADMIN_PANEL) },
                            icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) },
                            label = { Text("Admin", fontSize = 10.sp) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = EmeraldPrimary,
                                selectedTextColor = EmeraldPrimary,
                                unselectedIconColor = SlateTextSecondaryDark,
                                unselectedTextColor = SlateTextSecondaryDark
                            )
                        )
                    }
                    NavigationRailItem(
                        selected = currentScreen == AppScreen.SETTINGS,
                        onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                        icon = { Icon(Icons.Default.Store, contentDescription = null) },
                        label = { Text("Negócio", fontSize = 10.sp) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            unselectedIconColor = SlateTextSecondaryDark,
                            unselectedTextColor = SlateTextSecondaryDark
                        )
                    )
                }

                Scaffold(
                    modifier = Modifier.weight(1f),
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = "Negócio na Prática",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    activeBusiness?.let {
                                        Text(
                                            text = "${it.name} • ${it.currencySymbol}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            actions = {
                                val syncStatus by viewModel.syncStatus.collectAsState()
                                IconButton(
                                    onClick = { viewModel.syncWithCloud() },
                                    modifier = Modifier.testTag("action_cloud_sync_desktop")
                                ) {
                                    if (syncStatus.inProgress) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = EmeraldPrimary
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.CloudSync,
                                            contentDescription = "Sincronizar Cloud",
                                            tint = if (syncStatus.errorMessage != null) Color.Red else EmeraldPrimary
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.toggleDarkMode() },
                                    modifier = Modifier.testTag("toggle_dark_mode_desktop")
                                ) {
                                    Icon(
                                        imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        contentDescription = if (isDarkMode) "Modo Claro" else "Modo Noturno",
                                        tint = if (isDarkMode) Color.Yellow else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                TextButton(onClick = { viewModel.navigateTo(AppScreen.SETTINGS) }) {
                                    Text(
                                        text = activeBusiness?.name ?: "Mudar Negócio",
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        ScreenRouter(currentScreen, viewModel)
                    }
                }
            }
        } else {
            // Mobile Phone Layout with Bottom NavigationBar
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "Negócio na Prática",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                activeBusiness?.let {
                                    Text(
                                        text = "${it.name} • ${it.currencySymbol}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        actions = {
                            val syncStatus by viewModel.syncStatus.collectAsState()
                            IconButton(
                                onClick = { viewModel.syncWithCloud() },
                                modifier = Modifier.testTag("action_cloud_sync_mobile")
                            ) {
                                if (syncStatus.inProgress) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = EmeraldPrimary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = "Sincronizar Cloud",
                                        tint = if (syncStatus.errorMessage != null) Color.Red else EmeraldPrimary
                                    )
                                }
                            }
                            IconButton(
                                onClick = { viewModel.toggleDarkMode() },
                                modifier = Modifier.testTag("toggle_dark_mode_mobile")
                            ) {
                                Icon(
                                    imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = if (isDarkMode) "Modo Claro" else "Modo Noturno",
                                    tint = if (isDarkMode) Color.Yellow else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(
                                onClick = { viewModel.navigateTo(AppScreen.FEEDBACK) },
                                modifier = Modifier.testTag("nav_feedback")
                            ) {
                                Icon(Icons.Default.HelpOutline, contentDescription = "Suporte e Avaliação")
                            }
                            if (currentUser?.role == "ADMIN") {
                                IconButton(
                                    onClick = { viewModel.navigateTo(AppScreen.ADMIN_PANEL) },
                                    modifier = Modifier.testTag("nav_admin")
                                ) {
                                    Icon(
                                        Icons.Default.AdminPanelSettings,
                                        contentDescription = "Painel do Administrador Geral",
                                        tint = EmeraldPrimary
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        NavigationBarItem(
                            selected = currentScreen == AppScreen.DASHBOARD,
                            onClick = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                            icon = { Icon(if (currentScreen == AppScreen.DASHBOARD) Icons.Default.Dashboard else Icons.Outlined.Dashboard, contentDescription = null) },
                            label = { Text("Painel") },
                            modifier = Modifier.testTag("nav_dashboard")
                        )
                        NavigationBarItem(
                            selected = currentScreen == AppScreen.SERVICES,
                            onClick = { viewModel.navigateTo(AppScreen.SERVICES) },
                            icon = { Icon(if (currentScreen == AppScreen.SERVICES) Icons.Default.Handyman else Icons.Outlined.Handyman, contentDescription = null) },
                            label = { Text("Serviços") },
                            modifier = Modifier.testTag("nav_services")
                        )
                        NavigationBarItem(
                            selected = currentScreen == AppScreen.PRODUCTS_STOCK,
                            onClick = { viewModel.navigateTo(AppScreen.PRODUCTS_STOCK) },
                            icon = { Icon(if (currentScreen == AppScreen.PRODUCTS_STOCK) Icons.Default.Inventory2 else Icons.Outlined.Inventory2, contentDescription = null) },
                            label = { Text("Stock") },
                            modifier = Modifier.testTag("nav_stock")
                        )
                        NavigationBarItem(
                            selected = currentScreen == AppScreen.POS_SALES,
                            onClick = { viewModel.navigateTo(AppScreen.POS_SALES) },
                            icon = { Icon(if (currentScreen == AppScreen.POS_SALES) Icons.Default.PointOfSale else Icons.Outlined.PointOfSale, contentDescription = null) },
                            label = { Text("Vender") },
                            modifier = Modifier.testTag("nav_sales")
                        )
                        NavigationBarItem(
                            selected = currentScreen == AppScreen.DEBTS,
                            onClick = { viewModel.navigateTo(AppScreen.DEBTS) },
                            icon = { Icon(if (currentScreen == AppScreen.DEBTS) Icons.Default.ReceiptLong else Icons.Outlined.ReceiptLong, contentDescription = null) },
                            label = { Text("Dívidas") },
                            modifier = Modifier.testTag("nav_debts")
                        )
                        NavigationBarItem(
                            selected = currentScreen == AppScreen.FINANCE,
                            onClick = { viewModel.navigateTo(AppScreen.FINANCE) },
                            icon = { Icon(if (currentScreen == AppScreen.FINANCE) Icons.Default.Payments else Icons.Outlined.Payments, contentDescription = null) },
                            label = { Text("Finanças") },
                            modifier = Modifier.testTag("nav_finance")
                        )
                        NavigationBarItem(
                            selected = currentScreen == AppScreen.REPORTS_DRE,
                            onClick = { viewModel.navigateTo(AppScreen.REPORTS_DRE) },
                            icon = { Icon(if (currentScreen == AppScreen.REPORTS_DRE) Icons.Default.Assessment else Icons.Outlined.Assessment, contentDescription = null) },
                            label = { Text("DRE") },
                            modifier = Modifier.testTag("nav_reports")
                        )
                        NavigationBarItem(
                            selected = currentScreen == AppScreen.SETTINGS,
                            onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                            icon = { Icon(if (currentScreen == AppScreen.SETTINGS) Icons.Default.Store else Icons.Outlined.Store, contentDescription = null) },
                            label = { Text("Negócio") },
                            modifier = Modifier.testTag("nav_settings")
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    ScreenRouter(currentScreen, viewModel)
                }
            }
        }
    }
}

@Composable
fun ScreenRouter(currentScreen: AppScreen, viewModel: MainViewModel) {
    when (currentScreen) {
        AppScreen.LOGIN -> LoginScreen(viewModel = viewModel)
        AppScreen.REGISTER -> RegisterScreen(viewModel = viewModel)
        AppScreen.BUSINESS_SETUP -> BusinessSetupScreen(viewModel = viewModel)
        AppScreen.DASHBOARD -> DashboardScreen(viewModel = viewModel)
        AppScreen.SERVICES -> com.example.ui.screens.services.ServicesScreen(viewModel = viewModel)
        AppScreen.PRODUCTS_STOCK -> ProductsScreen(viewModel = viewModel)
        AppScreen.POS_SALES -> SalesScreen(viewModel = viewModel)
        AppScreen.DEBTS -> DebtsScreen(viewModel = viewModel)
        AppScreen.FINANCE -> FinanceScreen(viewModel = viewModel)
        AppScreen.REPORTS_DRE -> ReportsScreen(viewModel = viewModel)
        AppScreen.FEEDBACK -> FeedbackScreen(viewModel = viewModel)
        AppScreen.ADMIN_PANEL -> AdminScreen(viewModel = viewModel)
        AppScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
    }
}
