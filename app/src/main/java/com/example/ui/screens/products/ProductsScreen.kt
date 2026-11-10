package com.example.ui.screens.products

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProductEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(viewModel: MainViewModel) {
    val activeBusiness by viewModel.activeBusiness.collectAsState()
    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val movements by viewModel.stockMovements.collectAsState()

    val currency = activeBusiness?.currencySymbol ?: "MT"

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var stockFilter by remember { mutableStateOf("ALL") } // "ALL", "LOW", "OUT"
    var showAddDialog by remember { mutableStateOf(false) }
    var showStockEntryDialog by remember { mutableStateOf<ProductEntity?>(null) }
    var showAdjustmentDialog by remember { mutableStateOf<ProductEntity?>(null) }
    var showMovementsDialog by remember { mutableStateOf(false) }

    val filteredProducts = remember(products, searchQuery, selectedCategoryId, stockFilter) {
        products.filter { prod ->
            val matchesSearch = prod.name.contains(searchQuery, ignoreCase = true) ||
                    prod.sku.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategoryId == null || prod.categoryId == selectedCategoryId
            val matchesStock = when (stockFilter) {
                "LOW" -> prod.currentStock > 0.0 && prod.currentStock <= prod.minStockAlert
                "OUT" -> prod.currentStock <= 0.0
                else -> true
            }
            matchesSearch && matchesCategory && matchesStock
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = EmeraldPrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_add_product")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Novo Produto", fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Stats & Search
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Produtos & Stock",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${products.size} produtos cadastrados no inventário",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = { showMovementsDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Histórico", fontSize = 12.sp)
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar por nome do produto ou SKU...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpar")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_search_product")
                )
            }

            // Category Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategoryId == null,
                            onClick = { selectedCategoryId = null },
                            label = { Text("Todos (${products.size})") }
                        )
                    }
                    items(categories) { cat ->
                        val count = products.count { it.categoryId == cat.id }
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = { selectedCategoryId = if (selectedCategoryId == cat.id) null else cat.id },
                            label = { Text("${cat.name} ($count)") }
                        )
                    }
                }
            }

            // Stock Level Filters
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = stockFilter == "ALL",
                        onClick = { stockFilter = "ALL" },
                        label = { Text("Todos os Níveis") }
                    )
                    val lowCount = products.count { it.currentStock > 0 && it.currentStock <= it.minStockAlert }
                    FilterChip(
                        selected = stockFilter == "LOW",
                        onClick = { stockFilter = if (stockFilter == "LOW") "ALL" else "LOW" },
                        label = { Text("Stock Baixo ($lowCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberContainer,
                            selectedLabelColor = AmberOnContainer
                        )
                    )
                    val outCount = products.count { it.currentStock <= 0 }
                    FilterChip(
                        selected = stockFilter == "OUT",
                        onClick = { stockFilter = if (stockFilter == "OUT") "ALL" else "OUT" },
                        label = { Text("Esgotados ($outCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StatusError.copy(alpha = 0.2f),
                            selectedLabelColor = StatusError
                        )
                    )
                }
            }

            // Product List Items
            if (filteredProducts.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.Inventory2,
                        title = "Nenhum produto encontrado",
                        description = if (searchQuery.isBlank()) "Registe os seus produtos para controlar o stock e calcular lucros automaticamente."
                        else "Não encontramos produtos para a busca '$searchQuery'.",
                        actionLabel = "Registar Primeiro Produto",
                        onAction = { showAddDialog = true }
                    )
                }
            } else {
                items(filteredProducts, key = { it.id }) { product ->
                    ProductCard(
                        product = product,
                        currency = currency,
                        onStockEntry = { showStockEntryDialog = product },
                        onStockAdjust = { showAdjustmentDialog = product }
                    )
                }
            }
        }
    }

    // Add Product Dialog
    if (showAddDialog) {
        AddProductDialog(
            categories = categories,
            defaultUnit = activeBusiness?.defaultUnit ?: "un",
            activityType = activeBusiness?.activityType ?: "",
            currency = currency,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, catId, sku, pPrice, sPrice, stock, minAlert, unit, sizes, colors, isPaid, debtAmount, supplier, isWeight, wUnit ->
                viewModel.addProduct(
                    name = name,
                    categoryId = catId,
                    sku = sku,
                    purchasePrice = pPrice,
                    sellingPrice = sPrice,
                    initialStock = stock,
                    minStockAlert = minAlert,
                    unit = unit,
                    sizes = sizes,
                    colors = colors,
                    isPaidToSupplier = isPaid,
                    supplierDebtAmount = debtAmount,
                    supplierName = supplier,
                    isSoldByWeight = isWeight,
                    weightUnit = wUnit
                )
                showAddDialog = false
            }
        )
    }

    // Stock Entry / Purchase Dialog
    showStockEntryDialog?.let { product ->
        StockEntryDialog(
            product = product,
            currency = currency,
            onDismiss = { showStockEntryDialog = null },
            onConfirm = { qty, unitCost, supplier, paid ->
                viewModel.registerStockPurchase(product.id, qty, unitCost, supplier, paid)
                showStockEntryDialog = null
            }
        )
    }

    // Stock Adjustment / Loss Dialog
    showAdjustmentDialog?.let { product ->
        StockAdjustmentDialog(
            product = product,
            onDismiss = { showAdjustmentDialog = null },
            onConfirm = { diff, reason ->
                viewModel.registerStockAdjustment(product.id, diff, reason)
                showAdjustmentDialog = null
            }
        )
    }

    // Movements History Modal
    if (showMovementsDialog) {
        StockMovementsDialog(
            movements = movements,
            currency = currency,
            onDismiss = { showMovementsDialog = false }
        )
    }
}

