package com.example.edumonjetcompose.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.edumonjetcompose.R
import com.example.edumonjetcompose.network.ApiService
import com.example.edumonjetcompose.ui.theme.*
import com.google.gson.JsonObject
import kotlinx.coroutines.launch

data class UserData(
    val id: String,
    val nombre: String,
    val apellido: String,
    val cedula: String?,
    val correo: String,
    val telefono: String,
    val rol: String,
    val fotoPerfilUrl: String?,
    val estado: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    onLoginSuccess: (String, UserData) -> Unit
) {
    var telefono by remember { mutableStateOf("") }
    var contraseña by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Animaciones
    val infiniteTransition = rememberInfiniteTransition(label = "effects")

    val bubble1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "b1"
    )

    val bubble2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -25f,
        animationSpec = infiniteRepeatable(
            animation = tween(3400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "b2"
    )

    val bubble3 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 35f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "b3"
    )

    val bubble4 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -20f,
        animationSpec = infiniteRepeatable(
            animation = tween(3800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "b4"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "rotate"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

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
    ) {
        // Burbujas de fondo
        Box(modifier = Modifier.size(280.dp).offset(x = 250.dp, y = (-100).dp + bubble1.dp).scale(scale)
            .background(Brush.radialGradient(colors = listOf(AzulCielo.copy(alpha = 0.15f), AzulCielo.copy(alpha = 0.05f))), CircleShape))
        Box(modifier = Modifier.size(200.dp).offset(x = (-70).dp, y = 40.dp + bubble2.dp)
            .background(Brush.radialGradient(colors = listOf(VerdeLima.copy(alpha = 0.18f), VerdeLima.copy(alpha = 0.06f))), CircleShape))
        Box(modifier = Modifier.size(160.dp).offset(x = 280.dp, y = 200.dp + bubble3.dp)
            .background(Brush.radialGradient(colors = listOf(Fucsia.copy(alpha = 0.12f), Fucsia.copy(alpha = 0.04f))), CircleShape))
        Box(modifier = Modifier.size(120.dp).offset(x = 40.dp, y = 340.dp + bubble4.dp).background(Naranja.copy(alpha = 0.12f), CircleShape))
        Box(modifier = Modifier.size(140.dp).offset(x = (-60).dp, y = 500.dp + bubble1.dp)
            .background(Brush.radialGradient(colors = listOf(AzulCielo.copy(alpha = 0.14f), AzulCielo.copy(alpha = 0.05f))), CircleShape))
        Box(modifier = Modifier.size(110.dp).offset(x = 270.dp, y = 600.dp + bubble2.dp).background(VerdeLima.copy(alpha = 0.1f), CircleShape))
        Box(modifier = Modifier.size(80.dp).offset(x = 150.dp, y = 480.dp + bubble3.dp).background(Fucsia.copy(alpha = 0.09f), CircleShape))
        Box(modifier = Modifier.size(90.dp).offset(x = 60.dp, y = 160.dp + bubble4.dp).background(Naranja.copy(alpha = 0.11f), CircleShape))
        Box(modifier = Modifier.size(100.dp).offset(x = 200.dp, y = 420.dp + bubble1.dp).background(AzulCielo.copy(alpha = 0.08f), CircleShape))
        Box(modifier = Modifier.size(70.dp).offset(x = (-20).dp, y = 360.dp + bubble2.dp).background(VerdeLima.copy(alpha = 0.12f), CircleShape))
        Box(modifier = Modifier.size(140.dp).offset(x = 240.dp, y = 380.dp).rotate(rotation * 0.3f)
            .background(Fucsia.copy(alpha = 0.06f), RoundedCornerShape(20.dp)))
        Box(modifier = Modifier.size(100.dp).offset(x = 30.dp, y = 240.dp).rotate(-rotation * 0.5f)
            .background(Naranja.copy(alpha = 0.08f), RoundedCornerShape(16.dp)))

        // Contenido principal
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
                    .shadow(
                        elevation = 24.dp,
                        shape = CircleShape,
                        spotColor = AzulCielo.copy(alpha = 0.4f),
                        ambientColor = VerdeLima.copy(alpha = 0.2f)
                    )
                    .background(Color.White, CircleShape)
                    .padding(8.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                AzulCielo.copy(alpha = 0.2f),
                                AzulCielo.copy(alpha = 0.08f)
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.fondo),
                    contentDescription = "Logo EDUMON",
                    modifier = Modifier
                        .size(104.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "EDUMON",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 46.sp,
                    letterSpacing = 2.5.sp,
                    brush = Brush.horizontalGradient(colors = listOf(AzulCielo, VerdeLima))
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Educación y Monitoreo",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                ),
                color = GrisNeutral
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Indicadores coloridos
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(35.dp)
                        .height(6.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(AzulCielo, AzulCielo.copy(alpha = 0.6f))
                            ),
                            RoundedCornerShape(3.dp)
                        )
                )
                Box(
                    modifier = Modifier
                        .width(35.dp)
                        .height(6.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(VerdeLima, VerdeLima.copy(alpha = 0.6f))
                            ),
                            RoundedCornerShape(3.dp)
                        )
                )
                Box(
                    modifier = Modifier
                        .width(35.dp)
                        .height(6.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Fucsia, Fucsia.copy(alpha = 0.6f))
                            ),
                            RoundedCornerShape(3.dp)
                        )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Card del formulario
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .scale(scale)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        AzulCielo.copy(alpha = 0.2f),
                                        AzulCielo.copy(alpha = 0.08f)
                                    )
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Usuario",
                            tint = AzulCielo,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "¡Bienvenido!",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp
                        ),
                        color = FondoOscuroPrimario
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Ingresa para continuar",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                        color = GrisNeutral
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Campo Teléfono
                    OutlinedTextField(
                        value = telefono,
                        onValueChange = { telefono = it; errorMessage = null },
                        label = { Text("Número de teléfono") },
                        placeholder = { Text("3001234567") },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                AzulCielo.copy(alpha = 0.15f),
                                                AzulCielo.copy(alpha = 0.05f)
                                            )
                                        ),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Teléfono",
                                    tint = AzulCielo,
                                    modifier = Modifier.size(22.dp)
                                )
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
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AzulCielo,
                            unfocusedBorderColor = GrisClaro.copy(alpha = 0.6f),
                            focusedLabelColor = AzulCielo,
                            cursorColor = AzulCielo,
                            focusedContainerColor = AzulCielo.copy(alpha = 0.04f),
                            unfocusedContainerColor = GrisExtraClaro.copy(alpha = 0.6f)
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Campo Contraseña
                    OutlinedTextField(
                        value = contraseña,
                        onValueChange = { contraseña = it; errorMessage = null },
                        label = { Text("Contraseña") },
                        placeholder = { Text("Ingresa tu contraseña") },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                VerdeLima.copy(alpha = 0.15f),
                                                VerdeLima.copy(alpha = 0.05f)
                                            )
                                        ),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Contraseña",
                                    tint = VerdeLima,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility
                                    else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Ocultar contraseña"
                                    else "Mostrar contraseña",
                                    tint = GrisNeutral,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VerdeLima,
                            unfocusedBorderColor = GrisClaro.copy(alpha = 0.6f),
                            focusedLabelColor = VerdeLima,
                            cursorColor = VerdeLima,
                            focusedContainerColor = VerdeLima.copy(alpha = 0.04f),
                            unfocusedContainerColor = GrisExtraClaro.copy(alpha = 0.6f)
                        )
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Mensaje de error
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn() + expandVertically() + slideInVertically(),
                        exit = fadeOut() + shrinkVertically() + slideOutVertically()
                    ) {
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
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Error.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = "Error",
                                        tint = Error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    color = Error,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Botón de inicio de sesión
                    Button(
                        onClick = {
                            if (telefono.isBlank() || contraseña.isBlank()) {
                                errorMessage = "Completa todos los campos"
                                return@Button
                            }

                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                try {
                                    val response = ApiService.login(telefono, contraseña)

                                    if (response.isSuccessful) {
                                        val body: JsonObject? = response.body()
                                        val token = body?.get("token")?.asString
                                        val user = body?.getAsJsonObject("user")

                                        // Obtener primerInicioSesion del backend
                                        val primerInicioSesion = body?.get("primerInicioSesion")?.asBoolean ?: false

                                        if (!token.isNullOrEmpty() && user != null) {
                                            val rol = user.get("rol")?.asString ?: ""

                                            val userData = UserData(
                                                id = user.get("id")?.asString ?: "",
                                                nombre = user.get("nombre")?.asString ?: "",
                                                apellido = user.get("apellido")?.asString ?: "",
                                                cedula = user.get("cedula")?.takeIf { !it.isJsonNull }?.asString,
                                                correo = user.get("correo")?.asString ?: "",
                                                telefono = user.get("telefono")?.asString ?: "",
                                                rol = rol,
                                                fotoPerfilUrl = user.get("fotoPerfilUrl")?.takeIf { !it.isJsonNull }?.asString,
                                                estado = user.get("estado")?.asString ?: "activo"
                                            )

                                            when (rol) {
                                                "profesor", "docente" -> {
                                                    // Guardar datos y navegar
                                                    onLoginSuccess(token, userData)

                                                    if (primerInicioSesion) {
                                                        // Primer inicio: ir a selección de avatar
                                                        navController.navigate("avatar_selection/$token/${userData.fotoPerfilUrl ?: "null"}") {
                                                            popUpTo("login") { inclusive = true }
                                                        }
                                                    } else {
                                                        // Login normal: ir al screen del profesor definido en MainActivity
                                                        navController.navigate("profesor_screen") {
                                                            popUpTo("login") { inclusive = true }
                                                        }
                                                    }
                                                    isLoading = false
                                                }

                                                "padre" -> {
                                                    onLoginSuccess(token, userData)
                                                    isLoading = false
                                                }

                                                else -> {
                                                    errorMessage = "Rol no autorizado: $rol"
                                                    isLoading = false
                                                }
                                        }
                                        } else {
                                            errorMessage = "Respuesta del servidor inválida"
                                            isLoading = false
                                        }
                                    } else {
                                        errorMessage = when (response.code()) {
                                            400 -> "Teléfono o cédula inválidos"
                                            401 -> "Cédula incorrecta"
                                            404 -> "Usuario no encontrado"
                                            500 -> "Error del servidor. Intenta más tarde"
                                            else -> "Error de conexión (${response.code()})"
                                        }
                                        isLoading = false
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Sin conexión al servidor"
                                    isLoading = false
                                    e.printStackTrace()
                                }
                            }
                        },
                        enabled = telefono.isNotBlank() && contraseña.isNotBlank() && !isLoading,
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
                        if (isLoading) {
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
                                Text(
                                    "Iniciando sesión...",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Login,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Iniciar Sesión",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(AzulCielo, AzulCielo.copy(alpha = 0.6f))
                            ),
                            CircleShape
                        )
                )
                Text(
                    text = "EDUMON © 2025",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp
                    ),
                    color = GrisNeutral
                )
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(VerdeLima, VerdeLima.copy(alpha = 0.6f))
                            ),
                            CircleShape
                        )
                )
            }
        }
    }
}