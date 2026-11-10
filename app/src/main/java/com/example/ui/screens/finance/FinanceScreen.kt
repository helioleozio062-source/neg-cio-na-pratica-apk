package com.example.ui.screens.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DebtEntity
import com.example.data.model.FinancialTransactionEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(viewModel: MainViewModel) {
    val activeBusiness by viewModel.activeBusiness.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val dreReport by viewModel.dreReport.collectAsState()

    val currency = activeBusiness?.currencySymbol ?: "MT"

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Fluxo de Caixa, 1: A Receber, 2: A Pagar
    var showExpenseDialog by remember { mutableStateOf(false) }
    var showProLaboreDialog by remember { mutableStateOf(false) }
    var debtToPay by remember { mutableStateOf<DebtEntity?>(null) }

    val receivables = remember(debts) { debts.filter { it.type == "RECEIVABLE" } }
    val payables = remember(debts) { debts.filter { it.type == "PAYABLE" } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Summary Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                    Column {
                        Text(
                            text = "Saldo de Caixa Atual",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatCurrency(dreReport?.currentCashBalance ?: 0.0, currency),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary,
                            fontSize = 24.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { showExpenseDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("button_new_expense")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Despesa", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { showProLaboreDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Pró-Labore", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Dívidas a Receber", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            formatCurrency(dreReport?.pendingReceivables ?: 0.0, currency),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = AmberTertiary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Dívidas a Pagar", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            formatCurrency(dreReport?.pendingPayables ?: 0.0, currency),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = SkySecondary
                        )
                    }
                }
            }
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = EmeraldPrimary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Movimentos", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.SwapVert, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("A Receber (${receivables.count { it.status != "PAID" }})") },
                icon = { Icon(Icons.Default.ArrowDownward, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("A Pagar (${payables.count { it.status != "PAID" }})") },
                icon = { Icon(Icons.Default.ArrowUpward, contentDescription = null) }
            )
        }

        when (selectedTab) {
            0 -> {
                // Transactions list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (transactions.isEmpty()) {
                        item {
                            EmptyStateView(
                                icon = Icons.Default.AccountBalanceWallet,
                                title = "Nenhum movimento financeiro",
                                description = "Registe despesas operacionais ou vendas para acompanhar as entradas e saídas de dinheiro."
                            )
                        }
                    } else {
                        items(transactions, key = { it.id }) { trans ->
                            TransactionRow(trans = trans, currency = currency)
                        }
                    }
                }
            }
            1 -> {
                // Receivables
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (receivables.isEmpty()) {
                        item {
                            EmptyStateView(
                                icon = Icons.Default.CheckCircle,
                                title = "Sem créditos pendentes",
                                description = "Não há clientes com saldo em dívida neste momento."
                            )
                        }
                    } else {
                        items(receivables, key = { it.id }) { debt ->
                            DebtRow(debt = debt, currency = currency, onPay = { debtToPay = debt })
                        }
                    }
                }
            }
            2 -> {
                // Payables
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (payables.isEmpty()) {
                        item {
                            EmptyStateView(
                                icon = Icons.Default.ThumbUp,
                                title = "Sem contas a pagar pendentes",
                                description = "Todas as compras e compromissos com fornecedores estão liquidados."
                            )
                        }
                    } else {
                        items(payables, key = { it.id }) { debt ->
                            DebtRow(debt = debt, currency = currency, onPay = { debtToPay = debt })
                        }
                    }
                }
            }
        }
    }

    // New Expense Dialog
    if (showExpenseDialog) {
        NewExpenseDialog(
            currency = currency,
            onDismiss = { showExpenseDialog = false },
            onConfirm = { category, amount, desc, method ->
                viewModel.addExpense(category, amount, desc, method)
                showExpenseDialog = false
            }
        )
    }

    // Pro-Labore Dialog
    if (showProLaboreDialog) {
        ProLaboreDialog(
            currency = currency,
            onDismiss = { showProLaboreDialog = false },
            onConfirm = { amount, notes ->
                viewModel.addProLabore(amount, notes)
                showProLaboreDialog = false
            }
        )
    }

    // Debt Liquidation Dialog
    debtToPay?.let { debt ->
        PayDebtDialog(
            debt = debt,
            currency = currency,
            onDismiss = { debtToPay = null },
            onConfirm = { amount ->
                viewModel.payDebt(debt.id, amount)
                debtToPay = null
            }
        )
    }
}