@Composable
fun ProductCard(
    product: ProductEntity,
    currency: String,
    onStockEntry: () -> Unit,
    onStockAdjust: () -> Unit
) {
    val margin = if (product.purchasePrice > 0) {
        ((product.sellingPrice - product.purchasePrice) / product.purchasePrice) * 100.0
    } else 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val isWeight = product.isSoldByWeight || com.example.util.BusinessProfileUtils.isWeightUnit(product.unit)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isWeight) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = EmeraldContainer
                            ) {
                                Text(
                                    text = "⚖️ Venda por Peso",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldOnContainer,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    if (product.sku.isNotBlank()) {
                        Text(
                            text = "SKU: ${product.sku}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (product.sizes.isNotBlank()) {
                        Text(
                            text = "Tamanhos: ${product.sizes}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (!product.isPaidToSupplier && product.supplierDebtAmount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = StatusError.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "⚠️ Dívida Fornecedor: ${formatCurrency(product.supplierDebtAmount, currency)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusError,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                StockBadge(
                    currentStock = product.currentStock,
                    minAlert = product.minStockAlert,
                    unit = product.unit
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val isWeightProduct = product.isSoldByWeight || com.example.util.BusinessProfileUtils.isWeightUnit(product.unit)
            val unitSuffix = if (isWeightProduct) " / kg" else if (product.unit.isNotBlank()) " / ${product.unit}" else ""

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (isWeightProduct) "Preço Venda (por kg)" else "Preço de Venda",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(product.sellingPrice, currency) + unitSuffix,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                }

                Column {
                    Text(
                        text = if (isWeightProduct) "Preço Custo (por kg)" else "Preço de Custo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(product.purchasePrice, currency) + unitSuffix,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Margem Bruta",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "%.1f%%".format(margin),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (margin >= 30) EmeraldPrimary else StatusWarning
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onStockEntry,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Entrada", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onStockAdjust,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ajustar", fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductDialog(
    categories: List<com.example.data.model.CategoryEntity>,
    defaultUnit: String,
    activityType: String,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        categoryId: String,
        sku: String,
        purchasePrice: Double,
        sellingPrice: Double,
        initialStock: Double,
        minStockAlert: Double,
        unit: String,
        sizes: String,
        colors: String,
        isPaidToSupplier: Boolean,
        supplierDebtAmount: Double,
        supplierName: String,
        isSoldByWeight: Boolean,
        weightUnit: String
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf(categories.firstOrNull()?.id ?: "") }
    var sku by remember { mutableStateOf("") }
    var purchasePriceStr by remember { mutableStateOf("") }
    var sellingPriceStr by remember { mutableStateOf("") }
    var initialStockStr by remember { mutableStateOf("0") }
    var minStockAlertStr by remember { mutableStateOf("5") }
    var selectedUnit by remember { mutableStateOf(if (com.example.util.BusinessProfileUtils.isWeightRelevantBusiness(activityType)) "kg" else defaultUnit) }
    var customUnit by remember { mutableStateOf("") }
    var sizes by remember { mutableStateOf("") }
    var colors by remember { mutableStateOf("") }
    var isPaidToSupplier by remember { mutableStateOf(true) }
    var supplierDebtAmountStr by remember { mutableStateOf("") }
    var supplierName by remember { mutableStateOf("") }

    val effectiveUnit = if (selectedUnit == "outro") customUnit.trim().ifBlank { "un" } else selectedUnit
    val isWeightProduct = com.example.util.BusinessProfileUtils.isWeightUnit(effectiveUnit)
    val isLiquidProduct = com.example.util.BusinessProfileUtils.isLiquidUnit(effectiveUnit)

    val purchasePrice = purchasePriceStr.toDoubleOrNull() ?: 0.0
    val sellingPrice = sellingPriceStr.toDoubleOrNull() ?: 0.0
    val margin = if (purchasePrice > 0) ((sellingPrice - purchasePrice) / purchasePrice) * 100.0 else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isWeightProduct) Icons.Default.Scale else Icons.Default.AddBox,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isWeightProduct) "Novo Produto por Peso (kg)" else "Novo Produto & Fornecedor")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Banner explicativo se for produto vendido por peso (Mercearia, Talho, etc.)
                if (isWeightProduct) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = EmeraldContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚖️ Vendas por Peso: No balcão poderá vender fracções (100g, 250g, 500g, 750g, 1,5kg...). O preço total e stock são calculados automaticamente por (gramas ÷ 1.000) × Preço/kg.",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldOnContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (isWeightProduct) "Nome do Produto (ex: Açúcar, Arroz, Carne) *" else "Nome do Produto *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_new_prod_name")
                )

                // Seleção de Unidade de Medida
                Text(
                    text = "Unidade de Medida:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val unitOptions = listOf("kg", "g", "L", "ml", "un", "pct", "cx", "par", "pç", "outro")
                    items(unitOptions) { u ->
                        FilterChip(
                            selected = selectedUnit == u,
                            onClick = { selectedUnit = u },
                            label = { Text(u) }
                        )
                    }
                }

                if (selectedUnit == "outro") {
                    OutlinedTextField(
                        value = customUnit,
                        onValueChange = { customUnit = it },
                        label = { Text("Nome da Unidade Personalizada") },
                        placeholder = { Text("Ex: metro, fardo, lata") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = purchasePriceStr,
                        onValueChange = { purchasePriceStr = it },
                        label = { Text(if (isWeightProduct) "Custo por kg" else "Preço Custo") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sellingPriceStr,
                        onValueChange = { sellingPriceStr = it },
                        label = { Text(if (isWeightProduct) "Venda por kg *" else "Preço Venda *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_new_prod_price")
                    )
                }

                if (purchasePrice > 0 && sellingPrice > 0) {
                    Text(
                        text = "Margem de lucro prevista: %.1f%%".format(margin),
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = initialStockStr,
                        onValueChange = { initialStockStr = it },
                        label = { Text(if (isWeightProduct) "Stock Inicial (kg)" else "Stock Inicial") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minStockAlertStr,
                        onValueChange = { minStockAlertStr = it },
                        label = { Text(if (isWeightProduct) "Alerta Mín. (kg)" else "Alerta Mín.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text("Código de Barras / SKU (Opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (com.example.util.BusinessProfileUtils.isApparelBusiness(activityType)) {
                    OutlinedTextField(
                        value = sizes,
                        onValueChange = { sizes = it },
                        label = { Text("Tamanhos disponíveis (ex: S, M, L, XL ou 38-44)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = colors,
                        onValueChange = { colors = it },
                        label = { Text("Cores disponíveis (ex: Preto, Branco, Azul)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                Text(
                    text = "Condições com Fornecedor",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isPaidToSupplier,
                        onClick = {
                            isPaidToSupplier = true
                            supplierDebtAmountStr = ""
                        },
                        label = { Text("Pago ao Fornecedor") },
                        leadingIcon = if (isPaidToSupplier) {
                            { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                    FilterChip(
                        selected = !isPaidToSupplier,
                        onClick = {
                            isPaidToSupplier = false
                            val stock = initialStockStr.toDoubleOrNull() ?: 0.0
                            val totalCost = purchasePrice * stock
                            if (totalCost > 0 && supplierDebtAmountStr.isBlank()) {
                                supplierDebtAmountStr = "%.2f".format(totalCost)
                            }
                        },
                        label = { Text("Mercadoria a Dívida") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = StatusError.copy(alpha = 0.15f),
                            selectedLabelColor = StatusError
                        ),
                        leadingIcon = if (!isPaidToSupplier) {
                            { Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp), tint = StatusError) }
                        } else null
                    )
                }

                if (!isPaidToSupplier) {
                    OutlinedTextField(
                        value = supplierName,
                        onValueChange = { supplierName = it },
                        label = { Text("Nome / Contacto do Fornecedor") },
                        placeholder = { Text("Ex: Fornecedor Central, Distribuidora") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val stock = initialStockStr.toDoubleOrNull() ?: 0.0
                    val totalCost = purchasePrice * stock
                    OutlinedTextField(
                        value = supplierDebtAmountStr,
                        onValueChange = { supplierDebtAmountStr = it },
                        label = { Text("Valor que tenho em Dívida ($currency) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (totalCost > 0) {
                                TextButton(
                                    onClick = { supplierDebtAmountStr = "%.2f".format(totalCost) },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Text("Custo Total", fontSize = 11.sp)
                                }
                            }
                        }
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "💡 Esta dívida será automaticamente lançada em Contas a Pagar nas Finanças para que não perca o controlo.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val debtAmount = if (!isPaidToSupplier) (supplierDebtAmountStr.toDoubleOrNull() ?: 0.0) else 0.0
                    onConfirm(
                        name,
                        selectedCategoryId,
                        sku,
                        purchasePrice,
                        sellingPrice,
                        initialStockStr.toDoubleOrNull() ?: 0.0,
                        minStockAlertStr.toDoubleOrNull() ?: 5.0,
                        effectiveUnit,
                        sizes,
                        colors,
                        isPaidToSupplier,
                        debtAmount,
                        supplierName,
                        isWeightProduct,
                        if (isWeightProduct) "kg" else effectiveUnit
                    )
                },
                enabled = name.isNotBlank() && sellingPrice > 0.0
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun StockEntryDialog(
    product: ProductEntity,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (quantity: Double, unitCost: Double, supplierName: String, paidImmediately: Boolean) -> Unit
) {
    var quantityStr by remember { mutableStateOf("") }
    var unitCostStr by remember { mutableStateOf(if (product.purchasePrice > 0) product.purchasePrice.toString() else "") }
    var supplierName by remember { mutableStateOf("") }
    var paidImmediately by remember { mutableStateOf(true) }

    val qty = quantityStr.toDoubleOrNull() ?: 0.0
    val cost = unitCostStr.toDoubleOrNull() ?: product.purchasePrice
    val totalCost = qty * cost

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registar Entrada / Compra") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Produto: ${product.name}\nStock atual: ${product.currentStock.toInt()} ${product.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text("Quantidade a adicionar (${product.unit}) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_entry_qty")
                )

                OutlinedTextField(
                    value = unitCostStr,
                    onValueChange = { unitCostStr = it },
                    label = { Text("Custo unitário de compra ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = supplierName,
                    onValueChange = { supplierName = it },
                    label = { Text("Nome do Fornecedor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (totalCost > 0) {
                    Text(
                        text = "Total da Compra: ${formatCurrency(totalCost, currency)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = paidImmediately, onCheckedChange = { paidImmediately = it })
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (paidImmediately) "Pago imediatamente no caixa" else "A prazo (Registado como conta a pagar)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(qty, cost, supplierName, paidImmediately) },
                enabled = qty > 0.0
            ) {
                Text("Confirmar Entrada")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun StockAdjustmentDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onConfirm: (diff: Double, reason: String) -> Unit
) {
    var isLoss by remember { mutableStateOf(true) }
    var quantityStr by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    val reasons = listOf("Produto danificado", "Validade expirada", "Diferença em contagem", "Devolução ao fornecedor", "Outro")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajuste / Perda de Stock") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${product.name} (Stock atual: ${product.currentStock.toInt()} ${product.unit})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isLoss,
                        onClick = { isLoss = true },
                        label = { Text("Perda / Saída (-)") }
                    )
                    FilterChip(
                        selected = !isLoss,
                        onClick = { isLoss = false },
                        label = { Text("Ajuste de Ganho (+)") }
                    )
                }

                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text("Quantidade (${product.unit}) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Motivo do ajuste *") },
                    placeholder = { Text("Ex: Quebra, contagem, validade") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val qty = quantityStr.toDoubleOrNull() ?: 0.0
            Button(
                onClick = {
                    val finalDiff = if (isLoss) -qty else qty
                    onConfirm(finalDiff, reason)
                },
                enabled = qty > 0.0
            ) {
                Text("Gravar Ajuste")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun StockMovementsDialog(
    movements: List<com.example.data.model.StockMovementEntity>,
    currency: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Histórico de Movimentos de Stock") },
        text = {
            if (movements.isEmpty()) {
                Text("Ainda não existem movimentações de stock registadas.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(movements) { mov ->
                        val dateStr = remember(mov.timestamp) {
                            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "AO")).format(Date(mov.timestamp))
                        }
                        val isPositive = mov.type == "PURCHASE" || mov.type == "ADJUSTMENT_GAIN" || mov.type == "RETURN"

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = mov.productName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        text = "${mov.reason} • $dateStr",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = "${if (isPositive) "+" else "-"}${mov.quantity}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPositive) EmeraldPrimary else StatusWarning,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        }
    )
}
