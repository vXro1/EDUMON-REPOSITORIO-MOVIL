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
import com.example.edumonjetcompose.network.ApiService
import com.example.edumonjetcompose.ui.theme.*
import com.google.gson.JsonObject
import kotlinx.coroutines.launch

data class AvatarOption(
    val url: String,
    val name: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarSelectionScreen(
    navController: NavController,
    token: String,
    currentAvatarUrl: String?,
    onAvatarSelected: (String) -> Unit
) {
    // Estados
    var avatarList by remember { mutableStateOf<List<AvatarOption>>(emptyList()) }
    var selectedAvatar by remember { mutableStateOf<String?>(currentAvatarUrl) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Datos del usuario
    var userId by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var apellido by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var cedula by remember { mutableStateOf("") }
    var contraseña by remember { mutableStateOf("") }
    var userRol by remember { mutableStateOf("") }
    var primerInicioSesion by remember { mutableStateOf(true) }

    // Estados de validación
    var nombreError by remember { mutableStateOf<String?>(null) }
    var apellidoError by remember { mutableStateOf<String?>(null) }
    var correoError by remember { mutableStateOf<String?>(null) }
    var telefonoError by remember { mutableStateOf<String?>(null) }
    var cedulaError by remember { mutableStateOf<String?>(null) }
    var contraseñaError by remember { mutableStateOf<String?>(null) }

    // Estados de visibilidad
    var cedulaVisible by remember { mutableStateOf(false) }
    var contraseñaVisible by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    // Cargar datos del usuario y avatares
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                Log.d("AvatarSelection", "🔑 Token recibido: ${token.take(20)}...")

                val profileResponse = ApiService.getUserProfile(token)
                Log.d("AvatarSelection", "📥 Response code perfil: ${profileResponse.code()}")

                if (profileResponse.isSuccessful) {
                    val body = profileResponse.body()
                    val userObj = body?.getAsJsonObject("user") ?: body

                    if (userObj != null) {
                        userId = userObj.get("id")?.asString ?: userObj.get("_id")?.asString ?: ""
                        nombre = userObj.get("nombre")?.asString ?: ""
                        apellido = userObj.get("apellido")?.asString ?: ""
                        correo = userObj.get("correo")?.asString ?: ""
                        userRol = userObj.get("rol")?.asString ?: ""
                        primerInicioSesion = userObj.get("primerInicioSesion")?.asBoolean ?: true

                        // Extraer solo los 10 dígitos si viene con +57
                        val telefonoCompleto = userObj.get("telefono")?.asString ?: ""
                        telefono = if (telefonoCompleto.startsWith("+57")) {
                            telefonoCompleto.substring(3)
                        } else {
                            telefonoCompleto
                        }

                        cedula = userObj.get("cedula")?.takeIf { !it.isJsonNull }?.asString ?: ""

                        // Cargar avatar actual si existe
                        val avatarActual = userObj.get("fotoPerfilUrl")?.asString
                        if (!avatarActual.isNullOrEmpty()) {
                            selectedAvatar = avatarActual
                        }

                        Log.d("AvatarSelection", """
                            ✅ Usuario cargado:
                            - ID: $userId
                            - Nombre: $nombre $apellido
                            - Rol: $userRol
                            - Teléfono: $telefono
                            - Cédula: ${if (cedula.isNotEmpty()) "***" else "sin cédula"}
                            - Primer inicio: $primerInicioSesion
                            - Avatar actual: $avatarActual
                        """.trimIndent())
                    }
                } else {
                    val errorBody = profileResponse.errorBody()?.string()
                    errorMessage = "Error al cargar perfil (${profileResponse.code()}): $errorBody"
                    Log.e("AvatarSelection", "❌ Error perfil: $errorMessage")
                }

                // Cargar avatares predeterminados
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
                    Log.d("AvatarSelection", "✅ ${avatares.size} avatares cargados")
                } else {
                    val errorBody = avatarResponse.errorBody()?.string()
                    if (errorMessage == null) {
                        errorMessage = "Error al cargar avatares (${avatarResponse.code()})"
                    }
                    Log.e("AvatarSelection", "❌ Error avatares: $errorBody")
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.message}"
                Log.e("AvatarSelection", "❌ Excepción al cargar datos", e)
            } finally {
                isLoading = false
            }
        }
    }

    val scale by rememberInfiniteTransition(label = "scale").animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scaleAnim"
    )

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
                        colors = listOf(Color(0xFFF8FBFF), Color(0xFFFFFFFF))
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
                    // Card de bienvenida con avatar grande
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Avatar grande con animación
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .scale(if (selectedAvatar != null) scale else 1f)
                                    .shadow(
                                        elevation = 12.dp,
                                        shape = CircleShape,
                                        ambientColor = AzulCielo.copy(alpha = 0.3f)
                                    )
                                    .border(
                                        width = 5.dp,
                                        brush = Brush.linearGradient(
                                            colors = listOf(AzulCielo, VerdeLima)
                                        ),
                                        shape = CircleShape
                                    )
                                    .padding(6.dp)
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

                                    // Checkmark de seleccionado
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(38.dp)
                                            .background(
                                                Brush.radialGradient(
                                                    colors = listOf(VerdeLima, VerdeLima.copy(alpha = 0.9f))
                                                ),
                                                CircleShape
                                            )
                                            .border(4.dp, Color.White, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Seleccionado",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = "Sin avatar",
                                        tint = GrisClaro,
                                        modifier = Modifier.size(80.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = if (nombre.isNotEmpty()) "¡Hola, $nombre!" else "¡Bienvenido!",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = FondoOscuroPrimario,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Completa tu información y elige tu avatar para continuar",
                                style = MaterialTheme.typography.bodyMedium,
                                color = GrisNeutral,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Sección de Avatar
                    Text(
                        text = "Selecciona tu Avatar *",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = FondoOscuroPrimario
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Grid de avatares mejorado
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.height(320.dp)
                        ) {
                            items(avatarList) { avatar ->
                                AvatarItemEnhanced(
                                    avatar = avatar,
                                    isSelected = selectedAvatar == avatar.url,
                                    onClick = { selectedAvatar = avatar.url }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Información Personal *",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = FondoOscuroPrimario
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Nombre
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = {
                            nombre = it
                            nombreError = if (it.isBlank()) "El nombre es obligatorio" else null
                        },
                        label = { Text("Nombre *") },
                        placeholder = { Text("Ingresa tu nombre") },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = AzulCielo) },
                        isError = nombreError != null,
                        supportingText = nombreError?.let { { Text(it, color = Error) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AzulCielo,
                            unfocusedBorderColor = GrisClaro,
                            focusedLabelColor = AzulCielo,
                            cursorColor = AzulCielo
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Apellido
                    OutlinedTextField(
                        value = apellido,
                        onValueChange = {
                            apellido = it
                            apellidoError = if (it.isBlank()) "El apellido es obligatorio" else null
                        },
                        label = { Text("Apellido *") },
                        placeholder = { Text("Ingresa tu apellido") },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = AzulCielo) },
                        isError = apellidoError != null,
                        supportingText = apellidoError?.let { { Text(it, color = Error) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AzulCielo,
                            unfocusedBorderColor = GrisClaro,
                            focusedLabelColor = AzulCielo,
                            cursorColor = AzulCielo
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Teléfono con +57 obligatorio
                    OutlinedTextField(
                        value = telefono,
                        onValueChange = {
                            // Solo permitir dígitos y máximo 10
                            if (it.all { char -> char.isDigit() } && it.length <= 10) {
                                telefono = it
                                telefonoError = when {
                                    it.isBlank() -> "El teléfono es obligatorio"
                                    it.length < 10 -> "El teléfono debe tener 10 dígitos"
                                    else -> null
                                }
                            }
                        },
                        label = { Text("Teléfono *") },
                        placeholder = { Text("3001234567") },
                        prefix = {
                            Text(
                                "+57 ",
                                color = VerdeLima,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        },
                        leadingIcon = { Icon(Icons.Default.Phone, null, tint = VerdeLima) },
                        isError = telefonoError != null,
                        supportingText = {
                            if (telefonoError != null) {
                                Text(telefonoError!!, color = Error)
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
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VerdeLima,
                            unfocusedBorderColor = GrisClaro,
                            focusedLabelColor = VerdeLima,
                            cursorColor = VerdeLima
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Correo
                    OutlinedTextField(
                        value = correo,
                        onValueChange = {
                            correo = it
                            correoError = when {
                                it.isBlank() -> "El correo es obligatorio"
                                !android.util.Patterns.EMAIL_ADDRESS.matcher(it).matches() ->
                                    "Correo inválido"
                                else -> null
                            }
                        },
                        label = { Text("Correo Electrónico *") },
                        placeholder = { Text("ejemplo@correo.com") },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = Fucsia) },
                        isError = correoError != null,
                        supportingText = correoError?.let { { Text(it, color = Error) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Fucsia,
                            unfocusedBorderColor = GrisClaro,
                            focusedLabelColor = Fucsia,
                            cursorColor = Fucsia
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Cédula
                    OutlinedTextField(
                        value = cedula,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() } && it.length <= 10) {
                                cedula = it
                                cedulaError = when {
                                    it.isBlank() -> "La cédula es obligatoria"
                                    it.length < 6 -> "Mínimo 6 dígitos"
                                    else -> null
                                }
                            }
                        },
                        label = { Text("Cédula *") },
                        placeholder = { Text("6-10 dígitos") },
                        leadingIcon = { Icon(Icons.Default.Badge, null, tint = Naranja) },
                        trailingIcon = {
                            IconButton(onClick = { cedulaVisible = !cedulaVisible }) {
                                Icon(
                                    imageVector = if (cedulaVisible) Icons.Default.Visibility
                                    else Icons.Default.VisibilityOff,
                                    contentDescription = if (cedulaVisible) "Ocultar" else "Mostrar",
                                    tint = GrisNeutral
                                )
                            }
                        },
                        isError = cedulaError != null,
                        supportingText = cedulaError?.let { { Text(it, color = Error) } },
                        singleLine = true,
                        visualTransformation = if (cedulaVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Naranja,
                            unfocusedBorderColor = GrisClaro,
                            focusedLabelColor = Naranja,
                            cursorColor = Naranja
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Contraseña
                    OutlinedTextField(
                        value = contraseña,
                        onValueChange = {
                            contraseña = it
                            contraseñaError = when {
                                it.isBlank() -> "La contraseña es obligatoria"
                                it.length < 6 -> "Mínimo 6 caracteres"
                                it.length > 128 -> "Máximo 128 caracteres"
                                !it.matches(Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$")) ->
                                    "Debe tener minúscula, mayúscula y número"
                                else -> null
                            }
                        },
                        label = { Text("Contraseña *") },
                        placeholder = { Text("Mínimo 6 caracteres") },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color(0xFF9C27B0)) },
                        trailingIcon = {
                            IconButton(onClick = { contraseñaVisible = !contraseñaVisible }) {
                                Icon(
                                    imageVector = if (contraseñaVisible) Icons.Default.Visibility
                                    else Icons.Default.VisibilityOff,
                                    contentDescription = if (contraseñaVisible) "Ocultar" else "Mostrar",
                                    tint = GrisNeutral
                                )
                            }
                        },
                        isError = contraseñaError != null,
                        supportingText = contraseñaError?.let { { Text(it, color = Error) } },
                        singleLine = true,
                        visualTransformation = if (contraseñaVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF9C27B0),
                            unfocusedBorderColor = GrisClaro,
                            focusedLabelColor = Color(0xFF9C27B0),
                            cursorColor = Color(0xFF9C27B0)
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Mensaje de error
                    errorMessage?.let { error ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Error.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    null,
                                    tint = Error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = error,
                                    color = Error,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Botón de guardar
                    Button(
                        onClick = {
                            var hasErrors = false
                            errorMessage = null

                            // Validaciones
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

                            if (contraseña.isBlank()) {
                                contraseñaError = "La contraseña es obligatoria"
                                hasErrors = true
                            } else if (contraseña.length < 6) {
                                contraseñaError = "Mínimo 6 caracteres"
                                hasErrors = true
                            } else if (!contraseña.matches(Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$"))) {
                                contraseñaError = "Debe tener minúscula, mayúscula y número"
                                hasErrors = true
                            }

                            if (selectedAvatar == null) {
                                errorMessage = "Debes seleccionar un avatar"
                                hasErrors = true
                            }

                            if (hasErrors) {
                                return@Button
                            }

                            // Guardar datos
                            scope.launch {
                                isSaving = true
                                errorMessage = null
                                try {
                                    // Concatenar +57 al teléfono
                                    val telefonoCompleto = "+57$telefono"

                                    Log.d("AvatarSelection", """
                                        💾 Guardando datos del usuario:
                                        - ID: $userId
                                        - Rol: $userRol
                                        - Nombre: $nombre $apellido
                                        - Teléfono completo: $telefonoCompleto
                                        - Correo: $correo
                                        - Cédula: ${if (cedula.isNotEmpty()) "***" else "vacío"}
                                        - Avatar: $selectedAvatar
                                    """.trimIndent())

                                    // 1. Actualizar datos del usuario
                                    val updateBody = JsonObject().apply {
                                        addProperty("nombre", nombre.trim())
                                        addProperty("apellido", apellido.trim())
                                        addProperty("telefono", telefonoCompleto)
                                        addProperty("correo", correo.trim())
                                        addProperty("cedula", cedula.trim())
                                        addProperty("contraseña", contraseña.trim())
                                        addProperty("primerInicioSesion", false)
                                    }

                                    Log.d("AvatarSelection", "📤 JSON a enviar: $updateBody")

                                    val updateResponse = ApiService.updateUser(
                                        token = token,
                                        id = userId,
                                        body = updateBody
                                    )

                                    Log.d("AvatarSelection", "📥 Response code usuario: ${updateResponse.code()}")

                                    if (!updateResponse.isSuccessful) {
                                        val errorBody = updateResponse.errorBody()?.string()
                                        Log.e("AvatarSelection", """
                                            ❌ Error al actualizar usuario:
                                            - Code: ${updateResponse.code()}
                                            - Body: $errorBody
                                        """.trimIndent())

                                        errorMessage = when (updateResponse.code()) {
                                            400 -> "Datos inválidos. Verifica todos los campos."
                                            401 -> "Sesión expirada. Inicia sesión nuevamente."
                                            404 -> "Usuario no encontrado."
                                            409 -> "El correo o teléfono ya está registrado."
                                            500 -> "Error del servidor. Intenta nuevamente."
                                            else -> "Error al actualizar: ${updateResponse.code()}"
                                        }

                                        isSaving = false
                                        return@launch
                                    }

                                    val responseBody = updateResponse.body()
                                    Log.d("AvatarSelection", "✅ Usuario actualizado exitosamente: $responseBody")

                                    // 2. Actualizar avatar
                                    Log.d("AvatarSelection", "📸 Actualizando avatar: $selectedAvatar")

                                    val fotoResponse = ApiService.updateFotoPerfilPredeterminada(
                                        token = token,
                                        fotoPredeterminadaUrl = selectedAvatar!!
                                    )

                                    Log.d("AvatarSelection", "📥 Response code avatar: ${fotoResponse.code()}")

                                    if (fotoResponse.isSuccessful) {
                                        val fotoBody = fotoResponse.body()
                                        Log.d("AvatarSelection", "✅ Avatar actualizado exitosamente: $fotoBody")

                                        // Guardar avatar seleccionado en el callback
                                        selectedAvatar?.let { onAvatarSelected(it) }

                                        // Mostrar diálogo de éxito
                                        showSuccessDialog = true
                                    } else {
                                        val errorBody = fotoResponse.errorBody()?.string()
                                        Log.e("AvatarSelection", """
                                            ⚠️ Error al actualizar avatar:
                                            - Code: ${fotoResponse.code()}
                                            - Body: $errorBody
                                        """.trimIndent())

                                        // Aún así mostrar éxito porque los datos se guardaron
                                        errorMessage = "Datos guardados, pero error al actualizar avatar. Puedes cambiarlo después."
                                        showSuccessDialog = true
                                    }

                                } catch (e: Exception) {
                                    Log.e("AvatarSelection", "❌ Excepción al guardar", e)
                                    errorMessage = "Error de conexión: ${e.localizedMessage ?: e.message}"
                                    e.printStackTrace()
                                } finally {
                                    isSaving = false
                                }
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AzulCielo,
                            disabledContainerColor = GrisClaro.copy(alpha = 0.4f)
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 10.dp,
                            pressedElevation = 14.dp
                        )
                    ) {
                        if (isSaving) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text("Guardando...", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Guardar y Continuar",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    // Diálogo de éxito
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { },
            icon = {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    VerdeLima.copy(alpha = 0.3f),
                                    VerdeLima.copy(alpha = 0.1f)
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Éxito",
                        tint = VerdeLima,
                        modifier = Modifier.size(50.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "¡Perfil Completado!",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    textAlign = TextAlign.Center,
                    color = FondoOscuroPrimario
                )
            },
            text = {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = if (userRol == "profesor" || userRol == "docente") {
                            "Estimado Profesor,"
                        } else {
                            "Tu información se ha guardado correctamente."
                        },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = FondoOscuroPrimario
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (userRol == "profesor" || userRol == "docente") {
                        Text(
                            text = "Tu perfil ha sido actualizado exitosamente. Ahora puedes acceder a todas las funcionalidades de EDUMON.",
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                            color = GrisOscuro
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = AzulCielo.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(16.dp)
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
                                    text = "Usa la plataforma web para gestionar tus cursos y contenidos.",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = AzulCielo
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "¡Bienvenido a EDUMON! Ya puedes comenzar a explorar y realizar las tareas asignadas.",
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                            color = GrisOscuro
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false

                        // Navegar según el rol
                        try {
                            Log.d("AvatarSelection", "🚀 Navegando a home - Rol: $userRol")

                            navController.navigate("home") {
                                popUpTo(0) { inclusive = true }
                            }

                            Log.d("AvatarSelection", "✅ Navegación exitosa")

                        } catch (e: Exception) {
                            Log.e("AvatarSelection", "❌ Error en navegación", e)
                            // Intentar navegación alternativa
                            try {
                                navController.navigate("home") {
                                    popUpTo("avatar_selection") { inclusive = true }
                                }
                            } catch (e2: Exception) {
                                Log.e("AvatarSelection", "❌ Error crítico en navegación fallback", e2)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (userRol == "profesor" || userRol == "docente") AzulCielo else VerdeLima
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Text(
                        text = if (userRol == "profesor" || userRol == "docente") "Entendido" else "Comenzar",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp)
        )
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
                    elevation = if (isSelected) 12.dp else 4.dp,
                    shape = CircleShape,
                    ambientColor = if (isSelected) AzulCielo.copy(alpha = 0.4f) else Color.Gray.copy(alpha = 0.2f)
                )
                .border(
                    width = if (isSelected) 4.dp else 2.dp,
                    brush = if (isSelected) {
                        Brush.linearGradient(
                            colors = listOf(AzulCielo, VerdeLima)
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
            )

            // Indicador de selección
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
                        .size(32.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(VerdeLima, VerdeLima.copy(alpha = 0.9f))
                            ),
                            CircleShape
                        )
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