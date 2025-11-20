package com.example.edumonjetcompose

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.edumonjetcompose.models.ArchivoForo
import com.example.edumonjetcompose.models.DocenteInfo
import com.example.edumonjetcompose.models.Foro
import com.example.edumonjetcompose.models.MensajeForo
import com.example.edumonjetcompose.models.UsuarioInfo
import com.example.edumonjetcompose.network.ApiService
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import java.text.SimpleDateFormat
import java.util.*

// ==================== FUNCIONES AUXILIARES (PRIMERO) ====================

fun parseMensaje(mensajeObj: JsonObject): MensajeForo {
    val usuarioObj = mensajeObj.getAsJsonObject("usuarioId")

    val likesLista = try {
        val likedByElement = mensajeObj.get("likedBy")
        when {
            likedByElement != null && likedByElement.isJsonArray -> {
                likedByElement.asJsonArray.mapNotNull {
                    try { it.asString } catch (e: Exception) { null }
                }
            }
            else -> emptyList()
        }
    } catch (e: Exception) {
        emptyList()
    }

    return MensajeForo(
        id = mensajeObj.get("_id")?.asString ?: "",
        contenido = mensajeObj.get("contenido")?.asString ?: "",
        usuarioId = UsuarioInfo(
            id = usuarioObj.get("_id")?.asString ?: "",
            nombre = usuarioObj.get("nombre")?.asString ?: "Usuario",
            apellido = usuarioObj.get("apellido")?.asString ?: "",
            fotoPerfilUrl = usuarioObj.get("fotoPerfilUrl")?.asString,
            rol = usuarioObj.get("rol")?.asString ?: "padre"
        ),
        fechaCreacion = mensajeObj.get("fechaCreacion")?.asString ?: "",
        likes = likesLista,
        archivos = mensajeObj.getAsJsonArray("archivos")?.mapNotNull { archivo ->
            try {
                val archivoObj = archivo.asJsonObject
                ArchivoForo(
                    url = archivoObj.get("url")?.asString ?: "",
                    tipo = archivoObj.get("tipo")?.asString ?: "otro",
                    nombre = archivoObj.get("nombre")?.asString ?: "Archivo"
                )
            } catch (e: Exception) {
                null
            }
        } ?: emptyList(),
        respuestas = mensajeObj.getAsJsonArray("respuestas")?.mapNotNull {
            try {
                parseMensaje(it.asJsonObject)
            } catch (e: Exception) {
                null
            }
        } ?: emptyList()
    )
}

fun formatearFecha(fechaStr: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val fecha = inputFormat.parse(fechaStr)

        val ahora = Date()
        val diferencia = ahora.time - (fecha?.time ?: 0)

        when {
            diferencia < 60000 -> "Hace un momento"
            diferencia < 3600000 -> "Hace ${diferencia / 60000} min"
            diferencia < 86400000 -> "Hace ${diferencia / 3600000} h"
            diferencia < 604800000 -> {
                val dias = diferencia / 86400000
                if (dias == 1L) "Ayer" else "Hace $dias días"
            }
            else -> {
                val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                outputFormat.format(fecha ?: Date())
            }
        }
    } catch (e: Exception) {
        fechaStr
    }
}

// ==================== COMPONENTES AUXILIARES ====================

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Reintentar")
        }
    }
}

