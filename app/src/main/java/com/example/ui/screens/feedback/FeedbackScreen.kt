package com.example.ui.screens.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FeedbackEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val allFeedbacks by viewModel.feedbacks.collectAsState()

    val selectedTab by viewModel.feedbackSelectedTab.collectAsState()

    // State for Avaliação
    var reviewRating by remember { mutableIntStateOf(5) }
    var reviewTitle by remember { mutableStateOf("") }
    var reviewComment by remember { mutableStateOf("") }

    // State for Reclamação
    var complaintCategory by remember { mutableStateOf("Sistema") }
    var complaintTitle by remember { mutableStateOf("") }
    var complaintDesc by remember { mutableStateOf("") }

    // State for Sugestão
    var suggestionTitle by remember { mutableStateOf("") }
    var suggestionDesc by remember { mutableStateOf("") }

    val userFeedbacks = remember(allFeedbacks, currentUser) {
        allFeedbacks.filter { it.userId == currentUser?.id }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = EmeraldPrimary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { viewModel.setFeedbackSelectedTab(0) },
                text = { Text("Avaliar App") },
                icon = { Icon(Icons.Default.Star, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { viewModel.setFeedbackSelectedTab(1) },
                text = { Text("Reclamações") },
                icon = { Icon(Icons.Default.ReportProblem, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { viewModel.setFeedbackSelectedTab(2) },
                text = { Text("Sugestões") },
                icon = { Icon(Icons.Default.Lightbulb, contentDescription = null) }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // Avaliação (Review)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "Avalie o Negócio na Prática",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "A sua opinião é essencial para continuarmos a evoluir a plataforma para os empreendedores.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Star Rating Bar
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    (1..5).forEach { star ->
                                        IconButton(onClick = { reviewRating = star }) {
                                            Icon(
                                                imageVector = if (star <= reviewRating) Icons.Default.Star else Icons.Default.StarBorder,
                                                contentDescription = "$star estrelas",
                                                tint = AmberTertiary,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedTextField(
                                    value = reviewTitle,
                                    onValueChange = { reviewTitle = it },
                                    label = { Text("Título da Avaliação") },
                                    placeholder = { Text("Ex: Muito útil para controlar o stock") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = reviewComment,
                                    onValueChange = { reviewComment = it },
                                    label = { Text("Comentário ou Experiência (Opcional)") },
                                    minLines = 3,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        viewModel.submitFeedback(
                                            type = "REVIEW",
                                            rating = reviewRating,
                                            category = "Geral",
                                            title = reviewTitle.ifBlank { "Avaliação $reviewRating Estrelas" },
                                            description = reviewComment
                                        )
                                        reviewTitle = ""
                                        reviewComment = ""
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Enviar Avaliação")
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Reclamações (Complaints & Tracking)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "Registar Reclamação / Suporte",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Descreva o problema encontrado. Acompanhe abaixo o estado da sua reclamação em tempo real.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                val complaintCategories = listOf("Sistema", "Stock", "Vendas", "Cálculos Financeiros", "Outro")
                                Text("Categoria do Problema:", style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    complaintCategories.take(3).forEach { cat ->
                                        FilterChip(
                                            selected = complaintCategory == cat,
                                            onClick = { complaintCategory = cat },
                                            label = { Text(cat, fontSize = 11.sp) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = complaintTitle,
                                    onValueChange = { complaintTitle = it },
                                    label = { Text("Assunto da Reclamação *") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("input_complaint_title")
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = complaintDesc,
                                    onValueChange = { complaintDesc = it },
                                    label = { Text("Descrição detalhada do problema *") },
                                    minLines = 3,
                                    modifier = Modifier.fillMaxWidth().testTag("input_complaint_desc")
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        viewModel.submitFeedback(
                                            type = "COMPLAINT",
                                            rating = 1,
                                            category = complaintCategory,
                                            title = complaintTitle,
                                            description = complaintDesc
                                        )
                                        complaintTitle = ""
                                        complaintDesc = ""
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("button_submit_complaint"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusWarning),
                                    enabled = complaintTitle.isNotBlank() && complaintDesc.isNotBlank()
                                ) {
                                    Text("Submeter Reclamação")
                                }
                            }
                        }
                    }

                    // Existing Complaints Tracking List
                    item {
                        Text(
                            text = "As Minhas Reclamações & Estado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val userComplaints = userFeedbacks.filter { it.type == "COMPLAINT" }
                    if (userComplaints.isEmpty()) {
                        item {
                            EmptyStateView(
                                icon = Icons.Default.CheckCircle,
                                title = "Nenhuma reclamação aberta",
                                description = "Não possui reclamações pendentes no sistema."
                            )
                        }
                    } else {
                        items(userComplaints) { item ->
                            ComplaintStatusCard(item)
                        }
                    }
                }

                2 -> {
                    // Sugestões (Suggestions)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "Propor Melhoria ou Ideia",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Que funcionalidade gostaria de ver adicionada no Negócio na Prática?",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = suggestionTitle,
                                    onValueChange = { suggestionTitle = it },
                                    label = { Text("Título da Sugestão *") },
                                    placeholder = { Text("Ex: Emissão de faturas em PDF") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = suggestionDesc,
                                    onValueChange = { suggestionDesc = it },
                                    label = { Text("Como ajudaria o seu negócio? *") },
                                    minLines = 3,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        viewModel.submitFeedback(
                                            type = "SUGGESTION",
                                            rating = 5,
                                            category = "Melhoria",
                                            title = suggestionTitle,
                                            description = suggestionDesc
                                        )
                                        suggestionTitle = ""
                                        suggestionDesc = ""
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = suggestionTitle.isNotBlank() && suggestionDesc.isNotBlank()
                                ) {
                                    Text("Enviar Sugestão")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ComplaintStatusCard(item: FeedbackEntity) {
    val (statusLabel, statusColor, statusBg) = when (item.status) {
        "RECEBIDA" -> Triple("Recebida", Color(0xFF6B7280), Color(0xFFF3F4F6))
        "EM_ANALISE" -> Triple("Em análise", SkySecondary, SkyContainer)
        "EM_RESOLUCAO" -> Triple("Em resolução", AmberTertiary, AmberContainer)
        "RESOLVIDA" -> Triple("Resolvida", StatusSuccess, EmeraldContainer)
        else -> Triple(item.status, EmeraldPrimary, EmeraldContainer)
    }

    val dateStr = remember(item.createdAt) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "AO")).format(Date(item.createdAt))
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
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (item.adminNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Resposta do Suporte Negócio na Prática:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = item.adminNotes,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Registada em: $dateStr • Categoria: ${item.category}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}
