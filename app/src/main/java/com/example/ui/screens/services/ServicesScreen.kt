package com.example.ui.screens.services

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ServiceCatalogEntity
import com.example.data.model.ServiceEntity
import com.example.data.model.ServiceExpenseEntity
import com.example.data.model.ServicePaymentEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SectionHeader
import com.example.ui.components.formatCurrency
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.BusinessProfileUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesScreen(viewModel: MainViewModel) {
    val activeBusiness by viewModel.activeBusiness.collectAsState()
    val services by viewModel.services.collectAsState()
    val serviceCatalog by viewModel.serviceCatalog.collectAsState()
    val dreReport by viewModel.dreReport.collectAsState()
    val currency = activeBusiness?.currencySymbol ?: "MT"
    val activityType = activeBusiness?.activityType ?: "Geral"

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Todos / Agenda, 1: Agendados & Futuros, 2: Em Execução, 3: Concluídos, 4: Catálogo
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<String?>(null) } // null = Todos, "AGENDADO", "PENDENTE", "EM_EXECUCAO", "CONCLUIDO", "CANCELADO"
    var showRegisterServiceDialog by remember { mutableStateOf(false) }
    var isRegisteringPastService by remember { mutableStateOf(false) }
    var showCatalogDialog by remember { mutableStateOf(false) }
    var selectedServiceForDetail by remember { mutableStateOf<ServiceEntity?>(null) }
    var serviceForPayment by remember { mutableStateOf<ServiceEntity?>(null) }
    var serviceForExpense by remember { mutableStateOf<ServiceEntity?>(null) }

    // Filtragem avançada (Requisito 7: pesquisa por cliente, serviço, filtro por estado, etc.)
    val filteredServices = remember(services, searchQuery, statusFilter, selectedTab) {
        services.filter { srv ->
            val matchesQuery = searchQuery.isBlank() ||
                    srv.title.contains(searchQuery, ignoreCase = true) ||
                    srv.customerName.contains(searchQuery, ignoreCase = true) ||
                    srv.customerPhone.contains(searchQuery) ||
                    srv.description.contains(searchQuery, ignoreCase = true) ||
                    srv.targetEquipmentOrVehicle.contains(searchQuery, ignoreCase = true)

            val matchesTab = when (selectedTab) {
                1 -> srv.status == "AGENDADO" || srv.status == "PENDENTE"
                2 -> srv.status == "EM_EXECUCAO"
                3 -> srv.status == "CONCLUIDO" || srv.isPastService
                else -> true
            }

            val matchesStatus = statusFilter == null || srv.status == statusFilter

            matchesQuery && matchesTab && matchesStatus
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            if (selectedTab == 4) {
                ExtendedFloatingActionButton(
                    onClick = { showCatalogDialog = true },
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Novo no Catálogo", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("fab_add_catalog_item")
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(
                        onClick = {
                            isRegisteringPastService = true
                            showRegisterServiceDialog = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = SlateSurfaceVariantDark,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("btn_register_past_service")
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Serviço Já Realizado", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    ExtendedFloatingActionButton(
                        onClick = {
                            isRegisteringPastService = false
                            showRegisterServiceDialog = true
                        },
                        containerColor = EmeraldPrimary,
                        contentColor = Color.White,
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("Novo Serviço", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("fab_new_service")
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header e Resumo Adaptativo para Serviços
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("services_hero_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurfaceDark),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = BusinessProfileUtils.getActivityIcon(activityType),
                                        fontSize = 20.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Gestão de Serviços",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = "${activeBusiness?.name ?: "Negócio"} • ${activeBusiness?.activityType}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SlateTextSecondaryDark
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = EmeraldPrimary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "${services.size} Serviços",
                                    color = StatusSuccess,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Métricas financeiras reais dos serviços (Requisito 2 e 6)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Total Já Recebido
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.12f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Já Recebido", color = SlateTextSecondaryDark, fontSize = 10.sp)
                                    Text(
                                        text = formatCurrency(dreReport?.totalServicesReceived ?: 0.0, currency),
                                        color = EmeraldPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            // Total Ainda por Receber
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.12f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("A Receber", color = SlateTextSecondaryDark, fontSize = 10.sp)
                                    Text(
                                        text = formatCurrency(dreReport?.totalServicesPending ?: 0.0, currency),
                                        color = AmberTertiary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            // Despesas dos Serviços
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.12f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Custos/Desp.", color = SlateTextSecondaryDark, fontSize = 10.sp)
                                    Text(
                                        text = formatCurrency(dreReport?.totalServicesExpenses ?: 0.0, currency),
                                        color = StatusError,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Barra de Navegação de Abas (Agenda, Execução, Concluídos, Catálogo)
            item {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = {}
                ) {
                    val tabs = listOf(
                        "Todos (${services.size})",
                        "Futuros / Agenda (${services.count { it.status == "AGENDADO" || it.status == "PENDENTE" }})",
                        "Em Execução (${services.count { it.status == "EM_EXECUCAO" }})",
                        "Concluídos (${services.count { it.status == "CONCLUIDO" || it.isPastService }})",
                        "Catálogo Habitual (${serviceCatalog.size})"
                    )

                    tabs.forEachIndexed { index, label ->
                        val isSelected = selectedTab == index
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTab = index },
                            label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("tab_services_$index"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Campo de Pesquisa e Filtro de Estado (quando não está na aba do catálogo)
            if (selectedTab != 4) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Pesquisar por cliente, serviço ou contacto...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Limpar")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_search_services"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        // Chips de Estados Rápidos
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = statusFilter == null,
                                    onClick = { statusFilter = null },
                                    label = { Text("Todos Estados", fontSize = 11.sp) }
                                )
                            }
                            val statuses = listOf(
                                "AGENDADO" to "📅 Agendado",
                                "PENDENTE" to "⏳ Pendente",
                                "EM_EXECUCAO" to "⚙️ Em Execução",
                                "CONCLUIDO" to "✅ Concluído",
                                "CANCELADO" to "❌ Cancelado"
                            )
                            items(statuses) { (code, label) ->
                                FilterChip(
                                    selected = statusFilter == code,
                                    onClick = { statusFilter = if (statusFilter == code) null else code },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }

            // Conteúdo principal
            if (selectedTab == 4) {
                // Catálogo de Serviços
                if (serviceCatalog.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Default.Category,
                            title = "Nenhum serviço no catálogo",
                            description = "Cadastre os serviços que o seu negócio habitualmente presta (ex: Cortes, Tranças, Reparações, Formatação) para agilizar o registo.",
                            actionLabel = "Cadastrar Primeiro Serviço",
                            onAction = { showCatalogDialog = true },
                            modifier = Modifier.testTag("empty_catalog")
                        )
                    }
                } else {
                    item {
                        SectionHeader(
                            title = "Serviços Pré-cadastrados",
                            actionText = "+ Adicionar",
                            onActionClick = { showCatalogDialog = true }
                        )
                    }
                    items(serviceCatalog, key = { it.id }) { item ->
                        ServiceCatalogCard(
                            item = item,
                            currency = currency,
                            onUseToCreate = {
                                isRegisteringPastService = false
                                showRegisterServiceDialog = true
                            },
                            onDelete = { viewModel.deleteServiceCatalogItem(item) }
                        )
                    }
                }
            } else {
                // Lista de Serviços
                if (filteredServices.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Default.Build,
                            title = if (searchQuery.isNotBlank()) "Nenhum serviço encontrado" else "Nenhum serviço registado nesta secção",
                            description = if (searchQuery.isNotBlank()) "Tente pesquisar com outro termo ou limpe os filtros." else "Registe um novo agendamento ou um trabalho que já tenha prestado para acompanhar no sistema.",
                            actionLabel = "Registar Serviço Agora",
                            onAction = {
                                isRegisteringPastService = false
                                showRegisterServiceDialog = true
                            },
                            modifier = Modifier.testTag("empty_services_list")
                        )
                    }
                } else {
                    items(filteredServices, key = { it.id }) { srv ->
                        ServiceCard(
                            service = srv,
                            currency = currency,
                            onClick = { selectedServiceForDetail = srv },
                            onStatusChange = { newStatus -> viewModel.updateServiceStatus(srv.id, newStatus) },
                            onAddPayment = { serviceForPayment = srv },
                            onAddExpense = { serviceForExpense = srv }
                        )
                    }
                }
            }
        }
    }

    // Modal de Registo de Novo Serviço ou Serviço Já Realizado (Requisitos 1.A e 1.C)
    if (showRegisterServiceDialog) {
        RegisterServiceDialog(
            isPastService = isRegisteringPastService,
            currency = currency,
            activityType = activityType,
            catalogItems = serviceCatalog,
            onDismiss = { showRegisterServiceDialog = false },
            onSave = { serviceCatalogId, title, category, customerName, phone, desc, status, scheduledDate, scheduledTime, completedDate, location, price, initialPayment, estimatedExpenses, paymentMethod, notes, targetDetail ->
                viewModel.registerService(
                    serviceCatalogId = serviceCatalogId,
                    title = title,
                    category = category,
                    customerName = customerName,
                    customerPhone = phone,
                    description = desc,
                    status = status,
                    scheduledDate = scheduledDate,
                    scheduledTime = scheduledTime,
                    completedDate = completedDate,
                    location = location,
                    agreedPrice = price,
                    initialPayment = initialPayment,
                    estimatedExpenses = estimatedExpenses,
                    paymentMethod = paymentMethod,
                    notes = notes,
                    targetEquipmentOrVehicle = targetDetail,
                    isPastService = isRegisteringPastService,
                    onSuccess = { showRegisterServiceDialog = false }
                )
            }
        )
    }

    // Modal de Cadastro no Catálogo Habitual (Requisito 3)
    if (showCatalogDialog) {
        AddServiceCatalogDialog(
            currency = currency,
            onDismiss = { showCatalogDialog = false },
            onSave = { name, category, price, duration, desc, notes ->
                viewModel.addServiceCatalogItem(
                    name = name,
                    category = category,
                    standardPrice = price,
                    estimatedDurationMinutes = duration,
                    description = desc,
                    notes = notes
                )
                showCatalogDialog = false
            }
        )
    }

    // Modal de Detalhe e Histórico do Serviço Seleccionado
    selectedServiceForDetail?.let { srv ->
        ServiceDetailDialog(
            service = srv,
            currency = currency,
            viewModel = viewModel,
            onDismiss = { selectedServiceForDetail = null },
            onAddPayment = {
                serviceForPayment = srv
            },
            onAddExpense = {
                serviceForExpense = srv
            },
            onStatusChange = { newStatus ->
                viewModel.updateServiceStatus(srv.id, newStatus)
                selectedServiceForDetail = srv.copy(status = newStatus)
            },
            onDelete = {
                viewModel.deleteService(srv)
                selectedServiceForDetail = null
            }
        )
    }

    // Modal de Pagamento de Serviço (Adiantamento, Parcial ou Quitação - Requisito 2)
    serviceForPayment?.let { srv ->
        AddServicePaymentDialog(
            service = srv,
            currency = currency,
            onDismiss = { serviceForPayment = null },
            onConfirm = { amount, method, notes ->
                viewModel.addServicePayment(
                    serviceId = srv.id,
                    amount = amount,
                    paymentMethod = method,
                    notes = notes,
                    onSuccess = {
                        serviceForPayment = null
                        selectedServiceForDetail?.let {
                            selectedServiceForDetail = it.copy(amountReceived = it.amountReceived + amount)
                        }
                    }
                )
            }
        )
    }

    // Modal de Despesa / Custo do Serviço (Requisitos 1.A, 1.C e 2)
    serviceForExpense?.let { srv ->
        AddServiceExpenseDialog(
            service = srv,
            currency = currency,
            onDismiss = { serviceForExpense = null },
            onConfirm = { description, amount, isPaid, method, notes ->
                viewModel.addServiceExpense(
                    serviceId = srv.id,
                    description = description,
                    amount = amount,
                    isPaidNow = isPaid,
                    paymentMethod = method,
                    notes = notes,
                    onSuccess = {
                        serviceForExpense = null
                        selectedServiceForDetail?.let {
                            selectedServiceForDetail = it.copy(estimatedExpenses = it.estimatedExpenses + amount)
                        }
                    }
                )
            }
        )
    }
}

// =========================================================================
// COMPONENTES DE UI DO MÓDULO DE SERVIÇOS
// =========================================================================

@Composable
fun ServiceCard(
    service: ServiceEntity,
    currency: String,
    onClick: () -> Unit,
    onStatusChange: (String) -> Unit,
    onAddPayment: () -> Unit,
    onAddExpense: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("service_card_${service.id}"),
        shape = RoundedCornerShape(16.dp),
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                when (service.status) {
                                    "CONCLUIDO" -> StatusSuccess.copy(alpha = 0.15f)
                                    "EM_EXECUCAO" -> SkySecondary.copy(alpha = 0.15f)
                                    "AGENDADO" -> EmeraldPrimary.copy(alpha = 0.15f)
                                    "CANCELADO" -> StatusError.copy(alpha = 0.15f)
                                    else -> AmberTertiary.copy(alpha = 0.15f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (service.status) {
                                "CONCLUIDO" -> Icons.Default.CheckCircle
                                "EM_EXECUCAO" -> Icons.Default.Pending
                                "AGENDADO" -> Icons.Default.CalendarToday
                                "CANCELADO" -> Icons.Default.Cancel
                                else -> Icons.Default.HourglassEmpty
                            },
                            contentDescription = null,
                            tint = when (service.status) {
                                "CONCLUIDO" -> StatusSuccess
                                "EM_EXECUCAO" -> SkySecondary
                                "AGENDADO" -> EmeraldPrimary
                                "CANCELADO" -> StatusError
                                else -> AmberTertiary
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = service.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Cliente: ${service.customerName}" + if (service.customerPhone.isNotBlank()) " (${service.customerPhone})" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                ServiceStatusBadge(status = service.status)
            }

            if (service.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = service.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            if (service.targetEquipmentOrVehicle.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(14.dp), tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Ref: ${service.targetEquipmentOrVehicle}",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Linha financeira do serviço (Preço acordado, já recebido, pendente, custos)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Preço Acordado", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = formatCurrency(service.agreedPrice, currency),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Recebido", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = formatCurrency(service.amountReceived, currency),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = EmeraldPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "A Receber", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = formatCurrency(service.amountPending, currency),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (service.amountPending > 0) AmberTertiary else StatusSuccess
                    )
                }
            }

            // Agendamento e Ações Rápidas
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (service.scheduledDate.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${service.scheduledDate}${if (service.scheduledTime.isNotBlank()) " às ${service.scheduledTime}" else ""}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (service.completedDate.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(14.dp), tint = StatusSuccess)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Concluído em ${service.completedDate}",
                            fontSize = 11.sp,
                            color = StatusSuccess
                        )
                    }
                } else {
                    Text(text = "Sem data definida", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (service.amountPending > 0) {
                        FilledTonalButton(
                            onClick = onAddPayment,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cobrar", fontSize = 11.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = onAddExpense,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Custo", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceStatusBadge(status: String) {
    val (color, label) = when (status) {
        "AGENDADO" -> EmeraldPrimary to "Agendado"
        "PENDENTE" -> AmberTertiary to "Pendente"
        "EM_EXECUCAO" -> SkySecondary to "Em Execução"
        "CONCLUIDO" -> StatusSuccess to "Concluído"
        "CANCELADO" -> StatusError to "Cancelado"
        else -> MaterialTheme.colorScheme.primary to status
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = label,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun ServiceCatalogCard(
    item: ServiceCatalogEntity,
    currency: String,
    onUseToCreate: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = item.category,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (item.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Preço habitual: ${formatCurrency(item.standardPrice, currency)}",
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Duração: ~${item.estimatedDurationMinutes} min",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Remover", tint = StatusError)
            }
        }
    }
}

// =========================================================================
// DIÁLOGOS DE REGISTO, DETALHE, PAGAMENTO E DESPESA
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterServiceDialog(
    isPastService: Boolean,
    currency: String,
    activityType: String,
    catalogItems: List<ServiceCatalogEntity>,
    onDismiss: () -> Unit,
    onSave: (
        catalogId: String,
        title: String,
        category: String,
        customerName: String,
        phone: String,
        desc: String,
        status: String,
        scheduledDate: String,
        scheduledTime: String,
        completedDate: String,
        location: String,
        price: Double,
        initialPayment: Double,
        estimatedExpenses: Double,
        paymentMethod: String,
        notes: String,
        targetDetail: String
    ) -> Unit
) {
    var selectedCatalogItem by remember { mutableStateOf<ServiceCatalogEntity?>(null) }
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Geral") }
    var customerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(if (isPastService) "CONCLUIDO" else "AGENDADO") }
    var scheduledDate by remember { mutableStateOf(if (!isPastService) SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) else "") }
    var scheduledTime by remember { mutableStateOf(if (!isPastService) "10:00" else "") }
    var completedDate by remember { mutableStateOf(if (isPastService) SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) else "") }
    var location by remember { mutableStateOf("No estabelecimento / Oficina") }
    var priceText by remember { mutableStateOf("") }
    var initialPaymentText by remember { mutableStateOf("") }
    var expensesText by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("CASH") }
    var notes by remember { mutableStateOf("") }
    var targetDetail by remember { mutableStateOf("") } // Veículo, Equipamento ou Especificação

    val price = priceText.toDoubleOrNull() ?: 0.0
    val initialPayment = initialPaymentText.toDoubleOrNull() ?: 0.0
    val estimatedExpenses = expensesText.toDoubleOrNull() ?: 0.0
    val pendingAmount = (price - initialPayment).coerceAtLeast(0.0)
    val estimatedResult = price - estimatedExpenses

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (isPastService) "Registar Serviço Já Realizado" else "Registar Novo Serviço",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = if (isPastService) "Adicione trabalhos anteriores ao histórico e acompanhe pagamentos pendentes." else "Agende e controle o trabalho acordado com o cliente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Selector do Catálogo Habitual (Requisito 3)
                if (catalogItems.isNotEmpty()) {
                    item {
                        Text("Preencher a partir do Catálogo:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(catalogItems) { catItem ->
                                val isSelected = selectedCatalogItem?.id == catItem.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) {
                                            selectedCatalogItem = null
                                        } else {
                                            selectedCatalogItem = catItem
                                            title = catItem.name
                                            category = catItem.category
                                            description = catItem.description
                                            priceText = catItem.standardPrice.toString()
                                        }
                                    },
                                    label = { Text(catItem.name, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                // Nome do Serviço
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Nome do Serviço *") },
                        placeholder = { Text("Ex: Mudança de óleo, Tranças afros, Formatação") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_service_title"),
                        singleLine = true
                    )
                }

                // Cliente e Contacto
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text("Nome do Cliente *") },
                            placeholder = { Text("Ex: João Macamo") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_service_customer"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Contacto (opcional)") },
                            placeholder = { Text("84 / 85 / 87 ...") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_service_phone"),
                            singleLine = true
                        )
                    }
                }

                // Detalhe Adaptativo (Veículo, Equipamento ou Especificação - Requisito 4)
                item {
                    val labelText = when {
                        activityType.contains("Oficina", true) || activityType.contains("Mecânic", true) -> "Veículo / Matrícula / Modelo"
                        activityType.contains("Informática", true) || activityType.contains("Reparação", true) -> "Equipamento / Modelo / Marca"
                        activityType.contains("Fotografia", true) || activityType.contains("Evento", true) -> "Tipo de Evento / Local"
                        else -> "Equipamento / Referência do Cliente (opcional)"
                    }
                    OutlinedTextField(
                        value = targetDetail,
                        onValueChange = { targetDetail = it },
                        label = { Text(labelText) },
                        placeholder = { Text("Ex: Toyota Corolla ABM-123-MC / HP Pavilion") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Estado do Serviço (Requisito 1.B)
                if (!isPastService) {
                    item {
                        Text("Estado Inicial do Serviço:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val statusOptions = listOf(
                                "AGENDADO" to "📅 Agendado",
                                "PENDENTE" to "⏳ Pendente",
                                "EM_EXECUCAO" to "⚙️ Em Execução",
                                "CONCLUIDO" to "✅ Concluído"
                            )
                            items(statusOptions) { (code, label) ->
                                FilterChip(
                                    selected = status == code,
                                    onClick = { status = code },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                // Datas e Horários (Requisito 1.A e 1.C)
                item {
                    if (isPastService) {
                        OutlinedTextField(
                            value = completedDate,
                            onValueChange = { completedDate = it },
                            label = { Text("Data da Realização (AAAA-MM-DD) *") },
                            placeholder = { Text("Ex: 2026-11-10") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = scheduledDate,
                                onValueChange = { scheduledDate = it },
                                label = { Text("Data Prevista") },
                                placeholder = { Text("AAAA-MM-DD") },
                                modifier = Modifier.weight(1.2f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = scheduledTime,
                                onValueChange = { scheduledTime = it },
                                label = { Text("Horário") },
                                placeholder = { Text("14:30") },
                                modifier = Modifier.weight(0.8f),
                                singleLine = true
                            )
                        }
                    }
                }

                // Localização da realização
                item {
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Local de Realização") },
                        placeholder = { Text("Ex: Na oficina, No domicílio do cliente...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Preço acordado e Gestão Financeira (Requisito 2)
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Controlo Financeiro ($currency):", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                            OutlinedTextField(
                                value = priceText,
                                onValueChange = { priceText = it },
                                label = { Text("Preço Total Acordado * ($currency)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_service_price"),
                                singleLine = true
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = initialPaymentText,
                                    onValueChange = { initialPaymentText = it },
                                    label = { Text(if (isPastService) "Valor Recebido" else "Sinal / Adiantamento") },
                                    placeholder = { Text("0.00") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("input_service_initial_payment"),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = expensesText,
                                    onValueChange = { expensesText = it },
                                    label = { Text("Despesas Previstas") },
                                    placeholder = { Text("0.00") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("input_service_expenses"),
                                    singleLine = true
                                )
                            }

                            if (initialPayment > 0) {
                                Text("Forma do Pagamento Recebido:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val methods = listOf(
                                        "CASH" to "Dinheiro",
                                        "MPESA" to "M-Pesa",
                                        "EMOLA" to "e-Mola",
                                        "CARD" to "POS/Cartão",
                                        "TRANSFER" to "Transferência"
                                    )
                                    items(methods) { (mCode, mLabel) ->
                                        FilterChip(
                                            selected = paymentMethod == mCode,
                                            onClick = { paymentMethod = mCode },
                                            label = { Text(mLabel, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }

                            // Resumo automático (Requisito 2: Resultado estimado, valor pendente)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Ainda por Receber:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = formatCurrency(pendingAmount, currency),
                                            fontWeight = FontWeight.Bold,
                                            color = if (pendingAmount > 0) AmberTertiary else StatusSuccess,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Resultado Estimado:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = formatCurrency(estimatedResult, currency),
                                            fontWeight = FontWeight.Bold,
                                            color = if (estimatedResult >= 0) EmeraldPrimary else StatusError,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Descrição detalhada do trabalho
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descrição detalhada do trabalho") },
                        placeholder = { Text("Indique procedimentos, peças a aplicar ou exigências...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }

                // Observações adicionais
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Observações adicionais (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank() || customerName.isBlank() || price <= 0.0) return@Button
                    onSave(
                        selectedCatalogItem?.id ?: "",
                        title,
                        category,
                        customerName,
                        phone,
                        description,
                        status,
                        scheduledDate,
                        scheduledTime,
                        completedDate,
                        location,
                        price,
                        initialPayment,
                        estimatedExpenses,
                        paymentMethod,
                        notes,
                        targetDetail
                    )
                },
                enabled = title.isNotBlank() && customerName.isNotBlank() && price > 0.0,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("btn_confirm_register_service")
            ) {
                Text("Guardar Serviço", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun ServiceDetailDialog(
    service: ServiceEntity,
    currency: String,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onAddPayment: () -> Unit,
    onAddExpense: () -> Unit,
    onStatusChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    var payments by remember { mutableStateOf<List<ServicePaymentEntity>>(emptyList()) }
    var expenses by remember { mutableStateOf<List<ServiceExpenseEntity>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(service.id) {
        payments = viewModel.getPaymentsForService(service.id)
        expenses = viewModel.getExpensesForService(service.id)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = service.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        text = "Cliente: ${service.customerName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ServiceStatusBadge(status = service.status)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Alteração de Estado (Requisito 1.B)
                item {
                    Text("Atualizar Estado do Serviço:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val statuses = listOf(
                            "AGENDADO" to "📅 Agendado",
                            "PENDENTE" to "⏳ Pendente",
                            "EM_EXECUCAO" to "⚙️ Em Execução",
                            "CONCLUIDO" to "✅ Concluído",
                            "CANCELADO" to "❌ Cancelado"
                        )
                        items(statuses) { (code, label) ->
                            FilterChip(
                                selected = service.status == code,
                                onClick = { onStatusChange(code) },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Resumo Financeiro Completo (Requisito 2)
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateSurfaceDark)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Valor Total Acordado:", color = SlateTextSecondaryDark, fontSize = 12.sp)
                                Text(
                                    text = formatCurrency(service.agreedPrice, currency),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Valor Já Recebido:", color = SlateTextSecondaryDark, fontSize = 12.sp)
                                Text(
                                    text = formatCurrency(service.amountReceived, currency),
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Valor Ainda por Receber:", color = SlateTextSecondaryDark, fontSize = 12.sp)
                                Text(
                                    text = formatCurrency(service.amountPending, currency),
                                    fontWeight = FontWeight.Bold,
                                    color = if (service.amountPending > 0) AmberTertiary else StatusSuccess,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Despesas / Custos Associados:", color = SlateTextSecondaryDark, fontSize = 12.sp)
                                Text(
                                    text = formatCurrency(service.estimatedExpenses, currency),
                                    fontWeight = FontWeight.Bold,
                                    color = StatusError,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Resultado Estimado do Serviço:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(
                                    text = formatCurrency(service.estimatedNetResult, currency),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (service.estimatedNetResult >= 0) StatusSuccess else StatusError,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }

                // Detalhes operacionais
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (service.customerPhone.isNotBlank()) {
                            Text("📞 Contacto: ${service.customerPhone}", fontSize = 12.sp)
                        }
                        if (service.location.isNotBlank()) {
                            Text("📍 Local: ${service.location}", fontSize = 12.sp)
                        }
                        if (service.scheduledDate.isNotBlank()) {
                            Text("📅 Agendado para: ${service.scheduledDate} ${service.scheduledTime}", fontSize = 12.sp)
                        }
                        if (service.completedDate.isNotBlank()) {
                            Text("✅ Realizado em: ${service.completedDate}", fontSize = 12.sp, color = StatusSuccess)
                        }
                        if (service.targetEquipmentOrVehicle.isNotBlank()) {
                            Text("🔧 Equipamento/Veículo: ${service.targetEquipmentOrVehicle}", fontSize = 12.sp, color = EmeraldPrimary)
                        }
                        if (service.description.isNotBlank()) {
                            Text("📝 Trabalho: ${service.description}", fontSize = 12.sp)
                        }
                        if (service.notes.isNotBlank()) {
                            Text("💬 Observações: ${service.notes}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Histórico de Pagamentos Recebidos (Requisito 2)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Histórico de Pagamentos:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        if (service.amountPending > 0) {
                            TextButton(onClick = onAddPayment) {
                                Text("+ Registar Pagamento", fontSize = 11.sp, color = EmeraldPrimary)
                            }
                        }
                    }
                    if (payments.isEmpty()) {
                        Text("Nenhum pagamento registado ainda.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            payments.forEach { p ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = "${p.notes.ifBlank { "Pagamento" }} (${BusinessProfileUtils.formatPaymentMethod(p.paymentMethod)})",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(p.paymentDate)),
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "+${formatCurrency(p.amount, currency)}",
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldPrimary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Histórico de Despesas e Custos do Serviço
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Custos e Despesas do Serviço:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        TextButton(onClick = onAddExpense) {
                            Text("+ Associar Despesa", fontSize = 11.sp, color = StatusError)
                        }
                    }
                    if (expenses.isEmpty()) {
                        Text("Nenhum custo associado a este serviço.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            expenses.forEach { exp ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(text = exp.description, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                            Text(
                                                text = if (exp.isPaid) "Pago em ${exp.paymentMethod}" else "Custo previsto (a pagar)",
                                                fontSize = 9.sp,
                                                color = if (exp.isPaid) StatusSuccess else AmberTertiary
                                            )
                                        }
                                        Text(
                                            text = "-${formatCurrency(exp.amount, currency)}",
                                            fontWeight = FontWeight.Bold,
                                            color = StatusError,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fechar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDelete) {
                Text("Eliminar Serviço", color = StatusError)
            }
        }
    )
}

@Composable
fun AddServicePaymentDialog(
    service: ServiceEntity,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, method: String, notes: String) -> Unit
) {
    var amountText by remember { mutableStateOf(service.amountPending.toString()) }
    var paymentMethod by remember { mutableStateOf("CASH") }
    var notes by remember { mutableStateOf(if (service.amountReceived == 0.0) "Pagamento inicial" else "Quitação de serviço") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Registar Pagamento de Serviço", fontWeight = FontWeight.Bold)
                Text(
                    text = "${service.title} — ${service.customerName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = EmeraldPrimary.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Valor Pendente por Receber:", fontSize = 12.sp)
                        Text(
                            text = formatCurrency(service.amountPending, currency),
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary,
                            fontSize = 14.sp
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Valor a Receber ($currency) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Forma de Pagamento:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val methods = listOf(
                        "CASH" to "Dinheiro",
                        "MPESA" to "M-Pesa",
                        "EMOLA" to "e-Mola",
                        "CARD" to "POS/Cartão",
                        "TRANSFER" to "Transferência"
                    )
                    items(methods) { (mCode, mLabel) ->
                        FilterChip(
                            selected = paymentMethod == mCode,
                            onClick = { paymentMethod = mCode },
                            label = { Text(mLabel, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Descrição / Comprovativo") },
                    placeholder = { Text("Ex: Sinal de 50%, Pagamento integral...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        onConfirm(amount, paymentMethod, notes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Confirmar Entrada no Caixa")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun AddServiceExpenseDialog(
    service: ServiceEntity,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (description: String, amount: Double, isPaid: Boolean, method: String, notes: String) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var isPaidNow by remember { mutableStateOf(true) }
    var paymentMethod by remember { mutableStateOf("CASH") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Associar Custo ao Serviço", fontWeight = FontWeight.Bold)
                Text(
                    text = "${service.title} — ${service.customerName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição do Custo / Despesa *") },
                    placeholder = { Text("Ex: Peça sobressalente, produto capilar, transporte") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Valor do Custo ($currency) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isPaidNow,
                        onCheckedChange = { isPaidNow = it }
                    )
                    Text("Despesa já foi paga no caixa", fontSize = 12.sp)
                }

                if (isPaidNow) {
                    Text("Forma de Pagamento Utilizada:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val methods = listOf(
                            "CASH" to "Dinheiro",
                            "MPESA" to "M-Pesa",
                            "EMOLA" to "e-Mola",
                            "CARD" to "POS/Cartão",
                            "TRANSFER" to "Transferência"
                        )
                        items(methods) { (mCode, mLabel) ->
                            FilterChip(
                                selected = paymentMethod == mCode,
                                onClick = { paymentMethod = mCode },
                                label = { Text(mLabel, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (description.isNotBlank() && amount > 0) {
                        onConfirm(description, amount, isPaidNow, paymentMethod, notes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = StatusError)
            ) {
                Text("Registar Custo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun AddServiceCatalogDialog(
    currency: String,
    onDismiss: () -> Unit,
    onSave: (name: String, category: String, price: Double, duration: Int, desc: String, notes: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Geral") }
    var priceText by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("60") }
    var description by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Novo Serviço no Catálogo Habitual", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Serviço *") },
                    placeholder = { Text("Ex: Tranças, Mudança de óleo, Corte...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Categoria") },
                        placeholder = { Text("Cabelo, Motor, etc.") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { durationText = it },
                        label = { Text("Duração (min)") },
                        placeholder = { Text("60") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Preço Habitual ($currency) *") },
                    placeholder = { Text("Ex: 500.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição / O que inclui") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceText.toDoubleOrNull() ?: 0.0
                    val duration = durationText.toIntOrNull() ?: 60
                    if (name.isNotBlank() && price > 0.0) {
                        onSave(name, category, price, duration, description, notes)
                    }
                },
                enabled = name.isNotBlank() && (priceText.toDoubleOrNull() ?: 0.0) > 0.0,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Adicionar ao Catálogo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