@Composable
fun EmptyForosView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Forum,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No hay foros disponibles",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Los foros del curso aparecerán aquí",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EstadoForoBadge(estado: String) {
    val (backgroundColor, textColor, icon) = when (estado) {
        "abierto" -> Triple(
            Color(0xFF4CAF50).copy(alpha = 0.15f),
            Color(0xFF2E7D32),
            Icons.Default.Lock
        )
        else -> Triple(
            Color(0xFFF44336).copy(alpha = 0.15f),
            Color(0xFFC62828),
            Icons.Default.Lock
        )
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = textColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = estado.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ImagenesPreview(imagenes: List<ArchivoForo>) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        imagenes.forEach { imagen ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imagen.url)
                    .crossfade(true)
                    .build(),
                contentDescription = imagen.nombre,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun ArchivoSeleccionadoPreview(
    uri: Uri,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(70.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(uri)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
                .padding(2.dp)
                .background(Color.Black.copy(alpha = 0.7f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Eliminar",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun ArchivoChip(
    archivo: ArchivoForo,
    small: Boolean = false
) {
    val context = LocalContext.current

    val icon = when (archivo.tipo) {
        "imagen" -> Icons.Default.Image
        "video" -> Icons.Default.VideoLibrary
        "pdf" -> Icons.Default.Description
        "documento" -> Icons.Default.Description
        else -> Icons.Default.AttachFile
    }

    Surface(
        shape = RoundedCornerShape(if (small) 8.dp else 12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(archivo.url))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Error al abrir
                    }
                }
                .padding(
                    horizontal = if (small) 10.dp else 14.dp,
                    vertical = if (small) 8.dp else 10.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = archivo.tipo,
                modifier = Modifier.size(if (small) 18.dp else 22.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = archivo.nombre,
                    style = if (small) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = archivo.tipo.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = "Abrir",
                modifier = Modifier.size(if (small) 16.dp else 20.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun ImagenesGrid(imagenes: List<ArchivoForo>, small: Boolean = false) {
    val context = LocalContext.current

    when (imagenes.size) {
        1 -> {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imagenes[0].url)
                    .crossfade(true)
                    .build(),
                contentDescription = imagenes[0].nombre,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = if (small) 150.dp else 250.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(imagenes[0].url))
                            context.startActivity(intent)
                        } catch (e: Exception) { }
                    },
                contentScale = ContentScale.Crop
            )
        }
        2 -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                imagenes.forEach { imagen ->
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imagen.url)
                            .crossfade(true)
                            .build(),
                        contentDescription = imagen.nombre,
                        modifier = Modifier
                            .weight(1f)
                            .height(if (small) 100.dp else 150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(imagen.url))
                                    context.startActivity(intent)
                                } catch (e: Exception) { }
                            },
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        else -> {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    imagenes.take(2).forEach { imagen ->
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imagen.url)
                                .crossfade(true)
                                .build(),
                            contentDescription = imagen.nombre,
                            modifier = Modifier
                                .weight(1f)
                                .height(if (small) 80.dp else 120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(imagen.url))
                                        context.startActivity(intent)
                                    } catch (e: Exception) { }
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                if (imagenes.size > 2) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (small) 80.dp else 120.dp)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imagenes[2].url)
                                .crossfade(true)
                                .build(),
                            contentDescription = imagenes[2].nombre,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(imagenes[2].url))
                                        context.startActivity(intent)
                                    } catch (e: Exception) { }
                                },
                            contentScale = ContentScale.Crop
                        )

                        if (imagenes.size > 3) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .clip(RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+${imagenes.size - 3}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
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
fun ForoHeaderCard(foro: Foro) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = foro.titulo,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${foro.docenteId.nombre} ${foro.docenteId.apellido}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                EstadoForoBadge(estado = foro.estado)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = foro.descripcion,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            if (foro.archivos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Archivos adjuntos:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(8.dp))

                val imagenes = foro.archivos.filter { it.tipo == "imagen" }
                if (imagenes.isNotEmpty()) {
                    ImagenesGrid(imagenes = imagenes)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                foro.archivos.forEach { archivo ->
                    ArchivoChip(archivo = archivo)
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

// ==================== PANTALLA PRINCIPAL DE FOROS ====================
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
    var isRefreshing by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun cargarForos() {
        scope.launch {
            try {
                if (!isRefreshing) isLoading = true
                errorMessage = null

                val response = ApiService.getForosPorCurso(token, cursoId)

                if (response.isSuccessful) {
                    val body = response.body()
                    val forosArray = body?.getAsJsonArray("foros")

                    foros = forosArray?.mapNotNull { element ->
                        try {
                            val foroObj = element.asJsonObject
                            val docenteObj = foroObj.getAsJsonObject("docenteId")

                            Foro(
                                id = foroObj.get("_id").asString,
                                titulo = foroObj.get("titulo")?.asString ?: "Sin título",
                                descripcion = foroObj.get("descripcion")?.asString ?: "",
                                estado = foroObj.get("estado")?.asString ?: "cerrado",
                                docenteId = DocenteInfo(
                                    id = docenteObj.get("_id").asString,
                                    nombre = docenteObj.get("nombre")?.asString ?: "",
                                    apellido = docenteObj.get("apellido")?.asString ?: "",
                                    fotoPerfilUrl = docenteObj.get("fotoPerfilUrl")?.asString,
                                    rol = docenteObj.get("rol")?.asString ?: "docente"
                                ),
                                fechaCreacion = foroObj.get("fechaCreacion")?.asString ?: "",
                                totalMensajes = foroObj.get("totalMensajes")?.asInt ?: 0,
                                archivos = foroObj.getAsJsonArray("archivos")?.mapNotNull { archivo ->
                                    try {
                                        val archivoObj = archivo.asJsonObject
                                        ArchivoForo(
                                            url = archivoObj.get("url")?.asString ?: "",
                                            tipo = archivoObj.get("tipo")?.asString ?: "otro",
                                            nombre = archivoObj.get("nombre")?.asString ?: "Archivo"
                                        )
                                    } catch (e: Exception) {
                                        null
                                    }
                                } ?: emptyList()
                            )
                        } catch (e: Exception) {
                            null
                        }
                    } ?: emptyList()

                    if (isRefreshing) {
                        snackbarHostState.showSnackbar("Foros actualizados")
                    }
                } else {
                    errorMessage = "Error al cargar los foros: ${response.code()}"
                }
            } catch (e: Exception) {
                errorMessage = "Error: ${e.localizedMessage}"
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(cursoId) {
        cargarForos()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Foros del Curso")
                        if (foros.isNotEmpty()) {
                            Text(
                                text = "${foros.size} foro(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isRefreshing = true
                            cargarForos()
                        },
                        enabled = !isLoading && !isRefreshing
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading && !isRefreshing -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Cargando foros...")
                    }
                }
                errorMessage != null -> {
                    ErrorView(
                        message = errorMessage ?: "Error desconocido",
                        onRetry = { cargarForos() }
                    )
                }
                foros.isEmpty() -> {
                    EmptyForosView()
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(
                            items = foros,
                            key = { it.id }
                        ) { foro ->
                            ForoCard(
                                foro = foro,
                                onClick = {
                                    navController.navigate("foro_detalle/${foro.id}")
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }

            if (isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }
        }
    }
}

// ==================== TARJETA DE FORO ====================
@Composable
fun ForoCard(
    foro: Foro,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(foro.docenteId.fotoPerfilUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Foto docente",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "${foro.docenteId.nombre} ${foro.docenteId.apellido}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatearFecha(foro.fechaCreacion),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                EstadoForoBadge(estado = foro.estado)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = foro.titulo,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = foro.descripcion,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (foro.archivos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                val imagenesEnForo = foro.archivos.filter { it.tipo == "imagen" }
                if (imagenesEnForo.isNotEmpty()) {
                    ImagenesPreview(imagenes = imagenesEnForo.take(3))
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Attachment,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${foro.archivos.size} archivo(s) adjunto(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Divider(color = MaterialTheme.colorScheme.outlineVariant)

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Forum,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${foro.totalMensajes} mensaje(s)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                TextButton(onClick = onClick) {
                    Text("Ver foro")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ==================== PANTALLA DE DETALLE DEL FORO ====================
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

    var mostrarModalRespuesta by remember { mutableStateOf(false) }
    var mensajeAResponder by remember { mutableStateOf<MensajeForo?>(null) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    fun cargarDatos() {
        scope.launch {
            try {
                isLoading = true
                errorMessage = null

                val foroDeferred = async {
                    ApiService.getForoById(token, foroId)
                }
                val mensajesDeferred = async {
                    ApiService.getMensajesForo(token, foroId)
                }

                val foroResponse = foroDeferred.await()
                val mensajesResponse = mensajesDeferred.await()

                if (foroResponse.isSuccessful) {
                    val foroObj = foroResponse.body()?.getAsJsonObject("foro")
                    foroObj?.let {
                        val docenteObj = it.getAsJsonObject("docenteId")
                        foro = Foro(
                            id = it.get("_id").asString,
                            titulo = it.get("titulo")?.asString ?: "Sin título",
                            descripcion = it.get("descripcion")?.asString ?: "",
                            estado = it.get("estado")?.asString ?: "cerrado",
                            docenteId = DocenteInfo(
                                id = docenteObj.get("_id").asString,
                                nombre = docenteObj.get("nombre")?.asString ?: "",
                                apellido = docenteObj.get("apellido")?.asString ?: "",
                                fotoPerfilUrl = docenteObj.get("fotoPerfilUrl")?.asString,
                                rol = docenteObj.get("rol")?.asString ?: "docente"
                            ),
                            fechaCreacion = it.get("fechaCreacion")?.asString ?: "",
                            totalMensajes = it.get("totalMensajes")?.asInt ?: 0,
                            archivos = it.getAsJsonArray("archivos")?.mapNotNull { archivo ->
                                try {
                                    val archivoObj = archivo.asJsonObject
                                    ArchivoForo(
                                        url = archivoObj.get("url")?.asString ?: "",
                                        tipo = archivoObj.get("tipo")?.asString ?: "otro",
                                        nombre = archivoObj.get("nombre")?.asString ?: "Archivo"
                                    )
                                } catch (e: Exception) {
                                    null
                                }
                            } ?: emptyList()
                        )
                    }
                }

                if (mensajesResponse.isSuccessful) {
                    val mensajesArray = mensajesResponse.body()?.getAsJsonArray("mensajes")

                    mensajes = mensajesArray?.mapNotNull {
                        try {
                            parseMensaje(it.asJsonObject)
                        } catch (e: Exception) {
                            null
                        }
                    } ?: emptyList()
                }

            } catch (e: Exception) {
                errorMessage = "Error: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(foroId) {
        cargarDatos()
    }

    if (mostrarModalRespuesta) {
        ModalRespuesta(
            foro = foro,
            mensajeAResponder = mensajeAResponder,
            token = token,
            foroId = foroId,
            context = context,
            onDismiss = {
                mostrarModalRespuesta = false
                mensajeAResponder = null
            },
            onEnviado = {
                mostrarModalRespuesta = false
                mensajeAResponder = null
                cargarDatos()
                scope.launch {
                    snackbarHostState.showSnackbar("Mensaje enviado")
                    listState.animateScrollToItem(mensajes.size)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = foro?.titulo ?: "Cargando...",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${mensajes.size} mensaje(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { cargarDatos() }) {
                        Icon(Icons.Default.Refresh, "Actualizar")
                    }
                }
            )
        },
        floatingActionButton = {
            if (foro?.estado == "abierto") {
                ExtendedFloatingActionButton(
                    onClick = {
                        mostrarModalRespuesta = true
                        mensajeAResponder = null
                    },
                    icon = {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Escribir mensaje"
                        )
                    },
                    text = { Text("Escribir") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Cargando foro...")
                    }
                }
                errorMessage != null -> {
                    ErrorView(
                        message = errorMessage ?: "Error desconocido",
                        onRetry = { cargarDatos() }
                    )
                }
                foro == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Foro no encontrado",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            ForoHeaderCard(foro = foro!!)
                        }

                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Forum,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Mensajes (${mensajes.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (mensajes.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ChatBubbleOutline,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "No hay mensajes aún",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Sé el primero en participar",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        } else {
                            items(
                                items = mensajes,
                                key = { it.id }
                            ) { mensaje ->
                                MensajeCard(
                                    mensaje = mensaje,
                                    userId = userId,
                                    onResponder = {
                                        mensajeAResponder = mensaje
                                        mostrarModalRespuesta = true
                                    },
                                    onLike = {
                                        scope.launch {
                                            try {
                                                val response = ApiService.toggleLikeMensaje(token, mensaje.id)
                                                if (response.isSuccessful) {
                                                    cargarDatos()
                                                }
                                            } catch (e: Exception) {
                                                snackbarHostState.showSnackbar("Error: ${e.localizedMessage}")
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }
}

// ==================== MODAL PARA RESPONDER ====================
@Composable
fun ModalRespuesta(
    foro: Foro?,
    mensajeAResponder: MensajeForo?,
    token: String,
    foroId: String,
    context: android.content.Context,
    onDismiss: () -> Unit,
    onEnviado: () -> Unit
) {
    var nuevoMensaje by remember { mutableStateOf("") }
    var archivosSeleccionados by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var enviandoMensaje by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        archivosSeleccionados = uris
    }

    Dialog(
        onDismissRequest = { if (!enviandoMensaje) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !enviandoMensaje,
            dismissOnClickOutside = !enviandoMensaje
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (mensajeAResponder != null) "Responder mensaje" else "Nuevo mensaje",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = foro?.titulo ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !enviandoMensaje
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Divider()

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(20.dp)
                ) {
                    if (mensajeAResponder != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Reply,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Respondiendo a ${mensajeAResponder.usuarioId.nombre}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = mensajeAResponder.contenido,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    OutlinedTextField(
                        value = nuevoMensaje,
                        onValueChange = { nuevoMensaje = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        placeholder = {
                            Text(
                                "Escribe tu mensaje aquí...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        enabled = !enviandoMensaje,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    AnimatedVisibility(
                        visible = archivosSeleccionados.isNotEmpty(),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Text(
                                text = "Archivos adjuntos (${archivosSeleccionados.size})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val scrollState = rememberScrollState()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(scrollState),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                archivosSeleccionados.forEach { uri ->
                                    ArchivoSeleccionadoPreview(
                                        uri = uri,
                                        onRemove = {
                                            archivosSeleccionados = archivosSeleccionados.filter { it != uri }
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    OutlinedButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !enviandoMensaje
                    ) {
                        Icon(
                            Icons.Default.AttachFile,
                            contentDescription = "Adjuntar archivo",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Adjuntar archivos")
                    }
                }

                Divider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !enviandoMensaje
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            if (nuevoMensaje.isNotBlank()) {
                                scope.launch {
                                    try {
                                        enviandoMensaje = true
                                        val response = ApiService.crearMensajeForo(
                                            token = token,
                                            foroId = foroId,
                                            contenido = nuevoMensaje,
                                            respuestaA = mensajeAResponder?.id,
                                            archivos = archivosSeleccionados,
                                            context = context
                                        )

                                        if (response.isSuccessful) {
                                            onEnviado()
                                        }
                                    } catch (e: Exception) {
                                        // Error
                                    } finally {
                                        enviandoMensaje = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = nuevoMensaje.isNotBlank() && !enviandoMensaje
                    ) {
                        if (enviandoMensaje) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "Enviar",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enviar")
                        }
                    }
                }
            }
        }
    }
}

// ==================== MENSAJE CARD ====================
@Composable
fun MensajeCard(
    mensaje: MensajeForo,
    userId: String,
    onResponder: () -> Unit,
    onLike: () -> Unit
) {
    val isPropio = mensaje.usuarioId.id == userId
    val yaLeDioLike = mensaje.likes.contains(userId)

    var animateLike by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (animateLike) 1.3f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        finishedListener = { animateLike = false }
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPropio)
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(mensaje.usuarioId.fotoPerfilUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Foto usuario",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${mensaje.usuarioId.nombre} ${mensaje.usuarioId.apellido}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )

                        if (mensaje.usuarioId.rol == "docente") {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "DOCENTE",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (isPropio) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Tu mensaje",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatearFecha(mensaje.fechaCreacion),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = mensaje.contenido,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (mensaje.archivos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                val imagenes = mensaje.archivos.filter { it.tipo == "imagen" }
                if (imagenes.isNotEmpty()) {
                    ImagenesGrid(imagenes = imagenes)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                mensaje.archivos.filter { it.tipo != "imagen" }.forEach { archivo ->
                    ArchivoChip(archivo = archivo)
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            animateLike = true
                            onLike()
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (yaLeDioLike)
                                Icons.Default.ThumbUp
                            else
                                Icons.Default.ThumbUpOffAlt,
                            contentDescription = "Me gusta",
                            tint = if (yaLeDioLike)
                                Color(0xFF2196F3)
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(20.dp)
                                .scale(scale)
                        )
                    }
                    if (mensaje.likes.isNotEmpty()) {
                        Text(
                            text = "${mensaje.likes.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (yaLeDioLike)
                                Color(0xFF2196F3)
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Button(
                    onClick = onResponder,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Reply,
                        contentDescription = "Responder",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Responder", fontWeight = FontWeight.SemiBold)
                }
            }

            if (mensaje.respuestas.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "${mensaje.respuestas.size} respuesta(s):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    mensaje.respuestas.forEach { respuesta ->
                        RespuestaCard(
                            respuesta = respuesta,
                            userId = userId,
                            onLike = onLike
                        )
                    }
                }
            }
        }
    }
}

// ==================== RESPUESTA CARD ====================
@Composable
fun RespuestaCard(
    respuesta: MensajeForo,
    userId: String,
    onLike: () -> Unit
) {
    val isPropio = respuesta.usuarioId.id == userId
    val yaLeDioLike = respuesta.likes.contains(userId)

    var animateLike by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (animateLike) 1.3f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        finishedListener = { animateLike = false }
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isPropio)
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(respuesta.usuarioId.fotoPerfilUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Foto usuario",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${respuesta.usuarioId.nombre} ${respuesta.usuarioId.apellido}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (isPropio) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Tu respuesta",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        text = formatearFecha(respuesta.fechaCreacion),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = respuesta.contenido,
                style = MaterialTheme.typography.bodyMedium
            )

            if (respuesta.archivos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))

                val imagenes = respuesta.archivos.filter { it.tipo == "imagen" }
                if (imagenes.isNotEmpty()) {
                    ImagenesGrid(imagenes = imagenes, small = true)
                    Spacer(modifier = Modifier.height(6.dp))
                }

                respuesta.archivos.filter { it.tipo != "imagen" }.forEach { archivo ->
                    ArchivoChip(archivo = archivo, small = true)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            if (respuesta.likes.isNotEmpty() || true) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            animateLike = true
                            onLike()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (yaLeDioLike)
                                Icons.Default.ThumbUp
                            else
                                Icons.Default.ThumbUpOffAlt,
                            contentDescription = "Me gusta",
                            tint = if (yaLeDioLike)
                                Color(0xFF2196F3)
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(16.dp)
                                .scale(scale)
                        )
                    }
                    if (respuesta.likes.isNotEmpty()) {
                        Text(
                            text = "${respuesta.likes.size}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (yaLeDioLike)
                                Color(0xFF2196F3)
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}