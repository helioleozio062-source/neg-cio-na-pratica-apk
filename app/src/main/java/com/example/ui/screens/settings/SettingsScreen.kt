package com.example.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BusinessEntity
import com.example.ui.components.ChangePasswordDialog
import com.example.ui.components.SectionHeader
import com.example.ui.components.WebExportHelper
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.StatusError
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val activeBusiness by viewModel.activeBusiness.collectAsState()
    val businesses by viewModel.businesses.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    var showEditBusinessDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showAdminLoginDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Profile Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentUser?.fullName?.take(2)?.uppercase() ?: "US",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser?.fullName ?: "Utilizador",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentUser?.email ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (currentUser?.phone?.isNotBlank() == true) {
                            Text(
                                text = "Tel: ${currentUser?.phone}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (currentUser?.role == "ADMIN") {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Admin",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Active Business Details
        item {
            SectionHeader(
                title = "Negócio Atual",
                actionText = "Editar Dados",
                onActionClick = { showEditBusinessDialog = true }
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = activeBusiness?.name ?: "Sem negócio ativo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ramo de Atividade: ${activeBusiness?.activityType}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Moeda Padrão: ${activeBusiness?.currencySymbol} • Unidade de Medida: ${activeBusiness?.defaultUnit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (activeBusiness?.address?.isNotBlank() == true) {
                        Text(
                            text = "Endereço: ${activeBusiness?.address}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Multi-Business Switcher
        item {
            SectionHeader(
                title = "Os Meus Negócios (${businesses.size})",
                actionText = "+ Criar Outro Negócio",
                onActionClick = { viewModel.navigateTo(AppScreen.BUSINESS_SETUP) }
            )
        }

        items(businesses) { b ->
            val isCurrent = b.id == activeBusiness?.id
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.selectBusiness(b) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.surface
                ),
                border = if (isCurrent) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(EmeraldPrimary)) else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = if (isCurrent) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = b.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(text = "${b.activityType} • ${b.currencySymbol}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (isCurrent) {
                        Surface(shape = RoundedCornerShape(6.dp), color = EmeraldPrimary) {
                            Text("Ativo", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    } else {
                        TextButton(onClick = { viewModel.selectBusiness(b) }) {
                            Text("Mudar")
                        }
                    }
                }
            }
        }

        // Theme & Appearance (Modo Noturno)
        item {
            SectionHeader(title = "Aparência da Aplicação")
        }

        item {
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
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = EmeraldPrimary
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Modo Noturno (Dark Mode)",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isDarkMode) "Tema escuro ativado" else "Tema claro ativado",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { viewModel.setDarkMode(it) },
                        modifier = Modifier.testTag("switch_dark_mode")
                    )
                }
            }
        }

        // Quick Navigation to Other Sections
        item {
            SectionHeader(title = "Outras Áreas")
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("Avaliações, Reclamações & Sugestões") },
                        supportingContent = { Text("Fale com a equipa de suporte ou acompanhe o estado dos pedidos") },
                        leadingContent = { Icon(Icons.Default.Feedback, contentDescription = null, tint = EmeraldPrimary) },
                        modifier = Modifier.clickable { viewModel.navigateTo(AppScreen.FEEDBACK) }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ListItem(
                        headlineContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Painel do Administrador Geral", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (currentUser?.role == "ADMIN") EmeraldPrimary else MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = if (currentUser?.role == "ADMIN") "Sessão Ativa" else "Admin",
                                        color = if (currentUser?.role == "ADMIN") Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        },
                        supportingContent = { Text("admin@negocionapratica.com • Gestão global, métricas e segurança") },
                        leadingContent = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = EmeraldPrimary) },
                        modifier = Modifier.clickable {
                            if (currentUser?.role == "ADMIN") {
                                viewModel.navigateTo(AppScreen.ADMIN_PANEL)
                            } else {
                                showAdminLoginDialog = true
                            }
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                }
            }
        }

        // Account Security & Password/Code Management
        item {
            SectionHeader(title = "Segurança da Conta")
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (currentUser?.role == "ADMIN") StatusError else EmeraldPrimary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (currentUser?.role == "ADMIN") "Código do Administrador Geral" else "Palavra-passe de Acesso",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Mantenha o acesso protegido e privado",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (currentUser?.role == "ADMIN")
                            "Troque o código padrão ('admin123') para um código exclusivo seu. Assim, mais ninguém conseguirá aceder ao painel de administração geral sem a sua autorização."
                        else
                            "Altere a sua palavra-passe ou código de acesso regularmente para manter os dados do seu negócio seguros.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { showChangePasswordDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("button_settings_change_password"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (currentUser?.role == "ADMIN") StatusError else EmeraldPrimary
                        )
                    ) {
                        Icon(Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentUser?.role == "ADMIN") "Trocar Código do Administrador" else "Alterar Palavra-passe",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Web Portal & index.html Export
        item {
            SectionHeader(title = "Portal Web & Computador")
        }

        item {
            val context = LocalContext.current
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Código Fonte Web (index.html)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Aplicação web completa sincronizada para computador (Metical MT)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Baixe o ficheiro index.html autossuficiente para usar no computador, abrir offline em qualquer navegador ou hospedar no seu servidor próprio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { WebExportHelper.exportAndShareIndexHtml(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("button_settings_download_index_html"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Baixar Código index.html", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Cloud Sync Card
        item {
            SectionHeader(title = "Nuvem & Sincronização")
        }

        item {
            val syncStatus by viewModel.syncStatus.collectAsState()
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = EmeraldPrimary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Sincronização Cloud Firestore",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (syncStatus.lastSyncTime != null) "Última sincronização com sucesso" else "Trabalhe offline e sincronize quando quiser",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "O aplicativo funciona 100% offline salvando todos os dados no telemóvel. Ao clicar abaixo ou ligar a internet, seus dados são salvos na nuvem Firebase e sincronizados com o site para computador.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.syncWithCloud() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("button_settings_sync_cloud"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        enabled = !syncStatus.inProgress
                    ) {
                        if (syncStatus.inProgress) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("A sincronizar...", fontSize = 14.sp)
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sincronizar Nuvem Agora", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Web Portal for Computer Card
        item {
            SectionHeader(title = "Acesso no Computador (Site Web)")
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LaptopMac,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Site Web Sincronizado",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Acesse o sistema no computador ou tablet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Foi criado o site Web responsivo do 'Negócio na Prática'. Pode abrir o portal no navegador do seu computador para gerir stock, vendas de balcão e consultar o relatório DRE com tela ampla. Todos os dados são sincronizados em tempo real com a mesma base de dados na nuvem.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Logout Button
        item {
            Button(
                onClick = { viewModel.logout() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("button_logout"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StatusError)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Encerrar Sessão", fontWeight = FontWeight.Bold)
            }
        }
    }

    // Edit Business Dialog
    if (showEditBusinessDialog && activeBusiness != null) {
        val b = activeBusiness!!
        var name by remember { mutableStateOf(b.name) }
        var selectedActivityType by remember { mutableStateOf(b.activityType) }
        var selectedProvince by remember { mutableStateOf(b.province) }
        var city by remember { mutableStateOf(b.city) }
        var neighborhood by remember { mutableStateOf(b.neighborhood) }
        var address by remember { mutableStateOf(b.address) }
        var referencePoint by remember { mutableStateOf(b.referencePoint) }
        var phone by remember { mutableStateOf(b.phone) }
        var currency by remember { mutableStateOf(b.currencySymbol) }
        var defaultUnit by remember { mutableStateOf(b.defaultUnit) }

        var activityExpanded by remember { mutableStateOf(false) }
        var provinceExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showEditBusinessDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Editar Dados do Negócio")
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome do Negócio *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Tipo de Negócio (17 categorias)
                    ExposedDropdownMenuBox(
                        expanded = activityExpanded,
                        onExpandedChange = { activityExpanded = !activityExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedActivityType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo de Negócio *") },
                            leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = activityExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = activityExpanded,
                            onDismissRequest = { activityExpanded = false }
                        ) {
                            com.example.util.BusinessProfileUtils.BUSINESS_ACTIVITY_TYPES.forEach { act ->
                                DropdownMenuItem(
                                    text = { Text(act, fontWeight = if (act == selectedActivityType) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        selectedActivityType = act
                                        activityExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Província
                    ExposedDropdownMenuBox(
                        expanded = provinceExpanded,
                        onExpandedChange = { provinceExpanded = !provinceExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedProvince,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Província") },
                            leadingIcon = { Icon(Icons.Default.Map, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = provinceExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = provinceExpanded,
                            onDismissRequest = { provinceExpanded = false }
                        ) {
                            com.example.util.BusinessProfileUtils.MOZAMBIQUE_PROVINCES.forEach { prov ->
                                DropdownMenuItem(
                                    text = { Text(prov) },
                                    onClick = {
                                        selectedProvince = prov
                                        provinceExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("Cidade ou Distrito *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = neighborhood,
                        onValueChange = { neighborhood = it },
                        label = { Text("Bairro") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Endereço / Localização *") },
                        singleLine = false,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = referencePoint,
                        onValueChange = { referencePoint = it },
                        label = { Text("Ponto de Referência") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Telefone / Contacto") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = currency,
                            onValueChange = { currency = it },
                            label = { Text("Moeda (ex: MT)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = defaultUnit,
                            onValueChange = { defaultUnit = it },
                            label = { Text("Unidade (ex: kg, un)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateBusinessDetails(
                            b.copy(
                                name = name.trim(),
                                activityType = selectedActivityType,
                                province = selectedProvince,
                                city = city.trim(),
                                neighborhood = neighborhood.trim(),
                                address = address.trim(),
                                referencePoint = referencePoint.trim(),
                                phone = phone.trim(),
                                currencySymbol = currency.trim(),
                                defaultUnit = defaultUnit.trim()
                            )
                        )
                        showEditBusinessDialog = false
                    },
                    enabled = name.isNotBlank() && address.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Guardar Alterações")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditBusinessDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            isAdmin = currentUser?.role == "ADMIN",
            onDismiss = { showChangePasswordDialog = false },
            onSubmit = { oldPass, newPass, callback ->
                viewModel.changePassword(oldPass, newPass, callback)
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
                        text = "Introduza o código de segurança para aceder ao Painel Administrativo:",
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
                        modifier = Modifier.fillMaxWidth().testTag("input_admin_settings_code")
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
                        Text("Aceder como Admin")
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
