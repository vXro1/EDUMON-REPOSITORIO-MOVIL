package com.example.edumonjetcompose


import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.edumonjetcompose.network.ApiService
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Data classes
data class Foro(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val estado: String,
    val docenteId: DocenteInfo,
    val fechaCreacion: String,
    val totalMensajes: Int,
    val archivos: List<ArchivoForo>
)

data class DocenteInfo(
    val id: String,
    val nombre: String,
    val apellido: String,
    val fotoPerfilUrl: String?,
    val rol: String
)

data class ArchivoForo(
    val url: String,
    val tipo: String,
    val nombre: String
)

data class MensajeForo(
    val id: String,
    val contenido: String,
    val usuarioId: UsuarioInfo,
    val fechaCreacion: String,
    val likes: List<String>,
    val archivos: List<ArchivoForo>,
    val respuestas: List<MensajeForo>
)

data class UsuarioInfo(
    val id: String,
    val nombre: String,
    val apellido: String,
    val fotoPerfilUrl: String?,
    val rol: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForoScreen(
    navController: NavController,
    cursoId: String,
    token: String,
    userId: String
) {
    var foros by remember { mutableStateOf<List<Foro>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Cargar foros
    LaunchedEffect(cursoId) {
        scope.launch {
            try {
                isLoading = true
                errorMessage = null

                val response = ApiService.getForosPorCurso(token, cursoId)

                if (response.isSuccessful) {
                    val body = response.body()
                    val forosArray = body?.getAsJsonArray("foros")

                    foros = forosArray?.map { element ->
                        val foroObj = element.asJsonObject
                        val docenteObj = foroObj.getAsJsonObject("docenteId")

                        Foro(
                            id = foroObj.get("_id").asString,
                            titulo = foroObj.get("titulo").asString,
                            descripcion = foroObj.get("descripcion").asString,
                            estado = foroObj.get("estado").asString,
                            docenteId = DocenteInfo(
                                id = docenteObj.get("_id").asString,
                                nombre = docenteObj.get("nombre").asString,
                                apellido = docenteObj.get("apellido").asString,
                                fotoPerfilUrl = docenteObj.get("fotoPerfilUrl")?.asString,
                                rol = docenteObj.get("rol").asString
                            ),
                            fechaCreacion = foroObj.get("fechaCreacion").asString,
                            totalMensajes = foroObj.get("totalMensajes")?.asInt ?: 0,
                            archivos = foroObj.getAsJsonArray("archivos")?.map { archivo ->
                                val archivoObj = archivo.asJsonObject
                                ArchivoForo(
                                    url = archivoObj.get("url").asString,
                                    tipo = archivoObj.get("tipo").asString,
                                    nombre = archivoObj.get("nombre").asString
                                )
                            } ?: emptyList()
                        )
                    } ?: emptyList()
                } else {
                    errorMessage = "Error al cargar los foros"
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Foros del Curso") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = errorMessage ?: "Error desconocido",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                // Recargar
                            }
                        }) {
                            Text("Reintentar")
                        }
                    }
                }
                foros.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forum,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No hay foros disponibles",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(foros) { foro ->
                            ForoCard(
                                foro = foro,
                                onClick = {
                                    navController.navigate("foro_detalle/${foro.id}")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ForoCard(
    foro: Foro,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header con docente
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = foro.docenteId.fotoPerfilUrl,
                    contentDescription = "Foto docente",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${foro.docenteId.nombre} ${foro.docenteId.apellido}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = formatearFecha(foro.fechaCreacion),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Estado del foro
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (foro.estado == "abierto")
                        Color(0xFF4CAF50).copy(alpha = 0.1f)
                    else
                        Color(0xFFF44336).copy(alpha = 0.1f)
                ) {
                    Text(
                        text = foro.estado.uppercase(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (foro.estado == "abierto") Color(0xFF4CAF50) else Color(0xFFF44336),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Título y descripción
            Text(
                text = foro.titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = foro.descripcion,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Archivos adjuntos
            if (foro.archivos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Attachment,
                        contentDescription = "Archivos",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${foro.archivos.size} archivo(s) adjunto(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer con contador de mensajes
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Forum,
                    contentDescription = "Mensajes",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${foro.totalMensajes} mensaje(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Pantalla de detalle del foro
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForoDetalleScreen(
    navController: NavController,
    foroId: String,
    token: String,
    userId: String
) {
    var foro by remember { mutableStateOf<Foro?>(null) }
    var mensajes by remember { mutableStateOf<List<MensajeForo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var nuevoMensaje by remember { mutableStateOf("") }
    var respuestaA by remember { mutableStateOf<String?>(null) }
    var enviandoMensaje by remember { mutableStateOf(false) }

    var archivosSeleccionados by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        archivosSeleccionados = uris
    }

    // Cargar foro y mensajes
    fun cargarDatos() {
        scope.launch {
            try {
                isLoading = true
                errorMessage = null

                // Cargar foro
                val foroResponse = ApiService.getForoById(token, foroId)
                if (foroResponse.isSuccessful) {
                    val foroObj = foroResponse.body()?.getAsJsonObject("foro")
                    foroObj?.let {
                        val docenteObj = it.getAsJsonObject("docenteId")
                        foro = Foro(
                            id = it.get("_id").asString,
                            titulo = it.get("titulo").asString,
                            descripcion = it.get("descripcion").asString,
                            estado = it.get("estado").asString,
                            docenteId = DocenteInfo(
                                id = docenteObj.get("_id").asString,
                                nombre = docenteObj.get("nombre").asString,
                                apellido = docenteObj.get("apellido").asString,
                                fotoPerfilUrl = docenteObj.get("fotoPerfilUrl")?.asString,
                                rol = docenteObj.get("rol").asString
                            ),
                            fechaCreacion = it.get("fechaCreacion").asString,
                            totalMensajes = it.get("totalMensajes")?.asInt ?: 0,
                            archivos = it.getAsJsonArray("archivos")?.map { archivo ->
                                val archivoObj = archivo.asJsonObject
                                ArchivoForo(
                                    url = archivoObj.get("url").asString,
                                    tipo = archivoObj.get("tipo").asString,
                                    nombre = archivoObj.get("nombre").asString
                                )
                            } ?: emptyList()
                        )
                    }
                }

                // Cargar mensajes
                val mensajesResponse = ApiService.getMensajesForo(token, foroId)
                if (mensajesResponse.isSuccessful) {
                    val mensajesArray = mensajesResponse.body()?.getAsJsonArray("mensajes")
                    mensajes = mensajesArray?.map { parseMensaje(it.asJsonObject) } ?: emptyList()
                }

            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(foroId) {
        cargarDatos()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = foro?.titulo ?: "Cargando...",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                }
            )
        },
        bottomBar = {
            // Solo mostrar si el foro está abierto
            if (foro?.estado == "abierto") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Indicador de respuesta
                        if (respuestaA != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Reply,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Respondiendo...",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                IconButton(
                                    onClick = { respuestaA = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancelar",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Archivos seleccionados
                        if (archivosSeleccionados.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                archivosSeleccionados.forEach { uri ->
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        AsyncImage(
                                            model = uri,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        IconButton(
                                            onClick = {
                                                archivosSeleccionados = archivosSeleccionados.filter { it != uri }
                                            },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(20.dp)
                                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Eliminar",
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            IconButton(
                                onClick = { filePickerLauncher.launch("*/*") }
                            ) {
                                Icon(Icons.Default.AttachFile, "Adjuntar archivo")
                            }

                            OutlinedTextField(
                                value = nuevoMensaje,
                                onValueChange = { nuevoMensaje = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Escribe un mensaje...") },
                                maxLines = 4,
                                enabled = !enviandoMensaje
                            )

                            IconButton(
                                onClick = {
                                    if (nuevoMensaje.isNotBlank()) {
                                        scope.launch {
                                            try {
                                                enviandoMensaje = true
                                                val response = ApiService.crearMensajeForo(
                                                    token = token,
                                                    foroId = foroId,
                                                    contenido = nuevoMensaje,
                                                    respuestaA = respuestaA,
                                                    archivos = archivosSeleccionados,
                                                    context = context
                                                )

                                                if (response.isSuccessful) {
                                                    nuevoMensaje = ""
                                                    respuestaA = null
                                                    archivosSeleccionados = emptyList()
                                                    cargarDatos()
                                                }
                                            } catch (e: Exception) {
                                                errorMessage = e.message
                                            } finally {
                                                enviandoMensaje = false
                                            }
                                        }
                                    }
                                },
                                enabled = nuevoMensaje.isNotBlank() && !enviandoMensaje
                            ) {
                                if (enviandoMensaje) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                } else {
                                    Icon(Icons.Default.Send, "Enviar")
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = errorMessage ?: "Error desconocido",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { cargarDatos() }) {
                            Text("Reintentar")
                        }
                    }
                }
                foro == null -> {
                    Text(
                        text = "Foro no encontrado",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header del foro
                        item {
                            ForoHeaderCard(foro = foro!!)
                        }

                        // Mensajes
                        items(mensajes) { mensaje ->
                            MensajeCard(
                                mensaje = mensaje,
                                userId = userId,
                                onResponder = { respuestaA = mensaje.id },
                                onLike = {
                                    scope.launch {
                                        ApiService.toggleLikeMensaje(token, mensaje.id)
                                        cargarDatos()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ForoHeaderCard(foro: Foro) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = foro.titulo,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = foro.descripcion,
                style = MaterialTheme.typography.bodyMedium
            )

            if (foro.archivos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                foro.archivos.forEach { archivo ->
                    ArchivoChip(archivo = archivo)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun MensajeCard(
    mensaje: MensajeForo,
    userId: String,
    onResponder: () -> Unit,
    onLike: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header del mensaje
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = mensaje.usuarioId.fotoPerfilUrl,
                    contentDescription = "Foto usuario",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${mensaje.usuarioId.nombre} ${mensaje.usuarioId.apellido}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = formatearFecha(mensaje.fechaCreacion),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Badge de rol
                if (mensaje.usuarioId.rol == "docente") {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "DOCENTE",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Contenido
            Text(
                text = mensaje.contenido,
                style = MaterialTheme.typography.bodyMedium
            )

            // Archivos
            if (mensaje.archivos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                mensaje.archivos.forEach { archivo ->
                    ArchivoChip(archivo = archivo)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Acciones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    // Like
                    IconButton(onClick = onLike) {
                        Icon(
                            imageVector = if (mensaje.likes.contains(userId))
                                Icons.Default.Favorite
                            else
                                Icons.Default.FavoriteBorder,
                            contentDescription = "Me gusta",
                            tint = if (mensaje.likes.contains(userId))
                                Color.Red
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${mensaje.likes.size}",
                        modifier = Modifier.align(Alignment.CenterVertically),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Responder (solo si es mensaje del docente)
                if (mensaje.usuarioId.rol == "docente") {
                    TextButton(onClick = onResponder) {
                        Icon(
                            imageVector = Icons.Default.Reply,
                            contentDescription = "Responder",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Responder")
                    }
                }
            }

            // Respuestas
            if (mensaje.respuestas.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp)
                ) {
                    mensaje.respuestas.forEach { respuesta ->
                        RespuestaCard(respuesta = respuesta, userId = userId)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun RespuestaCard(
    respuesta: MensajeForo,
    userId: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = respuesta.usuarioId.fotoPerfilUrl,
                    contentDescription = "Foto usuario",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${respuesta.usuarioId.nombre} ${respuesta.usuarioId.apellido}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = formatearFecha(respuesta.fechaCreacion),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = respuesta.contenido,
                style = MaterialTheme.typography.bodySmall
            )

            if (respuesta.archivos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                respuesta.archivos.forEach { archivo ->
                    ArchivoChip(archivo = archivo, small = true)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun ArchivoChip(
    archivo: ArchivoForo,
    small: Boolean = false
) {
    val icon = when (archivo.tipo) {
        "imagen" -> Icons.Default.Image
        "video" -> Icons.Default.VideoLibrary
        "pdf" -> Icons.Default.PictureAsPdf
        else -> Icons.Default.AttachFile
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    // Abrir archivo (implementar según necesidad)
                }
                .padding(horizontal = if (small) 8.dp else 12.dp, vertical = if (small) 6.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = archivo.tipo,
                modifier = Modifier.size(if (small) 16.dp else 20.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = archivo.nombre,
                modifier = Modifier.weight(1f),
                style = if (small) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Descargar",
                modifier = Modifier.size(if (small) 16.dp else 20.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

// Función auxiliar para parsear mensajes del JSON
fun parseMensaje(mensajeObj: JsonObject): MensajeForo {
    val usuarioObj = mensajeObj.getAsJsonObject("usuarioId")

    return MensajeForo(
        id = mensajeObj.get("_id").asString,
        contenido = mensajeObj.get("contenido").asString,
        usuarioId = UsuarioInfo(
            id = usuarioObj.get("_id").asString,
            nombre = usuarioObj.get("nombre").asString,
            apellido = usuarioObj.get("apellido").asString,
            fotoPerfilUrl = usuarioObj.get("fotoPerfilUrl")?.asString,
            rol = usuarioObj.get("rol").asString
        ),
        fechaCreacion = mensajeObj.get("fechaCreacion").asString,
        likes = mensajeObj.getAsJsonArray("likes")?.map { it.asString } ?: emptyList(),
        archivos = mensajeObj.getAsJsonArray("archivos")?.map { archivo ->
            val archivoObj = archivo.asJsonObject
            ArchivoForo(
                url = archivoObj.get("url").asString,
                tipo = archivoObj.get("tipo").asString,
                nombre = archivoObj.get("nombre").asString
            )
        } ?: emptyList(),
        respuestas = mensajeObj.getAsJsonArray("respuestas")?.map {
            parseMensaje(it.asJsonObject)
        } ?: emptyList()
    )
}

// Función para formatear fechas
fun formatearFecha(fechaStr: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val fecha = inputFormat.parse(fechaStr)

        val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        outputFormat.format(fecha ?: Date())
    } catch (e: Exception) {
        fechaStr
    }
}