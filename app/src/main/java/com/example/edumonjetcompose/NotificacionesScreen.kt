package com.example.edumonjetcompose.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import com.example.edumonjetcompose.data.UserPreferences
import com.example.edumonjetcompose.models.Notificacion
import com.example.edumonjetcompose.network.ApiService
import com.example.edumonjetcompose.services.MyFirebaseMessagingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificacionesScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPreferences = remember { UserPreferences(context) }

    var token by remember { mutableStateOf<String?>(null) }
    var notificaciones by remember { mutableStateOf<List<Notificacion>>(emptyList()) }
    var noLeidas by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var recargarTrigger by remember { mutableStateOf(0) }

    val infiniteTransition = rememberInfiniteTransition(label = "refresh")
    val refreshRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == MyFirebaseMessagingService.ACTION_NUEVA_NOTIFICACION) {
                    android.util.Log.d("NotificacionesScreen", "🔔 Nueva notificación recibida via broadcast")
                    recargarTrigger++
                }
            }
        }

        val filter = IntentFilter(MyFirebaseMessagingService.ACTION_NUEVA_NOTIFICACION)
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter)

        onDispose {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver)
            android.util.Log.d("NotificacionesScreen", "🛑 BroadcastReceiver desregistrado")
        }
    }

    LaunchedEffect(Unit) {
        android.util.Log.d("NotificacionesScreen", "🔑 Cargando token...")
        val tokenActual = withContext(Dispatchers.IO) {
            userPreferences.getToken()
        }

        if (tokenActual.isNullOrEmpty()) {
            android.util.Log.e("NotificacionesScreen", "❌ No hay token guardado")
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Sesión expirada", Toast.LENGTH_SHORT).show()
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            }
        } else {
            token = tokenActual
            android.util.Log.d("NotificacionesScreen", "✅ Token cargado: ${tokenActual.take(20)}...")
        }
    }

    fun cargarNotificaciones(mostrarLoading: Boolean = true) {
        scope.launch {
            try {
                if (mostrarLoading) {
                    isLoading = true
                } else {
                    isRefreshing = true
                }
                errorMessage = null

                val tokenActual = token ?: withContext(Dispatchers.IO) {
                    userPreferences.getToken()
                }

                if (tokenActual.isNullOrEmpty()) {
                    android.util.Log.e("NotificacionesScreen", "❌ Token vacío al cargar notificaciones")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Sesión inválida", Toast.LENGTH_SHORT).show()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                    return@launch
                }

                android.util.Log.d("NotificacionesScreen", "📡 Solicitando notificaciones...")

                val response = withContext(Dispatchers.IO) {
                    ApiService.getMisNotificaciones(
                        token = tokenActual,
                        page = 1,
                        limit = 100,
                        leida = null
                    )
                }

                android.util.Log.d("NotificacionesScreen", "📥 Response code: ${response.code()}")

                when (response.code()) {
                    401 -> {
                        android.util.Log.e("NotificacionesScreen", "❌ Token expirado (401)")
                        withContext(Dispatchers.Main) {
                            withContext(Dispatchers.IO) {
                                userPreferences.clearToken()
                            }
                            Toast.makeText(context, "Sesión expirada. Inicia sesión nuevamente", Toast.LENGTH_LONG).show()
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                    200 -> {
                        val body = response.body()

                        if (body != null) {
                            android.util.Log.d("NotificacionesScreen", "✅ Response body recibido")

                            val notificacionesArray = body.getAsJsonArray("notificaciones")
                            noLeidas = body.get("noLeidas")?.asInt ?: 0

                            val notifList = mutableListOf<Notificacion>()

                            notificacionesArray?.forEach { element ->
                                try {
                                    val notifJson = element.asJsonObject

                                    // ✅ CORRECCIÓN: Extraer referenciaId como string
                                    val referenciaIdValue = notifJson.get("referenciaId")
                                    val referenciaIdString = when {
                                        referenciaIdValue == null || referenciaIdValue.isJsonNull -> null
                                        referenciaIdValue.isJsonObject -> referenciaIdValue.asJsonObject.get("_id")?.asString
                                        referenciaIdValue.isJsonPrimitive -> referenciaIdValue.asString
                                        else -> null
                                    }

                                    // ✅ Extraer título correctamente
                                    val tituloValue = notifJson.get("titulo")
                                    val titulo = when {
                                        tituloValue != null && !tituloValue.isJsonNull -> tituloValue.asString
                                        else -> {
                                            // Si no hay título, usar tipo de notificación
                                            val tipo = notifJson.get("tipo")?.asString ?: "info"
                                            when (tipo) {
                                                "entrega" -> "Nueva entrega"
                                                "tarea" -> "Nueva tarea"
                                                "evento" -> "Nuevo evento"
                                                "sistema" -> "Notificación del sistema"
                                                else -> "Notificación"
                                            }
                                        }
                                    }

                                    val notif = Notificacion(
                                        id = notifJson.get("_id")?.asString ?: "",
                                        usuarioId = notifJson.get("usuarioId")?.asString ?: "",
                                        titulo = titulo,
                                        mensaje = notifJson.get("mensaje")?.asString ?: "",
                                        tipo = notifJson.get("tipo")?.asString ?: "info",
                                        leido = notifJson.get("leido")?.asBoolean ?: false,
                                        fecha = notifJson.get("fecha")?.asString ?: "",
                                        referenciaId = referenciaIdString,
                                        referenciaModelo = notifJson.get("referenciaModelo")?.asString
                                    )
                                    notifList.add(notif)
                                } catch (e: Exception) {
                                    android.util.Log.e("NotificacionesScreen", "⚠️ Error parseando notificación: ${e.message}", e)
                                }
                            }

                            withContext(Dispatchers.Main) {
                                notificaciones = notifList
                                android.util.Log.d("NotificacionesScreen", """
                                    ✅ Notificaciones cargadas exitosamente
                                       Total: ${notifList.size}
                                       No leídas: $noLeidas
                                       Leídas: ${notifList.size - noLeidas}
                                """.trimIndent())

                                if (!mostrarLoading) {
                                    Toast.makeText(context, "✓ Actualizado (${notifList.size})", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                errorMessage = "Response vacío del servidor"
                                android.util.Log.e("NotificacionesScreen", "❌ Body es null")
                            }
                        }
                    }
                    else -> {
                        val errorBody = response.errorBody()?.string()
                        withContext(Dispatchers.Main) {
                            errorMessage = "Error: ${response.code()}"
                            android.util.Log.e("NotificacionesScreen", "❌ Error ${response.code()}: $errorBody")
                            Toast.makeText(context, "Error al cargar: ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("NotificacionesScreen", "❌ Exception al cargar notificaciones", e)
                withContext(Dispatchers.Main) {
                    errorMessage = e.message
                    Toast.makeText(context, "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                delay(300)
                isLoading = false
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(token) {
        if (token != null) {
            android.util.Log.d("NotificacionesScreen", "🚀 Iniciando carga de notificaciones")
            cargarNotificaciones()
        }
    }

    LaunchedEffect(recargarTrigger) {
        if (recargarTrigger > 0 && token != null) {
            android.util.Log.d("NotificacionesScreen", "🔄 Recargando por nueva notificación")
            delay(500)
            cargarNotificaciones(mostrarLoading = false)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8F9FF),
                        Color(0xFFFAFBFF),
                        Color.White
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    "Notificaciones",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp
                                    ),
                                    color = Color(0xFF1A1F36)
                                )
                                if (noLeidas > 0) {
                                    Text(
                                        "$noLeidas sin leer",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF4C6FFF),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { navController.navigateUp() },
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    "Volver",
                                    tint = Color(0xFF1A1F36)
                                )
                            }
                        },
                        actions = {
                            if (noLeidas > 0) {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                token?.let {
                                                    withContext(Dispatchers.IO) {
                                                        ApiService.marcarTodasLeidas(it)
                                                    }
                                                }
                                                cargarNotificaciones(false)
                                                Toast.makeText(
                                                    context,
                                                    "✓ Todas marcadas como leídas",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(
                                                    context,
                                                    "Error: ${e.message}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .size(40.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DoneAll,
                                        "Marcar todas leídas",
                                        tint = Color(0xFF4C6FFF)
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    android.util.Log.d("NotificacionesScreen", "🔄 Recarga manual iniciada")
                                    cargarNotificaciones(false)
                                },
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(40.dp)
                                    .rotate(if (isRefreshing) refreshRotation else 0f)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    "Actualizar",
                                    tint = if (isRefreshing) Color(0xFF4C6FFF) else Color(0xFF8B95A5)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.White,
                            titleContentColor = Color(0xFF1A1F36)
                        )
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when {
                    token == null || isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF4C6FFF),
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    "Cargando notificaciones...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color(0xFF8B95A5),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    errorMessage != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color(0xFFEF4444)
                                )
                                Text(
                                    "Error al cargar",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color(0xFF1A1F36),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    errorMessage ?: "Error desconocido",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF8B95A5),
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = { cargarNotificaciones() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4C6FFF)
                                    )
                                ) {
                                    Text("Reintentar")
                                }
                            }
                        }
                    }
                    notificaciones.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(24.dp),
                                modifier = Modifier.padding(40.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(120.dp)
                                        .background(Color(0xFFF0F2FF), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.NotificationsNone,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = Color(0xFF4C6FFF)
                                    )
                                }
                                Text(
                                    "Sin notificaciones",
                                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                                    color = Color(0xFF1A1F36),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Aquí aparecerán tus notificaciones\ncuando las recibas",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                    color = Color(0xFF8B95A5),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = notificaciones,
                                key = { it.id }
                            ) { notificacion ->
                                NotificacionItem(
                                    notificacion = notificacion,
                                    onMarcarLeida = {
                                        scope.launch {
                                            try {
                                                token?.let {
                                                    withContext(Dispatchers.IO) {
                                                        ApiService.marcarComoLeida(it, notificacion.id)
                                                    }
                                                }
                                                cargarNotificaciones(false)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    onEliminar = {
                                        showDeleteDialog = notificacion.id
                                    },
                                    onClick = {
                                        if (!notificacion.leido) {
                                            scope.launch {
                                                try {
                                                    token?.let {
                                                        withContext(Dispatchers.IO) {
                                                            ApiService.marcarComoLeida(it, notificacion.id)
                                                        }
                                                    }
                                                    cargarNotificaciones(false)
                                                } catch (e: Exception) {
                                                    android.util.Log.e("NotificacionesScreen", "Error: ${e.message}")
                                                }
                                            }
                                        }

                                        notificacion.referenciaId?.let { refId ->
                                            when (notificacion.tipo) {
                                                "curso" -> navController.navigate("infoCursoProfesor/$refId")
                                                "tarea" -> navController.navigate("detalleTareaProfesor/$refId")
                                                "entrega" -> navController.navigate("detalleEntrega/$refId")
                                                "calificacion" -> navController.navigate("verCalificaciones/$refId")
                                                "mensaje" -> navController.navigate("mensajes/$refId")
                                                "evento" -> navController.navigate("calendarioProfesor/$refId")
                                                "recordatorio" -> {
                                                    when (notificacion.referenciaModelo) {
                                                        "Tarea" -> navController.navigate("detalleTareaProfesor/$refId")
                                                        "Curso" -> navController.navigate("infoCursoProfesor/$refId")
                                                        "Evento" -> navController.navigate("calendarioProfesor/$refId")
                                                        else -> Toast.makeText(context, "Tipo no reconocido", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                else -> {
                                                    Toast.makeText(context, "Notificación: ${notificacion.titulo}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } ?: run {
                                            Toast.makeText(context, notificacion.titulo, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        }
                    }
                }
            }
        }

        if (showDeleteDialog != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = {
                    Text(
                        "Eliminar notificación",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF1A1F36)
                    )
                },
                text = {
                    Text(
                        "¿Estás seguro de que deseas eliminar esta notificación?",
                        fontSize = 15.sp,
                        color = Color(0xFF475569)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val notifId = showDeleteDialog!!
                            scope.launch {
                                try {
                                    token?.let {
                                        withContext(Dispatchers.IO) {
                                            ApiService.deleteNotificacion(it, notifId)
                                        }
                                    }
                                    cargarNotificaciones(false)
                                    Toast.makeText(context, "✓ Notificación eliminada", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                            showDeleteDialog = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Eliminar", fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteDialog = null },
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Cancelar", color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                    }
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = Color.White
            )
        }
    }
}

@Composable
fun NotificacionItem(
    notificacion: Notificacion,
    onMarcarLeida: () -> Unit,
    onEliminar: () -> Unit,
    onClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(50)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(400)) + slideInVertically(
            initialOffsetY = { it / 3 },
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (notificacion.leido) 2.dp else 6.dp,
                    shape = RoundedCornerShape(20.dp),
                    spotColor = if (notificacion.leido) Color(0x1A000000) else Color(0x334C6FFF)
                )
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (notificacion.leido) Color.White else Color(0xFFF0F4FF)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            color = when (notificacion.tipo) {
                                "curso" -> Color(0xFF4C6FFF).copy(alpha = 0.12f)
                                "tarea" -> Color(0xFF10B981).copy(alpha = 0.12f)
                                "entrega" -> Color(0xFFF59E0B).copy(alpha = 0.12f)
                                "calificacion" -> Color(0xFF8B5CF6).copy(alpha = 0.12f)
                                "mensaje" -> Color(0xFF06B6D4).copy(alpha = 0.12f)
                                "recordatorio" -> Color(0xFFEC4899).copy(alpha = 0.12f)
                                "evento" -> Color(0xFF14B8A6).copy(alpha = 0.12f)
                                "sistema" -> Color(0xFF64748B).copy(alpha = 0.12f)
                                else -> Color(0xFF94A3B8).copy(alpha = 0.12f)
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (notificacion.tipo) {
                            "curso" -> Icons.Default.School
                            "tarea" -> Icons.Default.Assignment
                            "entrega" -> Icons.Default.Upload
                            "calificacion" -> Icons.Default.Grade
                            "mensaje" -> Icons.Default.Message
                            "recordatorio" -> Icons.Default.Alarm
                            "evento" -> Icons.Default.Event
                            "sistema" -> Icons.Default.Settings
                            else -> Icons.Default.Info
                        },
                        contentDescription = null,
                        tint = when (notificacion.tipo) {
                            "curso" -> Color(0xFF4C6FFF)
                            "tarea" -> Color(0xFF10B981)
                            "entrega" -> Color(0xFFF59E0B)
                            "calificacion" -> Color(0xFF8B5CF6)
                            "mensaje" -> Color(0xFF06B6D4)
                            "recordatorio" -> Color(0xFFEC4899)
                            "evento" -> Color(0xFF14B8A6)
                            "sistema" -> Color(0xFF64748B)
                            else -> Color(0xFF94A3B8)
                        },
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = notificacion.titulo,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = if (notificacion.leido) FontWeight.SemiBold else FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = Color(0xFF1A1F36),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        if (!notificacion.leido) {
                            Box(
                                modifier = Modifier
                                    .size(11.dp)
                                    .background(Color(0xFF4C6FFF), CircleShape)
                            )
                        }
                    }

                    Text(
                        text = notificacion.mensaje,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = Color(0xFF475569),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = formatFecha(notificacion.fecha),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Opciones",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color.White, RoundedCornerShape(12.dp))
                    ) {
                        if (!notificacion.leido) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Marcar como leída",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                onClick = {
                                    onMarcarLeida()
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Check,
                                        null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )
                        }

                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Eliminar",
                                    color = Color(0xFFEF4444),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            onClick = {
                                onEliminar()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun formatFecha(fechaISO: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(fechaISO) ?: return "Fecha desconocida"

        val now = Calendar.getInstance()
        val notifDate = Calendar.getInstance().apply { time = date }

        val diffMillis = now.timeInMillis - notifDate.timeInMillis
        val diffMinutes = diffMillis / (1000 * 60)
        val diffHours = diffMillis / (1000 * 60 * 60)
        val diffDays = diffMillis / (1000 * 60 * 60 * 24)

        when {
            diffMinutes < 1 -> "Ahora mismo"
            diffMinutes < 60 -> "Hace ${diffMinutes}m"
            diffHours < 24 -> "Hace ${diffHours}h"
            diffDays == 1L -> "Ayer"
            diffDays < 7 -> "Hace ${diffDays}d"
            else -> SimpleDateFormat("dd MMM yyyy", Locale("es", "ES")).format(date)
        }
    } catch (e: Exception) {
        android.util.Log.e("NotificacionesScreen", "Error al formatear fecha: ${e.message}")
        "Fecha desconocida"
    }
}