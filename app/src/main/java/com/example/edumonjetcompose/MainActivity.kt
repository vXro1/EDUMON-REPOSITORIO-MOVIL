package com.example.edumonjetcompose

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.edumonjetcompose.data.UserPreferences
import com.example.edumonjetcompose.models.UserData
import com.example.edumonjetcompose.network.ApiService
import com.example.edumonjetcompose.screens.profesor.InfoCursoScreenProfesor
import com.example.edumonjetcompose.screens.profesor.ParticipantesCursoScreen
import com.example.edumonjetcompose.ui.*
import com.example.edumonjetcompose.ui.screens.*
import com.example.edumonjetcompose.ui.theme.AzulCielo
import com.example.edumonjetcompose.ui.theme.Blanco
import com.example.edumonjetcompose.ui.theme.Cyan
import com.example.edumonjetcompose.ui.theme.EDUMONTheme
import com.example.edumonjetcompose.ui.theme.ErrorOscuro
import com.example.edumonjetcompose.ui.theme.FondoClaro
import com.example.edumonjetcompose.ui.theme.GrisNeutral
import com.example.edumonjetcompose.ui.theme.Morado
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

// 🎨 Colores de la app
val AzulCielo = Color(0xFF00B9F0)
val VerdeLima = Color(0xFF7AD107)
val Fucsia = Color(0xFFFE327B)
val Celeste = Color(0xFF01C9F4)
val Naranja = Color(0xFFFA6D00)

