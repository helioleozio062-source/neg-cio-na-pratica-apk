package com.example.ui.screens.debts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DebtEntity
import com.example.data.model.DebtPaymentEntity
import com.example.ui.components.formatCurrency
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.PhoneUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(viewModel: MainViewModel) {
    val activeBusiness by viewModel.activeBusiness.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    val currency = activeBusiness?.currencySymbol ?: "MT"

    var searchQuery by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf("ALL") } // "ALL", "RECEIVABLE", "PAYABLE"
    var statusFilter by remember { mutableStateOf("ALL") } // "ALL", "PENDING", "PARTIAL", "PAID", "OVERDUE"
    var sortOrder by remember { mutableStateOf("DATE_DESC") } // "DATE_DESC", "DUE_ASC", "VALUE_DESC"

    var showAddDebtDialog by remember { mutableStateOf(false) }
    var selectedDebtForPayment by remember { mutableStateOf<DebtEntity?>(null) }
    var selectedDebtForDetails by remember { mutableStateOf<DebtEntity?>(null) }
    var debtToDelete by remember { mutableStateOf<DebtEntity?>(null) }

    val now = System.currentTimeMillis()

    // Indicadores do topo
    val totalReceivable = remember(debts) {
        debts.filter { it.type == "RECEIVABLE" && it.status != "PAID" }.sumOf { it.remainingAmount }
    }
    val totalPayable = remember(debts) {
        debts.filter { it.type == "PAYABLE" && it.status != "PAID" }.sumOf { it.remainingAmount }
    }
    val totalReceivedInPeriod = remember(transactions) {
        transactions.filter { it.type == "INCOME_DEBT_PAYMENT" }.sumOf { it.amount }
    }
    val totalPaidInPeriod = remember(transactions) {
        transactions.filter { it.type == "EXPENSE_PURCHASE" && it.category == "Fornecedores" }.sumOf { it.amount }
    }
    val countPending = remember(debts) {
        debts.count { it.status == "PENDING" || it.status == "PARTIAL" }
    }
    val countOverdue = remember(debts) {
        debts.count { it.status != "PAID" && it.dueDate != null && it.dueDate < now }
    }

    // Filtragem e ordenação
    val filteredDebts = remember(debts, searchQuery, typeFilter, statusFilter, sortOrder) {
        debts.filter { debt ->
            val matchesSearch = debt.partyName.contains(searchQuery, ignoreCase = true) ||
                    debt.description.contains(searchQuery, ignoreCase = true) ||
                    debt.phone.contains(searchQuery)

            val matchesType = when (typeFilter) {
                "RECEIVABLE" -> debt.type == "RECEIVABLE"
                "PAYABLE" -> debt.type == "PAYABLE"
                else -> true
            }

            val isOverdue = debt.status != "PAID" && debt.dueDate != null && debt.dueDate < now

            val matchesStatus = when (statusFilter) {
                "PENDING" -> debt.status == "PENDING" && !isOverdue
                "PARTIAL" -> debt.status == "PARTIAL" && !isOverdue
                "PAID" -> debt.status == "PAID"
                "OVERDUE" -> isOverdue
                else -> true
            }

            matchesSearch && matchesType && matchesStatus
        }.sortedWith { a, b ->
            when (sortOrder) {
                "DATE_DESC" -> b.createdAt.compareTo(a.createdAt)
                "DUE_ASC" -> {
                    val dueA = a.dueDate ?: Long.MAX_VALUE
                    val dueB = b.dueDate ?: Long.MAX_VALUE
                    dueA.compareTo(dueB)
                }
                "VALUE_DESC" -> b.remainingAmount.compareTo(a.remainingAmount)
                else -> b.createdAt.compareTo(a.createdAt)
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDebtDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Registar Dívida", fontWeight = FontWeight.Bold) },
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_debt")
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Gestão de Dívidas",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Controlo de crédito a clientes e pagamentos a fornecedores",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Cartões de Resumo e Indicadores Financeiros (Requisito 4.E)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Total a Receber (Clientes)
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("card_total_receivable"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = EmeraldPrimary.copy(alpha = 0.12f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("A Receber (Clientes)", style = MaterialTheme.typography.labelSmall, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = formatCurrency(totalReceivable, currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldPrimary
                                )
                            }
                        }

                        // Total a Pagar (Fornecedores)
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("card_total_payable"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("A Pagar (Negócio deve)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = formatCurrency(totalPayable, currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    // Métricas complementares: Amortizações recebidas / pagas + Contagens
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Recuperado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatCurrency(totalReceivedInPeriod, currency), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EmeraldPrimary)
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Pago a terceiros", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatCurrency(totalPaidInPeriod, currency), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = if (countOverdue > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Vencidas", style = MaterialTheme.typography.labelSmall, color = if (countOverdue > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$countOverdue dívida(s)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (countOverdue > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }

            // Barra de Pesquisa e Filtros (Requisito 4.B)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Pesquisar por cliente, fornecedor ou descrição...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpar")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_search_debts"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Filtro por Tipo de Dívida
                    TabRow(
                        selectedTabIndex = when (typeFilter) {
                            "RECEIVABLE" -> 1
                            "PAYABLE" -> 2
                            else -> 0
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        contentColor = EmeraldPrimary,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp))
                    ) {
                        Tab(
                            selected = typeFilter == "ALL",
                            onClick = { typeFilter = "ALL" },
                            text = { Text("Todas (${debts.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                        )
                        Tab(
                            selected = typeFilter == "RECEIVABLE",
                            onClick = { typeFilter = "RECEIVABLE" },
                            text = { Text("Clientes a Dever", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                        )
                        Tab(
                            selected = typeFilter == "PAYABLE",
                            onClick = { typeFilter = "PAYABLE" },
                            text = { Text("A Pagar (Fornecedores)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                        )
                    }

                    // Chips de Situação e Ordenação
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = statusFilter == "ALL",
                            onClick = { statusFilter = "ALL" },
                            label = { Text("Todas", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = statusFilter == "PENDING",
                            onClick = { statusFilter = "PENDING" },
                            label = { Text("Pendentes", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = statusFilter == "PARTIAL",
                            onClick = { statusFilter = "PARTIAL" },
                            label = { Text("Parciais", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = statusFilter == "OVERDUE",
                            onClick = { statusFilter = "OVERDUE" },
                            label = { Text("Vencidas ($countOverdue)", fontSize = 11.sp, color = if (countOverdue > 0) MaterialTheme.colorScheme.error else Color.Unspecified) }
                        )
                        FilterChip(
                            selected = statusFilter == "PAID",
                            onClick = { statusFilter = "PAID" },
                            label = { Text("Liquidadas", fontSize = 11.sp) }
                        )
                    }
                }
            }

            // Lista de Dívidas
            if (filteredDebts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotBlank() || typeFilter != "ALL" || statusFilter != "ALL")
                                    "Nenhuma dívida encontrada com os filtros aplicados."
                                else
                                    "Ainda não registou dívidas no negócio.",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Toque no botão '+ Registar Dívida' para apontar créditos de clientes ou contas a fornecedores.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                items(filteredDebts, key = { it.id }) { debt ->
                    DebtCardItem(
                        debt = debt,
                        currency = currency,
                        onPayClick = { selectedDebtForPayment = debt },
                        onDetailsClick = { selectedDebtForDetails = debt },
                        onDeleteClick = { debtToDelete = debt }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Modal de Registo de Nova Dívida (Requisito 4.A)
    if (showAddDebtDialog) {
        AddDebtDialog(
            currency = currency,
            onDismiss = { showAddDebtDialog = false },
            onConfirm = { type, partyName, phone, desc, amount, dueDate, notes ->
                viewModel.createDebt(
                    type = type,
                    partyName = partyName,
                    phone = phone,
                    description = desc,
                    amount = amount,
                    dueDate = dueDate,
                    notes = notes
                )
                showAddDebtDialog = false
            }
        )
    }

    // Modal de Pagamento / Amortização Parcial ou Total (Requisito 4.C)
    selectedDebtForPayment?.let { debt ->
        RecordDebtPaymentDialog(
            debt = debt,
            currency = currency,
            onDismiss = { selectedDebtForPayment = null },
            onConfirm = { amount, date, notes ->
                viewModel.payDebt(
                    debtId = debt.id,
                    amount = amount,
                    notes = notes,
                    paymentDate = date
                )
                selectedDebtForPayment = null
            }
        )
    }

    // Modal de Detalhes e Histórico Completo de Pagamentos (Requisito 4.C)
    selectedDebtForDetails?.let { debt ->
        DebtDetailsDialog(
            debt = debt,
            currency = currency,
            viewModel = viewModel,
            onDismiss = { selectedDebtForDetails = null },
            onPayClick = {
                selectedDebtForPayment = debt
                selectedDebtForDetails = null
            }
        )
    }

    // Confirmação de Eliminação
    debtToDelete?.let { debt ->
        AlertDialog(
            onDismissRequest = { debtToDelete = null },
            title = { Text("Eliminar Registo de Dívida") },
            text = {
                Text("Tem certeza que deseja eliminar o registo de dívida de ${debt.partyName} no valor de ${formatCurrency(debt.remainingAmount, currency)}? O histórico deste registo será removido.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDebt(debt)
                        debtToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { debtToDelete = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun DebtCardItem(
    debt: DebtEntity,
    currency: String,
    onPayClick: () -> Unit,
    onDetailsClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val now = System.currentTimeMillis()
    val isReceivable = debt.type == "RECEIVABLE"
    val isOverdue = debt.status != "PAID" && debt.dueDate != null && debt.dueDate < now
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "MZ")) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("debt_card_${debt.id}")
            .clickable { onDetailsClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOverdue) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tipo badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isReceivable) EmeraldPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isReceivable) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = if (isReceivable) EmeraldPrimary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = debt.partyName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isReceivable) "Cliente deve ao negócio" else "Negócio deve ao fornecedor",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Status Badge
                DebtStatusBadge(status = debt.status, isOverdue = isOverdue)
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (debt.description.isNotBlank()) {
                Text(
                    text = debt.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (debt.phone.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = PhoneUtils.formatDisplay(debt.phone),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Informações Financeiras e Vencimento
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Saldo em Falta",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(debt.remainingAmount, currency),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isReceivable) EmeraldPrimary else MaterialTheme.colorScheme.error
                    )
                    if (debt.initialAmount > debt.remainingAmount) {
                        Text(
                            text = "Original: ${formatCurrency(debt.initialAmount, currency)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (debt.dueDate != null) {
                        Text(
                            text = "Vencimento: ${dateFormat.format(Date(debt.dueDate))}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal,
                            color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isOverdue) {
                            Text(
                                text = "⚠️ PRAZO EXPIRADO",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Ações do Cartão
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDetailsClick) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Histórico")
                }

                if (debt.status != "PAID") {
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onPayClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isReceivable) EmeraldPrimary else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isReceivable) "Receber" else "Pagar")
                    }
                }
            }
        }
    }
}

@Composable
fun DebtStatusBadge(status: String, isOverdue: Boolean) {
    val (label, bg, textCol) = when {
        status == "PAID" -> Triple("LIQUIDADA", EmeraldPrimary.copy(alpha = 0.15f), EmeraldPrimary)
        isOverdue -> Triple("VENCIDA", MaterialTheme.colorScheme.error.copy(alpha = 0.2f), MaterialTheme.colorScheme.error)
        status == "PARTIAL" -> Triple("PARCIAL", Color(0xFF1976D2).copy(alpha = 0.15f), Color(0xFF1976D2))
        else -> Triple("PENDENTE", Color(0xFFF57C00).copy(alpha = 0.15f), Color(0xFFF57C00))
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textCol,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

// Diálogo de Registo de Nova Dívida (Requisito 4.A)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDebtDialog(
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (
        type: String,
        partyName: String,
        phone: String,
        desc: String,
        amount: Double,
        dueDate: Long?,
        notes: String
    ) -> Unit
) {
    var type by remember { mutableStateOf("RECEIVABLE") } // "RECEIVABLE" ou "PAYABLE"
    var partyName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var dueDateStr by remember { mutableStateOf("") } // YYYY-MM-DD
    var notes by remember { mutableStateOf("") }

    val amount = amountStr.toDoubleOrNull() ?: 0.0

    val commonReasonsReceivable = listOf(
        "Venda de produtos a crédito",
        "Empréstimo concedido",
        "Pagamento pendente de serviço",
        "Adiantamento a funcionário",
        "Outro"
    )

    val commonReasonsPayable = listOf(
        "Compra de mercadoria ao fornecedor",
        "Empréstimo recebido",
        "Aluguer / Renda pendente",
        "Serviço contratado a pagar",
        "Outro"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registar Nova Dívida") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Selector de Tipo de Dívida
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = type == "RECEIVABLE",
                        onClick = { type = "RECEIVABLE" },
                        label = { Text("Cliente deve (A Receber)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = type == "PAYABLE",
                        onClick = { type = "PAYABLE" },
                        label = { Text("Negócio deve (A Pagar)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = partyName,
                    onValueChange = { partyName = it },
                    label = { Text(if (type == "RECEIVABLE") "Nome do Cliente *" else "Nome do Fornecedor / Credor *") },
                    placeholder = { Text("Ex: João Silva, Armazém Central") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_debt_party")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Telefone / WhatsApp (Opcional)") },
                    placeholder = { Text("Ex: 841234567") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição da Origem da Dívida *") },
                    placeholder = { Text("Ex: Compra de 5 caixas de óleo a prazo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_debt_desc")
                )

                // Sugestões rápidas de descrição
                val suggestions = if (type == "RECEIVABLE") commonReasonsReceivable else commonReasonsPayable
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    suggestions.take(2).forEach { reason ->
                        SuggestionChip(
                            onClick = { description = reason },
                            label = { Text(reason, fontSize = 10.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Valor Total da Dívida ($currency) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_debt_amount")
                )

                OutlinedTextField(
                    value = dueDateStr,
                    onValueChange = { dueDateStr = it },
                    label = { Text("Prazo Previsto de Pagamento (Opcional)") },
                    placeholder = { Text("Ex: DD/MM/AAAA ou dias (ex: 15)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações Adicionais (Opcional)") },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedDueDate = parseDueDate(dueDateStr)
                    onConfirm(
                        type,
                        partyName,
                        phone,
                        description,
                        amount,
                        parsedDueDate,
                        notes
                    )
                },
                enabled = partyName.isNotBlank() && description.isNotBlank() && amount > 0.0,
                modifier = Modifier.testTag("btn_save_debt")
            ) {
                Text("Gravar Dívida")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// Diálogo de Pagamento / Amortização Parcial ou Total (Requisito 4.C)
@Composable
fun RecordDebtPaymentDialog(
    debt: DebtEntity,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, date: Long, notes: String) -> Unit
) {
    var amountStr by remember { mutableStateOf(debt.remainingAmount.toString()) }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isReceivable = debt.type == "RECEIVABLE"
    val inputAmount = amountStr.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isReceivable) "Registar Recebimento de Crédito" else "Pagar Conta a Fornecedor")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Parte: ${debt.partyName}\nDescrição: ${debt.description}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Saldo Atual Pendente: ${formatCurrency(debt.remainingAmount, currency)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isReceivable) EmeraldPrimary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.ExtraBold
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        val parsed = it.toDoubleOrNull() ?: 0.0
                        if (parsed > debt.remainingAmount) {
                            errorMessage = "O valor inserido é superior ao saldo em falta (${formatCurrency(debt.remainingAmount, currency)})."
                        } else {
                            errorMessage = null
                        }
                    },
                    label = { Text("Valor do Pagamento ($currency) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = errorMessage != null,
                    modifier = Modifier.fillMaxWidth().testTag("input_payment_amount")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (inputAmount > 0 && inputAmount <= debt.remainingAmount) {
                    val remainingAfter = debt.remainingAmount - inputAmount
                    Text(
                        text = if (remainingAfter <= 0.0) "✓ A dívida será completamente liquidada!"
                        else "Saldo restante após pagamento: ${formatCurrency(remainingAfter, currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (remainingAfter <= 0.0) EmeraldPrimary else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observação / Recibo / Método de Pagamento") },
                    placeholder = { Text("Ex: Pago em numerário, M-Pesa, transferido") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Nota: Este pagamento será integrado diretamente no fluxo de caixa real do negócio sem duplicações.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (inputAmount > 0.0 && inputAmount <= debt.remainingAmount) {
                        onConfirm(inputAmount, System.currentTimeMillis(), notes)
                    }
                },
                enabled = inputAmount > 0.0 && inputAmount <= debt.remainingAmount,
                modifier = Modifier.testTag("btn_confirm_debt_payment")
            ) {
                Text("Confirmar Pagamento")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// Diálogo de Detalhes e Histórico Completo de Amortizações (Requisito 4.C)
@Composable
fun DebtDetailsDialog(
    debt: DebtEntity,
    currency: String,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onPayClick: () -> Unit
) {
    var paymentsList by remember { mutableStateOf<List<DebtPaymentEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "MZ")) }

    LaunchedEffect(debt.id) {
        coroutineScope.launch {
            paymentsList = viewModel.getPaymentsForDebt(debt.id)
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Detalhes da Dívida")
                DebtStatusBadge(status = debt.status, isOverdue = false)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "Pessoa / Fornecedor: ${debt.partyName}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                if (debt.phone.isNotBlank()) {
                    Text(text = "Contacto: ${PhoneUtils.formatDisplay(debt.phone)}", style = MaterialTheme.typography.bodyMedium)
                }
                Text(text = "Descrição: ${debt.description}", style = MaterialTheme.typography.bodyMedium)
                if (debt.notes.isNotBlank()) {
                    Text(text = "Observações: ${debt.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Divider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Valor Inicial", style = MaterialTheme.typography.labelSmall)
                        Text(formatCurrency(debt.initialAmount, currency), fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Saldo em Falta", style = MaterialTheme.typography.labelSmall)
                        Text(
                            formatCurrency(debt.remainingAmount, currency),
                            fontWeight = FontWeight.ExtraBold,
                            color = if (debt.type == "RECEIVABLE") EmeraldPrimary else MaterialTheme.colorScheme.error
                        )
                    }
                }

                Divider()

                Text("Histórico de Pagamentos / Amortizações:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally))
                } else if (paymentsList.isEmpty()) {
                    Text(
                        text = "Ainda não foi registado nenhum pagamento para esta dívida.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(paymentsList) { payment ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(dateFormat.format(Date(payment.paymentDate)), style = MaterialTheme.typography.labelSmall)
                                        if (payment.notes.isNotBlank()) {
                                            Text(payment.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Text(
                                        formatCurrency(payment.amount, currency),
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (debt.status != "PAID") {
                Button(onClick = onPayClick) {
                    Text("Registar Pagamento")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        }
    )
}

private fun parseDueDate(input: String): Long? {
    if (input.isBlank()) return null
    val trimmed = input.trim()

    // Se for um número simples de dias (ex: "15" -> 15 dias a partir de hoje)
    val days = trimmed.toLongOrNull()
    if (days != null && days in 1..365) {
        return System.currentTimeMillis() + (days * 86400000L)
    }

    // Tentar formato DD/MM/AAAA
    return try {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "MZ"))
        sdf.parse(trimmed)?.time
    } catch (e: Exception) {
        null
    }
}
