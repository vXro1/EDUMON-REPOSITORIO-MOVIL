package com.example.edumonjetcompose

import android.os.Bundle
import android.util.Log
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.edumonjetcompose.data.UserPreferences
import com.example.edumonjetcompose.ui.*
import com.example.edumonjetcompose.ui.screens.CalendarioScreen
import com.example.edumonjetcompose.ui.screens.HomeScreenPadre
import com.example.edumonjetcompose.ui.theme.EDUMONTheme
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Colores de la aplicación
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

                var startDestination by remember { mutableStateOf<String?>(null) }
                var currentToken by remember { mutableStateOf<String?>(null) }
                var currentUserData by remember { mutableStateOf<UserData?>(null) }
                var currentPadreId by remember { mutableStateOf<String?>(null) }
                var isLoading by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    delay(1000)
                    val token = withContext(Dispatchers.IO) { userPrefs.getToken() }
                    val padreId = withContext(Dispatchers.IO) { userPrefs.getPadreId() }
                    currentToken = token
                    currentPadreId = padreId

                    startDestination = if (!token.isNullOrEmpty()) "home" else "login"

                    Log.d("MainActivity", """
                        App iniciada:
                        - Token: ${if (!token.isNullOrEmpty()) "Presente" else "Vacío"}
                        - PadreId: $padreId
                        - StartDestination: $startDestination
                    """.trimIndent())

                    delay(400)
                    isLoading = false
                }

                AnimatedVisibility(
                    visible = isLoading,
                    exit = fadeOut(animationSpec = tween(600))
                ) {
                    ModernSplashScreen()
                }

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
                                        userPrefs.saveToken(token)
                                        userPrefs.savePadreId(userData.id)

                                        currentToken = token
                                        currentUserData = userData
                                        currentPadreId = userData.id

                                        Log.d("Login", "Login exitoso - UserId: ${userData.id}")

                                        delay(200)
                                        when {
                                            userData.cedula.isNullOrBlank() -> {
                                                val userDataJson = Gson().toJson(userData)
                                                navController.navigate("register/$token/$userDataJson") {
                                                    popUpTo("login") { inclusive = true }
                                                }
                                            }
                                            userData.fotoPerfilUrl.isNullOrBlank() -> {
                                                navController.navigate("avatar_selection/$token/null") {
                                                    popUpTo("login") { inclusive = true }
                                                }
                                            }
                                            else -> {
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
                        ) { backStackEntry ->
                            val token = backStackEntry.arguments?.getString("token") ?: ""
                            val userDataJson = backStackEntry.arguments?.getString("userDataJson")
                            val userData = if (userDataJson != null && userDataJson != "null") {
                                try {
                                    Gson().fromJson(userDataJson, UserData::class.java)
                                } catch (e: Exception) {
                                    null
                                }
                            } else null

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
                        ) { backStackEntry ->
                            val token = backStackEntry.arguments?.getString("token") ?: ""
                            val currentAvatar = backStackEntry.arguments?.getString("currentAvatar")
                                ?.takeIf { it != "null" }

                            AvatarSelectionScreen(
                                navController = navController,
                                token = token,
                                currentAvatarUrl = currentAvatar,
                                onAvatarSelected = {
                                    scope.launch {
                                        delay(200)
                                        navController.navigate("home") {
                                            popUpTo("avatar_selection/{token}/{currentAvatar}") { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }

                        // ==================== HOME ====================
                        composable("home") {
                            val token = currentToken ?: ""
                            HomeScreenPadre(
                                token = token,
                                onNavigateToCurso = { cursoId ->
                                    navController.navigate("infoCurso/$cursoId")
                                },
                                onLogout = {
                                    scope.launch {
                                        userPrefs.clearToken()
                                        userPrefs.clearPadreId()
                                        currentToken = null
                                        currentPadreId = null
                                        delay(200)
                                        navController.navigate("login") {
                                            popUpTo("home") { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }

                        // ==================== CURSO ====================
                        composable("infoCurso/{id}") { backStackEntry ->
                            val cursoId = backStackEntry.arguments?.getString("id") ?: ""
                            val token = currentToken ?: ""
                            InfoCursoScreen(
                                navController = navController,
                                cursoId = cursoId,
                                token = token,
                                onNavigateToTarea = { tareaId ->
                                    Log.d("Navigation", "Navegando a tarea: $tareaId")
                                    navController.navigate("tarea_entrega/$tareaId")
                                }
                            )
                        }

                        // ==================== ENTREGA DE TAREA ====================
                        composable(
                            route = "tarea_entrega/{tareaId}",
                            arguments = listOf(
                                navArgument("tareaId") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val tareaId = backStackEntry.arguments?.getString("tareaId") ?: ""

                            var token by remember { mutableStateOf("") }
                            var padreId by remember { mutableStateOf("") }
                            var isLoadingData by remember { mutableStateOf(true) }

                            LaunchedEffect(Unit) {
                                token = withContext(Dispatchers.IO) {
                                    userPrefs.getToken() ?: ""
                                }
                                padreId = withContext(Dispatchers.IO) {
                                    userPrefs.getPadreId() ?: ""
                                }

                                Log.d("TareaEntregaScreen", "tareaId: $tareaId, padreId: $padreId")

                                delay(300)
                                isLoadingData = false
                            }

                            AnimatedVisibility(
                                visible = isLoadingData,
                                exit = fadeOut(animationSpec = tween(300))
                            ) {
                                ModernLoadingIndicator()
                            }

                            AnimatedVisibility(
                                visible = !isLoadingData && token.isNotEmpty() && padreId.isNotEmpty(),
                                enter = fadeIn(animationSpec = tween(300))
                            ) {
                                TareaEntregaScreen(
                                    navController = navController,
                                    tareaId = tareaId,
                                    token = token,
                                    padreId = padreId
                                )
                            }
                        }

                        // ==================== FORO (LISTA) ====================
                        composable(
                            route = "foro/{cursoId}",
                            arguments = listOf(navArgument("cursoId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val cursoId = backStackEntry.arguments?.getString("cursoId") ?: ""

                            var token by remember { mutableStateOf("") }
                            var userId by remember { mutableStateOf("") }
                            var isLoadingData by remember { mutableStateOf(true) }

                            LaunchedEffect(Unit) {
                                token = withContext(Dispatchers.IO) {
                                    userPrefs.getToken() ?: ""
                                }
                                userId = withContext(Dispatchers.IO) {
                                    userPrefs.getUserId() ?: ""
                                }
                                delay(200)
                                isLoadingData = false
                            }

                            AnimatedVisibility(
                                visible = isLoadingData,
                                exit = fadeOut(animationSpec = tween(300))
                            ) {
                                ModernLoadingIndicator()
                            }

                            AnimatedVisibility(
                                visible = !isLoadingData && token.isNotEmpty(),
                                enter = fadeIn(animationSpec = tween(300))
                            ) {
                                ForoScreen(
                                    navController = navController,
                                    cursoId = cursoId,
                                    token = token,
                                    userId = userId
                                )
                            }
                        }

                        // ==================== FORO DETALLE ====================
                        composable(
                            route = "foro_detalle/{foroId}",
                            arguments = listOf(navArgument("foroId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val foroId = backStackEntry.arguments?.getString("foroId") ?: ""

                            var token by remember { mutableStateOf("") }
                            var userId by remember { mutableStateOf("") }
                            var isLoadingData by remember { mutableStateOf(true) }

                            LaunchedEffect(Unit) {
                                token = withContext(Dispatchers.IO) {
                                    userPrefs.getToken() ?: ""
                                }
                                userId = withContext(Dispatchers.IO) {
                                    userPrefs.getUserId() ?: ""
                                }
                                delay(200)
                                isLoadingData = false
                            }

                            AnimatedVisibility(
                                visible = isLoadingData,
                                exit = fadeOut(animationSpec = tween(300))
                            ) {
                                ModernLoadingIndicator()
                            }

                            AnimatedVisibility(
                                visible = !isLoadingData && token.isNotEmpty(),
                                enter = fadeIn(animationSpec = tween(300))
                            ) {
                                ForoDetalleScreen(
                                    navController = navController,
                                    foroId = foroId,
                                    token = token,
                                    userId = userId
                                )
                            }
                        }

                        // ==================== CALENDARIO ====================
                        composable(
                            route = "calendario/{cursoId}",
                            arguments = listOf(navArgument("cursoId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val cursoId = backStackEntry.arguments?.getString("cursoId") ?: ""

                            var token by remember { mutableStateOf("") }
                            var isLoadingData by remember { mutableStateOf(true) }

                            LaunchedEffect(Unit) {
                                token = withContext(Dispatchers.IO) {
                                    userPrefs.getToken() ?: ""
                                }
                                delay(200)
                                isLoadingData = false
                            }

                            AnimatedVisibility(
                                visible = isLoadingData,
                                exit = fadeOut(animationSpec = tween(300))
                            ) {
                                ModernLoadingIndicator()
                            }

                            AnimatedVisibility(
                                visible = !isLoadingData && token.isNotEmpty(),
                                enter = fadeIn(animationSpec = tween(300))
                            ) {
                                CalendarioScreen(
                                    navController = navController,
                                    cursoId = cursoId,
                                    token = token
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

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
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
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
                modifier = Modifier
                    .width(220.dp)
                    .height(3.dp),
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
                fontWeight = FontWeight.Normal,
                color = Color(0xFF757575)
            )
        }
    }
}