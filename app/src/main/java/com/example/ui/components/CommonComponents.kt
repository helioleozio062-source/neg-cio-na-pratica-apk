package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

fun formatCurrency(amount: Double, symbol: String = "MT"): String {
    val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-MZ"))
    formatter.minimumFractionDigits = 2
    formatter.maximumFractionDigits = 2
    return "${formatter.format(amount)} $symbol"
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    iconColor: Color = EmeraldPrimary,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .testTag("metric_${title.lowercase().replace(" ", "_")}")
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 19.sp
            )

            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun StockBadge(currentStock: Double, minAlert: Double, unit: String) {
    val (bgColor, textColor, label) = when {
        currentStock <= 0.0 -> Triple(StatusError.copy(alpha = 0.15f), StatusError, "Esgotado")
        currentStock <= minAlert -> Triple(StatusWarning.copy(alpha = 0.18f), AmberTertiary, "Stock Baixo")
        else -> Triple(StatusSuccess.copy(alpha = 0.15f), StatusSuccess, "Normal")
    }

    val stockDisplay = if (com.example.util.BusinessProfileUtils.isWeightUnit(unit)) {
        com.example.util.BusinessProfileUtils.formatWeightDisplay(currentStock)
    } else if (currentStock % 1.0 == 0.0) {
        "${currentStock.toInt()} $unit"
    } else {
        String.format(java.util.Locale.US, "%.2f %s", currentStock, unit)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$label ($stockDisplay)",
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (actionText != null && onActionClick != null) {
            TextButton(
                onClick = onActionClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.testTag("action_${actionText.lowercase().replace(" ", "_")}")
            ) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun SimpleBarChart(
    values: List<Double>,
    labels: List<String>,
    currency: String,
    modifier: Modifier = Modifier
) {
    if (values.isEmpty()) return
    val maxVal = values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0

    Card(
        modifier = modifier.fillMaxWidth(),
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
                Text(
                    text = "Desempenho de Vendas Recente",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Últimos períodos",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                values.zip(labels).forEach { (value, label) ->
                    val fraction = (value / maxVal).toFloat().coerceIn(0.08f, 1f)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(EmeraldPrimary, EmeraldPrimary.copy(alpha = 0.5f))
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChangePasswordDialog(
    isAdmin: Boolean = false,
    onDismiss: () -> Unit,
    onSubmit: (currentPass: String, newPass: String, onResult: (Boolean, String) -> Unit) -> Unit
) {
    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var showCurrentPass by remember { mutableStateOf(false) }
    var showNewPass by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LockReset,
                    contentDescription = null,
                    tint = if (isAdmin) StatusError else EmeraldPrimary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isAdmin) "Trocar Código do Admin Geral" else "Alterar Palavra-passe",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isAdmin)
                        "Defina um novo código secreto. Assim que gravar, o código anterior deixará de funcionar e apenas quem souber o novo código terá acesso de Administrador."
                    else
                        "Introduza a sua palavra-passe atual e defina a nova para manter a sua conta segura.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (errorMessage != null) {
                    Surface(
                        color = StatusError.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = StatusError,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = currentPass,
                    onValueChange = {
                        currentPass = it
                        errorMessage = null
                    },
                    label = { Text("Código / Palavra-passe Atual") },
                    singleLine = true,
                    visualTransformation = if (showCurrentPass) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showCurrentPass = !showCurrentPass }) {
                            Icon(
                                if (showCurrentPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("input_current_password")
                )

                OutlinedTextField(
                    value = newPass,
                    onValueChange = {
                        newPass = it
                        errorMessage = null
                    },
                    label = { Text("Novo Código / Nova Palavra-passe") },
                    singleLine = true,
                    visualTransformation = if (showNewPass) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showNewPass = !showNewPass }) {
                            Icon(
                                if (showNewPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("input_new_password")
                )

                OutlinedTextField(
                    value = confirmPass,
                    onValueChange = {
                        confirmPass = it
                        errorMessage = null
                    },
                    label = { Text("Confirmar Novo Código") },
                    singleLine = true,
                    visualTransformation = if (showNewPass) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("input_confirm_password")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (currentPass.isBlank()) {
                        errorMessage = "Por favor, indique o código/palavra-passe atual."
                        return@Button
                    }
                    if (newPass.length < 4) {
                        errorMessage = "O novo código deve ter pelo menos 4 caracteres."
                        return@Button
                    }
                    if (newPass != confirmPass) {
                        errorMessage = "Os novos códigos não coincidem."
                        return@Button
                    }
                    isSubmitting = true
                    onSubmit(currentPass, newPass) { success, msg ->
                        isSubmitting = false
                        if (success) {
                            onDismiss()
                        } else {
                            errorMessage = msg
                        }
                    }
                },
                enabled = !isSubmitting && currentPass.isNotBlank() && newPass.isNotBlank() && confirmPass.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = if (isAdmin) StatusError else EmeraldPrimary),
                modifier = Modifier.testTag("button_confirm_change_password")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("A gravar...")
                } else {
                    Text("Guardar Novo Código")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting
            ) {
                Text("Cancelar")
            }
        }
    )
}

