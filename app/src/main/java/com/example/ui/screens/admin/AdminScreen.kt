package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BusinessEntity
import com.example.data.model.FeedbackEntity
import com.example.ui.components.ChangePasswordDialog
import com.example.ui.components.MetricCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.formatCurrency
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MainViewModel

@Composable
fun AdminScreen(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val adminStats by viewModel.adminStats.collectAsState()
    val allFeedbacks by viewModel.feedbacks.collectAsState()
    val allPlatformBusinesses by viewModel.allPlatformBusinesses.collectAsState()

    var selectedSection by remember { mutableIntStateOf(0) } // 0: Negócios & Taxas, 1: Pedidos, 2: Reclamações, 3: Avaliações
    var editingFeedback by remember { mutableStateOf<FeedbackEntity?>(null) }
    var showChangeCodeDialog by remember { mutableStateOf(false) }
    var showAddBusinessDialog by remember { mutableStateOf(false) }
    var businessFilter by remember { mutableStateOf("ALL") }
    var businessCitySearch by remember { mutableStateOf("") }

    val businessRequests = remember(allFeedbacks) { allFeedbacks.filter { it.type == "BUSINESS_REQUEST" } }
    val complaints = remember(allFeedbacks) { allFeedbacks.filter { it.type == "COMPLAINT" } }
    val reviews = remember(allFeedbacks) { allFeedbacks.filter { it.type == "REVIEW" } }
    val suggestions = remember(allFeedbacks) { allFeedbacks.filter { it.type == "SUGGESTION" } }

    // Bloqueio de Segurança Rigoroso (Requisito 1)
    if (currentUser?.role != "ADMIN") {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Acesso Restrito: Administrador",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Não possui autorização para aceder a esta área. Inicie sessão com credenciais de administrador.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Voltar ao Início")
                    }
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Admin Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_header_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurfaceDark)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Painel Administrativo da Plataforma",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Acesso exclusivo aos responsáveis do Negócio na Prática",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateTextSecondaryDark
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StatusWarning.copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Acesso Seguro", style = MaterialTheme.typography.labelSmall, color = StatusWarning)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Sessão ativa como: ${currentUser?.fullName} (${currentUser?.email})",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateTextSecondaryDark
                    )
                }
            }
        }

        // Admin Security & Password Change
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_security_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(StatusError.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = StatusError)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Código de Acesso do Administrador",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Mude o código para que mais ninguém consiga aceder ao painel",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Ao trocar o código, o anterior ('admin123') é invalidado e apenas quem souber o novo código poderá entrar como Administrador Geral.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showChangeCodeDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("button_change_admin_code"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                    ) {
                        Icon(Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Trocar Código do Administrador", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Platform KPIs
        item {
            SectionHeader(title = "Métricas Globais da Plataforma")
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Contas Ativas",
                    value = "${adminStats.totalUsers} utilizadores",
                    icon = Icons.Default.People,
                    subtitle = "${adminStats.totalBusinesses} negócios geridos",
                    iconColor = SkySecondary,
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "Volume Global",
                    value = formatCurrency(adminStats.totalSalesVolume, "MT"),
                    icon = Icons.Default.TrendingUp,
                    subtitle = "Vendas totais no app",
                    iconColor = EmeraldPrimary,
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
                    title = "Reclamações",
                    value = "${adminStats.pendingComplaints} pendentes",
                    icon = Icons.Default.ReportProblem,
                    subtitle = "${adminStats.resolvedComplaints} resolvidas",
                    iconColor = if (adminStats.pendingComplaints > 0) StatusError else StatusSuccess,
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "Satisfação",
                    value = "★ %.1f / 5.0".format(adminStats.averageRating),
                    icon = Icons.Default.Star,
                    subtitle = "${reviews.size} avaliações",
                    iconColor = AmberTertiary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Section Tabs
        item {
            ScrollableTabRow(
                selectedTabIndex = selectedSection,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = EmeraldPrimary,
                edgePadding = 8.dp
            ) {
                Tab(
                    selected = selectedSection == 0,
                    onClick = { selectedSection = 0 },
                    text = { Text("Negócios & Taxas (${allPlatformBusinesses.size})") }
                )
                Tab(
                    selected = selectedSection == 1,
                    onClick = { selectedSection = 1 },
                    text = { Text("Pedidos (${businessRequests.size})") }
                )
                Tab(
                    selected = selectedSection == 2,
                    onClick = { selectedSection = 2 },
                    text = { Text("Reclamações (${complaints.size})") }
                )
                Tab(
                    selected = selectedSection == 3,
                    onClick = { selectedSection = 3 },
                    text = { Text("Avaliações (${reviews.size + suggestions.size})") }
                )
            }
        }

        when (selectedSection) {
            0 -> {
                // Section 0: Businesses and Fees Management
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Controlo de Taxas & Cidades",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ative, desative ou adicione negócios de clientes que pagaram a taxa",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { showAddBusinessDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("admin_button_add_business")
                        ) {
                            Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Negócio", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Filter Chips
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = businessFilter == "ALL",
                            onClick = { businessFilter = "ALL" },
                            label = { Text("Todos (${allPlatformBusinesses.size})") }
                        )
                        FilterChip(
                            selected = businessFilter == "PAID",
                            onClick = { businessFilter = "PAID" },
                            label = { Text("Taxa Paga (${allPlatformBusinesses.count { it.isSubscriptionPaid }})") }
                        )
                        FilterChip(
                            selected = businessFilter == "UNPAID",
                            onClick = { businessFilter = "UNPAID" },
                            label = { Text("Taxa Pendente (${allPlatformBusinesses.count { !it.isSubscriptionPaid }})") }
                        )
                    }
                }

                // City / Name Search
                item {
                    OutlinedTextField(
                        value = businessCitySearch,
                        onValueChange = { businessCitySearch = it },
                        placeholder = { Text("Filtrar por cidade (ex: Maputo, Beira) ou nome...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                val filteredBusinesses = allPlatformBusinesses.filter { b ->
                    val matchesFilter = when (businessFilter) {
                        "PAID" -> b.isSubscriptionPaid
                        "UNPAID" -> !b.isSubscriptionPaid
                        "ACTIVE" -> b.isActive
                        "INACTIVE" -> !b.isActive
                        else -> true
                    }
                    val matchesSearch = businessCitySearch.isBlank() ||
                            b.city.contains(businessCitySearch, ignoreCase = true) ||
                            b.name.contains(businessCitySearch, ignoreCase = true) ||
                            b.ownerEmail.contains(businessCitySearch, ignoreCase = true)
                    matchesFilter && matchesSearch
                }

                if (filteredBusinesses.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("Nenhum negócio encontrado para os filtros selecionados.")
                            }
                        }
                    }
                } else {
                    items(filteredBusinesses, key = { it.id }) { b ->
                        AdminBusinessCard(
                            business = b,
                            onToggleActive = { active -> viewModel.adminToggleBusinessActive(b.id, active) },
                            onToggleFee = { paid -> viewModel.adminToggleBusinessFee(b.id, paid, 500.0) }
                        )
                    }
                }
            }

            1 -> {
                // Section 1: Business Requests
                if (businessRequests.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("Nenhum pedido de abertura de novo negócio pendente.")
                            }
                        }
                    }
                } else {
                    items(businessRequests) { req ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = req.title,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (req.status == "RESOLVIDA") EmeraldContainer else AmberContainer
                                    ) {
                                        Text(
                                            text = if (req.status == "RESOLVIDA") "Aprovado" else "Pendente",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (req.status == "RESOLVIDA") EmeraldOnContainer else AmberOnContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = req.description,
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (req.status != "RESOLVIDA") {
                                        Button(
                                            onClick = {
                                                // Extract details or auto approve
                                                viewModel.adminCreateBusiness(
                                                    name = req.businessName,
                                                    ownerEmail = req.userEmail,
                                                    activityType = "Mercearia / Retalho",
                                                    city = "Maputo",
                                                    currency = "MT",
                                                    isPaid = true,
                                                    feeAmount = 500.0,
                                                    phone = req.userEmail
                                                )
                                                viewModel.updateFeedbackStatus(req.id, "RESOLVIDA", "Negócio aprovado e registado pelo administrador.")
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Aprovar & Criar Negócio", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { editingFeedback = req },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(0.7f)
                                    ) {
                                        Text("Gerir / Notas", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Section 2: Complaints Management
                if (complaints.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("Nenhuma reclamação registada pelos utilizadores.")
                            }
                        }
                    }
                } else {
                    items(complaints) { complaint ->
                        AdminComplaintCard(
                            item = complaint,
                            onManage = { editingFeedback = complaint }
                        )
                    }
                }
            }

            3 -> {
                // Section 3: Reviews and Suggestions
                if (reviews.isEmpty() && suggestions.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("Nenhuma avaliação ou sugestão registada.")
                            }
                        }
                    }
                } else {
                    items(reviews) { rev ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = rev.title,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "★".repeat(rev.rating),
                                        color = AmberTertiary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                                if (rev.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = rev.description,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Por: ${rev.userEmail} • Negócio: ${rev.businessName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    items(suggestions) { sug ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = AmberTertiary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = sug.title,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = sug.description,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Proposto por: ${sug.userEmail} (${sug.businessName})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal to Manage / Respond to complaint
    editingFeedback?.let { fb ->
        AdminManageComplaintDialog(
            item = fb,
            onDismiss = { editingFeedback = null },
            onSave = { newStatus, notes ->
                viewModel.updateFeedbackStatus(fb.id, newStatus, notes)
                editingFeedback = null
            }
        )
    }

    if (showChangeCodeDialog) {
        ChangePasswordDialog(
            isAdmin = true,
            onDismiss = { showChangeCodeDialog = false },
            onSubmit = { oldPass, newPass, callback ->
                viewModel.changePassword(oldPass, newPass, callback)
            }
        )
    }

    if (showAddBusinessDialog) {
        AdminAddBusinessDialog(
            onDismiss = { showAddBusinessDialog = false },
            onConfirm = { name, email, activity, city, isPaid, feeAmount, phone ->
                viewModel.adminCreateBusiness(
                    name = name,
                    ownerEmail = email,
                    activityType = activity,
                    city = city,
                    currency = "MT",
                    isPaid = isPaid,
                    feeAmount = feeAmount,
                    phone = phone
                )
                showAddBusinessDialog = false
            }
        )
    }
}

@Composable
fun AdminComplaintCard(item: FeedbackEntity, onManage: () -> Unit) {
    val (statusLabel, statusColor, statusBg) = when (item.status) {
        "RECEBIDA" -> Triple("Recebida", Color(0xFF6B7280), Color(0xFFF3F4F6))
        "EM_ANALISE" -> Triple("Em análise", SkySecondary, SkyContainer)
        "EM_RESOLUCAO" -> Triple("Em resolução", AmberTertiary, AmberContainer)
        "RESOLVIDA" -> Triple("Resolvida", StatusSuccess, EmeraldContainer)
        else -> Triple(item.status, EmeraldPrimary, EmeraldContainer)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusBg
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Utilizador: ${item.userEmail} • Negócio: ${item.businessName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (item.adminNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Resposta atual do suporte: ${item.adminNotes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onManage,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Gerir / Responder", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun AdminManageComplaintDialog(
    item: FeedbackEntity,
    onDismiss: () -> Unit,
    onSave: (newStatus: String, notes: String) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(item.status) }
    var notes by remember { mutableStateOf(item.adminNotes) }

    val statusList = listOf(
        "RECEBIDA" to "Recebida",
        "EM_ANALISE" to "Em análise",
        "EM_RESOLUCAO" to "Em resolução",
        "RESOLVIDA" to "Resolvida"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gerir Reclamação") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Reclamação: ${item.title}\nDe: ${item.userEmail}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Text("Atualizar Estado do Processo:", style = MaterialTheme.typography.labelMedium)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    statusList.forEach { (code, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = selectedStatus == code,
                                onClick = { selectedStatus = code }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Resposta / Parecer do Suporte") },
                    placeholder = { Text("Ex: Situação verificada pela equipa técnica e resolvida.") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(selectedStatus, notes) }) {
                Text("Gravar Alterações")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun AdminBusinessCard(
    business: BusinessEntity,
    onToggleActive: (Boolean) -> Unit,
    onToggleFee: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_business_card_${business.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = business.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = business.activityType,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = SkySecondary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Cidade: ${business.city}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    // Active Status
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (business.isActive) EmeraldContainer else StatusError.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (business.isActive) "ATIVO" else "DESATIVADO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (business.isActive) EmeraldOnContainer else StatusError,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Subscription Fee Status
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (business.isSubscriptionPaid) SkyContainer else AmberContainer
                    ) {
                        Text(
                            text = if (business.isSubscriptionPaid) "TAXA PAGA (${business.subscriptionAmount.toInt()} MT)" else "TAXA PENDENTE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (business.isSubscriptionPaid) SkySecondary else AmberOnContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            if (business.ownerEmail.isNotBlank() || business.phone.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Contacto: ${business.ownerEmail.ifBlank { "Sem email" }} ${if (business.phone.isNotBlank()) "• Tel: ${business.phone}" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Toggle Active / Inactive
                if (business.isActive) {
                    OutlinedButton(
                        onClick = { onToggleActive(false) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Desativar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { onToggleActive(true) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ativar Negócio", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Toggle Fee Paid
                if (business.isSubscriptionPaid) {
                    OutlinedButton(
                        onClick = { onToggleFee(false) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.MoneyOff, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Taxa Pendente", fontSize = 11.sp)
                    }
                } else {
                    FilledTonalButton(
                        onClick = { onToggleFee(true) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Confirmar Taxa", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminAddBusinessDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        ownerEmail: String,
        activity: String,
        city: String,
        isPaid: Boolean,
        feeAmount: Double,
        phone: String
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var ownerEmail by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedActivity by remember { mutableStateOf(com.example.util.BusinessProfileUtils.BUSINESS_ACTIVITY_TYPES.first()) }
    var city by remember { mutableStateOf("Maputo") }
    var isPaid by remember { mutableStateOf(true) }
    var feeAmountStr by remember { mutableStateOf("500") }

    val activityList = com.example.util.BusinessProfileUtils.BUSINESS_ACTIVITY_TYPES

    val citiesList = listOf("Maputo", "Matola", "Beira", "Nampula", "Tete", "Quelimane", "Pemba", "Chimoio", "Xai-Xai", "Inhambane")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registar Novo Negócio (Admin)") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Negócio *") },
                    placeholder = { Text("Ex: Cantina Central, Boutique Chique") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = ownerEmail,
                    onValueChange = { ownerEmail = it },
                    label = { Text("Email ou Identificador do Proprietário *") },
                    placeholder = { Text("Ex: cliente@gmail.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Telefone / WhatsApp") },
                    placeholder = { Text("Ex: +258 84 123 4567") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("Cidade do Negócio *") },
                    placeholder = { Text("Ex: Maputo, Matola, Beira") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Ramo de Atividade:", style = MaterialTheme.typography.labelMedium)
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(activityList) { act ->
                        FilterChip(
                            selected = selectedActivity == act,
                            onClick = { selectedActivity = act },
                            label = { Text(act, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                Text("Estado da Taxa de Adesão:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isPaid,
                        onClick = { isPaid = true },
                        label = { Text("Taxa Paga") }
                    )
                    FilterChip(
                        selected = !isPaid,
                        onClick = { isPaid = false },
                        label = { Text("Taxa Pendente") }
                    )
                }

                OutlinedTextField(
                    value = feeAmountStr,
                    onValueChange = { feeAmountStr = it },
                    label = { Text("Valor da Taxa (MT)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        name,
                        ownerEmail,
                        selectedActivity,
                        city.ifBlank { "Maputo" },
                        isPaid,
                        feeAmountStr.toDoubleOrNull() ?: 500.0,
                        phone
                    )
                },
                enabled = name.isNotBlank() && ownerEmail.isNotBlank()
            ) {
                Text("Adicionar Negócio")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

