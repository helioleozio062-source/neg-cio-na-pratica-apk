package com.example.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val activeBusiness by viewModel.activeBusiness.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val dreReport by viewModel.dreReport.collectAsState()
    val products by viewModel.products.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val reservations by viewModel.reservations.collectAsState()
    val services by viewModel.services.collectAsState()

    val currency = activeBusiness?.currencySymbol ?: "MT"
    val activityType = activeBusiness?.activityType ?: "Geral"
    var showBalance by remember { mutableStateOf(true) }
    var showWebInfoDialog by remember { mutableStateOf(false) }
    var showAdminLoginDialog by remember { mutableStateOf(false) }

    val lowStockProducts = remember(products) {
        products.filter { it.currentStock <= it.minStockAlert }
    }

    // Recent 5 periods bar values for chart
    val chartValues = remember(sales) {
        if (sales.isEmpty()) listOf(0.0, 0.0, 0.0, 0.0, 0.0)
        else {
            val list = sales.take(5).map { it.totalAmount }.reversed()
            if (list.size < 5) list + List(5 - list.size) { 0.0 } else list
        }
    }
    val chartLabels = listOf("Seg", "Ter", "Qua", "Qui", "Hoje")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Business Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Olá, ${currentUser?.fullName?.split(" ")?.firstOrNull() ?: "Empreendedor"}!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Painel de controlo de ${activeBusiness?.name ?: "Negócio na Prática"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = EmeraldContainer,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = activeBusiness?.activityType ?: "Geral",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldOnContainer
                        )
                    }
                }
            }
        }

        // Hero Cash Balance Card (Stitch style)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_hero_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurfaceDark),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Saldo Disponível em Caixa",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateTextSecondaryDark
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { showBalance = !showBalance },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (showBalance) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Ocultar/Mostrar Saldo",
                                    tint = SlateTextSecondaryDark,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldPrimary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Caixa Real",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusSuccess,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (showBalance) formatCurrency(dreReport?.currentCashBalance ?: 0.0, currency) else "•••••••• $currency",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 28.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Quick Action Buttons matching Stitch Design System (Adaptado para Serviços e Vendas - Requisitos 4, 5 e 6)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { viewModel.navigateTo(AppScreen.SERVICES) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("quick_action_servicos"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.Handyman, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Serviços", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        FilledTonalButton(
                            onClick = { viewModel.navigateTo(AppScreen.POS_SALES) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("quick_action_vender"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = SlateSurfaceVariantDark,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Vender", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                        }

                        FilledTonalButton(
                            onClick = { viewModel.navigateTo(AppScreen.PRODUCTS_STOCK) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = SlateSurfaceVariantDark,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Stock", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                        }

                        FilledTonalButton(
                            onClick = { viewModel.navigateTo(AppScreen.FINANCE) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = SlateSurfaceVariantDark,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Despesa", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Complaints, Web Portal & Mozambique Currency Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_complaints_web_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(StatusWarning.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ReportProblem,
                                    contentDescription = null,
                                    tint = StatusWarning,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Reclamações & Portal Web",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Moeda Principal: Metical de Moçambique (MT)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EmeraldContainer
                        ) {
                            Text(
                                text = "MT 🇲🇿",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldOnContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Acompanhe o estado das reclamações e utilize o sistema sincronizado no computador via Web ou no telemóvel offline.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.navigateToComplaints() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("action_go_to_complaints"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusWarning),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Feedback, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ver Reclamações", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showWebInfoDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("action_open_web_info"),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Acesso no PC (Web)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { WebExportHelper.exportAndShareIndexHtml(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("action_download_index_html_card"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Baixar Código index.html (Para Criar Web no PC)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    if (currentUser?.role == "ADMIN") {
                        Button(
                            onClick = { viewModel.navigateTo(AppScreen.ADMIN_PANEL) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("action_admin_complaints"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Abrir Painel do Administrador Geral", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        FilledTonalButton(
                            onClick = { showAdminLoginDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("action_admin_switch_open"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp), tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Aceder à Conta do Administrador Geral", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Indicadores Específicos do Ramo de Actividade (Requisito 5)
        item {
            SectionHeader(
                title = "Painel: $activityType",
                actionText = "Ver Stock",
                onActionClick = { viewModel.navigateTo(AppScreen.PRODUCTS_STOCK) }
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when {
                    com.example.util.BusinessProfileUtils.isWeightRelevantBusiness(activityType) -> {
                        val weightCount = products.count { it.isSoldByWeight || com.example.util.BusinessProfileUtils.isWeightUnit(it.unit) }
                        MetricCard(
                            title = "Produtos & Peso",
                            value = "${products.size} itens",
                            icon = Icons.Default.Scale,
                            subtitle = "$weightCount vendido(s) por kg/peso",
                            iconColor = EmeraldPrimary,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.navigateTo(AppScreen.PRODUCTS_STOCK) }
                        )
                        MetricCard(
                            title = "Alertas de Stock",
                            value = "${lowStockProducts.size} baixos",
                            icon = Icons.Default.Warning,
                            subtitle = if (lowStockProducts.isEmpty()) "Stock regularizado" else "Necessitam reposição",
                            iconColor = if (lowStockProducts.isEmpty()) StatusSuccess else AmberTertiary,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.navigateTo(AppScreen.PRODUCTS_STOCK) }
                        )
                    }
                    com.example.util.BusinessProfileUtils.isApparelBusiness(activityType) -> {
                        val totalPieces = products.sumOf { it.currentStock }
                        MetricCard(
                            title = "Peças Disponíveis",
                            value = "${totalPieces.toInt()} peças",
                            icon = Icons.Default.Checkroom,
                            subtitle = "${products.size} modelos cadastrados",
                            iconColor = SkySecondary,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.navigateTo(AppScreen.PRODUCTS_STOCK) }
                        )
                        MetricCard(
                            title = "Alertas de Reposição",
                            value = "${lowStockProducts.size} modelos",
                            icon = Icons.Default.Warning,
                            subtitle = "Tamanhos em falta",
                            iconColor = if (lowStockProducts.isEmpty()) StatusSuccess else StatusError,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.navigateTo(AppScreen.PRODUCTS_STOCK) }
                        )
                    }
                    com.example.util.BusinessProfileUtils.isSalonOrBarber(activityType) -> {
                        MetricCard(
                            title = "Serviços & Agenda",
                            value = "${services.size} serviços",
                            icon = Icons.Default.Handyman,
                            subtitle = "${services.count { it.status == "AGENDADO" }} agendados • ${services.count { it.status == "CONCLUIDO" }} feitos",
                            iconColor = EmeraldPrimary,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.navigateTo(AppScreen.SERVICES) }
                        )
                        MetricCard(
                            title = "Produtos Utilizados",
                            value = "${products.size} itens",
                            icon = Icons.Default.Spa,
                            subtitle = "Materiais de estética",
                            iconColor = SkySecondary,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.navigateTo(AppScreen.PRODUCTS_STOCK) }
                        )
                    }
                    com.example.util.BusinessProfileUtils.isServiceBusiness(activityType) -> {
                        MetricCard(
                            title = "Gestão de Serviços",
                            value = "${services.size} trabalhos",
                            icon = Icons.Default.Handyman,
                            subtitle = "${services.count { it.status == "EM_EXECUCAO" }} em andamento • ${services.count { it.status == "AGENDADO" }} agendados",
                            iconColor = EmeraldPrimary,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.navigateTo(AppScreen.SERVICES) }
                        )
                        MetricCard(
                            title = "Trabalhos Concluídos",
                            value = "${services.count { it.status == "CONCLUIDO" || it.isPastService }} realizados",
                            icon = Icons.Default.CheckCircle,
                            subtitle = "Total: ${formatCurrency(dreReport?.totalServicesRevenue ?: 0.0, currency)}",
                            iconColor = SkySecondary,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.navigateTo(AppScreen.SERVICES) }
                        )
                    }
                    com.example.util.BusinessProfileUtils.isFoodOrRestaurant(activityType) -> {
                        MetricCard(
                            title = "Mesas & Reservas",
                            value = "${reservations.count { it.status == "CONFIRMED" }} mesas",
                            icon = Icons.Default.Restaurant,
                            subtitle = "Clientes agendados",
                            iconColor = SkySecondary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Cardápio & Stock",
                            value = "${products.size} pratos/bebidas",
                            icon = Icons.Default.Fastfood,
                            subtitle = "${lowStockProducts.size} em falta",
                            iconColor = if (lowStockProducts.isEmpty()) EmeraldPrimary else StatusWarning,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.navigateTo(AppScreen.PRODUCTS_STOCK) }
                        )
                    }
                    else -> {
                        MetricCard(
                            title = "Artigos em Stock",
                            value = "${products.size} cadastrados",
                            icon = Icons.Default.Inventory2,
                            subtitle = "${lowStockProducts.size} com alerta",
                            iconColor = EmeraldPrimary,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.navigateTo(AppScreen.PRODUCTS_STOCK) }
                        )
                        MetricCard(
                            title = "Vendas Realizadas",
                            value = "${dreReport?.salesCount ?: 0} vendas",
                            icon = Icons.Default.Receipt,
                            subtitle = "No período seleccionado",
                            iconColor = SkySecondary,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.navigateTo(AppScreen.POS_SALES) }
                        )
                    }
                }
            }
        }

        // Se o negócio presta serviços ou tem serviços registados, exibir card de indicadores de serviços (Requisito 6)
        if (services.isNotEmpty() || com.example.util.BusinessProfileUtils.isServiceBusiness(activityType)) {
            item {
                SectionHeader(
                    title = "Indicadores de Serviços Prestados",
                    actionText = "Ver Todos",
                    onActionClick = { viewModel.navigateTo(AppScreen.SERVICES) }
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Recebido por Serviços",
                        value = formatCurrency(dreReport?.totalServicesReceived ?: 0.0, currency),
                        icon = Icons.Default.Payments,
                        subtitle = "${services.count { it.status == "CONCLUIDO" || it.isPastService }} concluídos",
                        iconColor = EmeraldPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppScreen.SERVICES) }
                    )

                    MetricCard(
                        title = "Serviços a Receber",
                        value = formatCurrency(dreReport?.totalServicesPending ?: 0.0, currency),
                        icon = Icons.Default.PendingActions,
                        subtitle = "${services.count { it.amountPending > 0 }} com saldo pendente",
                        iconColor = AmberTertiary,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppScreen.SERVICES) }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Custos de Serviços",
                        value = formatCurrency(dreReport?.totalServicesExpenses ?: 0.0, currency),
                        icon = Icons.Default.MoneyOff,
                        subtitle = "Peças e materiais gastos",
                        iconColor = StatusError,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppScreen.SERVICES) }
                    )

                    MetricCard(
                        title = "Resultado dos Serviços",
                        value = formatCurrency(dreReport?.estimatedServicesNetResult ?: 0.0, currency),
                        icon = Icons.Default.AccountBalanceWallet,
                        subtitle = "Lucro gerado pelos serviços",
                        iconColor = if ((dreReport?.estimatedServicesNetResult ?: 0.0) >= 0) EmeraldPrimary else StatusError,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateTo(AppScreen.SERVICES) }
                    )
                }
            }
        }

        // Key Performance Indicators (Grid)
        item {
            SectionHeader(
                title = "Resultados Reais do Negócio",
                actionText = "Ver DRE Completo",
                onActionClick = { viewModel.navigateTo(AppScreen.REPORTS_DRE) }
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Vendas do Mês",
                    value = formatCurrency(dreReport?.grossRevenue ?: 0.0, currency),
                    icon = Icons.Default.TrendingUp,
                    subtitle = "${dreReport?.salesCount ?: 0} vendas efetuadas",
                    iconColor = EmeraldPrimary,
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "Lucro Bruto",
                    value = formatCurrency(dreReport?.grossProfit ?: 0.0, currency),
                    icon = Icons.Default.MonetizationOn,
                    subtitle = "Margem: %.1f%%".format(dreReport?.grossMarginPercentage ?: 0.0),
                    iconColor = SkySecondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Despesas Operacionais",
                    value = formatCurrency(dreReport?.operationalExpenses ?: 0.0, currency),
                    icon = Icons.Default.ReceiptLong,
                    subtitle = "Aluguer, salários e contas",
                    iconColor = StatusError,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(AppScreen.FINANCE) }
                )

                MetricCard(
                    title = "Resultado Líquido",
                    value = formatCurrency(dreReport?.netProfit ?: 0.0, currency),
                    icon = Icons.Default.AccountBalance,
                    subtitle = if ((dreReport?.netProfit ?: 0.0) >= 0) "Lucro do período" else "Atenção: Prejuízo",
                    iconColor = if ((dreReport?.netProfit ?: 0.0) >= 0) EmeraldPrimary else StatusError,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "A Receber (Fiado)",
                    value = formatCurrency(dreReport?.pendingReceivables ?: 0.0, currency),
                    icon = Icons.Default.AssignmentReturn,
                    subtitle = "Créditos concedidos a clientes",
                    iconColor = AmberTertiary,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(AppScreen.FINANCE) }
                )

                MetricCard(
                    title = "A Pagar (Fornecedores)",
                    value = formatCurrency(dreReport?.pendingPayables ?: 0.0, currency),
                    icon = Icons.Default.AccountBalanceWallet,
                    subtitle = "Compras a prazo pendentes",
                    iconColor = SkySecondary,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(AppScreen.FINANCE) }
                )
            }
        }

        // Recent Sales Chart
        item {
            SimpleBarChart(
                values = chartValues,
                labels = chartLabels,
                currency = currency
            )
        }

        // Low stock warning alerts
        if (lowStockProducts.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AmberContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = AmberOnContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Alertas de Stock Baixo (${lowStockProducts.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberOnContainer
                                )
                            }
                            TextButton(onClick = { viewModel.navigateTo(AppScreen.PRODUCTS_STOCK) }) {
                                Text("Repor Stock", color = AmberOnContainer, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        lowStockProducts.take(3).forEach { prod ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = prod.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = AmberOnContainer
                                )
                                StockBadge(
                                    currentStock = prod.currentStock,
                                    minAlert = prod.minStockAlert,
                                    unit = prod.unit
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Transactions & Sales
        item {
            SectionHeader(
                title = "Últimas Vendas Registadas",
                actionText = "Ver Todas",
                onActionClick = { viewModel.navigateTo(AppScreen.POS_SALES) }
            )
        }

        if (sales.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Ainda não existem vendas registadas neste negócio.\nToque em 'Vender' para começar!",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(sales.take(4)) { sale ->
                val dateStr = remember(sale.timestamp) {
                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "AO")).format(Date(sale.timestamp))
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (sale.isCancelled) StatusError.copy(alpha = 0.15f)
                                        else if (sale.paymentStatus == "PAID") EmeraldContainer
                                        else AmberContainer
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (sale.isCancelled) Icons.Default.Cancel
                                    else if (sale.paymentStatus == "PAID") Icons.Default.CheckCircle
                                    else Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = if (sale.isCancelled) StatusError
                                    else if (sale.paymentStatus == "PAID") EmeraldPrimary
                                    else AmberTertiary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = sale.customerName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$dateStr • ${if (sale.paymentStatus == "PAID") "À Vista" else "A Crédito"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = formatCurrency(sale.totalAmount, currency),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (sale.isCancelled) StatusError else MaterialTheme.colorScheme.onSurface
                            )
                            if (sale.isCancelled) {
                                Text("Cancelada", style = MaterialTheme.typography.labelSmall, color = StatusError)
                            } else if (sale.paymentStatus == "CREDIT" && sale.amountDue > 0) {
                                Text(
                                    "Falta: ${formatCurrency(sale.amountDue, currency)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AmberTertiary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showWebInfoDialog) {
        AlertDialog(
            onDismissRequest = { showWebInfoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Computer, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Portal Web & Sincronização")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Como aceder às Reclamações e ao Portal Web:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text("1. No Aplicativo Android: Toque no botão 'Ver Reclamações' acima para abrir a aba Reclamações e submeter novos incidentes ou acompanhar a resolução em tempo real.")
                    Text("2. No Computador (Web): Aceda ao portal web em 'web/index.html' no navegador. Ele sincroniza automaticamente com o Firestore na Nuvem e suporta modo offline.")
                    Text("3. Moeda Principal: O sistema utiliza o Metical de Moçambique (MT) como moeda de cálculo e exibição em todas as vendas, compras, stock e relatórios DRE.")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        WebExportHelper.exportAndShareIndexHtml(context)
                        showWebInfoDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Baixar index.html", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWebInfoDialog = false }) {
                    Text("Fechar")
                }
            }
        )
    }

    if (showAdminLoginDialog) {
        var adminCodeInput by remember { mutableStateOf("admin123") }
        var adminError by remember { mutableStateOf<String?>(null) }
        var isVerifying by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isVerifying) showAdminLoginDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Conta de Administrador Geral", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Utilizador: admin@negocionapratica.com",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Introduza o código de segurança do Administrador Geral:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (adminError != null) {
                        Surface(
                            color = StatusError.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = adminError ?: "",
                                color = StatusError,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    OutlinedTextField(
                        value = adminCodeInput,
                        onValueChange = {
                            adminCodeInput = it
                            adminError = null
                        },
                        label = { Text("Código de Administrador") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_admin_switch_code")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isVerifying = true
                        viewModel.switchToAdmin(adminCodeInput) { success, msg ->
                            isVerifying = false
                            if (success) {
                                showAdminLoginDialog = false
                            } else {
                                adminError = msg
                            }
                        }
                    },
                    enabled = !isVerifying && adminCodeInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Entrar como Admin")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdminLoginDialog = false }, enabled = !isVerifying) {
                    Text("Cancelar")
                }
            }
        )
    }
}
