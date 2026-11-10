package com.example.ui.screens.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.data.repository.DreReportData
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ReportPeriod

@Composable
fun ReportsScreen(viewModel: MainViewModel) {
    val activeBusiness by viewModel.activeBusiness.collectAsState()
    val dreReport by viewModel.dreReport.collectAsState()
    val reportPeriod by viewModel.reportPeriod.collectAsState()
    val products by viewModel.products.collectAsState()

    val currency = activeBusiness?.currencySymbol ?: "MT"
    var dreViewMode by remember { mutableStateOf(0) } // 0: Gráfico, 1: Planilha, 2: Ambos

    val inventoryCostValue = remember(products) {
        products.sumOf { it.currentStock * it.purchasePrice }
    }
    val inventoryRetailValue = remember(products) {
        products.sumOf { it.currentStock * it.sellingPrice }
    }

    LaunchedEffect(reportPeriod) {
        viewModel.refreshDreReport()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title & Period Selector with Export Action
        item {
            val context = androidx.compose.ui.platform.LocalContext.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Relatórios & DRE",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Demonstração do Resultado com dados reais",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = {
                        val dre = dreReport
                        val periodLabel = when (reportPeriod) {
                            ReportPeriod.TODAY -> "Hoje"
                            ReportPeriod.WEEK -> "Últimos 7 Dias"
                            ReportPeriod.MONTH -> "Mês Atual"
                            ReportPeriod.ALL -> "Todo o Período"
                        }
                        val shareText = buildString {
                            appendLine("📊 DRE - DEMONSTRAÇÃO DO RESULTADO DO EXERCÍCIO")
                            appendLine("Empresa: ${activeBusiness?.name ?: "Negócio na Prática"}")
                            appendLine("Período: $periodLabel")
                            appendLine("---------------------------------------")
                            appendLine("(+) Receita Bruta de Vendas: ${formatCurrency(dre?.grossRevenue ?: 0.0, currency)}")
                            appendLine("(-) Custo das Mercadorias (CMV): ${formatCurrency(dre?.costOfGoodsSold ?: 0.0, currency)}")
                            appendLine("(=) Lucro Bruto: ${formatCurrency(dre?.grossProfit ?: 0.0, currency)} (Margem: %.1f%%)".format(dre?.grossMarginPercentage ?: 0.0))
                            appendLine("(-) Despesas Operacionais: ${formatCurrency(dre?.operationalExpenses ?: 0.0, currency)}")
                            appendLine("---------------------------------------")
                            appendLine("(=) RESULTADO / LUCRO LÍQUIDO: ${formatCurrency(dre?.netProfit ?: 0.0, currency)}")
                            appendLine("Saldo em Caixa Disponível: ${formatCurrency(dre?.currentCashBalance ?: 0.0, currency)}")
                            appendLine("Dívidas a Receber: ${formatCurrency(dre?.pendingReceivables ?: 0.0, currency)}")
                            appendLine("Dívidas a Pagar: ${formatCurrency(dre?.pendingPayables ?: 0.0, currency)}")
                            appendLine("---------------------------------------")
                            appendLine("Negócio na Prática • Gestão Comercial Inteligente")
                        }
                        val sendIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(android.content.Intent.createChooser(sendIntent, "Exportar Relatório DRE"))
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Exportar", fontSize = 12.sp)
                }
            }
        }

        // Period filter chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = reportPeriod == ReportPeriod.TODAY,
                        onClick = { viewModel.setReportPeriod(ReportPeriod.TODAY) },
                        label = { Text("Hoje") },
                        modifier = Modifier.testTag("filter_today")
                    )
                }
                item {
                    FilterChip(
                        selected = reportPeriod == ReportPeriod.WEEK,
                        onClick = { viewModel.setReportPeriod(ReportPeriod.WEEK) },
                        label = { Text("Últimos 7 Dias") },
                        modifier = Modifier.testTag("filter_week")
                    )
                }
                item {
                    FilterChip(
                        selected = reportPeriod == ReportPeriod.MONTH,
                        onClick = { viewModel.setReportPeriod(ReportPeriod.MONTH) },
                        label = { Text("Mês Atual") },
                        modifier = Modifier.testTag("filter_month")
                    )
                }
                item {
                    FilterChip(
                        selected = reportPeriod == ReportPeriod.ALL,
                        onClick = { viewModel.setReportPeriod(ReportPeriod.ALL) },
                        label = { Text("Todo o Período") },
                        modifier = Modifier.testTag("filter_all")
                    )
                }
            }
        }

        // DRE Presentation Format Selector (Gráfico vs Planilha)
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("dre_view_selector"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = dreViewMode == 0,
                        onClick = { dreViewMode = 0 },
                        label = { Text("📊 Gráfico DRE") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = dreViewMode == 1,
                        onClick = { dreViewMode = 1 },
                        label = { Text("📑 Planilha DRE") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = dreViewMode == 2,
                        onClick = { dreViewMode = 2 },
                        label = { Text("Ambos") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 1. DRE Chart View (Gráfico Comparativo)
        if (dreViewMode == 0 || dreViewMode == 2) {
            item {
                DreChartView(
                    dreReport = dreReport,
                    currency = currency
                )
            }
        }

        // 2. DRE Spreadsheet View (Planilha Estruturada)
        if (dreViewMode == 1 || dreViewMode == 2) {
            item {
                DreSpreadsheetView(
                    dreReport = dreReport,
                    currency = currency,
                    businessName = activeBusiness?.name ?: "Negócio na Prática"
                )
            }
        }

        // Cash vs Profit Distinction Card (Prompt Principle)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = SkySecondary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Diferença entre Caixa e Lucro",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Saldo de Caixa (${formatCurrency(dreReport?.currentCashBalance ?: 0.0, currency)}): O dinheiro vivo atualmente disponível na conta.\n" +
                                "• Lucro Líquido (${formatCurrency(dreReport?.netProfit ?: 0.0, currency)}): Receita menos o custo do que foi vendido e despesas.\n" +
                                "• Dívidas a Receber (${formatCurrency(dreReport?.pendingReceivables ?: 0.0, currency)}): Dinheiro de vendas a crédito que ainda vai entrar no caixa.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Inventory Valuation Card
        item {
            SectionHeader(title = "Avaliação de Inventário / Stock")
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Stock a Preço Custo",
                    value = formatCurrency(inventoryCostValue, currency),
                    icon = Icons.Default.Inventory,
                    subtitle = "Capital empatado em mercadorias",
                    iconColor = SkySecondary,
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "Stock a Preço Venda",
                    value = formatCurrency(inventoryRetailValue, currency),
                    icon = Icons.Default.LocalOffer,
                    subtitle = "Potencial de faturação bruta",
                    iconColor = EmeraldPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Top Selling Products
        item {
            SectionHeader(title = "Produtos Mais Vendidos no Período")
        }

        val topProducts = dreReport?.topSellingProducts ?: emptyList()
        if (topProducts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Nenhuma venda efetuada no intervalo selecionado.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(topProducts) { prod ->
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
                        Column {
                            Text(
                                text = prod.productName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Quantidade vendida: ${prod.totalQuantity.toInt()} unidades",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = formatCurrency(prod.totalRevenue, currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DreLineItem(
    prefix: String,
    title: String,
    value: String,
    color: Color,
    subtitle: String? = null,
    isBold: Boolean = false,
    fontSize: androidx.compose.ui.unit.TextUnit = 15.sp
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$prefix ",
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontSize = fontSize
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
                    fontSize = fontSize
                )
            }

            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                color = color,
                fontSize = fontSize
            )
        }

        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 24.dp)
            )
        }
    }
}