@Composable
fun TransactionRow(trans: FinancialTransactionEntity, currency: String) {
    val isIncome = trans.type.startsWith("INCOME_")
    val dateStr = remember(trans.timestamp) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "AO")).format(Date(trans.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isIncome) EmeraldContainer else StatusError.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (isIncome) EmeraldPrimary else StatusError,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = trans.description,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val methodDesc = com.example.util.BusinessProfileUtils.formatPaymentMethod(trans.paymentMethod)
                    Text(
                        text = "${trans.category} • $dateStr • $methodDesc",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "${if (isIncome) "+" else "-"}${formatCurrency(trans.amount, currency)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isIncome) EmeraldPrimary else StatusError
            )
        }
    }
}

@Composable
fun DebtRow(debt: DebtEntity, currency: String, onPay: () -> Unit) {
    val isReceivable = debt.type == "RECEIVABLE"
    val isPaid = debt.status == "PAID"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = debt.partyName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isReceivable) "Crédito a cliente" else "Dívida a fornecedor",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrency(debt.remainingAmount, currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isPaid) EmeraldPrimary else if (isReceivable) AmberTertiary else SkySecondary
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isPaid) EmeraldContainer else AmberContainer
                    ) {
                        Text(
                            text = if (isPaid) "Liquidado" else if (debt.status == "PARTIAL") "Parcial" else "Pendente",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isPaid) EmeraldOnContainer else AmberOnContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (!isPaid) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onPay,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isReceivable) "Registar Recebimento" else "Pagar Conta", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewExpenseDialog(
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (category: String, amount: Double, desc: String, method: String) -> Unit
) {
    var category by remember { mutableStateOf("Aluguer") }
    var amountStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("CASH") }

    val categories = listOf(
        "Aluguer do Espaço",
        "Salários & Pessoal",
        "Energia & Água",
        "Transporte & Logística",
        "Manutenção",
        "Marketing & Comunicação",
        "Outros Gastos"
    )

    var catExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova Despesa Operacional") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(
                    expanded = catExpanded,
                    onExpandedChange = { catExpanded = !catExpanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoria") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    catExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Valor da Despesa ($currency) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_expense_amount")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição detalhada") },
                    placeholder = { Text("Ex: Pagamento da renda do mês") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Forma de Pagamento:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
                val expenseMethods = listOf(
                    "CASH" to "Dinheiro",
                    "MPESA" to "M-Pesa",
                    "EMOLA" to "e-Mola",
                    "MKESH" to "mKesh",
                    "CARD" to "Cartão / POS",
                    "TRANSFER" to "Transferência"
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(expenseMethods) { (key, label) ->
                        FilterChip(
                            selected = method == key,
                            onClick = { method = key },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            val amt = amountStr.toDoubleOrNull() ?: 0.0
            Button(
                onClick = { onConfirm(category, amt, description, method) },
                enabled = amt > 0.0,
                modifier = Modifier.testTag("button_confirm_expense")
            ) {
                Text("Gravar Despesa")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun ProLaboreDialog(
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, notes: String) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Retirada Pessoal (Pró-Labore)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Registar retirada de dinheiro para uso pessoal do proprietário. O valor reduzirá o saldo de caixa e constará no DRE para cálculo correto do lucro líquido.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Valor a retirar ($currency) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observação / Motivo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val amt = amountStr.toDoubleOrNull() ?: 0.0
            Button(
                onClick = { onConfirm(amt, notes) },
                enabled = amt > 0.0
            ) {
                Text("Confirmar Retirada")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun PayDebtDialog(
    debt: DebtEntity,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double) -> Unit
) {
    var amountStr by remember { mutableStateOf(debt.remainingAmount.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (debt.type == "RECEIVABLE") "Registar Recebimento de Crédito" else "Pagar Conta a Fornecedor") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Parte: ${debt.partyName}\nSaldo Pendente: ${formatCurrency(debt.remainingAmount, currency)}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Valor a amortizar ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val amt = amountStr.toDoubleOrNull() ?: 0.0
            Button(
                onClick = { onConfirm(amt.coerceAtMost(debt.remainingAmount)) },
                enabled = amt > 0.0
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
