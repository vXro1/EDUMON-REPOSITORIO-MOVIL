package com.example.edumonjetcompose.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.edumonjetcompose.network.ApiService
import com.example.edumonjetcompose.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    token: String,
    userData: UserData?,
    onRegisterSuccess: () -> Unit
) {
    var nombre by remember { mutableStateOf(userData?.nombre ?: "") }
    var apellido by remember { mutableStateOf(userData?.apellido ?: "") }
    var cedula by remember { mutableStateOf(userData?.cedula ?: "") }
    var correo by remember { mutableStateOf(userData?.correo ?: "") }
    var telefono by remember { mutableStateOf(userData?.telefono ?: "") }
    var contraseña by remember { mutableStateOf("") }
    var confirmarContraseña by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var showPasswordFields by remember { mutableStateOf(userData == null) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val isUpdateMode = userData != null

    // Animación
    val scale by rememberInfiniteTransition(label = "scale").animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
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
                        if (isUpdateMode) "Completar Perfil" else "Registro",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AzulCielo,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF8FBFF),
                            Color(0xFFFFFFFF),
                            Color(0xFFFFFBF8)
                        )
                    )
                )
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Ícono principal
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(scale)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    AzulCielo.copy(alpha = 0.2f),
                                    AzulCielo.copy(alpha = 0.05f)
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isUpdateMode) Icons.Default.Edit else Icons.Default.PersonAdd,
                        contentDescription = if (isUpdateMode) "Editar" else "Registro",
                        modifier = Modifier.size(50.dp),
                        tint = AzulCielo
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = if (isUpdateMode) "Completa tus Datos" else "Crear Cuenta",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ),
                    color = FondoOscuroPrimario
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isUpdateMode)
                        "Por favor, completa o actualiza tu información"
                    else
                        "Completa tus datos para registrarte",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                    color = GrisNeutral,
                    textAlign = TextAlign.Center
                )

                if (isUpdateMode) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Naranja.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                null,
                                tint = Naranja,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Completa tu cédula para continuar",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Naranja
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Card con formulario
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.95f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Nombre
                        OutlinedTextField(
                            value = nombre,
                            onValueChange = {
                                nombre = it
                                errorMessage = null
                            },
                            label = { Text("Nombre") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, null, tint = AzulCielo)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AzulCielo,
                                focusedLabelColor = AzulCielo,
                                cursorColor = AzulCielo
                            )
                        )

                        // Apellido
                        OutlinedTextField(
                            value = apellido,
                            onValueChange = {
                                apellido = it
                                errorMessage = null
                            },
                            label = { Text("Apellido") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, null, tint = AzulCielo)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AzulCielo,
                                focusedLabelColor = AzulCielo,
                                cursorColor = AzulCielo
                            )
                        )

                        // Cédula
                        OutlinedTextField(
                            value = cedula,
                            onValueChange = {
                                if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                                    cedula = it
                                    errorMessage = null
                                }
                            },
                            label = { Text("Cédula *") },
                            placeholder = { Text("1234567890") },
                            leadingIcon = {
                                Icon(Icons.Default.Badge, null, tint = VerdeLima)
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VerdeLima,
                                focusedLabelColor = VerdeLima,
                                cursorColor = VerdeLima
                            ),
                            supportingText = {
                                Text(
                                    "6-10 dígitos numéricos",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GrisNeutral
                                )
                            }
                        )

                        // Correo
                        OutlinedTextField(
                            value = correo,
                            onValueChange = {
                                correo = it
                                errorMessage = null
                            },
                            label = { Text("Correo electrónico") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, null, tint = Fucsia)
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Fucsia,
                                focusedLabelColor = Fucsia,
                                cursorColor = Fucsia
                            )
                        )

                        // Teléfono
                        OutlinedTextField(
                            value = telefono,
                            onValueChange = {
                                if (it.length <= 10) {
                                    telefono = it
                                    errorMessage = null
                                }
                            },
                            label = { Text("Teléfono") },
                            placeholder = { Text("3001234567") },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, null, tint = Naranja)
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Naranja,
                                focusedLabelColor = Naranja,
                                cursorColor = Naranja
                            )
                        )

                        // Campos de contraseña (solo para registro nuevo)
                        if (!isUpdateMode) {
                            Divider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = GrisClaro
                            )

                            Text(
                                text = "Seguridad",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = FondoOscuroPrimario
                            )

                            // Contraseña
                            OutlinedTextField(
                                value = contraseña,
                                onValueChange = {
                                    contraseña = it
                                    errorMessage = null
                                },
                                label = { Text("Contraseña") },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, null, tint = VerdeLima)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            if (passwordVisible) Icons.Default.Visibility
                                            else Icons.Default.VisibilityOff,
                                            null,
                                            tint = GrisNeutral
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible)
                                    VisualTransformation.None
                                else
                                    PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = VerdeLima,
                                    focusedLabelColor = VerdeLima,
                                    cursorColor = VerdeLima
                                ),
                                supportingText = {
                                    Text(
                                        "Mínimo 6 caracteres, incluye mayúscula, minúscula y número",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = GrisNeutral
                                    )
                                }
                            )

                            // Confirmar contraseña
                            OutlinedTextField(
                                value = confirmarContraseña,
                                onValueChange = {
                                    confirmarContraseña = it
                                    errorMessage = null
                                },
                                label = { Text("Confirmar contraseña") },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, null, tint = VerdeLima)
                                },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        confirmPasswordVisible = !confirmPasswordVisible
                                    }) {
                                        Icon(
                                            if (confirmPasswordVisible) Icons.Default.Visibility
                                            else Icons.Default.VisibilityOff,
                                            null,
                                            tint = GrisNeutral
                                        )
                                    }
                                },
                                visualTransformation = if (confirmPasswordVisible)
                                    VisualTransformation.None
                                else
                                    PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = VerdeLima,
                                    focusedLabelColor = VerdeLima,
                                    cursorColor = VerdeLima
                                )
                            )
                        }

                        // Mensaje de error
                        AnimatedVisibility(
                            visible = errorMessage != null,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Error.copy(alpha = 0.1f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Error,
                                        null,
                                        tint = Error,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = errorMessage ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Error
                                    )
                                }
                            }
                        }

                        // Mensaje de éxito
                        AnimatedVisibility(
                            visible = successMessage != null,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = VerdeLima.copy(alpha = 0.1f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        null,
                                        tint = VerdeLima,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = successMessage ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = VerdeLima
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Botón de registro/actualización
                        Button(
                            onClick = {
                                // Validaciones
                                when {
                                    nombre.isBlank() -> errorMessage = "El nombre es requerido"
                                    apellido.isBlank() -> errorMessage = "El apellido es requerido"
                                    cedula.isBlank() -> errorMessage = "La cédula es requerida"
                                    cedula.length < 6 -> errorMessage = "La cédula debe tener al menos 6 dígitos"
                                    correo.isBlank() -> errorMessage = "El correo es requerido"
                                    !correo.contains("@") -> errorMessage = "Correo inválido"
                                    telefono.isBlank() -> errorMessage = "El teléfono es requerido"
                                    !isUpdateMode && contraseña.isBlank() -> errorMessage = "La contraseña es requerida"
                                    !isUpdateMode && contraseña.length < 6 -> errorMessage = "La contraseña debe tener al menos 6 caracteres"
                                    !isUpdateMode && !contraseña.matches(Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$")) ->
                                        errorMessage = "La contraseña debe contener mayúscula, minúscula y número"
                                    !isUpdateMode && contraseña != confirmarContraseña -> errorMessage = "Las contraseñas no coinciden"
                                    else -> {
                                        scope.launch {
                                            isLoading = true
                                            errorMessage = null

                                            try {
                                                val response = if (isUpdateMode) {
                                                    // Actualizar usuario existente CON cédula
                                                    ApiService.updateUserWithCedula(
                                                        token = token,
                                                        id = userData!!.id,
                                                        nombre = nombre,
                                                        apellido = apellido,
                                                        cedula = cedula,
                                                        correo = correo,
                                                        telefono = telefono
                                                    )
                                                } else {
                                                    // Registrar nuevo usuario
                                                    ApiService.registerUser(
                                                        nombre = nombre,
                                                        apellido = apellido,
                                                        correo = correo,
                                                        telefono = telefono,
                                                        contraseña = contraseña,
                                                        rol = "padre"
                                                    )
                                                }

                                                if (response.isSuccessful) {
                                                    successMessage = if (isUpdateMode)
                                                        "Datos actualizados. Redirigiendo..."
                                                    else
                                                        "Registro exitoso. Redirigiendo..."
                                                    kotlinx.coroutines.delay(1500)
                                                    onRegisterSuccess()
                                                } else {
                                                    errorMessage = "Error: ${response.code()}"
                                                }
                                            } catch (e: Exception) {
                                                errorMessage = "Error de conexión: ${e.message}"
                                            } finally {
                                                isLoading = false
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            enabled = !isLoading,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AzulCielo,
                                disabledContainerColor = GrisClaro
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 8.dp,
                                pressedElevation = 12.dp
                            )
                        ) {
                            if (isLoading) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White,
                                        strokeWidth = 2.5.dp
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        if (isUpdateMode) "Actualizando..." else "Registrando...",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isUpdateMode) Icons.Default.Save else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        if (isUpdateMode) "Actualizar Datos" else "Registrarse",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}