package com.example.ui.screens.sales

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.data.model.ProductEntity
import com.example.data.model.SaleEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(viewModel: MainViewModel) {
    val activeBusiness by viewModel.activeBusiness.collectAsState()
    val products by viewModel.products.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val sales by viewModel.sales.collectAsState()

    val currency = activeBusiness?.currencySymbol ?: "MT"

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Nova Venda (PDV), 1: Histórico
    var searchQuery by remember { mutableStateOf("") }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var weightProductSelected by remember { mutableStateOf<ProductEntity?>(null) }
    var showCartDetails by remember { mutableStateOf(false) }
    var saleToCancel by remember { mutableStateOf<SaleEntity?>(null) }
    var receiptSale by remember { mutableStateOf<SaleEntity?>(null) }

    val filteredProducts = remember(products, searchQuery) {
        products.filter { it.name.contains(searchQuery, ignoreCase = true) || it.sku.contains(searchQuery, ignoreCase = true) }
    }

    val cartTotal = remember(cartItems) {
        cartItems.sumOf { it.quantity * it.product.sellingPrice }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Tab selector
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = EmeraldPrimary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Caixa / Nova Venda", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.PointOfSale, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Histórico (${sales.size})", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Receipt, contentDescription = null) }
            )
        }

        if (selectedTab == 0) {
            // PDV Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Cart overview pill if items present
                if (cartItems.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = EmeraldContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.clickable { showCartDetails = !showCartDetails }) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${cartItems.size} artigo(s) no carrinho",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = EmeraldOnContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = if (showCartDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = EmeraldOnContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = formatCurrency(cartTotal, currency),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldOnContainer
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TextButton(onClick = { viewModel.clearCart() }) {
                                        Text("Limpar", color = EmeraldOnContainer)
                                    }
                                    Button(
                                        onClick = { showCheckoutDialog = true },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        modifier = Modifier.testTag("button_checkout")
                                    ) {
                                        Text("Cobrar", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Itemized Cart list preview
                            AnimatedVisibility(visible = showCartDetails) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Divider(color = EmeraldOnContainer.copy(alpha = 0.2f))
                                    cartItems.forEach { item ->
                                        val isWeight = item.product.isSoldByWeight || com.example.util.BusinessProfileUtils.isWeightUnit(item.product.unit)
                                        val qtyLabel = if (item.customUnitDisplay.isNotBlank()) item.customUnitDisplay
                                        else if (isWeight) com.example.util.BusinessProfileUtils.formatWeightDisplay(item.quantity)
                                        else "${item.quantity.toInt()} un"

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.product.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = EmeraldOnContainer
                                                )
                                                Text(
                                                    text = "$qtyLabel × ${formatCurrency(item.product.sellingPrice, currency)}${if (isWeight) "/kg" else ""}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = EmeraldOnContainer.copy(alpha = 0.8f)
                                                )
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = formatCurrency(item.quantity * item.product.sellingPrice, currency),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = EmeraldOnContainer
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                IconButton(
                                                    onClick = { viewModel.removeFromCart(item.product.id) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = "Remover", tint = EmeraldOnContainer, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Search product to sell
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar produto para adicionar à venda...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )

                // Products list for POS
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredProducts) { product ->
                        val cartItem = cartItems.find { it.product.id == product.id }
                        val isWeight = product.isSoldByWeight || com.example.util.BusinessProfileUtils.isWeightUnit(product.unit)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isWeight) {
                                        weightProductSelected = product
                                    } else {
                                        viewModel.addToCart(product)
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (cartItem != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = product.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (isWeight) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = EmeraldContainer
                                            ) {
                                                Text(
                                                    text = "⚖️ Por Peso (kg)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = EmeraldOnContainer,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = formatCurrency(product.sellingPrice, currency) + if (isWeight) "/kg" else "",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = EmeraldPrimary
                                        )
                                        val stockText = if (isWeight) {
                                            "Stock: ${com.example.util.BusinessProfileUtils.formatWeightDisplay(product.currentStock)}"
                                        } else {
                                            "Stock: ${product.currentStock.toInt()} ${product.unit}"
                                        }
                                        Text(
                                            text = stockText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (product.currentStock <= product.minStockAlert) StatusWarning else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (cartItem != null && isWeight) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val inCartDisplay = cartItem.customUnitDisplay.ifBlank { com.example.util.BusinessProfileUtils.formatWeightDisplay(cartItem.quantity) }
                                        Text(
                                            text = "No carrinho: $inCartDisplay (${formatCurrency(cartItem.quantity * cartItem.product.sellingPrice, currency)})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (isWeight) {
                                    Button(
                                        onClick = { weightProductSelected = product },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Scale, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Pesar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else if (cartItem != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.updateCartItemQuantity(product.id, cartItem.quantity - 1.0) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Diminuir", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        Text(
                                            text = "${cartItem.quantity.toInt()}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        IconButton(
                                            onClick = { viewModel.updateCartItemQuantity(product.id, cartItem.quantity + 1.0) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.AddCircle, contentDescription = "Aumentar", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                } else {
                                    FilledTonalIconButton(
                                        onClick = { viewModel.addToCart(product) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Adicionar")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Sales History
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (sales.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Default.ReceiptLong,
                            title = "Nenhuma venda registada",
                            description = "Todas as vendas à vista e a crédito efetuadas aparecerão listadas aqui com opção de anulação ou consulta."
                        )
                    }
                } else {
                    items(sales, key = { it.id }) { sale ->
                        val dateStr = remember(sale.timestamp) {
                            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "AO")).format(Date(sale.timestamp))
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Venda #${sale.id.takeLast(6)} • ${sale.customerName}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = "$dateStr • Pagamento: ${com.example.util.BusinessProfileUtils.formatPaymentMethod(sale.paymentMethod)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = formatCurrency(sale.totalAmount, currency),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = if (sale.isCancelled) StatusError else EmeraldPrimary
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (sale.isCancelled) StatusError.copy(alpha = 0.15f)
                                            else if (sale.paymentStatus == "PAID") EmeraldContainer
                                            else AmberContainer
                                        ) {
                                            Text(
                                                text = if (sale.isCancelled) "Cancelada" else if (sale.paymentStatus == "PAID") "Pago" else "A Crédito",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (sale.isCancelled) StatusError else if (sale.paymentStatus == "PAID") EmeraldOnContainer else AmberOnContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                if (sale.paymentStatus == "CREDIT" && sale.amountDue > 0 && !sale.isCancelled) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Valor em falta: ${formatCurrency(sale.amountDue, currency)} (Entrada paga: ${formatCurrency(sale.amountPaid, currency)})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AmberTertiary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                if (!sale.isCancelled) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = { receiptSale = sale },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.testTag("button_receipt_${sale.id}")
                                        ) {
                                            Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Recibo", fontSize = 12.sp)
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        TextButton(
                                            onClick = { saleToCancel = sale },
                                            colors = ButtonDefaults.textButtonColors(contentColor = StatusError),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Anular", fontSize = 12.sp)
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

    // Checkout Modal
    if (showCheckoutDialog) {
        CheckoutDialog(
            cartItems = cartItems,
            totalGross = cartTotal,
            currency = currency,
            onDismiss = { showCheckoutDialog = false },
            onConfirm = { customer, discount, isCredit, method, paidAmount, dueDate, notes ->
                viewModel.finalizeSale(
                    customerName = customer,
                    discount = discount,
                    paymentStatus = if (isCredit) "CREDIT" else "PAID",
                    paymentMethod = method,
                    amountPaid = paidAmount,
                    dueDate = dueDate,
                    notes = notes
                )
                showCheckoutDialog = false
            }
        )
    }

    // Cancel Sale Confirmation Dialog
    saleToCancel?.let { sale ->
        AlertDialog(
            onDismissRequest = { saleToCancel = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = StatusError) },
            title = { Text("Anular Venda #${sale.id.takeLast(6)}") },
            text = {
                Text(
                    "Tem a certeza de que deseja anular esta venda de ${formatCurrency(sale.totalAmount, currency)}?\n\nO stock de todos os itens vendidos será reposto imediatamente e o registo financeiro de caixa será estornado de forma consistente.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelSale(sale.id)
                        saleToCancel = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Confirmar Anulação")
                }
            },
            dismissButton = {
                TextButton(onClick = { saleToCancel = null }) {
                    Text("Voltar")
                }
            }
        )
    }

    // Receipt Dialog
    receiptSale?.let { sale ->
        ReceiptDialog(
            sale = sale,
            businessName = activeBusiness?.name ?: "Negócio na Prática",
            businessPhone = activeBusiness?.phone ?: "",
            currency = currency,
            viewModel = viewModel,
            onDismiss = { receiptSale = null }
        )
    }

    // Weight Sale Dialog (Requisito 3 - Mercearia / Vendas por Peso)
    weightProductSelected?.let { prod ->
        WeightSaleDialog(
            product = prod,
            currency = currency,
            onDismiss = { weightProductSelected = null },
            onConfirm = { quantityInKg, customDisplay ->
                viewModel.addToCartWithQuantity(prod, quantityInKg, customDisplay)
                weightProductSelected = null
            }
        )
    }
}

@Composable
fun CheckoutDialog(
    cartItems: List<com.example.data.repository.CartItem>,
    totalGross: Double,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (
        customer: String,
        discount: Double,
        isCredit: Boolean,
        method: String,
        amountPaid: Double,
        dueDate: Long?,
        notes: String
    ) -> Unit
) {
    var customerName by remember { mutableStateOf("Cliente Geral") }
    var discountStr by remember { mutableStateOf("0") }
    var isCredit by remember { mutableStateOf(false) }
    var selectedMethod by remember { mutableStateOf("CASH") }
    var amountPaidStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val discount = discountStr.toDoubleOrNull() ?: 0.0
    val totalNet = (totalGross - discount).coerceAtLeast(0.0)

    val methods = listOf(
        "CASH" to "Dinheiro (Numerário)",
        "MPESA" to "M-Pesa",
        "EMOLA" to "e-Mola",
        "MKESH" to "mKesh",
        "CARD" to "Cartão / POS",
        "TRANSFER" to "Transferência Bancária"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Finalizar Venda") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Cart summary
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Total a Pagar: ${formatCurrency(totalNet, currency)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                        if (discount > 0) {
                            Text(
                                text = "Desconto aplicado: -${formatCurrency(discount, currency)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusWarning
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Nome do Cliente *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_customer_name")
                )

                OutlinedTextField(
                    value = discountStr,
                    onValueChange = { discountStr = it },
                    label = { Text("Desconto ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Type of Sale: À Vista vs A Crédito
                Text(
                    text = "Condição de Pagamento:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isCredit,
                        onClick = { isCredit = false },
                        label = { Text("À Vista (Pago)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isCredit,
                        onClick = { isCredit = true },
                        label = { Text("A Crédito (Fiado)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (!isCredit) {
                    // Payment Method selector
                    Text(
                        text = "Forma de Recebimento:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(methods) { (key, label) ->
                            FilterChip(
                                selected = selectedMethod == key,
                                onClick = { selectedMethod = key },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                } else {
                    // Credit details
                    OutlinedTextField(
                        value = amountPaidStr,
                        onValueChange = { amountPaidStr = it },
                        label = { Text("Entrada paga agora ($currency)") },
                        placeholder = { Text("0.00 se nada for pago hoje") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val paidNow = amountPaidStr.toDoubleOrNull() ?: 0.0
                    val remaining = (totalNet - paidNow).coerceAtLeast(0.0)
                    Text(
                        text = "Saldo restante que ficará a dever: ${formatCurrency(remaining, currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmberTertiary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val paidAmount = if (isCredit) (amountPaidStr.toDoubleOrNull() ?: 0.0) else totalNet
                    val dueDate = if (isCredit) System.currentTimeMillis() + 86400000L * 15 else null
                    onConfirm(
                        customerName,
                        discount,
                        isCredit,
                        selectedMethod,
                        paidAmount,
                        dueDate,
                        notes
                    )
                },
                modifier = Modifier.testTag("button_confirm_sale")
            ) {
                Text("Confirmar Venda")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun ReceiptDialog(
    sale: SaleEntity,
    businessName: String,
    businessPhone: String,
    currency: String,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var items by remember { mutableStateOf<List<com.example.data.model.SaleItemEntity>>(emptyList()) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(sale.id) {
        items = viewModel.getSaleItems(sale.id)
    }

    val dateStr = remember(sale.timestamp) {
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("pt", "AO")).format(Date(sale.timestamp))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recibo de Venda", fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar")
                }
            }
        },
        text = {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(businessName, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    if (businessPhone.isNotBlank()) {
                        Text("Contacto: $businessPhone", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("Recibo Nº: #${sale.id.takeLast(8).uppercase()}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    Text("Data: $dateStr", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Cliente: ${sale.customerName}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Artigos:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))

                    items.forEach { item ->
                        val qtyText = if (item.originalQuantityDisplay.isNotBlank()) item.originalQuantityDisplay
                        else if (com.example.util.BusinessProfileUtils.isWeightUnit(item.unit)) com.example.util.BusinessProfileUtils.formatWeightDisplay(item.quantity)
                        else "${item.quantity.toInt()}x"

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("$qtyText ${item.productName}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text(formatCurrency(item.subtotal, currency), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))

                    if (sale.discount > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Desconto:", style = MaterialTheme.typography.bodySmall)
                            Text("-${formatCurrency(sale.discount, currency)}", style = MaterialTheme.typography.bodySmall, color = StatusWarning)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Líquido:", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(formatCurrency(sale.totalAmount, currency), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EmeraldPrimary)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Modalidade:", style = MaterialTheme.typography.bodySmall)
                        val methodDesc = com.example.util.BusinessProfileUtils.formatPaymentMethod(sale.paymentMethod)
                        Text(if (sale.paymentStatus == "PAID") "À Vista ($methodDesc)" else "A Crédito (Fiado)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }

                    if (sale.paymentStatus == "CREDIT" && sale.amountDue > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Valor em Falta:", style = MaterialTheme.typography.bodySmall, color = AmberTertiary)
                            Text(formatCurrency(sale.amountDue, currency), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = AmberTertiary)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Barcode graphic representation
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .background(Color.White, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth(0.85f).height(24.dp)) {
                            val barCount = 38
                            val barWidth = size.width / (barCount * 1.5f)
                            for (i in 0 until barCount) {
                                if (i % 3 != 0) {
                                    drawRect(
                                        color = Color.Black,
                                        topLeft = androidx.compose.ui.geometry.Offset(i * barWidth * 1.5f, 0f),
                                        size = androidx.compose.ui.geometry.Size(barWidth, size.height)
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = "*${sale.id.takeLast(12).uppercase()}*",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = 9.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val shareText = buildString {
                        appendLine("🧾 RECIBO DE VENDA - $businessName")
                        appendLine("Recibo: #${sale.id.takeLast(8).uppercase()}")
                        appendLine("Data: $dateStr")
                        appendLine("Cliente: ${sale.customerName}")
                        appendLine("------------------------------")
                        items.forEach {
                            val qtyDesc = if (it.originalQuantityDisplay.isNotBlank()) it.originalQuantityDisplay
                            else if (com.example.util.BusinessProfileUtils.isWeightUnit(it.unit)) com.example.util.BusinessProfileUtils.formatWeightDisplay(it.quantity)
                            else "${it.quantity.toInt()}x"
                            appendLine("$qtyDesc ${it.productName} = ${formatCurrency(it.subtotal, currency)}")
                        }
                        appendLine("------------------------------")
                        appendLine("TOTAL: ${formatCurrency(sale.totalAmount, currency)}")
                        val methodDesc = com.example.util.BusinessProfileUtils.formatPaymentMethod(sale.paymentMethod)
                        appendLine("PAGAMENTO: ${if (sale.paymentStatus == "PAID") "À Vista ($methodDesc)" else "A Crédito"}")
                        appendLine("Negócio na Prática • Gestão Comercial")
                    }
                    val sendIntent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    context.startActivity(android.content.Intent.createChooser(sendIntent, "Partilhar Recibo"))
                }
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Partilhar Recibo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightSaleDialog(
    product: ProductEntity,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (quantityInKg: Double, customDisplay: String) -> Unit
) {
    var inputMode by remember { mutableStateOf("g") } // "g" or "kg"
    var inputValue by remember { mutableStateOf("250") } // Default 250 g

    // Opções Rápidas de Quantidade (Requisito 3.D):
    // 100 g, 250 g, 500 g, 750 g, 1 kg, 1,5 kg, 2 kg
    val quickOptions = listOf(
        "100 g" to (100.0 to "g"),
        "250 g" to (250.0 to "g"),
        "500 g" to (500.0 to "g"),
        "750 g" to (750.0 to "g"),
        "1 kg" to (1.0 to "kg"),
        "1,5 kg" to (1.5 to "kg"),
        "2 kg" to (2.0 to "kg")
    )

    val cleanInput = inputValue.replace(",", ".").trim()
    val numericInput = cleanInput.toDoubleOrNull() ?: 0.0

    val (grams, kg) = if (inputMode == "g") {
        numericInput to (numericInput / 1000.0)
    } else {
        (numericInput * 1000.0) to numericInput
    }

    // Fórmula oficial do Requisito 3.C:
    // Preço da venda = (Quantidade em gramas ÷ 1.000) × Preço por quilograma
    val totalPrice = com.example.util.BusinessProfileUtils.calculateWeightPrice(grams, product.sellingPrice)

    val remainingStock = product.currentStock - kg
    val isStockExceeded = kg > product.currentStock

    val displayLabel = if (kg < 1.0) "${grams.toInt()} g" else if (kg % 1.0 == 0.0) "${kg.toInt()} kg" else String.format(Locale.US, "%.2f kg", kg)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Scale, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Venda por Peso", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(product.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Info Bar (Preço por kg e Stock disponível)
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateSurfaceDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Preço por kg:", style = MaterialTheme.typography.labelSmall, color = SlateTextSecondaryDark)
                            Text(
                                text = "${formatCurrency(product.sellingPrice, currency)} / kg",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Stock disponível:", style = MaterialTheme.typography.labelSmall, color = SlateTextSecondaryDark)
                            Text(
                                text = com.example.util.BusinessProfileUtils.formatWeightDisplay(product.currentStock),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (product.currentStock <= product.minStockAlert) AmberTertiary else EmeraldPrimary
                            )
                        }
                    }
                }

                // Opções Rápidas de Quantidade (Requisito 3.D)
                Text(
                    text = "Selecção Rápida de Quantidade:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(quickOptions) { (label, data) ->
                        val (valNum, mode) = data
                        val isSelected = (mode == inputMode && (
                                if (mode == "g") numericInput == valNum
                                else numericInput == valNum
                        ))
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                inputMode = mode
                                inputValue = if (valNum % 1.0 == 0.0) valNum.toInt().toString() else valNum.toString()
                            },
                            label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Campo de Introdução Manual e Selector de Unidade (g / kg)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputValue,
                        onValueChange = { inputValue = it },
                        label = { Text(if (inputMode == "g") "Quantidade em Gramas" else "Quantidade em Quilogramas") },
                        placeholder = { Text(if (inputMode == "g") "Ex: 250, 350, 750" else "Ex: 1, 1.5, 2.35") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_weight_quantity"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Alternador g vs kg
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(2.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (inputMode == "g") EmeraldPrimary else Color.Transparent,
                            modifier = Modifier.clickable {
                                if (inputMode != "g") {
                                    inputMode = "g"
                                    if (numericInput > 0) inputValue = (numericInput * 1000.0).toInt().toString()
                                }
                            }
                        ) {
                            Text(
                                "g",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                fontWeight = FontWeight.Bold,
                                color = if (inputMode == "g") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (inputMode == "kg") EmeraldPrimary else Color.Transparent,
                            modifier = Modifier.clickable {
                                if (inputMode != "kg") {
                                    inputMode = "kg"
                                    if (numericInput > 0) inputValue = String.format(Locale.US, "%.2f", numericInput / 1000.0)
                                }
                            }
                        ) {
                            Text(
                                "kg",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                fontWeight = FontWeight.Bold,
                                color = if (inputMode == "kg") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Cálculo Automático em Destaque (Requisito 3.C)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Fórmula: (Quantidade em g ÷ 1.000) × Preço por kg",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldOnContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "(${grams.toInt()} g ÷ 1.000) × ${String.format(Locale.US, "%.2f", product.sellingPrice)} MT",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldOnContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total a Pagar:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldOnContainer
                            )
                            Text(
                                text = formatCurrency(totalPrice, currency),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldOnContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Stock restante após venda: ${com.example.util.BusinessProfileUtils.formatWeightDisplay(remainingStock.coerceAtLeast(0.0))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldOnContainer.copy(alpha = 0.9f)
                        )
                    }
                }

                // Alerta de Stock Insuficiente (Requisito 3.E)
                if (isStockExceeded) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = StatusError.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = StatusError, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "A quantidade solicitada (${com.example.util.BusinessProfileUtils.formatWeightDisplay(kg)}) ultrapassa o stock disponível (${com.example.util.BusinessProfileUtils.formatWeightDisplay(product.currentStock)})!",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusError,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(kg, displayLabel)
                },
                enabled = kg > 0.0 && !isStockExceeded,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("button_confirm_weight_sale")
            ) {
                Text("Adicionar à Venda (${formatCurrency(totalPrice, currency)})", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

