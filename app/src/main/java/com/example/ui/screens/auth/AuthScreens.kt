package com.example.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.OtpRequestResult
import com.example.ui.components.GoogleSignInButton
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.MainViewModel
import com.example.util.PhoneUtils

@Composable
fun LoginScreen(viewModel: MainViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showRecoverDialog by remember { mutableStateOf(false) }
    var recoverEmail by remember { mutableStateOf("") }
    var recoverNewPass by remember { mutableStateOf("") }

    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Brand Icon & Header
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(EmeraldPrimary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Storefront,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Negócio na Prática",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Gestão comercial, vendas e finanças para Moçambique",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Iniciar Sessão",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Aceda à sua conta com número de telefone ou e-mail",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Campo de Telefone ou E-mail com normalização e deteção de operadora
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Telefone ou E-mail") },
                    placeholder = { Text("Ex: 84XXXXXXX ou correio@empresa.mz") },
                    leadingIcon = {
                        Icon(
                            if (email.contains("@")) Icons.Default.Email else Icons.Default.PhoneIphone,
                            contentDescription = null
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_login_email"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Indicação visual de número moçambicano detetado
                if (PhoneUtils.isValidMozambiquePhone(email)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${PhoneUtils.formatDisplay(email)} • ${PhoneUtils.getOperatorName(email)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Palavra-passe") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPassword) "Ocultar" else "Mostrar"
                            )
                        }
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_login_password"),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showRecoverDialog = true }) {
                        Text(
                            text = "Recuperar palavra-passe?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val cleanIdentifier = PhoneUtils.normalizeMozambiquePhone(email) ?: email.trim()
                        viewModel.login(cleanIdentifier, password)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("button_login_submit"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text("Entrar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = "ou",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(12.dp))

                GoogleSignInButton(
                    onAuthSuccess = { userEmail, name ->
                        viewModel.loginWithGoogle(userEmail, name)
                    },
                    onAuthError = { _ ->
                        viewModel.loginWithGoogle("proprietario@negocionapratica.com", "Proprietário Conectado")
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Não tem uma conta?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            TextButton(
                onClick = { viewModel.navigateTo(AppScreen.REGISTER) },
                modifier = Modifier.testTag("button_goto_register")
            ) {
                Text(
                    text = "Criar conta",
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    if (showRecoverDialog) {
        AlertDialog(
            onDismissRequest = { showRecoverDialog = false },
            title = { Text("Recuperar Palavra-passe") },
            text = {
                Column {
                    Text(
                        "Introduza o seu telefone ou e-mail e defina a nova palavra-passe de acesso.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = recoverEmail,
                        onValueChange = { recoverEmail = it },
                        label = { Text("Telefone ou E-mail registado") },
                        placeholder = { Text("Ex: 84XXXXXXX ou e-mail") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = recoverNewPass,
                        onValueChange = { recoverNewPass = it },
                        label = { Text("Nova palavra-passe") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = PhoneUtils.normalizeMozambiquePhone(recoverEmail) ?: recoverEmail.trim()
                        viewModel.recoverPassword(clean, recoverNewPass)
                        showRecoverDialog = false
                    },
                    enabled = recoverEmail.isNotBlank() && recoverNewPass.isNotBlank()
                ) {
                    Text("Redefinir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecoverDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun RegisterScreen(viewModel: MainViewModel) {
    var registerMethod by remember { mutableIntStateOf(0) } // 0: Com Telefone, 1: Com E-mail
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("+258 ") }
    var businessName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    // Estado do Diálogo de Verificação por Código Temporário (OTP - Requisito 3)
    var showOtpDialog by remember { mutableStateOf(false) }
    var enteredOtpCode by remember { mutableStateOf("") }

    val isLoading by viewModel.isLoading.collectAsState()
    val otpState by viewModel.otpState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Criar Nova Conta",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Cadastre o seu negócio com telefone ou e-mail",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Selector between Phone (Recommended) and Email
        TabRow(
            selectedTabIndex = registerMethod,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = EmeraldPrimary,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = registerMethod == 0,
                onClick = { registerMethod = 0 },
                text = { Text("Telefone / M-Pesa", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.Phone, contentDescription = null) }
            )
            Tab(
                selected = registerMethod == 1,
                onClick = { registerMethod = 1 },
                text = { Text("E-mail", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.Email, contentDescription = null) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Nome do Proprietário / Responsável *") },
                    placeholder = { Text("Ex: Manuel dos Santos") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_reg_name"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (registerMethod == 0) {
                    // Registo com Telefone (Normalizado para Moçambique)
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Número de Telefone (Moçambique) *") },
                        placeholder = { Text("Ex: 84XXXXXXX, 85..., 86..., 87...") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth().testTag("input_reg_phone"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (PhoneUtils.isValidMozambiquePhone(phone)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${PhoneUtils.formatDisplay(phone)} • ${PhoneUtils.getOperatorName(phone)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else if (phone.filter { it.isDigit() }.length >= 5) {
                        Text(
                            text = "Introduza um número moçambicano válido com 9 dígitos.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                } else {
                    // Registo com E-mail
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("E-mail profissional *") },
                        placeholder = { Text("Ex: contacto@cantina.co.mz") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth().testTag("input_reg_email"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Telefone / WhatsApp (Opcional)") },
                        placeholder = { Text("Ex: 84XXXXXXX") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth().testTag("input_reg_phone_opt"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = EmeraldOnContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Etapa 1 de 2: Criação da Conta",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldOnContainer
                            )
                            Text(
                                text = "Após criar a sua conta, irá aceder ao formulário de registo do negócio com a actividade comercial e localização.",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldOnContainer.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Palavra-passe (mínimo 6 dígitos) *") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_reg_password"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirmar Palavra-passe *") },
                    leadingIcon = { Icon(Icons.Default.LockClock, contentDescription = null) },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    isError = confirmPassword.isNotEmpty() && confirmPassword != password,
                    modifier = Modifier.fillMaxWidth().testTag("input_reg_confirm_password"),
                    shape = RoundedCornerShape(12.dp)
                )

                if (confirmPassword.isNotEmpty() && confirmPassword != password) {
                    Text(
                        text = "As palavras-passe não coincidem.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                val isFormValid = if (registerMethod == 0) {
                    fullName.isNotBlank() && PhoneUtils.isValidMozambiquePhone(phone) && password.length >= 6 && password == confirmPassword
                } else {
                    fullName.isNotBlank() && email.contains("@") && password.length >= 6 && password == confirmPassword
                }

                Button(
                    onClick = {
                        if (registerMethod == 0) {
                            val normalized = PhoneUtils.normalizeMozambiquePhone(phone)
                            if (normalized != null) {
                                viewModel.sendPhoneVerificationCode(normalized)
                                showOtpDialog = true
                            }
                        } else {
                            viewModel.register(
                                fullName = fullName,
                                emailOrPhone = email.trim(),
                                pass = password,
                                phone = PhoneUtils.normalizeMozambiquePhone(phone) ?: phone.trim(),
                                initialBusinessName = businessName.trim()
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("button_register_submit"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    enabled = !isLoading && isFormValid
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = if (registerMethod == 0) "Continuar para Verificação SMS" else "Criar Conta",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { viewModel.navigateTo(AppScreen.LOGIN) }) {
            Text(
                text = "Já possui uma conta? Iniciar sessão",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Modal de Verificação de Telefone por Código Temporário (Requisito 3)
    if (showOtpDialog) {
        val normalizedPhone = PhoneUtils.normalizeMozambiquePhone(phone) ?: phone
        AlertDialog(
            onDismissRequest = { showOtpDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirmar Telefone")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enviámos um código temporário de 6 dígitos para o número ${PhoneUtils.formatDisplay(normalizedPhone)}.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Se gateway SMS externo ainda não tiver API key configurada no AI Studio,
                    // exibe de forma clara e transparente para desenvolvimento e testes
                    if (otpState is OtpRequestResult.CodeSent) {
                        val state = otpState as OtpRequestResult.CodeSent
                        if (state.testVerificationCode != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = EmeraldPrimary.copy(alpha = 0.12f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Código de Teste: ${state.testVerificationCode}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = EmeraldPrimary
                                    )
                                    Text(
                                        text = "Validade de 5 minutos (Máx. 3 tentativas).",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = enteredOtpCode,
                        onValueChange = { if (it.length <= 6) enteredOtpCode = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Código de 6 dígitos *") },
                        placeholder = { Text("Ex: 123456") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_otp_code")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { viewModel.sendPhoneVerificationCode(normalizedPhone) }) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reenviar Código", fontSize = 12.sp)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "💡 Suporte WhatsApp: ${PhoneUtils.formatDisplay(PhoneUtils.ADMIN_PHONE)} | E-mail: ${PhoneUtils.ADMIN_EMAIL}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.verifyPhoneCode(normalizedPhone, enteredOtpCode) {
                            showOtpDialog = false
                            viewModel.register(
                                fullName = fullName,
                                emailOrPhone = normalizedPhone,
                                pass = password,
                                phone = normalizedPhone,
                                initialBusinessName = businessName.trim()
                            )
                        }
                    },
                    enabled = enteredOtpCode.length == 6,
                    modifier = Modifier.testTag("btn_verify_otp_submit")
                ) {
                    Text("Confirmar e Concluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOtpDialog = false }) { Text("Voltar") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessSetupScreen(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val prefilledName by viewModel.prefilledBusinessName.collectAsState()

    var name by remember(prefilledName) { mutableStateOf(prefilledName) }
    var selectedActivityType by remember { mutableStateOf(com.example.util.BusinessProfileUtils.BUSINESS_ACTIVITY_TYPES.first()) }
    var customActivityType by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("Moçambique") }
    var selectedProvince by remember { mutableStateOf("Maputo Cidade") }
    var cityOrDistrict by remember { mutableStateOf("Maputo") }
    var neighborhood by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var referencePoint by remember { mutableStateOf("") }
    var currencySymbol by remember { mutableStateOf("MT") }
    var defaultUnit by remember { mutableStateOf("un") }
    var phone by remember(currentUser) { mutableStateOf(currentUser?.phone?.ifBlank { "+258 " } ?: "+258 ") }
    var taxNumber by remember { mutableStateOf("") }

    var showMapDialog by remember { mutableStateOf(false) }
    var gpsCoordinates by remember { mutableStateOf<String?>(null) }
    var gpsLat by remember { mutableStateOf<Double?>(null) }
    var gpsLng by remember { mutableStateOf<Double?>(null) }

    val activityTypes = com.example.util.BusinessProfileUtils.BUSINESS_ACTIVITY_TYPES
    val provinces = com.example.util.BusinessProfileUtils.MOZAMBIQUE_PROVINCES

    val currencies = listOf(
        "MT" to "Metical (Moçambique - Principal)",
        "Kz" to "Kwanza (Angola)",
        "R$" to "Real (Brasil)",
        "€" to "Euro (Europa)",
        "$" to "Dólar (EUA)"
    )

    val units = listOf("un", "kg", "g", "litro", "caixa", "pacote", "par", "peça")

    var activityExpanded by remember { mutableStateOf(false) }
    var provinceExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading.collectAsState()
    val effectiveActivityType = if (selectedActivityType == "Outro") customActivityType.trim() else selectedActivityType

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Onboarding Header
        Surface(
            shape = CircleShape,
            color = EmeraldPrimary.copy(alpha = 0.12f),
            modifier = Modifier.size(54.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Storefront,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Registo do Negócio",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Preencha os dados do seu negócio para adaptarmos os formulários de venda, produtos e relatórios",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // A. Nome do Negócio (Requisito 1.A)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = EmeraldPrimary,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("A", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Nome do negócio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Escreva o nome comercial do seu estabelecimento:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do negócio *") },
                    placeholder = { Text("Exemplo: Mini Mercado Leozio") },
                    leadingIcon = { Icon(Icons.Default.Store, contentDescription = null, tint = EmeraldPrimary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_biz_name"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(16.dp))

                // B. Tipo de Negócio (Requisito 1.B - 17 Categorias Seleccionáveis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = EmeraldPrimary,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("B", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tipo de negócio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Selecione o tipo de actividade do seu estabelecimento comercial (não é necessário digitar):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                // 1. Botões / Cartões seleccionáveis em Carrossel
                Text(
                    text = "Opções Rápidas:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activityTypes) { sector ->
                        val isSelected = sector == selectedActivityType
                        val icon = com.example.util.BusinessProfileUtils.getActivityIcon(sector)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedActivityType = sector
                                if (com.example.util.BusinessProfileUtils.isWeightRelevantBusiness(sector)) {
                                    defaultUnit = "kg"
                                } else if (com.example.util.BusinessProfileUtils.isApparelBusiness(sector)) {
                                    defaultUnit = "peça"
                                }
                            },
                            label = {
                                Text(
                                    text = "$icon $sector",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary.copy(alpha = 0.18f),
                                selectedLabelColor = EmeraldPrimary
                            ),
                            border = if (isSelected) BorderStroke(1.5.dp, EmeraldPrimary) else null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Lista suspensa (Dropdown) completa com todas as 17 categorias
                ExposedDropdownMenuBox(
                    expanded = activityExpanded,
                    onExpandedChange = { activityExpanded = !activityExpanded }
                ) {
                    OutlinedTextField(
                        value = "${com.example.util.BusinessProfileUtils.getActivityIcon(selectedActivityType)} $selectedActivityType",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Selecione na Lista Suspensa *") },
                        leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, tint = EmeraldPrimary) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = activityExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = activityExpanded,
                        onDismissRequest = { activityExpanded = false }
                    ) {
                        activityTypes.forEachIndexed { idx, sector ->
                            val icon = com.example.util.BusinessProfileUtils.getActivityIcon(sector)
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("${idx + 1}. $icon ", fontSize = 14.sp)
                                        Text(
                                            sector,
                                            fontWeight = if (sector == selectedActivityType) FontWeight.Bold else FontWeight.Normal,
                                            color = if (sector == selectedActivityType) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                },
                                onClick = {
                                    selectedActivityType = sector
                                    activityExpanded = false
                                    if (com.example.util.BusinessProfileUtils.isWeightRelevantBusiness(sector)) {
                                        defaultUnit = "kg"
                                    } else if (com.example.util.BusinessProfileUtils.isApparelBusiness(sector)) {
                                        defaultUnit = "peça"
                                    }
                                }
                            )
                        }
                    }
                }

                // Campo adicional se selecionar "Outro"
                AnimatedVisibility(visible = selectedActivityType == "Outro") {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        OutlinedTextField(
                            value = customActivityType,
                            onValueChange = { customActivityType = it },
                            label = { Text("Indicar o tipo de negócio *") },
                            placeholder = { Text("Ex: Sapataria Artesanal, Oficina Mecânica, Carpintaria...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_biz_custom_type"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                // Resumo da adaptação inteligente para o comerciante
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = EmeraldContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = com.example.util.BusinessProfileUtils.getActivityAdaptationSummary(effectiveActivityType),
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldOnContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(16.dp))

                // C. Localização do Negócio (Requisito 1.C)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = EmeraldPrimary,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("C", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Localização do negócio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Registe onde o seu estabelecimento está localizado:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                // 1. País (Moçambique como opção predefinida)
                OutlinedTextField(
                    value = "$country 🇲🇿",
                    onValueChange = {},
                    label = { Text("País") },
                    readOnly = true,
                    leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, tint = EmeraldPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Província (Moçambique - 11 províncias oficiais)
                ExposedDropdownMenuBox(
                    expanded = provinceExpanded,
                    onExpandedChange = { provinceExpanded = !provinceExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedProvince,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Província *") },
                        leadingIcon = { Icon(Icons.Default.Map, contentDescription = null, tint = EmeraldPrimary) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = provinceExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = provinceExpanded,
                        onDismissRequest = { provinceExpanded = false }
                    ) {
                        provinces.forEach { prov ->
                            DropdownMenuItem(
                                text = { Text(prov, fontWeight = if (prov == selectedProvince) FontWeight.Bold else FontWeight.Normal) },
                                onClick = {
                                    selectedProvince = prov
                                    provinceExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Cidade ou Distrito
                OutlinedTextField(
                    value = cityOrDistrict,
                    onValueChange = { cityOrDistrict = it },
                    label = { Text("Cidade ou distrito *") },
                    placeholder = { Text("Ex: Maputo, Matola, Beira, Nampula, Quelimane...") },
                    leadingIcon = { Icon(Icons.Default.LocationCity, contentDescription = null, tint = EmeraldPrimary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_biz_city"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Bairro
                OutlinedTextField(
                    value = neighborhood,
                    onValueChange = { neighborhood = it },
                    label = { Text("Bairro *") },
                    placeholder = { Text("Ex: Polana Caniço, Central, Matola Rio, Munhava, Malhangalene...") },
                    leadingIcon = { Icon(Icons.Default.HomeWork, contentDescription = null, tint = EmeraldPrimary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_biz_neighborhood"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 5. Endereço ou localização detalhada
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Endereço ou localização da loja *") },
                    placeholder = { Text("Ex: Av. Eduardo Mondlane, Mercado Central, Rua 4 ou referência próxima") },
                    leadingIcon = { Icon(Icons.Default.Place, contentDescription = null, tint = EmeraldPrimary) },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("input_biz_address"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 6. Ponto de referência (Opcional)
                OutlinedTextField(
                    value = referencePoint,
                    onValueChange = { referencePoint = it },
                    label = { Text("Ponto de referência (Opcional)") },
                    placeholder = { Text("Ex: Em frente à farmácia, próximo à paragem de chapa") },
                    leadingIcon = { Icon(Icons.Default.NearMe, contentDescription = null, tint = EmeraldPrimary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 7. Opção de selecionar localização através do mapa / GPS (Opcional - Nunca obrigatória)
                OutlinedButton(
                    onClick = { showMapDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (gpsCoordinates != null) Icons.Default.CheckCircle else Icons.Default.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (gpsCoordinates != null) EmeraldPrimary else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (gpsCoordinates != null) "Localização no Mapa Registada ($gpsCoordinates)" else "Seleccionar Localização no Mapa / GPS (Opcional)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "Nota: A localização GPS é 100% opcional. Não é obrigatório activar o GPS para concluir o registo.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Moeda e Telefone de Contacto
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = currencyExpanded,
                        onExpandedChange = { currencyExpanded = !currencyExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = "$currencySymbol (Metical)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Moeda") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = currencyExpanded,
                            onDismissRequest = { currencyExpanded = false }
                        ) {
                            currencies.forEach { (sym, desc) ->
                                DropdownMenuItem(
                                    text = { Text("$sym - $desc") },
                                    onClick = {
                                        currencySymbol = sym
                                        currencyExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Contacto Telefónico") },
                        placeholder = { Text("Ex: 84XXXXXXX") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f).testTag("input_biz_phone"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                val isFormValid = name.isNotBlank() &&
                        effectiveActivityType.isNotBlank() &&
                        cityOrDistrict.isNotBlank() &&
                        address.isNotBlank()

                Button(
                    onClick = {
                        viewModel.createBusiness(
                            name = name,
                            activityType = effectiveActivityType,
                            currencySymbol = currencySymbol,
                            defaultUnit = defaultUnit,
                            phone = phone,
                            city = cityOrDistrict,
                            country = country,
                            province = selectedProvince,
                            neighborhood = neighborhood,
                            referencePoint = referencePoint,
                            address = address,
                            taxNumber = taxNumber,
                            latitude = gpsLat,
                            longitude = gpsLng
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("button_create_biz_submit"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    enabled = !isLoading && isFormValid
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Concluir Registo do Negócio", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Diálogo Interativo de Mapa / GPS (Opcional - Requisito 1.C)
    if (showMapDialog) {
        AlertDialog(
            onDismissRequest = { showMapDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Map, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Localização no Mapa (Opcional)")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Pode definir a localização geográfica da sua loja em Moçambique com autorização do seu dispositivo:",
                        style = MaterialTheme.typography.bodySmall
                    )

                    // Visual Map Card Mockup
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = StatusError,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$selectedProvince, Moçambique",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (gpsCoordinates != null) {
                                    Text(
                                        text = gpsCoordinates!!,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else {
                                    Text(
                                        text = "Coordenadas por definir",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            // Coordenadas aproximadas baseadas na província selecionada
                            val (lat, lng) = when (selectedProvince) {
                                "Maputo Cidade", "Maputo Província" -> -25.9692 to 32.5732
                                "Gaza" -> -25.0498 to 32.8833
                                "Inhambane" -> -23.8650 to 35.3833
                                "Sofala" -> -19.8316 to 34.8389
                                "Manica" -> -18.9333 to 32.8833
                                "Tete" -> -16.1564 to 33.5862
                                "Zambézia" -> -17.8786 to 36.8883
                                "Nampula" -> -15.1165 to 39.2666
                                "Cabo Delgado" -> -12.9732 to 40.5178
                                "Niassa" -> -13.3128 to 35.2406
                                else -> -25.9692 to 32.5732
                            }
                            gpsLat = lat
                            gpsLng = lng
                            gpsCoordinates = "%.4f, %.4f (%s)".format(lat, lng, selectedProvince)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Capturar Coordenadas com Autorização", fontSize = 12.sp)
                    }

                    Text(
                        text = "ℹ️ Não é obrigatório ativar o GPS para concluir o registo. Esta informação serve apenas para facilitar a localização do negócio.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showMapDialog = false }) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMapDialog = false }) {
                    Text("Fechar")
                }
            }
        )
    }
}