class MainActivity : ComponentActivity() {
    @Composable
    fun AppNavigation(
        navController: NavHostController,
        startDestination: String,
        userPrefs: UserPreferences,
        currentToken: String?,
        currentUserData: UserData?,
        currentPadreId: String?,
        onTokenUpdate: (String?) -> Unit,
        onUserDataUpdate: (UserData?) -> Unit,
        onPadreIdUpdate: (String?) -> Unit,
        reloadUserData: suspend (String) -> UserData?
    ) {
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        var token by remember { mutableStateOf(currentToken) }
        var userData by remember { mutableStateOf(currentUserData) }
        var padreId by remember { mutableStateOf(currentPadreId) }

        NavHost(
            navController = navController,
            startDestination = startDestination,
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

            // ═══════════════════════════════════════════════════════════════
            // 🔐 AUTENTICACIÓN
            // ═══════════════════════════════════════════════════════════════

            composable("login") {
                LoginScreen(
                    navController = navController,
                    onLoginSuccess = { newToken, newUserData ->
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                userPrefs.saveToken(newToken)
                                userPrefs.saveUserId(newUserData.id)
                                userPrefs.savePadreId(newUserData.id)
                            }

                            token = newToken
                            userData = newUserData
                            padreId = newUserData.id

                            onTokenUpdate(newToken)
                            onUserDataUpdate(newUserData)
                            onPadreIdUpdate(newUserData.id)

                            delay(200)
                            when {
                                newUserData.cedula.isNullOrBlank() -> {
                                    val userDataJson = Gson().toJson(newUserData)
                                    navController.navigate("register/$newToken/$userDataJson") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                                newUserData.fotoPerfilUrl.isNullOrBlank() -> {
                                    navController.navigate("avatar_selection/$newToken/null") {
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
                val routeToken = entry.arguments?.getString("token") ?: ""
                val userDataJson = entry.arguments?.getString("userDataJson")
                val routeUserData = try {
                    if (!userDataJson.isNullOrBlank() && userDataJson != "null")
                        Gson().fromJson(userDataJson, UserData::class.java)
                    else null
                } catch (e: Exception) {
                    null
                }

                RegisterScreen(
                    navController = navController,
                    token = routeToken,
                    userData = routeUserData,
                    onRegisterSuccess = {
                        scope.launch {
                            delay(200)
                            if (routeUserData != null) {
                                navController.navigate("avatar_selection/$routeToken/${routeUserData.fotoPerfilUrl ?: "null"}") {
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
                val routeToken = entry.arguments?.getString("token") ?: ""
                val currentAvatar = entry.arguments?.getString("currentAvatar")?.takeIf { it != "null" }

                AvatarSelectionScreen(
                    navController = navController,
                    token = routeToken,
                    currentAvatarUrl = currentAvatar,
                    onAvatarSelected = { avatarUrl ->
                        scope.launch {
                            val updatedUserData = reloadUserData(routeToken)

                            if (updatedUserData != null) {
                                userData = updatedUserData
                                token = routeToken
                                padreId = updatedUserData.id

                                onUserDataUpdate(updatedUserData)
                                onTokenUpdate(routeToken)
                                onPadreIdUpdate(updatedUserData.id)

                                delay(300)
                                navController.navigate("home") {
                                    popUpTo(0) { inclusive = true }
                                }
                            } else {
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

            // ═══════════════════════════════════════════════════════════════
            // 🏠 HOME
            // ═══════════════════════════════════════════════════════════════

            composable("home") {
                var homeUserData by remember { mutableStateOf(userData) }
                var isLoadingHome by remember { mutableStateOf(homeUserData == null) }

                LaunchedEffect(Unit) {
                    if (token.isNullOrEmpty()) {
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                        return@LaunchedEffect
                    }

                    if (homeUserData == null) {
                        isLoadingHome = true
                        val reloadedData = reloadUserData(token!!)

                        if (reloadedData != null) {
                            homeUserData = reloadedData
                            userData = reloadedData
                            onUserDataUpdate(reloadedData)
                        } else {
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
                    homeUserData == null -> {
                        Box(modifier = Modifier.fillMaxSize())
                    }
                    homeUserData!!.rol.equals("profesor", true) ||
                            homeUserData!!.rol.equals("docente", true) -> {
                        ProfesorHomeScreen(navController = navController)
                    }
                    homeUserData!!.rol.equals("padre", true) -> {
                        HomeScreenPadre(
                            navController = navController,
                            token = token ?: "",
                            onLogout = {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        userPrefs.clearAll()
                                    }
                                    token = null
                                    userData = null
                                    padreId = null
                                    onTokenUpdate(null)
                                    onUserDataUpdate(null)
                                    onPadreIdUpdate(null)
                                    delay(200)
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }
                    else -> {
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

            // ═══════════════════════════════════════════════════════════════
            // 📚 CURSOS
            // ═══════════════════════════════════════════════════════════════

            composable(
                route = "infoCurso/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { entry ->
                val cursoId = entry.arguments?.getString("id") ?: ""
                var localToken by remember { mutableStateOf("") }
                var isLoadingToken by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    localToken = withContext(Dispatchers.IO) {
                        userPrefs.getToken() ?: token ?: ""
                    }
                    delay(100)
                    isLoadingToken = false
                }

                if (isLoadingToken) {
                    ModernLoadingIndicator()
                } else {
                    InfoCursoScreen(
                        navController = navController,
                        cursoId = cursoId,
                        token = localToken,
                        onNavigateToTarea = { tareaId ->
                            navController.navigate("tarea_detalle/$tareaId/${padreId ?: ""}")
                        }
                    )
                }
            }

            composable(
                route = "infoCursoProfesor/{cursoId}",
                arguments = listOf(
                    navArgument("cursoId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val cursoId = backStackEntry.arguments?.getString("cursoId") ?: ""

                var localToken by remember { mutableStateOf("") }
                var isLoadingToken by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    localToken = withContext(Dispatchers.IO) {
                        userPrefs.getToken() ?: token ?: ""
                    }
                    Log.d("InfoCursoNav", "Token cargado para curso: ${localToken.take(20)}...")
                    delay(100)
                    isLoadingToken = false
                }

                if (isLoadingToken) {
                    ModernLoadingIndicator()
                } else {
                    InfoCursoScreenProfesor(
                        navController = navController,
                        cursoId = cursoId,
                        token = localToken
                    )
                }
            }

            composable(
                route = "participantesCurso/{cursoId}",
                arguments = listOf(
                    navArgument("cursoId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val cursoId = backStackEntry.arguments?.getString("cursoId") ?: ""
                var localToken by remember { mutableStateOf("") }
                var isLoadingToken by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    localToken = withContext(Dispatchers.IO) {
                        userPrefs.getToken() ?: token ?: ""
                    }
                    delay(100)
                    isLoadingToken = false
                }

                if (isLoadingToken) {
                    ModernLoadingIndicator()
                } else {
                    ParticipantesCursoScreen(
                        navController = navController,
                        cursoId = cursoId,
                        token = localToken
                    )
                }
            }

            // ═══════════════════════════════════════════════════════════════
            // 📦 MÓDULOS
            // ═══════════════════════════════════════════════════════════════

            composable(
                route = "modulosCurso/{cursoId}",
                arguments = listOf(
                    navArgument("cursoId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val cursoId = backStackEntry.arguments?.getString("cursoId") ?: ""
                var localToken by remember { mutableStateOf("") }
                var isLoadingToken by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    localToken = withContext(Dispatchers.IO) {
                        userPrefs.getToken() ?: token ?: ""
                    }
                    delay(100)
                    isLoadingToken = false
                }

                if (isLoadingToken) {
                    ModernLoadingIndicator()
                } else {
                    ListarModulosScreen(
                        navController = navController,
                        cursoId = cursoId,
                        token = localToken
                    )
                }
            }

            composable(
                route = "crearModulo/{cursoId}",
                arguments = listOf(
                    navArgument("cursoId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val cursoId = backStackEntry.arguments?.getString("cursoId") ?: ""
                var localToken by remember { mutableStateOf("") }
                var isLoadingToken by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    localToken = withContext(Dispatchers.IO) {
                        userPrefs.getToken() ?: token ?: ""
                    }
                    delay(100)
                    isLoadingToken = false
                }

                if (isLoadingToken) {
                    ModernLoadingIndicator()
                } else {
                    CrearModuloScreen(
                        navController = navController,
                        cursoId = cursoId,
                        token = localToken
                    )
                }
            }

            // ═══════════════════════════════════════════════════════════════
            // 📝 TAREAS
            // ═══════════════════════════════════════════════════════════════

            composable(
                route = "tarea_detalle/{tareaId}/{padreId}",
                arguments = listOf(
                    navArgument("tareaId") { type = NavType.StringType },
                    navArgument("padreId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                var localToken by remember { mutableStateOf("") }
                var localPadreId by remember { mutableStateOf("") }
                var isLoadingData by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    localToken = withContext(Dispatchers.IO) {
                        userPrefs.getToken() ?: token ?: ""
                    }
                    localPadreId = withContext(Dispatchers.IO) {
                        userPrefs.getPadreId() ?: padreId ?: backStackEntry.arguments?.getString("padreId") ?: ""
                    }
                    delay(100)
                    isLoadingData = false
                }

                if (isLoadingData) {
                    ModernLoadingIndicator()
                } else {
                    TareaDetalleScreen(
                        navController = navController,
                        tareaId = backStackEntry.arguments?.getString("tareaId") ?: "",
                        token = localToken,
                        padreId = localPadreId
                    )
                }
            }

            composable(
                route = "detalleTareaProfesor/{tareaId}",
                arguments = listOf(
                    navArgument("tareaId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val tareaId = backStackEntry.arguments?.getString("tareaId") ?: ""
                var localToken by remember { mutableStateOf("") }
                var isLoadingToken by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    localToken = withContext(Dispatchers.IO) {
                        userPrefs.getToken() ?: token ?: ""
                    }
                    delay(100)
                    isLoadingToken = false
                }

                if (isLoadingToken) {
                    ModernLoadingIndicator()
                } else {
                    DetalleTareaProfesorScreen(
                        navController = navController,
                        tareaId = tareaId,
                        token = localToken
                    )
                }
            }

            composable(
                route = "crearTarea/{cursoId}",
                arguments = listOf(
                    navArgument("cursoId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val cursoId = backStackEntry.arguments?.getString("cursoId") ?: ""
                var localToken by remember { mutableStateOf("") }
                var isLoadingToken by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    localToken = withContext(Dispatchers.IO) {
                        userPrefs.getToken() ?: token ?: ""
                    }
                    delay(100)
                    isLoadingToken = false
                }

                if (isLoadingToken) {
                    ModernLoadingIndicator()
                } else {
                    CrearTareaScreen(
                        navController = navController,
                        cursoId = cursoId,
                        token = localToken
                    )
                }
            }

            composable(
                route = "tarea_entrega/{tareaId}/{padreId}",
                arguments = listOf(
                    navArgument("tareaId") { type = NavType.StringType },
                    navArgument("padreId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                var localToken by remember { mutableStateOf("") }
                var localPadreId by remember { mutableStateOf("") }
                var isLoadingData by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    localToken = withContext(Dispatchers.IO) {
                        userPrefs.getToken() ?: token ?: ""
                    }
                    localPadreId = withContext(Dispatchers.IO) {
                        userPrefs.getPadreId() ?: padreId ?: backStackEntry.arguments?.getString("padreId") ?: ""
                    }
                    delay(100)
                    isLoadingData = false
                }

                if (isLoadingData) {
                    ModernLoadingIndicator()
                } else {
                    HacerEntregaScreen(
                        navController = navController,
                        tareaId = backStackEntry.arguments?.getString("tareaId") ?: "",
                        padreId = localPadreId,
                        token = localToken
                    )
                }
            }

            composable(
                route = "tarea_entrega/{tareaId}",
                arguments = listOf(navArgument("tareaId") { type = NavType.StringType })
            ) { entry ->
                val tareaId = entry.arguments?.getString("tareaId") ?: ""
                var localToken by remember { mutableStateOf("") }
                var localPadreId by remember { mutableStateOf("") }
                var isLoadingData by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    localToken = withContext(Dispatchers.IO) {
                        userPrefs.getToken() ?: token ?: ""
                    }
                    localPadreId = withContext(Dispatchers.IO) {
                        userPrefs.getPadreId() ?: padreId ?: ""
                    }
                    delay(100)
                    isLoadingData = false
                }

                if (isLoadingData) {
                    ModernLoadingIndicator()
                } else {
                    HacerEntregaScreen(
                        navController = navController,
                        tareaId = tareaId,
                        token = localToken,
                        padreId = localPadreId
                    )
                }
            }
            composable(
                route = "detalleEntrega/{entregaId}",
                arguments = listOf(
                    navArgument("entregaId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val entregaId = backStackEntry.arguments?.getString("entregaId") ?: ""
                var localToken by remember { mutableStateOf("") }
                var isLoadingToken by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    localToken = withContext(Dispatchers.IO) {
                        userPrefs.getToken() ?: token ?: ""
                    }
                    delay(100)
                    isLoadingToken = false
                }

                if (isLoadingToken) {
                    ModernLoadingIndicator()
                } else {
                    // REEMPLAZA 'DetalleEntregaScreen' con el nombre real de tu pantalla
                    DetalleEntregaProfesorScreen(
                        navController = navController,
                        entregaId = entregaId,
                        token = localToken
                    )
                }
            }
            // ═══════════════════════════════════════════════════════════════
            // 💬 FOROS
            // ═══════════════════════════════════════════════════════════════

            composable(
                route = "foro/{cursoId}",
                arguments = listOf(navArgument("cursoId") { type = NavType.StringType })
            ) { entry ->
                val cursoId = entry.arguments?.getString("cursoId") ?: ""
                var localToken by remember { mutableStateOf("") }
                var userId by remember { mutableStateOf("") }
                var isLoadingData by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    localToken = withContext(Dispatchers.IO) {
                        userPrefs.getToken() ?: token ?: ""
                    }
                    userId = withContext(Dispatchers.IO) {
                        userPrefs.getUserId() ?: userData?.id ?: ""
                    }
                    delay(100)
                    isLoadingData = false
                }

                if (isLoadingData) {
                    ModernLoadingIndicator()
                } else {
                    ForoScreen(navController, cursoId, localToken, userId)
                }
            }

            composable(
                route = "foro_detalle/{foroId}",
                arguments = listOf(navArgument("foroId") { type = NavType.StringType })
            ) { entry ->
                val foroId = entry.arguments?.getString("foroId") ?: ""
                var localToken by remember { mutableStateOf("") }
                var userId by remember { mutableStateOf("") }
                var isLoadingData by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    localToken = withContext(Dispatchers.IO) {
                        userPrefs.getToken() ?: token ?: ""
                    }
                    userId = withContext(Dispatchers.IO) {
                        userPrefs.getUserId() ?: userData?.id ?: ""
                    }
                    delay(100)
                    isLoadingData = false
                }

                if (isLoadingData) {
                    ModernLoadingIndicator()
                } else {
                    ForoDetalleScreen(navController, foroId, localToken, userId)
                }
            }

            composable(
                route = "crearForo/{cursoId}",
                arguments = listOf(
                    navArgument("cursoId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val cursoId = backStackEntry.arguments?.getString("cursoId") ?: ""

                var localToken by remember { mutableStateOf<String?>(null) }
                var userId by remember { mutableStateOf<String?>(null) }
                var isLoadingCredentials by remember { mutableStateOf(true) }
                var hasError by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    try {
                        Log.d("CrearForoNav", "🔄 Cargando credenciales para CrearForo...")

                        localToken = withContext(Dispatchers.IO) {
                            userPrefs.getToken() ?: token
                        }
                        userId = withContext(Dispatchers.IO) {
                            userPrefs.getUserId() ?: userData?.id
                        }

                        Log.d("CrearForoNav", "✅ Credenciales cargadas:")
                        Log.d("CrearForoNav", "  - Token: ${if (!localToken.isNullOrEmpty()) "OK (${localToken!!.length} chars)" else "VACÍO"}")
                        Log.d("CrearForoNav", "  - UserId: ${userId ?: "VACÍO"}")

                        if (localToken.isNullOrEmpty() || userId.isNullOrEmpty()) {
                            Log.e("CrearForoNav", "❌ Credenciales inválidas")
                            hasError = true
                        }

                        delay(100)
                        isLoadingCredentials = false

                    } catch (e: Exception) {
                        Log.e("CrearForoNav", "❌ Error al cargar credenciales", e)
                        hasError = true
                        isLoadingCredentials = false
                    }
                }

                when {
                    isLoadingCredentials -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(FondoClaro),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = ErrorOscuro,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "Error de sesión",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ErrorOscuro
                                )
                            }
                        }
                    }

                    else -> {
                        CrearForoScreen(
                            navController = navController,
                            cursoId = cursoId,
                            token = localToken!!,
                            userId = userId!!
                        )
                    }
                }
            }
// ═══════════════════════════════════════════════════════════════
// 🔔 NOTIFICACIONES
// ═══════════════════════════════════════════════════════════════

            composable("notificaciones") {
                var localToken by remember { mutableStateOf("") }
                var isLoadingToken by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    localToken = withContext(Dispatchers.IO) {
                        userPrefs.getToken() ?: token ?: ""
                    }
                    delay(100)
                    isLoadingToken = false
                }

                if (isLoadingToken) {
                    ModernLoadingIndicator()
                } else {
                    if (localToken.isEmpty()) {
                        // Si no hay token, redirigir al login
                        LaunchedEffect(Unit) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "Sesión expirada. Inicia sesión nuevamente.",
                                    Toast.LENGTH_LONG
                                ).show()
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    } else {
                        NotificacionesScreen(navController = navController)
                    }
                }
            }
            // ═══════════════════════════════════════════════════════════════
            // 📅 CALENDARIO
            // ═══════════════════════════════════════════════════════════════

            composable(
                route = "calendario/{cursoId}",
                arguments = listOf(navArgument("cursoId") { type = NavType.StringType })
            ) { entry ->
                val cursoId = entry.arguments?.getString("cursoId") ?: ""
                var localToken by remember { mutableStateOf("") }
                var isLoadingToken by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    localToken = withContext(Dispatchers.IO) {
                        userPrefs.getToken() ?: token ?: ""
                    }
                    delay(100)
                    isLoadingToken = false
                }

                if (isLoadingToken) {
                    ModernLoadingIndicator()
                } else {
                    CalendarioScreenPadre(navController, cursoId, localToken)
                }
            }

            composable(
                route = "calendarioProfesor/{cursoId}",
                arguments = listOf(
                    navArgument("cursoId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val cursoId = backStackEntry.arguments?.getString("cursoId") ?: ""
                var localToken by remember { mutableStateOf("") }
                var isLoadingToken by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    localToken = withContext(Dispatchers.IO) {
                        userPrefs.getToken() ?: token ?: ""
                    }
                    delay(100)
                    isLoadingToken = false
                }

                if (isLoadingToken) {
                    ModernLoadingIndicator()
                } else {
                    CalendarioScreenProfesor(
                        navController = navController,
                        cursoId = cursoId,
                        token = localToken
                    )
                }
            }

            composable(
                route = "crearEvento/{cursoId}",
                arguments = listOf(
                    navArgument("cursoId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val cursoId = backStackEntry.arguments?.getString("cursoId") ?: ""
                var localToken by remember { mutableStateOf("") }
                var isLoadingToken by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    localToken = withContext(Dispatchers.IO) {
                        userPrefs.getToken() ?: token ?: ""
                    }
                    delay(100)
                    isLoadingToken = false
                }

                if (isLoadingToken) {
                    ModernLoadingIndicator()
                } else {
                    CrearEventoScreen(
                        navController = navController,
                        cursoId = cursoId,
                        token = localToken
                    )
                }
            }
        }
    }

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
                    delay(2500) // Tiempo para ver el splash
                    val token = withContext(Dispatchers.IO) { userPrefs.getToken() }
                    val padreId = withContext(Dispatchers.IO) { userPrefs.getPadreId() }
                    currentToken = token
                    currentPadreId = padreId

                    if (!token.isNullOrEmpty()) {
                        currentUserData = reloadUserData(token)
                        Log.d("MainActivity", "📱 Datos cargados en inicio - UserData: ${currentUserData?.nombre}")
                    }

                    startDestination = if (!token.isNullOrEmpty()) "home" else "login"

                    Log.d("MainActivity", """
                        ✅ App iniciada:
                        - Token: ${if (!token.isNullOrEmpty()) "Presente" else "Vacío"}
                        - PadreId: $padreId
                        - UserData: ${currentUserData?.nombre ?: "null"}
                        - StartDestination: $startDestination
                    """.trimIndent())

                    delay(400)
                    isLoading = false
                }

                // Pantalla de carga inicial con burbujas animadas
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
                    AppNavigation(
                        navController = navController,
                        startDestination = startDestination ?: "login",
                        userPrefs = userPrefs,
                        currentToken = currentToken,
                        currentUserData = currentUserData,
                        currentPadreId = currentPadreId,
                        onTokenUpdate = { token -> currentToken = token },
                        onUserDataUpdate = { data -> currentUserData = data },
                        onPadreIdUpdate = { id -> currentPadreId = id },
                        reloadUserData = { token -> reloadUserData(token) }
                    )
                }
            }
        }
    }
}

// ==================== SPLASH SCREEN ====================

data class Burbuja(
    val id: Int,
    val startX: Float,
    val size: Float,
    val duration: Int,
    val delay: Int,
    val color: Color
)

@Composable
fun ModernSplashScreen() {
    val burbujas = remember {
        List(30) { index ->
            Burbuja(
                id = index,
                startX = Random.nextFloat(),
                size = Random.nextFloat() * 60f + 30f,
                duration = Random.nextInt(5000, 10000),
                delay = Random.nextInt(0, 3000),
                color = listOf(
                    AzulCielo.copy(alpha = 0.15f),
                    Celeste.copy(alpha = 0.12f),
                    VerdeLima.copy(alpha = 0.1f),
                    Fucsia.copy(alpha = 0.08f),
                    Naranja.copy(alpha = 0.1f),
                    Morado.copy(alpha = 0.1f),
                    Cyan.copy(alpha = 0.12f)
                ).random()
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Blanco)
    ) {
        // Burbujas flotantes
        burbujas.forEach { burbuja ->
            BurbujaAnimada(burbuja = burbuja)
        }

        // Contenido principal
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LogoAnimadoPremium()
            Spacer(modifier = Modifier.height(64.dp))
            TituloAnimado()
            Spacer(modifier = Modifier.height(16.dp))
            SubtituloAnimado()
            Spacer(modifier = Modifier.height(80.dp))
            LoadingIndicatorPremium()
        }

        // Versión
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = "v1.0.0",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = GrisNeutral,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
fun BurbujaAnimada(burbuja: Burbuja) {
    val infiniteTransition = rememberInfiniteTransition(label = "burbuja_${burbuja.id}")

    // Movimiento vertical de abajo hacia arriba
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 1.2f,
        targetValue = -0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = burbuja.duration,
                easing = LinearEasing,
                delayMillis = burbuja.delay
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "offsetY"
    )

    // Movimiento horizontal sutil (ondulación)
    val offsetX by infiniteTransition.animateFloat(
        initialValue = burbuja.startX - 0.05f,
        targetValue = burbuja.startX + 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = burbuja.duration / 2,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetX"
    )

    // Escala pulsante
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = burbuja.duration / 3,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Alpha que aparece y desaparece
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = burbuja.duration
                0f at 0
                1f at 500
                1f at (burbuja.duration * 0.8).toInt()
                0f at burbuja.duration
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .size(burbuja.size.dp)
                .offset(
                    x = (offsetX * 1000).dp,
                    y = (offsetY * 2000).dp
                )
                .scale(scale)
                .alpha(alpha)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            burbuja.color,
                            burbuja.color.copy(alpha = burbuja.color.alpha * 0.5f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )
    }
}

@Composable
fun LogoAnimadoPremium() {
    val infiniteTransition = rememberInfiniteTransition(label = "logo")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val glow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = Modifier.size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        // Anillo exterior rotatorio con gradiente
        Surface(
            modifier = Modifier
                .size(220.dp)
                .graphicsLayer {
                    rotationZ = rotation
                },
            shape = CircleShape,
            color = Color.Transparent,
            border = androidx.compose.foundation.BorderStroke(
                3.dp,
                Brush.sweepGradient(
                    colors = listOf(
                        AzulCielo.copy(alpha = 0.8f),
                        VerdeLima.copy(alpha = 0.8f),
                        Fucsia.copy(alpha = 0.8f),
                        Naranja.copy(alpha = 0.8f),
                        AzulCielo.copy(alpha = 0.8f)
                    )
                )
            )
        ) {}

        // Resplandor intermedio
        Surface(
            modifier = Modifier
                .size(190.dp)
                .scale(scale),
            shape = CircleShape,
            color = AzulCielo.copy(alpha = glow * 0.1f)
        ) {}

        // Logo central - AQUÍ VA TU IMAGEN
        Surface(
            modifier = Modifier
                .size(160.dp)
                .scale(scale),
            shape = CircleShape,
            color = Blanco,
            shadowElevation = 24.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                AzulCielo.copy(alpha = 0.1f),
                                Blanco
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // REEMPLAZA ESTO CON TU IMAGEN DEL LOGO
                Image(
                    painter = painterResource(id = R.drawable.fondo), // <-- PON AQUÍ TU LOGO
                    contentDescription = "Logo Edumon",
                    modifier = Modifier
                        .size(120.dp)
                        .padding(20.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Partículas orbitales
        for (i in 0..2) {
            val orbitRotation by infiniteTransition.animateFloat(
                initialValue = i * 120f,
                targetValue = i * 120f + 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(8000 + i * 1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "orbit$i"
            )

            Box(
                modifier = Modifier
                    .size(220.dp)
                    .graphicsLayer {
                        rotationZ = orbitRotation
                    }
            ) {
                Surface(
                    modifier = Modifier
                        .size(12.dp)
                        .offset(y = (-110).dp)
                        .align(Alignment.TopCenter),
                    shape = CircleShape,
                    color = listOf(VerdeLima, Fucsia, Naranja)[i],
                    shadowElevation = 4.dp
                ) {}
            }
        }
    }
}

@Composable
fun TituloAnimado() {
    val infiniteTransition = rememberInfiniteTransition(label = "titulo")

    val shimmer by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    Box {
        Text(
            text = "EDUMON",
            fontSize = 48.sp,
            fontWeight = FontWeight.Black,
            color = AzulCielo,
            letterSpacing = 6.sp,
            style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = AzulCielo.copy(alpha = 0.3f),
                    offset = androidx.compose.ui.geometry.Offset(0f, 4f),
                    blurRadius = 8f
                )
            )
        )
    }
}

@Composable
fun SubtituloAnimado() {
    val alpha by rememberInfiniteTransition(label = "subtitulo").animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Plataforma Educativa",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = GrisOscuro.copy(alpha = alpha),
            letterSpacing = 2.sp
        )

        Box(
            modifier = Modifier
                .width(80.dp)
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            VerdeLima.copy(alpha = alpha),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
fun LoadingIndicatorPremium() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")

    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .width(240.dp)
                .height(4.dp)
                .background(
                    GrisClaro,
                    androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                AzulCielo,
                                VerdeLima,
                                Fucsia
                            )
                        ),
                        androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                    )
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (i in 0..2) {
                val dotScale by infiniteTransition.animateFloat(
                    initialValue = 0.5f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = FastOutSlowInEasing, delayMillis = i * 200),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dot$i"
                )

                Surface(
                    modifier = Modifier
                        .size(8.dp)
                        .scale(dotScale),
                    shape = CircleShape,
                    color = AzulCielo
                ) {}
            }
        }
    }
}

// ==================== MODERN LOADING INDICATOR ====================

@Composable
fun ModernLoadingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "modernLoading")

    // Rotación del gradiente circular
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Pulsación del logo
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Alpha del resplandor
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Blanco),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                // Anillo exterior rotatorio con gradiente
                Surface(
                    modifier = Modifier
                        .size(120.dp)
                        .graphicsLayer {
                            rotationZ = rotation
                        },
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(
                        4.dp,
                        Brush.sweepGradient(
                            colors = listOf(
                                AzulCielo,
                                VerdeLima,
                                Fucsia,
                                Naranja,
                                AzulCielo
                            )
                        )
                    )
                ) {}

                // Resplandor intermedio
                Surface(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(scale),
                    shape = CircleShape,
                    color = AzulCielo.copy(alpha = glowAlpha * 0.2f)
                ) {}

                // Logo central - AQUÍ VA TU IMAGEN
                Surface(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(scale),
                    shape = CircleShape,
                    color = Blanco,
                    shadowElevation = 16.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        AzulCielo.copy(alpha = 0.1f),
                                        Blanco
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // REEMPLAZA ESTO CON TU IMAGEN DEL LOGO
                        Image(
                            painter = painterResource(id = R.drawable.edumonavatar1), // <-- PON AQUÍ TU LOGO
                            contentDescription = "Logo Edumon",
                            modifier = Modifier
                                .size(60.dp)
                                .padding(12.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                // Puntos orbitales
                for (i in 0..2) {
                    val orbitRotation by infiniteTransition.animateFloat(
                        initialValue = i * 120f,
                        targetValue = i * 120f + 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(3000 + i * 500, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "orbit$i"
                    )

                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .graphicsLayer {
                                rotationZ = orbitRotation
                            }
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(8.dp)
                                .offset(y = (-60).dp)
                                .align(Alignment.TopCenter),
                            shape = CircleShape,
                            color = listOf(VerdeLima, Fucsia, Naranja)[i],
                            shadowElevation = 4.dp
                        ) {}
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Puntos animados de carga
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0..2) {
                    val dotScale by infiniteTransition.animateFloat(
                        initialValue = 0.5f,
                        targetValue = 1.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                600,
                                easing = FastOutSlowInEasing,
                                delayMillis = i * 200
                            ),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot$i"
                    )

                    Surface(
                        modifier = Modifier
                            .size(10.dp)
                            .scale(dotScale),
                        shape = CircleShape,
                        color = listOf(AzulCielo, VerdeLima, Fucsia)[i]
                    ) {}
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Cargando",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AzulCielo
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Preparando tu experiencia educativa",
                fontSize = 14.sp,
                color = GrisNeutral,
                fontWeight = FontWeight.Medium
            )
        }
    }
}