@Composable
fun DreChartView(
    dreReport: DreReportData?,
    currency: String
) {
    val grossRev = dreReport?.grossRevenue ?: 0.0
    val cmv = dreReport?.costOfGoodsSold ?: 0.0
    val grossProfit = dreReport?.grossProfit ?: 0.0
    val expenses = dreReport?.operationalExpenses ?: 0.0
    val netProfit = dreReport?.netProfit ?: 0.0

    val maxVal = maxOf(grossRev, cmv + expenses, 1.0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_dre_chart"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BarChart, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Gráfico Comparativo do DRE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldContainer
                ) {
                    Text(
                        text = "Visual M3",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldOnContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bars
            DreBarItem(
                label = "Receita Bruta (+)",
                value = grossRev,
                fraction = if (maxVal > 0) (grossRev / maxVal).toFloat().coerceIn(0.05f, 1f) else 0.05f,
                barColor = EmeraldPrimary,
                currency = currency,
                percentage = "100%"
            )

            Spacer(modifier = Modifier.height(12.dp))

            DreBarItem(
                label = "Custo Mercadorias / CMV (-)",
                value = cmv,
                fraction = if (maxVal > 0 && cmv > 0) (cmv / maxVal).toFloat().coerceIn(0.05f, 1f) else 0f,
                barColor = StatusError,
                currency = currency,
                percentage = if (grossRev > 0) "%.1f%%".format((cmv / grossRev) * 100) else "0%"
            )

            Spacer(modifier = Modifier.height(12.dp))

            DreBarItem(
                label = "Lucro Bruto (=)",
                value = grossProfit,
                fraction = if (maxVal > 0 && grossProfit > 0) (grossProfit / maxVal).toFloat().coerceIn(0.05f, 1f) else 0f,
                barColor = SkySecondary,
                currency = currency,
                percentage = "%.1f%%".format(dreReport?.grossMarginPercentage ?: 0.0)
            )

            Spacer(modifier = Modifier.height(12.dp))

            DreBarItem(
                label = "Despesas Operacionais (-)",
                value = expenses,
                fraction = if (maxVal > 0 && expenses > 0) (expenses / maxVal).toFloat().coerceIn(0.05f, 1f) else 0f,
                barColor = AmberTertiary,
                currency = currency,
                percentage = if (grossRev > 0) "%.1f%%".format((expenses / grossRev) * 100) else "0%"
            )

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.height(14.dp))

            DreBarItem(
                label = "Resultado / Lucro Líquido (=)",
                value = netProfit,
                fraction = if (maxVal > 0 && Math.abs(netProfit) > 0) (Math.abs(netProfit) / maxVal).toFloat().coerceIn(0.05f, 1f) else 0f,
                barColor = if (netProfit >= 0) EmeraldPrimary else StatusError,
                currency = currency,
                percentage = if (grossRev > 0) "%.1f%%".format((netProfit / grossRev) * 100) else "0%",
                isResult = true
            )
        }
    }
}

