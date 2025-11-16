package com.example.edumonjetcompose.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.edumonjetcompose.R
import com.example.edumonjetcompose.models.UserData
import com.example.edumonjetcompose.network.ApiService
import com.example.edumonjetcompose.ui.theme.*
import kotlinx.coroutines.launch
import android.util.Log

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
    val scrollState = rememberScrollState()

    // Obtener dimensiones de la pantalla
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    val isSmallDevice = screenHeight < 700.dp

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
        // Burbujas de fondo - Ajustadas para dispositivos pequeños
        Box(modifier = Modifier.size(if (isSmallDevice) 200.dp else 280.dp).offset(x = screenWidth * 0.7f, y = (-50).dp + bubble1.dp).scale(scale)
            .background(Brush.radialGradient(colors = listOf(AzulCielo.copy(alpha = 0.15f), AzulCielo.copy(alpha = 0.05f))), CircleShape))
        Box(modifier = Modifier.size(if (isSmallDevice) 150.dp else 200.dp).offset(x = (-40).dp, y = 40.dp + bubble2.dp)
            .background(Brush.radialGradient(colors = listOf(VerdeLima.copy(alpha = 0.18f), VerdeLima.copy(alpha = 0.06f))), CircleShape))
        Box(modifier = Modifier.size(if (isSmallDevice) 120.dp else 160.dp).offset(x = screenWidth * 0.75f, y = 200.dp + bubble3.dp)
            .background(Brush.radialGradient(colors = listOf(Fucsia.copy(alpha = 0.12f), Fucsia.copy(alpha = 0.04f))), CircleShape))
        Box(modifier = Modifier.size(if (isSmallDevice) 90.dp else 120.dp).offset(x = 40.dp, y = 340.dp + bubble4.dp).background(Naranja.copy(alpha = 0.12f), CircleShape))
        Box(modifier = Modifier.size(if (isSmallDevice) 100.dp else 140.dp).offset(x = (-40).dp, y = 500.dp + bubble1.dp)
            .background(Brush.radialGradient(colors = listOf(AzulCielo.copy(alpha = 0.14f), AzulCielo.copy(alpha = 0.05f))), CircleShape))
        Box(modifier = Modifier.size(if (isSmallDevice) 80.dp else 110.dp).offset(x = screenWidth * 0.7f, y = 600.dp + bubble2.dp).background(VerdeLima.copy(alpha = 0.1f), CircleShape))
        Box(modifier = Modifier.size(if (isSmallDevice) 60.dp else 80.dp).offset(x = screenWidth * 0.4f, y = 480.dp + bubble3.dp).background(Fucsia.copy(alpha = 0.09f), CircleShape))
        Box(modifier = Modifier.size(if (isSmallDevice) 70.dp else 90.dp).offset(x = 60.dp, y = 160.dp + bubble4.dp).background(Naranja.copy(alpha = 0.11f), CircleShape))

        // Contenido principal con scroll
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .padding(top = if (isSmallDevice) 16.dp else 32.dp, bottom = 24.dp)
                .imePadding(), // Ajuste automático cuando aparece el teclado
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (isSmallDevice) 12.dp else 16.dp)
        ) {
            Spacer(modifier = Modifier.height(if (isSmallDevice) 8.dp else 16.dp))

            // Logo
            Box(
                modifier = Modifier
                    .size(if (isSmallDevice) 90.dp else 120.dp)
                    .scale(scale)
                    .shadow(
                        elevation = 20.dp,
                        shape = CircleShape,
                        spotColor = AzulCielo.copy(alpha = 0.4f),
                        ambientColor = VerdeLima.copy(alpha = 0.2f)
                    )
                    .background(Color.White, CircleShape)
                    .padding(6.dp)
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
                        .size(if (isSmallDevice) 78.dp else 104.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Text(
                text = "EDUMON",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (isSmallDevice) 36.sp else 46.sp,
                    letterSpacing = 2.sp,
                    brush = Brush.horizontalGradient(colors = listOf(AzulCielo, VerdeLima))
                )
            )

            Text(
                text = "Educación y Monitoreo",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = if (isSmallDevice) 13.sp else 15.sp,
                    letterSpacing = 0.8.sp
                ),
                color = GrisNeutral
            )

            // Indicadores coloridos
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(30.dp)
                        .height(5.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(AzulCielo, AzulCielo.copy(alpha = 0.6f))
                            ),
                            RoundedCornerShape(3.dp)
                        )
                )
                Box(
                    modifier = Modifier
                        .width(30.dp)
                        .height(5.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(VerdeLima, VerdeLima.copy(alpha = 0.6f))
                            ),
                            RoundedCornerShape(3.dp)
                        )
                )
                Box(
                    modifier = Modifier
                        .width(30.dp)
                        .height(5.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Fucsia, Fucsia.copy(alpha = 0.6f))
                            ),
                            RoundedCornerShape(3.dp)
                        )
                )
            }

            Spacer(modifier = Modifier.height(if (isSmallDevice) 8.dp else 16.dp))

            // Card del formulario
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = if (isSmallDevice) 24.dp else 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isSmallDevice) 60.dp else 70.dp)
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
                            modifier = Modifier.size(if (isSmallDevice) 28.dp else 34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(if (isSmallDevice) 12.dp else 16.dp))

                    Text(
                        text = "¡Bienvenido!",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isSmallDevice) 24.sp else 28.sp
                        ),
                        color = FondoOscuroPrimario
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Ingresa para continuar",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = if (isSmallDevice) 13.sp else 15.sp),
                        color = GrisNeutral
                    )

                    Spacer(modifier = Modifier.height(if (isSmallDevice) 16.dp else 20.dp))

                    // Campo Teléfono
                    OutlinedTextField(
                        value = telefono,
                        onValueChange = {
                            val filtered = it.filter { char -> char.isDigit() || char == '+' }
                            telefono = filtered
                            errorMessage = null
                        },
                        label = { Text("Número de teléfono", fontSize = if (isSmallDevice) 13.sp else 14.sp) },
                        placeholder = { Text("3001234567 o +573001234567", fontSize = if (isSmallDevice) 12.sp else 13.sp) },
                        supportingText = {
                            Text(
                                "Puedes usar el número con o sin +57",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = if (isSmallDevice) 11.sp else 12.sp),
                                color = GrisNeutral.copy(alpha = 0.7f)
                            )
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(if (isSmallDevice) 38.dp else 42.dp)
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
                                    modifier = Modifier.size(if (isSmallDevice) 20.dp else 22.dp)
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
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AzulCielo,
                            unfocusedBorderColor = GrisClaro.copy(alpha = 0.6f),
                            focusedLabelColor = AzulCielo,
                            cursorColor = AzulCielo,
                            focusedContainerColor = AzulCielo.copy(alpha = 0.04f),
                            unfocusedContainerColor = GrisExtraClaro.copy(alpha = 0.6f)
                        )
                    )

                    Spacer(modifier = Modifier.height(if (isSmallDevice) 12.dp else 16.dp))

                    // Campo Contraseña
                    OutlinedTextField(
                        value = contraseña,
                        onValueChange = { contraseña = it; errorMessage = null },
                        label = { Text("Contraseña", fontSize = if (isSmallDevice) 13.sp else 14.sp) },
                        placeholder = { Text("Ingresa tu contraseña", fontSize = if (isSmallDevice) 12.sp else 13.sp) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(if (isSmallDevice) 38.dp else 42.dp)
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
                                    modifier = Modifier.size(if (isSmallDevice) 20.dp else 22.dp)
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
                                    modifier = Modifier.size(if (isSmallDevice) 22.dp else 24.dp)
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VerdeLima,
                            unfocusedBorderColor = GrisClaro.copy(alpha = 0.6f),
                            focusedLabelColor = VerdeLima,
                            cursorColor = VerdeLima,
                            focusedContainerColor = VerdeLima.copy(alpha = 0.04f),
                            unfocusedContainerColor = GrisExtraClaro.copy(alpha = 0.6f)
                        )
                    )

                    Spacer(modifier = Modifier.height(if (isSmallDevice) 12.dp else 14.dp))

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
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(if (isSmallDevice) 32.dp else 36.dp)
                                        .background(Error.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = "Error",
                                        tint = Error,
                                        modifier = Modifier.size(if (isSmallDevice) 18.dp else 20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    color = Error,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = if (isSmallDevice) 13.sp else 14.sp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(if (isSmallDevice) 16.dp else 20.dp))

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
                                    val telefonoLimpio = telefono.trim()

                                    Log.d("LoginScreen", "🔐 Iniciando login")
                                    Log.d("LoginScreen", "Teléfono original: $telefono")
                                    Log.d("LoginScreen", "Teléfono limpio: $telefonoLimpio")

                                    val response = ApiService.login(telefonoLimpio, contraseña)

                                    if (response.isSuccessful) {
                                        val body = response.body()
                                        Log.d("LoginScreen", "✅ Respuesta exitosa del servidor")
                                        Log.d("LoginScreen", "Body completo: $body")

                                        val token = body?.get("token")?.asString
                                        val userObject = body?.getAsJsonObject("user")
                                        val primerInicioSesion = body?.get("primerInicioSesion")?.asBoolean ?: false

                                        Log.d("LoginScreen", "Token: $token")
                                        Log.d("LoginScreen", "User Object: $userObject")
                                        Log.d("LoginScreen", "Primer inicio: $primerInicioSesion")

                                        if (!token.isNullOrEmpty() && userObject != null) {
                                            val rol = userObject.get("rol")?.asString ?: ""
                                            Log.d("LoginScreen", "Rol del usuario: $rol")

                                            val userData = UserData(
                                                id = userObject.get("id")?.asString ?: userObject.get("_id")?.asString ?: "",
                                                nombre = userObject.get("nombre")?.asString ?: "",
                                                apellido = userObject.get("apellido")?.asString ?: "",
                                                cedula = userObject.get("cedula")?.takeIf { !it.isJsonNull }?.asString,
                                                correo = userObject.get("correo")?.asString ?: "",
                                                telefono = userObject.get("telefono")?.asString ?: "",
                                                rol = rol,
                                                fotoPerfilUrl = userObject.get("fotoPerfilUrl")?.takeIf { !it.isJsonNull }?.asString,
                                                estado = userObject.get("estado")?.asString ?: "activo"
                                            )

                                            Log.d("LoginScreen", "UserData creado: $userData")

                                            onLoginSuccess(token, userData)
                                            Log.d("LoginScreen", "✅ Datos guardados exitosamente")

                                            when (rol.lowercase()) {
                                                "profesor", "docente" -> {
                                                    if (primerInicioSesion) {
                                                        Log.d("LoginScreen", "📍 Navegando a avatar_selection")
                                                        navController.navigate("avatar_selection/$token/${userData.fotoPerfilUrl ?: "null"}") {
                                                            popUpTo("login") { inclusive = true }
                                                        }
                                                    } else {
                                                        Log.d("LoginScreen", "📍 Navegando a profesor_screen")
                                                        navController.navigate("profesor_screen") {
                                                            popUpTo("login") { inclusive = true }
                                                        }
                                                    }
                                                }
                                                "padre" -> {
                                                    if (primerInicioSesion) {
                                                        Log.d("LoginScreen", "📍 Navegando a avatar_selection (padre)")
                                                        navController.navigate("avatar_selection/$token/${userData.fotoPerfilUrl ?: "null"}") {
                                                            popUpTo("login") { inclusive = true }
                                                        }
                                                    } else {
                                                        Log.d("LoginScreen", "📍 Navegando a padre_screen")
                                                        navController.navigate("padre_screen") {
                                                            popUpTo("login") { inclusive = true }
                                                        }
                                                    }
                                                }
                                                else -> {
                                                    errorMessage = "Rol no autorizado: $rol"
                                                    Log.e("LoginScreen", "❌ Rol no reconocido: $rol")
                                                }
                                            }
                                        } else {
                                            errorMessage = "Respuesta del servidor inválida"
                                            Log.e("LoginScreen", "❌ Token o user object nulos")
                                        }
                                    } else {
                                        val errorBody = response.errorBody()?.string()
                                        Log.e("LoginScreen", "❌ Error ${response.code()}: $errorBody")

                                        errorMessage = when (response.code()) {
                                            400 -> "Datos inválidos"
                                            401 -> "Contraseña incorrecta"
                                            404 -> "Usuario no encontrado"
                                            500 -> "Error del servidor. Intenta más tarde"
                                            else -> "Error de conexión (${response.code()})"
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("LoginScreen", "❌ Excepción en login", e)
                                    errorMessage = "Sin conexión al servidor"
                                    e.printStackTrace()
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        enabled = telefono.isNotBlank() && contraseña.isNotBlank() && !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isSmallDevice) 54.dp else 58.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AzulCielo,
                            disabledContainerColor = GrisClaro.copy(alpha = 0.4f)
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
                                    color = Color.White,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(if (isSmallDevice) 22.dp else 24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Iniciando sesión...",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (isSmallDevice) 15.sp else 17.sp
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
                                    modifier = Modifier.size(if (isSmallDevice) 22.dp else 24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "Iniciar Sesión",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (isSmallDevice) 16.sp else 18.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isSmallDevice) 12.dp else 16.dp))

            // Footer
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
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
                        fontSize = if (isSmallDevice) 11.sp else 12.sp,
                        letterSpacing = 1.sp
                    ),
                    color = GrisNeutral
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(VerdeLima, VerdeLima.copy(alpha = 0.6f))
                            ),
                            CircleShape
                        )
                )
            }

            // Espaciado final para asegurar que todo es visible
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}