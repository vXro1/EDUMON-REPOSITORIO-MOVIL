package com.example.edumonjetcompose.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.edumonjetcompose.data.UserPreferences
import com.example.edumonjetcompose.models.CursoItem
import com.example.edumonjetcompose.network.ApiService
import com.example.edumonjetcompose.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfesorHomeScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPreferences = remember { UserPreferences(context) }

    var token by remember { mutableStateOf<String?>(null) }
    var userData by remember { mutableStateOf<com.example.edumonjetcompose.models.UserData?>(null) }
    var cursos by remember { mutableStateOf<List<CursoItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 🔔 Estado para notificaciones
    var notificacionesNoLeidas by remember { mutableStateOf(0) }

    // 🔔 Función para cargar el contador de notificaciones
    fun cargarContadorNotificaciones() {
        if (token.isNullOrEmpty()) return

        scope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiService.getMisNotificaciones(
                        token = token!!,
                        page = 1,
                        limit = 1,
                        leida = false
                    )
                }

                if (response.isSuccessful) {
                    val body = response.body()
                    val noLeidas = body?.get("noLeidas")?.asInt ?: 0

                    withContext(Dispatchers.Main) {
                        notificacionesNoLeidas = noLeidas
                        android.util.Log.d("ProfesorHome", " Notificaciones no leídas: $noLeidas")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfesorHome", "Error al cargar notificaciones: ${e.message}")
            }
        }
    }

    // Cargar datos del usuario y token
    LaunchedEffect(Unit) {
        try {
            token = withContext(Dispatchers.IO) {
                userPreferences.getToken()
            }

            android.util.Log.d("ProfesorHome", "Token cargado: ${!token.isNullOrEmpty()}")

            if (token.isNullOrEmpty()) {
                android.util.Log.e("ProfesorHome", "Token vacío o nulo")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Sesión inválida", Toast.LENGTH_LONG).show()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
                return@LaunchedEffect
            }

            android.util.Log.d("ProfesorHome", "🔄 Obteniendo perfil desde servidor...")
            val response = withContext(Dispatchers.IO) {
                ApiService.getUserProfile(token!!)
            }

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    userData = com.example.edumonjetcompose.models.UserData(
                        id = body.get("_id")?.asString ?: "",
                        nombre = body.get("nombre")?.asString ?: "",
                        apellido = body.get("apellido")?.asString ?: "",
                        correo = body.get("correo")?.asString ?: "",
                        rol = body.get("rol")?.asString ?: "",
                        telefono = body.get("telefono")?.asString ?: "",
                        cedula = body.get("cedula")?.takeIf { !it.isJsonNull }?.asString,
                        fotoPerfilUrl = body.get("fotoPerfilUrl")?.takeIf { !it.isJsonNull }?.asString,
                        estado = body.get("estado")?.asString ?: "activo"
                    )

                    android.util.Log.d("ProfesorHome", "✅ Usuario: ${userData?.nombre} ${userData?.apellido}")

                    withContext(Dispatchers.IO) {
                        userPreferences.saveUserData(userData!!)
                    }
                } else {
                    throw Exception("Respuesta del servidor vacía")
                }
            } else {
                throw Exception("Error al cargar perfil: ${response.code()}")
            }

            if (userData == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error al cargar datos", Toast.LENGTH_LONG).show()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
                return@LaunchedEffect
            }

            android.util.Log.d("ProfesorHome", "🔄 Iniciando carga de cursos...")
            cargarCursos(token!!, scope, context) { cursosResponse ->
                android.util.Log.d("ProfesorHome", "✅ Cursos cargados: ${cursosResponse.size}")
                cursos = cursosResponse
                isLoading = false

                // 🔔 Cargar notificaciones después de cargar cursos
                cargarContadorNotificaciones()
            }
        } catch (e: Exception) {
            android.util.Log.e("ProfesorHome", "❌ Error en LaunchedEffect", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                errorMessage = "Error: ${e.message}"
                isLoading = false
            }
        }
    }

    // 🔔 Actualización periódica de notificaciones cada 60 segundos
    LaunchedEffect(token) {
        if (!token.isNullOrEmpty()) {
            while (true) {
                delay(60000) // 60 segundos
                cargarContadorNotificaciones()
            }
        }
    }

    // Animaciones suaves
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                contentColor = AzulCielo,
                tonalElevation = 12.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                    label = { Text("Inicio", fontWeight = FontWeight.SemiBold) },
                    selected = true,
                    onClick = { },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AzulCielo,
                        selectedTextColor = AzulCielo,
                        indicatorColor = AzulCielo.copy(alpha = 0.15f),
                        unselectedIconColor = GrisNeutral,
                        unselectedTextColor = GrisNeutral
                    )
                )

                // 🔔 Navegación a Notificaciones con Badge
                NavigationBarItem(
                    icon = {
                        BadgedBox(
                            badge = {
                                if (notificacionesNoLeidas > 0) {
                                    Badge(
                                        containerColor = Error,
                                        contentColor = Color.White
                                    ) {
                                        Text(
                                            text = if (notificacionesNoLeidas > 99) "99+" else notificacionesNoLeidas.toString(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Notificaciones"
                            )
                        }
                    },
                    label = { Text("Avisos", fontWeight = FontWeight.Medium) },
                    selected = false,
                    onClick = {
                        navController.navigate("notificaciones")
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AzulCielo,
                        selectedTextColor = AzulCielo,
                        indicatorColor = AzulCielo.copy(alpha = 0.15f),
                        unselectedIconColor = GrisNeutral,
                        unselectedTextColor = GrisNeutral
                    )
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Configuración") },
                    label = { Text("Ajustes", fontWeight = FontWeight.Medium) },
                    selected = false,
                    onClick = { Toast.makeText(context, "Configuración", Toast.LENGTH_SHORT).show() },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AzulCielo,
                        selectedTextColor = AzulCielo,
                        indicatorColor = AzulCielo.copy(alpha = 0.15f),
                        unselectedIconColor = GrisNeutral,
                        unselectedTextColor = GrisNeutral
                    )
                )

                NavigationBarItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Salir") },
                    label = { Text("Salir", fontWeight = FontWeight.Medium) },
                    selected = false,
                    onClick = {
                        scope.launch {
                            try {
                                token?.let {
                                    withContext(Dispatchers.IO) {
                                        ApiService.logout(it)
                                    }
                                }
                                withContext(Dispatchers.IO) {
                                    userPreferences.clearAll()
                                }
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Error al cerrar sesión", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Error,
                        selectedTextColor = Error,
                        indicatorColor = Error.copy(alpha = 0.15f),
                        unselectedIconColor = GrisNeutral,
                        unselectedTextColor = GrisNeutral
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            AzulCielo.copy(alpha = 0.05f),
                            Color.White,
                            AzulCielo.copy(alpha = 0.02f)
                        )
                    )
                )
        ) {
            // Burbujas decorativas animadas
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .offset(x = (-100).dp, y = 50.dp)
                    .scale(scale)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                AzulCielo.copy(alpha = 0.1f),
                                AzulCielo.copy(alpha = 0.02f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )

            Box(
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 50.dp, y = (-50).dp)
                    .scale(scale * 0.8f)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                AzulCieloClaro.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header con información del profesor
                item(key = "header") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            AzulCielo.copy(alpha = 0.9f),
                                            AzulCieloClaro.copy(alpha = 0.95f)
                                        )
                                    )
                                )
                                .padding(24.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "¡Hola!",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${userData?.nombre ?: "Profesor"}",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 28.sp
                                        ),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${userData?.apellido ?: ""}",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White.copy(alpha = 0.95f),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Email,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.9f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = userData?.correo ?: "",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Phone,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.9f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = userData?.telefono ?: "",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                    }

                                    // 🔔 Badge de notificaciones en el header
                                    if (notificacionesNoLeidas > 0) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            modifier = Modifier
                                                .background(
                                                    Color.White.copy(alpha = 0.2f),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable { navController.navigate("notificaciones") }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Notifications,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "$notificacionesNoLeidas nuevas",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .shadow(8.dp, CircleShape)
                                        .background(Color.White, CircleShape)
                                        .clickable {
                                            Toast
                                                .makeText(context, "Perfil", Toast.LENGTH_SHORT)
                                                .show()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!userData?.fotoPerfilUrl.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(userData?.fotoPerfilUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Foto perfil",
                                            modifier = Modifier
                                                .size(86.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Perfil",
                                            tint = AzulCielo,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Título de sección
                item(key = "section_title") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Mis Cursos",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = FondoOscuroPrimario
                            )
                            Text(
                                text = "${cursos.size} ${if (cursos.size == 1) "curso" else "cursos"} activos",
                                style = MaterialTheme.typography.bodyMedium,
                                color = GrisNeutral
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "🚧 Función en mantenimiento",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.height(45.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = AzulCielo.copy(alpha = 0.15f),
                                contentColor = AzulCielo
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Crear", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Lista de cursos o estados
                when {
                    isLoading -> {
                        item(key = "loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        color = AzulCielo,
                                        strokeWidth = 3.dp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "Cargando cursos...",
                                        color = GrisNeutral,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                    errorMessage != null -> {
                        item(key = "error") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Error.copy(alpha = 0.1f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = Error,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = errorMessage ?: "Error desconocido",
                                        color = Error,
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                    cursos.isEmpty() -> {
                        item(key = "empty") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = AzulCielo.copy(alpha = 0.05f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(40.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = null,
                                        tint = AzulCielo.copy(alpha = 0.6f),
                                        modifier = Modifier.size(80.dp)
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Text(
                                        text = "Sin cursos asignados",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = GrisOscuro,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Los cursos aparecerán aquí cuando sean creados",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = GrisNeutral,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        items(
                            items = cursos,
                            key = { curso -> curso.id }
                        ) { curso ->
                            CursoCard(
                                curso = curso,
                                onClick = {
                                    navController.navigate("infoCursoProfesor/${curso.id}")
                                }
                            )
                        }
                    }
                }

                // Espaciado final
                item(key = "footer_spacer") {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

fun cargarCursos(
    token: String,
    scope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context,
    onResult: (List<CursoItem>) -> Unit
) {
    scope.launch {
        try {
            val response = withContext(Dispatchers.IO) {
                ApiService.getCursos(
                    token = token,
                    page = 1,
                    limit = 50
                )
            }

            if (response.isSuccessful) {
                val body = response.body()
                val cursosArray = body?.getAsJsonArray("cursos")

                val cursosList = cursosArray?.map { cursoElement ->
                    val cursoJson = cursoElement.asJsonObject

                    val docenteJson = cursoJson.getAsJsonObject("docenteId")
                    val docenteNombre = if (docenteJson != null) {
                        "${docenteJson.get("nombre")?.asString ?: ""} ${docenteJson.get("apellido")?.asString ?: ""}"
                    } else {
                        "Sin docente"
                    }

                    val participantesArray = cursoJson.getAsJsonArray("participantes")
                    val totalParticipantes = participantesArray?.size() ?: 0
                    val fotoPortadaUrl = cursoJson.get("fotoPortadaUrl")?.takeIf { !it.isJsonNull }?.asString

                    CursoItem(
                        id = cursoJson.get("_id")?.asString ?: "",
                        nombre = cursoJson.get("nombre")?.asString ?: "Sin nombre",
                        descripcion = cursoJson.get("descripcion")?.takeIf { !it.isJsonNull }?.asString,
                        fotoPortadaUrl = fotoPortadaUrl,
                        estado = cursoJson.get("estado")?.asString ?: "activo",
                        totalParticipantes = totalParticipantes,
                        docenteNombre = docenteNombre
                    )
                } ?: emptyList()

                withContext(Dispatchers.Main) {
                    onResult(cursosList)
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error al cargar cursos: ${response.code()}", Toast.LENGTH_SHORT).show()
                    onResult(emptyList())
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                onResult(emptyList())
            }
        }
    }
}

@Composable
fun CursoCard(
    curso: CursoItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                AzulCielo.copy(alpha = 0.3f),
                                AzulCieloClaro.copy(alpha = 0.5f)
                            )
                        )
                    )
            ) {
                if (!curso.fotoPortadaUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(curso.fotoPortadaUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Portada del curso",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.4f)
                                )
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(
                            if (curso.estado == "activo") VerdeLima.copy(alpha = 0.95f)
                            else GrisClaro.copy(alpha = 0.95f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = curso.estado.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (curso.estado == "activo") Color.White else GrisOscuro
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = curso.nombre,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = FondoOscuroPrimario,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (!curso.descripcion.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = curso.descripcion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = GrisNeutral,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(AzulCielo.copy(alpha = 0.15f), CircleShape)
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = AzulCielo,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "${curso.totalParticipantes} estudiantes",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = GrisOscuro
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(AzulCielo, CircleShape)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Ver curso",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}