@Composable
fun DreBarItem(
    label: String,
    value: Double,
    fraction: Float,
    barColor: Color,
    currency: String,
    percentage: String,
    isResult: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = if (isResult) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                fontWeight = if (isResult) FontWeight.Bold else FontWeight.Medium
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatCurrency(value, currency),
                    fontWeight = FontWeight.Bold,
                    color = barColor,
                    style = if (isResult) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = barColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = percentage,
                        style = MaterialTheme.typography.labelSmall,
                        color = barColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isResult) 14.dp else 10.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .clip(RoundedCornerShape(6.dp))
                        .background(barColor)
                )
            }
        }
    }
}

@Composable
fun DreSpreadsheetView(
    dreReport: DreReportData?,
    currency: String,
    businessName: String
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val grossRev = dreReport?.grossRevenue ?: 0.0
    val cmv = dreReport?.costOfGoodsSold ?: 0.0
    val grossProfit = dreReport?.grossProfit ?: 0.0
    val expenses = dreReport?.operationalExpenses ?: 0.0
    val netProfit = dreReport?.netProfit ?: 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_dre_spreadsheet"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TableChart, contentDescription = null, tint = SkySecondary, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Planilha Estruturada (DRE)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tabela Contábil com Análise Vertical",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FilledTonalButton(
                    onClick = {
                        val csv = buildString {
                            appendLine("Código,Rubrica Contábil,Natureza,Valor ($currency),Percentual s/ Vendas")
                            appendLine("1.0,Receita Operacional Bruta,(+),${grossRev},100.0%")
                            appendLine("2.0,Custo das Mercadorias Vendidas (CMV),(-),${cmv},${if (grossRev > 0) "%.1f%%".format((cmv / grossRev) * 100) else "0%"}")
                            appendLine("3.0,LUCRO BRUTO DO EXERCÍCIO,(=),${grossProfit},${"%.1f%%".format(dreReport?.grossMarginPercentage ?: 0.0)}")
                            appendLine("4.0,Despesas Operacionais e Pró-Labore,(-),${expenses},${if (grossRev > 0) "%.1f%%".format((expenses / grossRev) * 100) else "0%"}")
                            appendLine("5.0,RESULTADO / LUCRO LÍQUIDO,(=),${netProfit},${if (grossRev > 0) "%.1f%%".format((netProfit / grossRev) * 100) else "0%"}")
                            appendLine(",,,")
                            appendLine("Saldo de Caixa Disponível,,${dreReport?.currentCashBalance ?: 0.0},")
                            appendLine("Dívidas a Receber (Crédito Clientes),,${dreReport?.pendingReceivables ?: 0.0},")
                            appendLine("Dívidas a Pagar (Fornecedores),,${dreReport?.pendingPayables ?: 0.0},")
                        }
                        WebExportHelper.exportAndShareCsv(context, "Planilha_DRE_${businessName.replace(" ", "_")}.csv", csv)
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Baixar CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Rubrica Contábil", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.8f))
                Text("Tipo", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.6f))
                Text("Valor ($currency)", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.4f))
                Text("% Vendas", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.9f))
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Rows
            SpreadsheetRow(
                code = "1.0",
                description = "Receita Bruta das Vendas",
                nature = "(+)",
                value = formatCurrency(grossRev, currency),
                percentage = "100.0%",
                color = EmeraldPrimary,
                isBold = true
            )

            SpreadsheetRow(
                code = "2.0",
                description = "(-) Custo das Mercadorias (CMV)",
                nature = "(-)",
                value = formatCurrency(cmv, currency),
                percentage = if (grossRev > 0) "%.1f%%".format((cmv / grossRev) * 100) else "0.0%",
                color = StatusError,
                isEven = true
            )

            SpreadsheetRow(
                code = "3.0",
                description = "(=) Lucro Bruto",
                nature = "(=)",
                value = formatCurrency(grossProfit, currency),
                percentage = "%.1f%%".format(dreReport?.grossMarginPercentage ?: 0.0),
                color = EmeraldPrimary,
                isBold = true
            )

            SpreadsheetRow(
                code = "4.0",
                description = "(-) Despesas Operacionais",
                nature = "(-)",
                value = formatCurrency(expenses, currency),
                percentage = if (grossRev > 0) "%.1f%%".format((expenses / grossRev) * 100) else "0.0%",
                color = StatusError,
                isEven = true
            )

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), thickness = 1.5.dp)

            SpreadsheetRow(
                code = "5.0",
                description = "(=) RESULTADO / LUCRO LÍQUIDO",
                nature = "(=)",
                value = formatCurrency(netProfit, currency),
                percentage = if (grossRev > 0) "%.1f%%".format((netProfit / grossRev) * 100) else "0.0%",
                color = if (netProfit >= 0) EmeraldPrimary else StatusError,
                isBold = true,
                isHighlighted = true
            )
        }
    }
}

@Composable
fun SpreadsheetRow(
    code: String,
    description: String,
    nature: String,
    value: String,
    percentage: String,
    color: Color,
    isBold: Boolean = false,
    isEven: Boolean = false,
    isHighlighted: Boolean = false
) {
    val bg = if (isHighlighted) color.copy(alpha = 0.12f)
    else if (isEven) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.8f)) {
            Text(
                text = "$code $description",
                fontSize = 11.sp,
                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                color = if (isHighlighted) color else MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = nature,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.weight(0.6f)
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = if (isHighlighted) color else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1.4f)
        )
        Text(
            text = percentage,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.weight(0.9f)
        )
    }
}

