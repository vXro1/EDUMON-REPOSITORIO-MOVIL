package com.example.edumonjetcompose

import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.edumonjetcompose.models.*
import com.example.edumonjetcompose.network.ApiService
import com.example.edumonjetcompose.ui.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleEntregaProfesorScreen(
    navController: NavController,
    entregaId: String,
    token: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var entrega by remember { mutableStateOf<DetalleEntrega?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showCalificarDialog by remember { mutableStateOf(false) }

    var nota by remember { mutableStateOf("") }
    var comentario by remember { mutableStateOf("") }
    var isCalificando by remember { mutableStateOf(false) }

    // Log para debug
    LaunchedEffect(Unit) {
        android.util.Log.d("DETALLE_ENTREGA", "Token recibido: ${token.take(50)}...")
        android.util.Log.d("DETALLE_ENTREGA", "EntregaId: $entregaId")
    }

    // Función para cargar los datos de la entrega
    fun cargarEntrega() {
        scope.launch {
            isLoading = true
            try {
                android.util.Log.d("DETALLE_ENTREGA", "Cargando entrega con ID: $entregaId")

                val response = ApiService.getEntregaById(token, entregaId)

                android.util.Log.d("DETALLE_ENTREGA", "Response code: ${response.code()}")

                if (response.isSuccessful) {
                    val body = response.body()
                    body?.let { json ->
                        val id = json.get("_id")?.asString ?: ""
                        val textoRespuesta = json.get("textoRespuesta")?.asString ?: ""
                        val fechaEntrega = json.get("fechaEntrega")?.asString
                        val estado = json.get("estado")?.asString ?: "pendiente"

                        // Calificación
                        val calificacionObj = json.getAsJsonObject("calificacion")
                        val notaActual = calificacionObj?.get("nota")?.asDouble
                        val comentarioActual = calificacionObj?.get("comentario")?.asString

                        // Estudiante
                        val padreObj = json.getAsJsonObject("padreId")
                        val estudianteNombre = "${padreObj?.get("nombre")?.asString ?: ""} ${padreObj?.get("apellido")?.asString ?: ""}"
                        val estudianteFoto = padreObj?.get("fotoPerfilUrl")?.asString

                        // Tarea
                        val tareaObj = json.getAsJsonObject("tareaId")
                        val tareaTitulo = tareaObj?.get("titulo")?.asString ?: ""

                        // Archivos
                        val archivosArray = json.getAsJsonArray("archivosAdjuntos")
                        val archivos = mutableListOf<ArchivoAdjuntoEntrega>()
                        archivosArray?.forEach { element ->
                            val archivoObj = element.asJsonObject
                            archivos.add(
                                ArchivoAdjuntoEntrega(
                                    url = archivoObj.get("url")?.asString ?: "",
                                    nombreOriginal = archivoObj.get("nombreOriginal")?.asString ?: "",
                                    tipoArchivo = archivoObj.get("tipoArchivo")?.asString ?: "",
                                    tamano = archivoObj.get("tamano")?.asLong ?: 0L
                                )
                            )
                        }

                        entrega = DetalleEntrega(
                            id = id,
                            estudianteNombre = estudianteNombre,
                            estudianteFoto = estudianteFoto,
                            tareaTitulo = tareaTitulo,
                            textoRespuesta = textoRespuesta,
                            fechaEntrega = fechaEntrega,
                            estado = estado,
                            nota = notaActual,
                            comentario = comentarioActual,
                            archivos = archivos
                        )

                        // Pre-cargar nota y comentario si ya está calificada
                        if (notaActual != null) {
                            nota = notaActual.toString()
                        }
                        if (comentarioActual != null) {
                            comentario = comentarioActual
                        }

                        android.util.Log.d("DETALLE_ENTREGA", "Entrega cargada exitosamente")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("DETALLE_ENTREGA", "Error: ${response.code()} - $errorBody")
                    snackbarHostState.showSnackbar("Error al cargar la entrega: ${response.code()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("DETALLE_ENTREGA", "Exception: ${e.message}", e)
                snackbarHostState.showSnackbar("Error al cargar la entrega: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    // Cargar detalle de la entrega
    LaunchedEffect(entregaId) {
        cargarEntrega()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Detalle de Entrega",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FondoCard,
                    titleContentColor = GrisOscuro
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            val entregaActual = entrega
            if (entregaActual != null && (entregaActual.estado == "enviada" || entregaActual.estado == "tarde")) {
                ExtendedFloatingActionButton(
                    onClick = { showCalificarDialog = true },
                    containerColor = VerdeLima,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Star, "Calificar")
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (entregaActual.nota == null) "Calificar" else "Editar nota",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        containerColor = FondoClaro
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AzulCielo)
            }
        } else {
            val entregaActual = entrega
            if (entregaActual == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = GrisClaro
                        )
                        Text(
                            "No se pudo cargar la entrega",
                            style = MaterialTheme.typography.bodyLarge,
                            color = GrisMedio
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header con info del estudiante
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = FondoCard,
                            tonalElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar
                                val fotoUrl = entregaActual.estudianteFoto
                                if (fotoUrl != null) {
                                    AsyncImage(
                                        model = fotoUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(GrisClaro),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Surface(
                                        modifier = Modifier.size(64.dp),
                                        shape = CircleShape,
                                        color = AzulCielo.copy(alpha = 0.2f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                entregaActual.estudianteNombre.firstOrNull()?.uppercase() ?: "?",
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = AzulCielo
                                            )
                                        }
                                    }
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        entregaActual.estudianteNombre,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = GrisOscuro
                                    )

                                    Text(
                                        entregaActual.tareaTitulo,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = GrisMedio
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = when (entregaActual.estado) {
                                                "calificada" -> VerdeLima.copy(alpha = 0.15f)
                                                "tarde" -> ErrorOscuro.copy(alpha = 0.15f)
                                                "enviada" -> Advertencia.copy(alpha = 0.15f)
                                                else -> GrisClaro
                                            }
                                        ) {
                                            Text(
                                                when (entregaActual.estado) {
                                                    "calificada" -> "Calificada"
                                                    "tarde" -> "Entrega tardía"
                                                    "enviada" -> "Enviada"
                                                    else -> "Pendiente"
                                                },
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = when (entregaActual.estado) {
                                                    "calificada" -> VerdeLima
                                                    "tarde" -> ErrorOscuro
                                                    "enviada" -> Advertencia
                                                    else -> GrisMedio
                                                }
                                            )
                                        }

                                        val fechaEntregaVal = entregaActual.fechaEntrega
                                        if (fechaEntregaVal != null) {
                                            Icon(
                                                Icons.Default.Schedule,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = GrisMedio
                                            )
                                            Text(
                                                formatearFechaEntrega(fechaEntregaVal),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = GrisMedio
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Calificación actual (si existe)
                    val notaActual = entregaActual.nota
                    if (notaActual != null) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = VerdeLima.copy(alpha = 0.1f),
                                border = BorderStroke(2.dp, VerdeLima)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = VerdeLima
                                    ) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .padding(12.dp)
                                                .size(32.dp),
                                            tint = Color.White
                                        )
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            "Calificación",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = GrisMedio,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            String.format("%.1f / 100", notaActual),
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = VerdeLima
                                        )
                                    }

                                    TextButton(
                                        onClick = { showCalificarDialog = true }
                                    ) {
                                        Text("Editar")
                                    }
                                }
                            }
                        }
                    }

                    // Respuesta del estudiante
                    item {
                        InfoSection(
                            title = "Respuesta del estudiante",
                            icon = Icons.Default.Message
                        ) {
                            if (entregaActual.textoRespuesta.isNotEmpty()) {
                                Text(
                                    entregaActual.textoRespuesta,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = GrisOscuro,
                                    lineHeight = 24.sp
                                )
                            } else {
                                Text(
                                    "Sin respuesta de texto",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = GrisMedio,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    }

                    // Archivos adjuntos
                    if (entregaActual.archivos.isNotEmpty()) {
                        item {
                            InfoSection(
                                title = "Archivos adjuntos (${entregaActual.archivos.size})",
                                icon = Icons.Default.AttachFile
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    entregaActual.archivos.forEach { archivo ->
                                        ArchivoEntregaCardClickeable(archivo = archivo)
                                    }
                                }
                            }
                        }
                    }

                    // Comentario del profesor (si existe)
                    val comentarioProfesor = entregaActual.comentario
                    if (comentarioProfesor != null) {
                        item {
                            InfoSection(
                                title = "Comentario del profesor",
                                icon = Icons.Default.Comment
                            ) {
                                Text(
                                    comentarioProfesor,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = GrisOscuro,
                                    lineHeight = 24.sp
                                )
                            }
                        }
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    // Diálogo para calificar
    if (showCalificarDialog) {
        val entregaActual = entrega
        AlertDialog(
            onDismissRequest = { showCalificarDialog = false },
            title = {
                Text(
                    if (entregaActual?.nota == null) "Calificar entrega" else "Editar calificación",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Campo de nota
                    OutlinedTextField(
                        value = nota,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.toDoubleOrNull()?.let { it in 0.0..100.0 } == true) {
                                nota = newValue
                            }
                        },
                        label = { Text("Nota (0 - 100)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Star, null, tint = VerdeLima)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VerdeLima,
                            focusedLabelColor = VerdeLima,
                            cursorColor = VerdeLima
                        )
                    )

                    // Campo de comentario
                    OutlinedTextField(
                        value = comentario,
                        onValueChange = { comentario = it },
                        label = { Text("Comentario (opcional)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        maxLines = 4,
                        leadingIcon = {
                            Icon(
                                Icons.Default.Comment,
                                null,
                                tint = AzulCielo,
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .size(24.dp)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AzulCielo,
                            focusedLabelColor = AzulCielo,
                            cursorColor = AzulCielo
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val notaDouble = nota.toDoubleOrNull()
                        if (notaDouble == null || notaDouble < 0.0 || notaDouble > 100.0) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Nota inválida. Debe estar entre 0 y 100")
                            }
                            return@Button
                        }

                        val comentarioFinal = comentario.ifBlank { null }

                        scope.launch {
                            isCalificando = true
                            try {
                                val docenteId = getUserIdFromToken(token)

                                android.util.Log.d("CALIFICAR_ENTREGA", "Token: ${token.take(50)}...")
                                android.util.Log.d("CALIFICAR_ENTREGA", "DocenteId: $docenteId")
                                android.util.Log.d("CALIFICAR_ENTREGA", "EntregaId: $entregaId")
                                android.util.Log.d("CALIFICAR_ENTREGA", "Nota: $notaDouble")

                                if (docenteId.isEmpty()) {
                                    snackbarHostState.showSnackbar("Error: No se pudo obtener el ID del docente")
                                    return@launch
                                }

                                val response = ApiService.calificarEntrega(
                                    token = token,
                                    entregaId = entregaId,
                                    nota = notaDouble,
                                    comentario = comentarioFinal,
                                    docenteId = docenteId
                                )

                                android.util.Log.d("CALIFICAR_ENTREGA", "Response code: ${response.code()}")

                                if (response.isSuccessful) {
                                    snackbarHostState.showSnackbar("Entrega calificada exitosamente")
                                    showCalificarDialog = false
                                    cargarEntrega()
                                } else {
                                    val errorBody = response.errorBody()?.string()
                                    android.util.Log.e("CALIFICAR_ENTREGA", "Error: $errorBody")
                                    snackbarHostState.showSnackbar("Error al calificar: ${response.code()}")
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                android.util.Log.e("CALIFICAR_ENTREGA", "Exception: ${e.message}", e)
                                snackbarHostState.showSnackbar("Error: ${e.message}")
                            } finally {
                                isCalificando = false
                            }
                        }
                    },
                    enabled = !isCalificando,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VerdeLima
                    )
                ) {
                    if (isCalificando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(if (entregaActual?.nota == null) "Calificar" else "Actualizar")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCalificarDialog = false },
                    enabled = !isCalificando
                ) {
                    Text("Cancelar", color = GrisMedio)
                }
            }
        )
    }
}

// ==================== COMPONENTES ====================

@Composable
fun InfoSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = FondoCard,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = AzulCielo,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GrisOscuro
                )
            }
            content()
        }
    }
}

@Composable
fun ArchivoEntregaCardClickeable(
    archivo: ArchivoAdjuntoEntrega
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                abrirArchivo(context, archivo.url)
            },
        shape = RoundedCornerShape(8.dp),
        color = AzulCielo.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, AzulCielo.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = AzulCielo.copy(alpha = 0.15f)
            ) {
                Icon(
                    when {
                        archivo.tipoArchivo.contains("image") -> Icons.Default.Image
                        archivo.tipoArchivo.contains("video") -> Icons.Default.VideoLibrary
                        archivo.tipoArchivo.contains("pdf") -> Icons.Default.PictureAsPdf
                        else -> Icons.Default.InsertDriveFile
                    },
                    contentDescription = null,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp),
                    tint = AzulCielo
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    archivo.nombreOriginal,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = GrisOscuro,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${formatearTamano(archivo.tamano)} • ${archivo.tipoArchivo}",
                    style = MaterialTheme.typography.bodySmall,
                    color = GrisMedio
                )
            }

            Icon(
                Icons.Default.OpenInNew,
                contentDescription = "Abrir",
                tint = AzulCielo,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ==================== FUNCIONES AUXILIARES ====================

fun abrirArchivo(context: android.content.Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                setPackage("com.android.chrome")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun getUserIdFromToken(token: String): String {
    return try {
        val cleanToken = token.removePrefix("Bearer ").trim()
        val parts = cleanToken.split(".")

        if (parts.size == 3) {
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP))
            val json = com.google.gson.JsonParser.parseString(payload).asJsonObject
            val userId = json.get("userId")?.asString ?: ""

            android.util.Log.d("JWT_DECODE", "Extracted userId: $userId")
            userId
        } else {
            android.util.Log.e("JWT_DECODE", "Invalid token format - parts: ${parts.size}")
            ""
        }
    } catch (e: Exception) {
        android.util.Log.e("JWT_DECODE", "Error decoding token: ${e.message}", e)
        ""
    }
}

fun formatearFechaEntrega(fecha: String): String {
    return try {
        val sdfInput = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
        sdfInput.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val date = sdfInput.parse(fecha)

        val sdfOutput = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale("es", "ES"))
        sdfOutput.format(date!!)
    } catch (e: Exception) {
        fecha
    }
}

fun formatearTamano(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }
}

// ==================== MODELO DE DATOS ====================

data class DetalleEntrega(
    val id: String,
    val estudianteNombre: String,
    val estudianteFoto: String?,
    val tareaTitulo: String,
    val textoRespuesta: String,
    val fechaEntrega: String?,
    val estado: String,
    val nota: Double?,
    val comentario: String?,
    val archivos: List<ArchivoAdjuntoEntrega>
)