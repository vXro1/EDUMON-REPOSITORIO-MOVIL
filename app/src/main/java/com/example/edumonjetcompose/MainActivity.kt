package com.example.edumonjetcompose

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.edumonjetcompose.data.UserPreferences
import com.example.edumonjetcompose.network.ApiService
import com.example.edumonjetcompose.ui.*
import com.example.edumonjetcompose.ui.screens.*
import com.example.edumonjetcompose.ui.theme.EDUMONTheme
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 🎨 Colores de la app
val AzulCielo = Color(0xFF00B9F0)
val VerdeLima = Color(0xFF7AD107)
val Fucsia = Color(0xFFFE327B)
val Celeste = Color(0xFF01C9F4)
val Naranja = Color(0xFFFA6D00)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userPrefs = UserPreferences(this)

        setContent {
            EDUMONTheme {
                val navController = rememberNavController()
                val scope = rememberCoroutineScope()
                val context = LocalContext.current

                var startDestination by remember { mutableStateOf<String?>(null) }
                var currentToken by remember { mutableStateOf<String?>(null) }
                var currentUserData by remember { mutableStateOf<UserData?>(null) }
                var currentPadreId by remember { mutableStateOf<String?>(null) }
                var isLoading by remember { mutableStateOf(true) }

                // Función para recargar datos del usuario
                suspend fun reloadUserData(token: String): UserData? {
                    return try {
                        Log.d("MainActivity", "🔄 Recargando datos del usuario con token...")

                        val response = withContext(Dispatchers.IO) {
                            ApiService.getUserProfile(token)
                        }

                        if (response.isSuccessful) {
                            val body = response.body()
                            val userObj = body?.getAsJsonObject("user") ?: body

                            if (userObj != null) {
                                val userData = UserData(
                                    id = userObj.get("id")?.asString ?: userObj.get("_id")?.asString ?: "",
                                    nombre = userObj.get("nombre")?.asString ?: "",
                                    apellido = userObj.get("apellido")?.asString ?: "",
                                    correo = userObj.get("correo")?.asString ?: "",
                                    telefono = userObj.get("telefono")?.asString ?: "",
                                    cedula = userObj.get("cedula")?.takeIf { !it.isJsonNull }?.asString,
                                    rol = userObj.get("rol")?.asString ?: "",
                                    fotoPerfilUrl = userObj.get("fotoPerfilUrl")?.takeIf { !it.isJsonNull }?.asString,
                                    estado = userObj.get("estado")?.asString ?: "activo",
                                )

                                Log.d("MainActivity", """
                                    ✅ Datos del usuario recargados:
                                    - ID: ${userData.id}
                                    - Nombre: ${userData.nombre} ${userData.apellido}
                                    - Rol: ${userData.rol}
                                    - Cédula: ${if (!userData.cedula.isNullOrBlank()) "Presente" else "Vacía"}
                                    - Avatar: ${if (!userData.fotoPerfilUrl.isNullOrBlank()) "Presente" else "Vacío"}
                                """.trimIndent())

                                // Guardar datos actualizados
                                withContext(Dispatchers.IO) {
                                    userPrefs.saveUserId(userData.id)
                                    userPrefs.savePadreId(userData.id)
                                }

                                userData
                            } else {
                                Log.e("MainActivity", "❌ userObj es null")
                                null
                            }
                        } else {
                            Log.e("MainActivity", "❌ Error al recargar perfil: ${response.code()}")
                            null
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "❌ Excepción al recargar datos", e)
                        null
                    }
                }

                // 🚀 Inicialización
                LaunchedEffect(Unit) {
                    delay(1000)
                    val token = withContext(Dispatchers.IO) { userPrefs.getToken() }
                    val padreId = withContext(Dispatchers.IO) { userPrefs.getPadreId() }
                    currentToken = token
                    currentPadreId = padreId

                    // Si hay token, cargar datos del usuario
                    if (!token.isNullOrEmpty()) {
                        currentUserData = reloadUserData(token)
                        Log.d("MainActivity", "📱 Datos cargados en inicio - UserData: ${currentUserData?.nombre}")
                    }

                    startDestination = if (!token.isNullOrEmpty()) "home" else "login"

                    Log.d("MainActivity", """
                        App iniciada:
                        - Token: ${if (!token.isNullOrEmpty()) "Presente (${token.take(20)}...)" else "Vacío"}
                        - PadreId: $padreId
                        - UserData: ${currentUserData?.nombre ?: "null"}
                        - StartDestination: $startDestination
                    """.trimIndent())

                    delay(400)
                    isLoading = false
                }

                // Pantalla de carga inicial
                AnimatedVisibility(
                    visible = isLoading,
                    exit = fadeOut(animationSpec = tween(600))
                ) {
                    ModernSplashScreen()
                }

                // Contenido principal
                AnimatedVisibility(
                    visible = !isLoading && startDestination != null,
                    enter = fadeIn(animationSpec = tween(600))
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = startDestination ?: "login",
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(350))
                        },
                        exitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { -it / 4 },
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(350))
                        },
                        popEnterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { -it / 4 },
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(350))
                        },
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(350))
                        }
                    ) {

                        // ==================== LOGIN ====================
                        composable("login") {
                            LoginScreen(
                                navController = navController,
                                onLoginSuccess = { token, userData ->
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            userPrefs.saveToken(token)
                                            userPrefs.saveUserId(userData.id)
                                            userPrefs.savePadreId(userData.id)
                                        }

                                        currentToken = token
                                        currentUserData = userData
                                        currentPadreId = userData.id

                                        Log.d("Login", """
                                            Login exitoso:
                                            - UserId: ${userData.id}
                                            - Rol: ${userData.rol}
                                            - Cédula: ${if (!userData.cedula.isNullOrBlank()) "Presente" else "Vacía"}
                                            - Avatar: ${if (!userData.fotoPerfilUrl.isNullOrBlank()) "Presente" else "Vacío"}
                                        """.trimIndent())

                                        delay(200)
                                        when {
                                            userData.cedula.isNullOrBlank() -> {
                                                Log.d("Login", "➡️ Navegando a registro (falta cédula)")
                                                val userDataJson = Gson().toJson(userData)
                                                navController.navigate("register/$token/$userDataJson") {
                                                    popUpTo("login") { inclusive = true }
                                                }
                                            }
                                            userData.fotoPerfilUrl.isNullOrBlank() -> {
                                                Log.d("Login", "➡️ Navegando a avatar (falta foto)")
                                                navController.navigate("avatar_selection/$token/null") {
                                                    popUpTo("login") { inclusive = true }
                                                }
                                            }
                                            else -> {
                                                Log.d("Login", "➡️ Navegando a home (perfil completo)")
                                                navController.navigate("home") {
                                                    popUpTo("login") { inclusive = true }
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }

                        // ==================== REGISTRO ====================
                        composable(
                            route = "register/{token}/{userDataJson}",
                            arguments = listOf(
                                navArgument("token") { type = NavType.StringType },
                                navArgument("userDataJson") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = "null"
                                }
                            )
                        ) { entry ->
                            val token = entry.arguments?.getString("token") ?: ""
                            val userDataJson = entry.arguments?.getString("userDataJson")
                            val userData = try {
                                if (!userDataJson.isNullOrBlank() && userDataJson != "null")
                                    Gson().fromJson(userDataJson, UserData::class.java)
                                else null
                            } catch (e: Exception) {
                                null
                            }

                            RegisterScreen(
                                navController = navController,
                                token = token,
                                userData = userData,
                                onRegisterSuccess = {
                                    scope.launch {
                                        delay(200)
                                        if (userData != null) {
                                            navController.navigate("avatar_selection/$token/${userData.fotoPerfilUrl ?: "null"}") {
                                                popUpTo("register/{token}/{userDataJson}") { inclusive = true }
                                            }
                                        } else {
                                            navController.navigate("login") {
                                                popUpTo("register/{token}/{userDataJson}") { inclusive = true }
                                            }
                                        }
                                    }
                                }
                            )
                        }

                        // ==================== AVATAR ====================
                        composable(
                            route = "avatar_selection/{token}/{currentAvatar}",
                            arguments = listOf(
                                navArgument("token") { type = NavType.StringType },
                                navArgument("currentAvatar") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = "null"
                                }
                            )
                        ) { entry ->
                            val token = entry.arguments?.getString("token") ?: ""
                            val currentAvatar = entry.arguments?.getString("currentAvatar")
                                ?.takeIf { it != "null" }

                            AvatarSelectionScreen(
                                navController = navController,
                                token = token,
                                currentAvatarUrl = currentAvatar,
                                onAvatarSelected = { avatarUrl ->
                                    scope.launch {
                                        Log.d("Avatar", "✅ Avatar seleccionado: $avatarUrl")

                                        // Recargar datos del usuario después de guardar
                                        val updatedUserData = reloadUserData(token)

                                        if (updatedUserData != null) {
                                            currentUserData = updatedUserData
                                            currentToken = token
                                            currentPadreId = updatedUserData.id

                                            Log.d("Avatar", "✅ Datos actualizados, navegando a home")

                                            delay(300)
                                            navController.navigate("home") {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        } else {
                                            Log.e("Avatar", "❌ Error al recargar datos")
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(
                                                    context,
                                                    "Error al actualizar perfil. Intenta nuevamente.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }
                                }
                            )
                        }

                        // ==================== HOME ====================
                        composable("home") {
                            val token = currentToken
                            var userData by remember { mutableStateOf(currentUserData) }
                            var isLoadingHome by remember { mutableStateOf(userData == null) }

                            // Recargar datos si no están disponibles
                            LaunchedEffect(Unit) {
                                Log.d("Home", """
                                    🏠 Entrando a Home:
                                    - Token presente: ${!token.isNullOrEmpty()}
                                    - UserData presente: ${userData != null}
                                    - Rol: ${userData?.rol}
                                """.trimIndent())

                                if (token.isNullOrEmpty()) {
                                    Log.e("Home", "❌ Token vacío, redirigiendo a login")
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                    return@LaunchedEffect
                                }

                                if (userData == null) {
                                    Log.d("Home", "🔄 UserData es null, recargando desde API...")
                                    isLoadingHome = true

                                    val reloadedData = reloadUserData(token)

                                    if (reloadedData != null) {
                                        userData = reloadedData
                                        currentUserData = reloadedData
                                        Log.d("Home", "✅ Datos recargados exitosamente")
                                    } else {
                                        Log.e("Home", "❌ Error al recargar datos, redirigiendo a login")
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                context,
                                                "Error al cargar perfil. Inicia sesión nuevamente.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            navController.navigate("login") {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        }
                                        return@LaunchedEffect
                                    }

                                    isLoadingHome = false
                                }
                            }

                            when {
                                isLoadingHome -> {
                                    ModernLoadingIndicator()
                                }
                                userData == null -> {
                                    // Ya se maneja en el LaunchedEffect
                                    Box(modifier = Modifier.fillMaxSize())
                                }
                                userData!!.rol.equals("profesor", true) ||
                                        userData!!.rol.equals("docente", true) -> {
                                    Log.d("Home", "👨‍🏫 Mostrando pantalla de profesor")
                                    ProfesorHomeScreen(navController = navController)
                                }
                                userData!!.rol.equals("padre", true) -> {
                                    Log.d("Home", "👨‍👩‍👧 Mostrando pantalla de padre")
                                    HomeScreenPadre(
                                        token = token ?: "",
                                        onNavigateToCurso = { cursoId ->
                                            navController.navigate("infoCurso/$cursoId")
                                        },
                                        onLogout = {
                                            scope.launch {
                                                withContext(Dispatchers.IO) {
                                                    userPrefs.clearAll()
                                                }
                                                currentToken = null
                                                currentUserData = null
                                                currentPadreId = null
                                                delay(200)
                                                navController.navigate("login") {
                                                    popUpTo(0) { inclusive = true }
                                                }
                                            }
                                        }
                                    )
                                }
                                else -> {
                                    Log.e("Home", "❌ Rol desconocido: ${userData!!.rol}")
                                    LaunchedEffect(Unit) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                context,
                                                "Rol de usuario no válido",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            navController.navigate("login") {
                                                popUpTo(0) { inclusive = true }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ==================== CURSO ====================
                        composable("infoCurso/{id}") { entry ->
                            val cursoId = entry.arguments?.getString("id") ?: ""
                            val token = currentToken ?: ""
                            InfoCursoScreen(
                                navController = navController,
                                cursoId = cursoId,
                                token = token,
                                onNavigateToTarea = { tareaId ->
                                    navController.navigate("tarea_entrega/$tareaId")
                                }
                            )
                        }

                        // ==================== PROFESOR HOME ====================
                        composable("profesor_home") {
                            ProfesorHomeScreen(navController = navController)
                        }

                        // ==================== ENTREGA DE TAREA ====================
                        composable(
                            route = "tarea_entrega/{tareaId}",
                            arguments = listOf(
                                navArgument("tareaId") { type = NavType.StringType }
                            )
                        ) { entry ->
                            val tareaId = entry.arguments?.getString("tareaId") ?: ""
                            var token by remember { mutableStateOf("") }
                            var padreId by remember { mutableStateOf("") }
                            var isLoadingData by remember { mutableStateOf(true) }

                            LaunchedEffect(Unit) {
                                token = withContext(Dispatchers.IO) { userPrefs.getToken() ?: "" }
                                padreId = withContext(Dispatchers.IO) { userPrefs.getPadreId() ?: "" }
                                delay(300)
                                isLoadingData = false
                            }

                            if (isLoadingData) ModernLoadingIndicator()
                            else TareaEntregaScreen(
                                navController = navController,
                                tareaId = tareaId,
                                token = token,
                                padreId = padreId
                            )
                        }

                        // ==================== FORO ====================
                        composable(
                            route = "foro/{cursoId}",
                            arguments = listOf(navArgument("cursoId") { type = NavType.StringType })
                        ) { entry ->
                            val cursoId = entry.arguments?.getString("cursoId") ?: ""
                            var token by remember { mutableStateOf("") }
                            var userId by remember { mutableStateOf("") }

                            LaunchedEffect(Unit) {
                                token = withContext(Dispatchers.IO) { userPrefs.getToken() ?: "" }
                                userId = withContext(Dispatchers.IO) { userPrefs.getUserId() ?: "" }
                            }

                            ForoScreen(navController, cursoId, token, userId)
                        }

                        // ==================== FORO DETALLE ====================
                        composable(
                            route = "foro_detalle/{foroId}",
                            arguments = listOf(navArgument("foroId") { type = NavType.StringType })
                        ) { entry ->
                            val foroId = entry.arguments?.getString("foroId") ?: ""
                            var token by remember { mutableStateOf("") }
                            var userId by remember { mutableStateOf("") }

                            LaunchedEffect(Unit) {
                                token = withContext(Dispatchers.IO) { userPrefs.getToken() ?: "" }
                                userId = withContext(Dispatchers.IO) { userPrefs.getUserId() ?: "" }
                            }

                            ForoDetalleScreen(navController, foroId, token, userId)
                        }

                        // ==================== CALENDARIO ====================
                        composable(
                            route = "calendario/{cursoId}",
                            arguments = listOf(navArgument("cursoId") { type = NavType.StringType })
                        ) { entry ->
                            val cursoId = entry.arguments?.getString("cursoId") ?: ""
                            var token by remember { mutableStateOf("") }

                            LaunchedEffect(Unit) {
                                token = withContext(Dispatchers.IO) { userPrefs.getToken() ?: "" }
                            }

                            CalendarioScreen(navController, cursoId, token)
                        }
                    }
                }
            }
        }
    }
}

// ==================== SPLASH Y LOADING ====================

@Composable
fun ModernSplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(40.dp)
        ) {
            Box(
                modifier = Modifier.size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .size(180.dp)
                        .scale(scale),
                    shape = CircleShape,
                    color = Celeste.copy(alpha = 0.15f)
                ) {}
                Surface(
                    modifier = Modifier.size(140.dp),
                    shape = CircleShape,
                    color = AzulCielo,
                    shadowElevation = 16.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "E",
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = "EDUMON",
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = AzulCielo,
                letterSpacing = 3.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Plataforma Educativa",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF616161),
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(56.dp))
            LinearProgressIndicator(
                modifier = Modifier.width(220.dp).height(3.dp),
                color = VerdeLima,
                trackColor = Color(0xFFF0F0F0)
            )
        }
    }
}

@Composable
fun ModernLoadingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = AzulCielo,
                strokeWidth = 5.dp,
                trackColor = Color(0xFFF0F0F0)
            )
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "Cargando",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AzulCielo
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Por favor espera un momento",
                fontSize = 14.sp,
                color = Color(0xFF757575)
            )
        }
    }
}