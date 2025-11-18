package com.example.edumonjetcompose.ui

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.edumonjetcompose.models.AvatarOption
import com.example.edumonjetcompose.network.ApiService
import com.example.edumonjetcompose.ui.theme.*
import com.google.gson.JsonObject
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarSelectionScreen(
    navController: NavController,
    token: String,
    currentAvatarUrl: String?,
    onAvatarSelected: (String) -> Unit
) {
    var avatarList by remember { mutableStateOf<List<AvatarOption>>(emptyList()) }
    var selectedAvatar by remember { mutableStateOf<String?>(currentAvatarUrl) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var currentStep by remember { mutableStateOf(1) } // 1: Avatar, 2: Datos personales, 3: Cambiar contraseña

    var userId by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var apellido by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var cedula by remember { mutableStateOf("") }
    var userRol by remember { mutableStateOf("") }

    var contraseñaActual by remember { mutableStateOf("") }
    var contraseñaNueva by remember { mutableStateOf("") }
    var contraseñaConfirmar by remember { mutableStateOf("") }

    var nombreError by remember { mutableStateOf<String?>(null) }
    var apellidoError by remember { mutableStateOf<String?>(null) }
    var correoError by remember { mutableStateOf<String?>(null) }
    var telefonoError by remember { mutableStateOf<String?>(null) }
    var cedulaError by remember { mutableStateOf<String?>(null) }
    var contraseñaActualError by remember { mutableStateOf<String?>(null) }
    var contraseñaNuevaError by remember { mutableStateOf<String?>(null) }
    var contraseñaConfirmarError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val profileResponse = ApiService.getUserProfile(token)
                if (profileResponse.isSuccessful) {
                    val body = profileResponse.body()
                    val userObj = body?.getAsJsonObject("user") ?: body

                    if (userObj != null) {
                        userId = userObj.get("id")?.asString ?: userObj.get("_id")?.asString ?: ""
                        nombre = userObj.get("nombre")?.asString ?: ""
                        apellido = userObj.get("apellido")?.asString ?: ""
                        correo = userObj.get("correo")?.asString ?: ""
                        userRol = userObj.get("rol")?.asString ?: ""

                        val telefonoCompleto = userObj.get("telefono")?.asString ?: ""
                        telefono = if (telefonoCompleto.startsWith("+57")) {
                            telefonoCompleto.substring(3)
                        } else {
                            telefonoCompleto
                        }

                        cedula = userObj.get("cedula")?.takeIf { !it.isJsonNull }?.asString ?: ""

                        val avatarActual = userObj.get("fotoPerfilUrl")?.asString
                        if (!avatarActual.isNullOrEmpty()) {
                            selectedAvatar = avatarActual
                        }
                    }
                } else {
                    errorMessage = "Error al cargar perfil"
                }

                val avatarResponse = ApiService.getFotosPredeterminadas(token)
                if (avatarResponse.isSuccessful) {
                    val body = avatarResponse.body()
                    val fotosArray = body?.getAsJsonArray("fotos")

                    val avatares = mutableListOf<AvatarOption>()
                    fotosArray?.forEach { element ->
                        val obj = element.asJsonObject
                        avatares.add(
                            AvatarOption(
                                url = obj.get("url")?.asString.orEmpty(),
                                name = obj.get("nombre")?.asString.orEmpty()
                            )
                        )
                    }
                    avatarList = avatares
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Completa tu Perfil",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AzulCielo,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFF5F8FC), Color(0xFFFFFFFF))
                    )
                )
                .padding(padding)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(56.dp),
                            color = AzulCielo,
                            strokeWidth = 5.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Cargando información...",
                            style = MaterialTheme.typography.titleMedium,
                            color = GrisNeutral
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(20.dp)
                ) {
                    // Indicador de progreso
                    StepIndicator(currentStep = currentStep, totalSteps = 3)

                    Spacer(modifier = Modifier.height(24.dp))

                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            fadeIn() + slideInHorizontally { it } togetherWith
                                    fadeOut() + slideOutHorizontally { -it }
                        },
                        label = "stepTransition"
                    ) { step ->
                        when (step) {
                            1 -> AvatarSelectionStep(
                                avatarList = avatarList,
                                selectedAvatar = selectedAvatar,
                                onAvatarSelected = { selectedAvatar = it },
                                nombre = nombre,
                                errorMessage = errorMessage
                            )
                            2 -> PersonalInfoStep(
                                nombre = nombre,
                                apellido = apellido,
                                telefono = telefono,
                                correo = correo,
                                cedula = cedula,
                                nombreError = nombreError,
                                apellidoError = apellidoError,
                                telefonoError = telefonoError,
                                correoError = correoError,
                                cedulaError = cedulaError,
                                onNombreChange = {
                                    nombre = it
                                    nombreError = if (it.isBlank()) "El nombre es obligatorio" else null
                                },
                                onApellidoChange = {
                                    apellido = it
                                    apellidoError = if (it.isBlank()) "El apellido es obligatorio" else null
                                },
                                onTelefonoChange = {
                                    if (it.all { char -> char.isDigit() } && it.length <= 10) {
                                        telefono = it
                                        telefonoError = when {
                                            it.isBlank() -> "El teléfono es obligatorio"
                                            it.length < 10 -> "El teléfono debe tener 10 dígitos"
                                            else -> null
                                        }
                                    }
                                },
                                onCorreoChange = {
                                    correo = it
                                    correoError = when {
                                        it.isBlank() -> "El correo es obligatorio"
                                        !android.util.Patterns.EMAIL_ADDRESS.matcher(it).matches() ->
                                            "Correo inválido"
                                        else -> null
                                    }
                                },
                                onCedulaChange = {
                                    if (it.all { char -> char.isDigit() } && it.length <= 10) {
                                        cedula = it
                                        cedulaError = when {
                                            it.isBlank() -> "La cédula es obligatoria"
                                            it.length < 6 -> "Mínimo 6 dígitos"
                                            else -> null
                                        }
                                    }
                                },
                                focusManager = focusManager,
                                errorMessage = errorMessage
                            )
                            3 -> PasswordChangeStep(
                                contraseñaActual = contraseñaActual,
                                contraseñaNueva = contraseñaNueva,
                                contraseñaConfirmar = contraseñaConfirmar,
                                contraseñaActualError = contraseñaActualError,
                                contraseñaNuevaError = contraseñaNuevaError,
                                contraseñaConfirmarError = contraseñaConfirmarError,
                                onContraseñaActualChange = {
                                    contraseñaActual = it
                                    contraseñaActualError = if (it.isBlank()) "La contraseña actual es obligatoria" else null
                                },
                                onContraseñaNuevaChange = {
                                    contraseñaNueva = it
                                    contraseñaNuevaError = when {
                                        it.isBlank() -> "La contraseña nueva es obligatoria"
                                        it.length < 6 -> "Mínimo 6 caracteres"
                                        it.length > 128 -> "Máximo 128 caracteres"
                                        !it.matches(Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$")) ->
                                            "Debe tener minúscula, mayúscula y número"
                                        else -> null
                                    }
                                },
                                onContraseñaConfirmarChange = {
                                    contraseñaConfirmar = it
                                    contraseñaConfirmarError = when {
                                        it.isBlank() -> "Debes confirmar la contraseña"
                                        it != contraseñaNueva -> "Las contraseñas no coinciden"
                                        else -> null
                                    }
                                },
                                focusManager = focusManager,
                                errorMessage = errorMessage
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Botones de navegación
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (currentStep > 1) {
                            OutlinedButton(
                                onClick = {
                                    currentStep--
                                    errorMessage = null
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = AzulCielo
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(width = 2.dp)
                            ) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Anterior", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            }
                        }

                        Button(
                            onClick = {
                                when (currentStep) {
                                    1 -> {
                                        // Validar avatar
                                        if (selectedAvatar == null) {
                                            errorMessage = "Debes seleccionar un avatar"
                                            return@Button
                                        }
                                        errorMessage = null
                                        currentStep = 2
                                    }
                                    2 -> {
                                        // Validar datos personales
                                        var hasErrors = false
                                        errorMessage = null

                                        if (nombre.isBlank()) {
                                            nombreError = "El nombre es obligatorio"
                                            hasErrors = true
                                        }
                                        if (apellido.isBlank()) {
                                            apellidoError = "El apellido es obligatorio"
                                            hasErrors = true
                                        }
                                        if (telefono.isBlank()) {
                                            telefonoError = "El teléfono es obligatorio"
                                            hasErrors = true
                                        } else if (telefono.length != 10) {
                                            telefonoError = "El teléfono debe tener 10 dígitos"
                                            hasErrors = true
                                        }
                                        if (correo.isBlank()) {
                                            correoError = "El correo es obligatorio"
                                            hasErrors = true
                                        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                                            correoError = "Correo inválido"
                                            hasErrors = true
                                        }
                                        if (cedula.isBlank()) {
                                            cedulaError = "La cédula es obligatoria"
                                            hasErrors = true
                                        } else if (cedula.length < 6 || cedula.length > 10) {
                                            cedulaError = "La cédula debe tener entre 6 y 10 dígitos"
                                            hasErrors = true
                                        }

                                        if (!hasErrors) {
                                            currentStep = 3
                                        }
                                    }
                                    3 -> {
                                        // Validar y guardar todo
                                        var hasErrors = false
                                        errorMessage = null

                                        if (contraseñaActual.isBlank()) {
                                            contraseñaActualError = "La contraseña actual es obligatoria"
                                            hasErrors = true
                                        }
                                        if (contraseñaNueva.isBlank()) {
                                            contraseñaNuevaError = "La contraseña nueva es obligatoria"
                                            hasErrors = true
                                        } else if (contraseñaNueva.length < 6) {
                                            contraseñaNuevaError = "Mínimo 6 caracteres"
                                            hasErrors = true
                                        } else if (!contraseñaNueva.matches(Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$"))) {
                                            contraseñaNuevaError = "Debe tener minúscula, mayúscula y número"
                                            hasErrors = true
                                        }
                                        if (contraseñaConfirmar.isBlank()) {
                                            contraseñaConfirmarError = "Debes confirmar la contraseña"
                                            hasErrors = true
                                        } else if (contraseñaConfirmar != contraseñaNueva) {
                                            contraseñaConfirmarError = "Las contraseñas no coinciden"
                                            hasErrors = true
                                        }

                                        if (hasErrors) return@Button

                                        scope.launch {
                                            isSaving = true
                                            errorMessage = null
                                            try {
                                                val telefonoCompleto = "+57$telefono"

                                                // Paso 1: Actualizar datos personales
                                                val updateBody = JsonObject().apply {
                                                    addProperty("nombre", nombre.trim())
                                                    addProperty("apellido", apellido.trim())
                                                    addProperty("telefono", telefonoCompleto)
                                                    addProperty("correo", correo.trim())
                                                    addProperty("cedula", cedula.trim())
                                                    addProperty("primerInicioSesion", false)
                                                }

                                                val updateResponse = ApiService.updateUser(
                                                    token = token,
                                                    id = userId,
                                                    body = updateBody
                                                )

                                                if (!updateResponse.isSuccessful) {
                                                    errorMessage = when (updateResponse.code()) {
                                                        400 -> "Datos inválidos. Verifica todos los campos."
                                                        401 -> "Sesión expirada. Inicia sesión nuevamente."
                                                        404 -> "Usuario no encontrado."
                                                        409 -> "El correo o teléfono ya está registrado."
                                                        500 -> "Error del servidor. Intenta nuevamente."
                                                        else -> "Error al actualizar (${updateResponse.code()})"
                                                    }
                                                    isSaving = false
                                                    return@launch
                                                }

                                                // Paso 2: Actualizar avatar
                                                val fotoResponse = ApiService.updateFotoPerfilPredeterminada(
                                                    token = token,
                                                    fotoPredeterminadaUrl = selectedAvatar!!
                                                )

                                                if (!fotoResponse.isSuccessful) {
                                                    Log.e("AvatarSelection", "Error al actualizar avatar: ${fotoResponse.code()}")
                                                }

                                                // Paso 3: Cambiar contraseña
                                                val passwordBody = JsonObject().apply {
                                                    addProperty("contraseñaActual", contraseñaActual)
                                                    addProperty("contraseñaNueva", contraseñaNueva)
                                                }

                                                val passwordResponse = ApiService.changePassword(
                                                    token = token,
                                                    contraseñaActual = contraseñaActual,
                                                    contraseñaNueva = contraseñaNueva
                                                )

                                                if (!passwordResponse.isSuccessful) {
                                                    val errorBody = passwordResponse.errorBody()?.string()
                                                    Log.e("AvatarSelection", "Error al cambiar contraseña: $errorBody")
                                                    errorMessage = when (passwordResponse.code()) {
                                                        400 -> "La contraseña actual es incorrecta"
                                                        401 -> "Sesión expirada"
                                                        else -> "Error al cambiar contraseña"
                                                    }
                                                    isSaving = false
                                                    return@launch
                                                }

                                                // Todo exitoso
                                                selectedAvatar?.let { onAvatarSelected(it) }
                                                showSuccessDialog = true

                                            } catch (e: Exception) {
                                                errorMessage = "Error de conexión: ${e.localizedMessage ?: e.message}"
                                                Log.e("AvatarSelection", "Error en proceso completo", e)
                                            } finally {
                                                isSaving = false
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = !isSaving,
                            modifier = Modifier
                                .weight(if (currentStep > 1) 1f else 1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when (currentStep) {
                                    1 -> AzulCielo
                                    2 -> AzulCielo
                                    else -> VerdeLima
                                },
                                disabledContainerColor = GrisClaro.copy(alpha = 0.5f)
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 8.dp
                            )
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text(
                                    when (currentStep) {
                                        1, 2 -> "Continuar"
                                        else -> "Guardar"
                                    },
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    when (currentStep) {
                                        1, 2 -> Icons.Default.ArrowForward
                                        else -> Icons.Default.Check
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    if (showSuccessDialog) {
        SuccessDialog(
            userRol = userRol,
            onDismiss = {
                showSuccessDialog = false
                navController.navigate("home") {
                    popUpTo(0) { inclusive = true }
                }
            }
        )
    }
}

@Composable
fun StepIndicator(currentStep: Int, totalSteps: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { index ->
            val step = index + 1
            val isActive = step <= currentStep
            val isCurrent = step == currentStep

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = when {
                                isCurrent -> AzulCielo
                                isActive -> VerdeLima
                                else -> GrisClaro
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isActive && !isCurrent) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = step.toString(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                if (step < totalSteps) {
                    Divider(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        color = if (isActive) VerdeLima else GrisClaro,
                        thickness = 2.dp
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(
            text = "Avatar",
            style = MaterialTheme.typography.bodySmall,
            color = if (currentStep >= 1) AzulCielo else GrisNeutral,
            fontWeight = if (currentStep == 1) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Text(
            text = "Datos",
            style = MaterialTheme.typography.bodySmall,
            color = if (currentStep >= 2) AzulCielo else GrisNeutral,
            fontWeight = if (currentStep == 2) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Text(
            text = "Contraseña",
            style = MaterialTheme.typography.bodySmall,
            color = if (currentStep >= 3) AzulCielo else GrisNeutral,
            fontWeight = if (currentStep == 3) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AvatarSelectionStep(
    avatarList: List<AvatarOption>,
    selectedAvatar: String?,
    onAvatarSelected: (String) -> Unit,
    nombre: String,
    errorMessage: String?
) {
    val scale by rememberInfiniteTransition(label = "scale").animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scaleAnim"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .scale(if (selectedAvatar != null) scale else 1f)
                        .shadow(
                            elevation = 12.dp,
                            shape = CircleShape,
                            ambientColor = AzulCielo.copy(alpha = 0.3f)
                        )
                        .border(
                            width = 5.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(AzulCielo, VerdeLima, Fucsia)
                            ),
                            shape = CircleShape
                        )
                        .padding(5.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedAvatar != null) {
                        AsyncImage(
                            model = selectedAvatar,
                            contentDescription = "Avatar seleccionado",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 8.dp, y = 8.dp)
                                .size(44.dp)
                                .background(VerdeLima, CircleShape)
                                .border(4.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Seleccionado",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Sin avatar",
                            tint = GrisClaro,
                            modifier = Modifier.size(90.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = if (nombre.isNotEmpty()) "¡Hola, $nombre!" else "¡Bienvenido!",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = FondoOscuroPrimario,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Elige el avatar que mejor te represente",
                    style = MaterialTheme.typography.bodyLarge,
                    color = GrisNeutral,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Selecciona tu Avatar",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = FondoOscuroPrimario
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (avatarList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AzulCielo)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Cargando avatares...", color = GrisNeutral)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.height(450.dp)
                ) {
                    items(avatarList) { avatar ->
                        AvatarItemEnhanced(
                            avatar = avatar,
                            isSelected = selectedAvatar == avatar.url,
                            onClick = { onAvatarSelected(avatar.url) }
                        )
                    }
                }
            }
        }

        errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun PersonalInfoStep(
    nombre: String,
    apellido: String,
    telefono: String,
    correo: String,
    cedula: String,
    nombreError: String?,
    apellidoError: String?,
    telefonoError: String?,
    correoError: String?,
    cedulaError: String?,
    onNombreChange: (String) -> Unit,
    onApellidoChange: (String) -> Unit,
    onTelefonoChange: (String) -> Unit,
    onCorreoChange: (String) -> Unit,
    onCedulaChange: (String) -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager,
    errorMessage: String?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = AzulCielo,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Información Personal",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = FondoOscuroPrimario
                )

                Text(
                    text = "Completa tus datos para continuar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GrisNeutral,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = onNombreChange,
            label = { Text("Nombre") },
            placeholder = { Text("Ingresa tu nombre") },
            leadingIcon = { Icon(Icons.Default.Person, null, tint = AzulCielo) },
            isError = nombreError != null,
            supportingText = nombreError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AzulCielo,
                unfocusedBorderColor = GrisClaro,
                focusedLabelColor = AzulCielo,
                cursorColor = AzulCielo
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = apellido,
            onValueChange = onApellidoChange,
            label = { Text("Apellido") },
            placeholder = { Text("Ingresa tu apellido") },
            leadingIcon = { Icon(Icons.Default.Person, null, tint = AzulCielo) },
            isError = apellidoError != null,
            supportingText = apellidoError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AzulCielo,
                unfocusedBorderColor = GrisClaro,
                focusedLabelColor = AzulCielo,
                cursorColor = AzulCielo
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = telefono,
            onValueChange = onTelefonoChange,
            label = { Text("Teléfono") },
            placeholder = { Text("3001234567") },
            prefix = {
                Text(
                    "+57 ",
                    color = VerdeLima,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            },
            leadingIcon = { Icon(Icons.Default.Phone, null, tint = VerdeLima) },
            isError = telefonoError != null,
            supportingText = {
                if (telefonoError != null) {
                    Text(telefonoError, color = MaterialTheme.colorScheme.error)
                } else {
                    Text("Formato: +57 seguido de 10 dígitos", color = GrisNeutral)
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VerdeLima,
                unfocusedBorderColor = GrisClaro,
                focusedLabelColor = VerdeLima,
                cursorColor = VerdeLima
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = correo,
            onValueChange = onCorreoChange,
            label = { Text("Correo Electrónico") },
            placeholder = { Text("ejemplo@correo.com") },
            leadingIcon = { Icon(Icons.Default.Email, null, tint = Fucsia) },
            isError = correoError != null,
            supportingText = correoError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Fucsia,
                unfocusedBorderColor = GrisClaro,
                focusedLabelColor = Fucsia,
                cursorColor = Fucsia
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = cedula,
            onValueChange = onCedulaChange,
            label = { Text("Cédula") },
            placeholder = { Text("6-10 dígitos") },
            leadingIcon = { Icon(Icons.Default.Badge, null, tint = Naranja) },
            isError = cedulaError != null,
            supportingText = cedulaError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Naranja,
                unfocusedBorderColor = GrisClaro,
                focusedLabelColor = Naranja,
                cursorColor = Naranja
            )
        )

        errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun PasswordChangeStep(
    contraseñaActual: String,
    contraseñaNueva: String,
    contraseñaConfirmar: String,
    contraseñaActualError: String?,
    contraseñaNuevaError: String?,
    contraseñaConfirmarError: String?,
    onContraseñaActualChange: (String) -> Unit,
    onContraseñaNuevaChange: (String) -> Unit,
    onContraseñaConfirmarChange: (String) -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager,
    errorMessage: String?
) {
    var contraseñaActualVisible by remember { mutableStateOf(false) }
    var contraseñaNuevaVisible by remember { mutableStateOf(false) }
    var contraseñaConfirmarVisible by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFF9C27B0),
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Cambia tu Contraseña",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = FondoOscuroPrimario
                )

                Text(
                    text = "Por seguridad, debes cambiar tu contraseña temporal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GrisNeutral,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3E0)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Naranja,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Tu contraseña debe tener al menos 6 caracteres, incluir mayúsculas, minúsculas y números",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8D6E63)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = contraseñaActual,
            onValueChange = onContraseñaActualChange,
            label = { Text("Contraseña Actual") },
            placeholder = { Text("Ingresa tu contraseña actual") },
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color(0xFF9C27B0)) },
            trailingIcon = {
                IconButton(onClick = { contraseñaActualVisible = !contraseñaActualVisible }) {
                    Icon(
                        imageVector = if (contraseñaActualVisible) Icons.Default.Visibility
                        else Icons.Default.VisibilityOff,
                        contentDescription = if (contraseñaActualVisible) "Ocultar" else "Mostrar",
                        tint = GrisNeutral
                    )
                }
            },
            isError = contraseñaActualError != null,
            supportingText = contraseñaActualError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            singleLine = true,
            visualTransformation = if (contraseñaActualVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF9C27B0),
                unfocusedBorderColor = GrisClaro,
                focusedLabelColor = Color(0xFF9C27B0),
                cursorColor = Color(0xFF9C27B0)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = contraseñaNueva,
            onValueChange = onContraseñaNuevaChange,
            label = { Text("Contraseña Nueva") },
            placeholder = { Text("Mínimo 6 caracteres") },
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = VerdeLima) },
            trailingIcon = {
                IconButton(onClick = { contraseñaNuevaVisible = !contraseñaNuevaVisible }) {
                    Icon(
                        imageVector = if (contraseñaNuevaVisible) Icons.Default.Visibility
                        else Icons.Default.VisibilityOff,
                        contentDescription = if (contraseñaNuevaVisible) "Ocultar" else "Mostrar",
                        tint = GrisNeutral
                    )
                }
            },
            isError = contraseñaNuevaError != null,
            supportingText = contraseñaNuevaError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            singleLine = true,
            visualTransformation = if (contraseñaNuevaVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VerdeLima,
                unfocusedBorderColor = GrisClaro,
                focusedLabelColor = VerdeLima,
                cursorColor = VerdeLima
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = contraseñaConfirmar,
            onValueChange = onContraseñaConfirmarChange,
            label = { Text("Confirmar Contraseña") },
            placeholder = { Text("Repite la contraseña nueva") },
            leadingIcon = { Icon(Icons.Default.Lock, null, tint = AzulCielo) },
            trailingIcon = {
                IconButton(onClick = { contraseñaConfirmarVisible = !contraseñaConfirmarVisible }) {
                    Icon(
                        imageVector = if (contraseñaConfirmarVisible) Icons.Default.Visibility
                        else Icons.Default.VisibilityOff,
                        contentDescription = if (contraseñaConfirmarVisible) "Ocultar" else "Mostrar",
                        tint = GrisNeutral
                    )
                }
            },
            isError = contraseñaConfirmarError != null,
            supportingText = contraseñaConfirmarError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            singleLine = true,
            visualTransformation = if (contraseñaConfirmarVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AzulCielo,
                unfocusedBorderColor = GrisClaro,
                focusedLabelColor = AzulCielo,
                cursorColor = AzulCielo
            )
        )

        errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun AvatarItemEnhanced(
    avatar: AvatarOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scaleAnim"
    )

    val elevation by animateDpAsState(
        targetValue = if (isSelected) 12.dp else 4.dp,
        animationSpec = tween(300),
        label = "elevationAnim"
    )

    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 4.dp else 2.dp,
        animationSpec = tween(300),
        label = "borderAnim"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .scale(scale)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = elevation,
                    shape = CircleShape,
                    ambientColor = if (isSelected) AzulCielo.copy(alpha = 0.4f)
                    else Color.Gray.copy(alpha = 0.2f)
                )
                .border(
                    width = borderWidth,
                    brush = if (isSelected) {
                        Brush.linearGradient(
                            colors = listOf(AzulCielo, VerdeLima, Fucsia)
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(GrisClaro, GrisClaro)
                        )
                    },
                    shape = CircleShape
                )
                .padding(if (isSelected) 4.dp else 2.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = avatar.url,
                contentDescription = avatar.name,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .alpha(if (isSelected) 1f else 0.7f)
            )

            AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 6.dp, y = 6.dp)
                        .size(32.dp)
                        .background(VerdeLima, CircleShape)
                        .border(3.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Seleccionado",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SuccessDialog(
    userRol: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        icon = {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        VerdeLima.copy(alpha = 0.15f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Éxito",
                    tint = VerdeLima,
                    modifier = Modifier.size(48.dp)
                )
            }
        },
        title = {
            Text(
                text = "¡Perfil Completado!",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center,
                color = FondoOscuroPrimario
            )
        },
        text = {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = if (userRol == "profesor" || userRol == "docente") {
                        "Estimado Profesor"
                    } else {
                        "Tu información se ha guardado correctamente"
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = FondoOscuroPrimario
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (userRol == "profesor" || userRol == "docente") {
                    Text(
                        text = "Tu perfil ha sido actualizado exitosamente y tu contraseña ha sido cambiada. Ahora puedes acceder a todas las funcionalidades de EDUMON.",
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = GrisOscuro
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = AzulCielo.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Computer,
                                contentDescription = null,
                                tint = AzulCielo,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Usa la plataforma web para gestionar tus cursos y contenidos",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = AzulCielo
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Bienvenido a EDUMON. Tu contraseña ha sido actualizada exitosamente. Ya puedes comenzar a explorar y realizar las tareas asignadas.",
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = GrisOscuro
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (userRol == "profesor" || userRol == "docente")
                        AzulCielo else VerdeLima
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = if (userRol == "profesor" || userRol == "docente")
                        "Entendido" else "Comenzar",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}