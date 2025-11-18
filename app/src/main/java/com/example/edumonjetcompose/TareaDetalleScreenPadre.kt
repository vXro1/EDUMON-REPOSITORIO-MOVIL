@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.edumonjetcompose.ui

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.edumonjetcompose.models.ArchivoAdjunto
import com.example.edumonjetcompose.models.Entrega
import com.example.edumonjetcompose.models.TareaDetalle
import com.example.edumonjetcompose.network.ApiService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

const val BASE_URL = "https://backend-edumon.onrender.com"
const val TAG = "TareaDetalleScreen"

@Composable
fun TareaDetalleScreen(
    navController: NavController,
    tareaId: String,
    token: String,
    padreId: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var tarea by remember { mutableStateOf<TareaDetalle?>(null) }
    var miEntrega by remember { mutableStateOf<Entrega?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    fun cargarDatos() {
        scope.launch {
            isLoading = true
            errorMessage = null

            try {
                Log.d(TAG, "📡 Cargando tarea: $tareaId")
                val tareaResponse = ApiService.getTareaById(token, tareaId)

                if (tareaResponse.isSuccessful) {
                    val body = tareaResponse.body()
                    val tareaJson = body?.getAsJsonObject("tarea") ?: body

                    tareaJson?.let { t ->
                        val archivos = mutableListOf<ArchivoAdjunto>()
                        try {
                            val archivosArray = t.getAsJsonArray("archivosAdjuntos")
                            archivosArray?.forEach { elemento ->
                                val obj = elemento.asJsonObject
                                archivos.add(
                                    ArchivoAdjunto(
                                        tipo = obj.get("tipo")?.asString ?: "archivo",
                                        url = obj.get("url")?.asString ?: "",
                                        nombre = obj.get("nombre")?.asString ?: "Archivo",
                                        descripcion = obj.get("descripcion")?.asString
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parseando archivos", e)
                        }

                        tarea = TareaDetalle(
                            id = t.get("_id")?.asString ?: "",
                            titulo = t.get("titulo")?.asString ?: "Sin título",
                            descripcion = t.get("descripcion")?.asString ?: "",
                            fechaCreacion = t.get("fechaCreacion")?.asString ?: "",
                            fechaEntrega = t.get("fechaEntrega")?.asString ?: "",
                            estado = t.get("estado")?.asString ?: "publicada",
                            tipoEntrega = t.get("tipoEntrega")?.asString ?: "texto",
                            criterios = t.get("criterios")?.asString ?: "",
                            archivosAdjuntos = archivos
                        )

                        Log.d(TAG, "✅ Tarea cargada: ${tarea?.titulo}")
                        Log.d(TAG, "📦 Archivos adjuntos: ${archivos.size}")
                    }

                    // Verificar si hay entrega previa
                    try {
                        val entregaResponse = ApiService.getEntregasByPadreAndTarea(token, tareaId)
                        if (entregaResponse.isSuccessful) {
                            val entregaBody = entregaResponse.body()
                            val entregaJson = entregaBody?.getAsJsonObject("entrega")
                                ?: entregaBody?.getAsJsonArray("entregas")?.firstOrNull()?.asJsonObject

                            entregaJson?.let { e ->
                                val archivosEntrega = try {
                                    val arr = e.getAsJsonArray("archivos")
                                    arr?.map { it.asString } ?: emptyList()
                                } catch (ex: Exception) {
                                    emptyList()
                                }

                                miEntrega = Entrega(
                                    id = e.get("_id")?.asString ?: "",
                                    textoRespuesta = e.get("textoRespuesta")?.asString ?: "",
                                    fechaEntrega = e.get("fechaEntrega")?.asString ?: "",
                                    estado = e.get("estado")?.asString ?: "borrador",
                                    nota = e.get("calificacion")?.asJsonObject?.get("nota")?.asString,
                                    comentario = e.get("calificacion")?.asJsonObject?.get("comentario")?.asString,
                                    archivos = archivosEntrega
                                )
                                Log.d(TAG, "✅ Entrega encontrada: estado=${miEntrega?.estado}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error cargando entrega", e)
                    }

                } else {
                    errorMessage = "Error ${tareaResponse.code()}: No se pudo cargar la tarea"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error general", e)
                errorMessage = "Error de conexión: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun eliminarEntrega() {
        scope.launch {
            isDeleting = true
            try {
                val response = ApiService.eliminarEntrega(token, miEntrega!!.id)
                if (response.isSuccessful) {
                    snackbarHostState.showSnackbar("Entrega eliminada correctamente")
                    miEntrega = null
                    showDeleteDialog = false
                } else {
                    snackbarHostState.showSnackbar("Error al eliminar la entrega")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error eliminando entrega", e)
                snackbarHostState.showSnackbar("Error: ${e.message}")
            } finally {
                isDeleting = false
            }
        }
    }

    LaunchedEffect(tareaId) {
        cargarDatos()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalles de la Tarea", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            val tareaActiva = tarea?.estado == "publicada"
            val puedeEntregar = tareaActiva && (miEntrega == null || miEntrega?.estado == "borrador")

            if (puedeEntregar && !isLoading) {
                ExtendedFloatingActionButton(
                    onClick = {
                        navController.navigate("tarea_entrega/$tareaId/$padreId")
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    icon = {
                        Icon(
                            if (miEntrega?.estado == "borrador") Icons.Default.Edit else Icons.Default.Send,
                            contentDescription = null
                        )
                    },
                    text = {
                        Text(if (miEntrega?.estado == "borrador") "Continuar Entrega" else "Hacer Entrega")
                    }
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
                isLoading -> TareaLoadingView()
                errorMessage != null -> TareaErrorView(
                    message = errorMessage ?: "",
                    onRetry = { cargarDatos() },
                    onBack = { navController.popBackStack() }
                )
                tarea == null -> TareaEmptyView { navController.popBackStack() }
                else -> TareaDetalleContent(
                    tarea = tarea!!,
                    miEntrega = miEntrega,
                    onOpenFile = { url ->
                        try {
                            val fullUrl = if (url.startsWith("http")) url else BASE_URL + url
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            scope.launch {
                                snackbarHostState.showSnackbar("No se pudo abrir el archivo")
                            }
                        }
                    },
                    onEditarEntrega = {
                        navController.navigate("tarea_entrega/$tareaId/$padreId")
                    },
                    onEliminarEntrega = {
                        showDeleteDialog = true
                    }
                )
            }
        }

        // Diálogo de confirmación para eliminar
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
                title = { Text("Eliminar Entrega") },
                text = { Text("¿Estás seguro de que deseas eliminar esta entrega? Esta acción no se puede deshacer.") },
                confirmButton = {
                    Button(
                        onClick = { eliminarEntrega() },
                        enabled = !isDeleting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        if (isDeleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onError
                            )
                        } else {
                            Text("Eliminar")
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteDialog = false },
                        enabled = !isDeleting
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
fun TareaDetalleContent(
    tarea: TareaDetalle,
    miEntrega: Entrega?,
    onOpenFile: (String) -> Unit,
    onEditarEntrega: () -> Unit,
    onEliminarEntrega: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header con título y estado
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = tarea.titulo,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        EstadoBadge(estado = tarea.estado)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Fecha de entrega
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (isVencida(tarea.fechaEntrega) && tarea.estado == "publicada")
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Fecha de entrega",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = formatFecha(tarea.fechaEntrega),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (isVencida(tarea.fechaEntrega) && tarea.estado == "publicada")
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Tipo de entrega
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            getIconForTipo(tarea.tipoEntrega),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Tipo de entrega",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = getTipoTexto(tarea.tipoEntrega),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Descripción
        if (tarea.descripcion.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Descripción",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = tarea.descripcion,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }

        // Criterios
        if (tarea.criterios.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Checklist,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Criterios de Evaluación",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Text(
                            text = tarea.criterios,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }

        // Material de apoyo
        if (tarea.archivosAdjuntos.isNotEmpty()) {
            item {
                Text(
                    text = "Material de Apoyo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(count = tarea.archivosAdjuntos.size) { index ->
                val archivo = tarea.archivosAdjuntos[index]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenFile(archivo.url) },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (archivo.tipo == "enlace")
                                Icons.Default.Link
                            else
                                Icons.Default.AttachFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = archivo.nombre,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            archivo.descripcion?.let { desc ->
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Icon(
                            Icons.Default.OpenInNew,
                            contentDescription = "Abrir",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Divider
        item {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }

        // Estado de mi entrega
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mi Entrega",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (miEntrega != null) {
                    EstadoChip(estado = miEntrega.estado)
                }
            }
        }

        if (miEntrega != null) {
            item {
                ResumenEntregaCard(
                    entrega = miEntrega,
                    onEditar = onEditarEntrega,
                    onEliminar = onEliminarEntrega,
                    puedeEditar = miEntrega.estado == "borrador"
                )
            }
        } else {
            item {
                NoEntregaCard(estado = tarea.estado)
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun ResumenEntregaCard(
    entrega: Entrega,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    puedeEditar: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            // Header con icono y título
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        when (entrega.estado) {
                            "borrador" -> Icons.Default.Edit
                            "enviada", "tarde" -> Icons.Default.CheckCircle
                            "calificada" -> Icons.Default.Star
                            else -> Icons.Default.CheckCircle
                        },
                        contentDescription = null,
                        tint = when (entrega.estado) {
                            "borrador" -> MaterialTheme.colorScheme.secondary
                            "enviada" -> MaterialTheme.colorScheme.primary
                            "tarde" -> MaterialTheme.colorScheme.tertiary
                            "calificada" -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(32.dp)
                    )

                    Column {
                        Text(
                            text = when (entrega.estado) {
                                "borrador" -> "Borrador Guardado"
                                "enviada" -> "Entrega Realizada"
                                "tarde" -> "Entrega Tardía"
                                "calificada" -> "Entrega Calificada"
                                else -> "Entrega Realizada"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (entrega.fechaEntrega.isNotEmpty()) {
                            Text(
                                text = formatFecha(entrega.fechaEntrega),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Botones de acción solo si es borrador
                if (puedeEditar) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onEditar,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Editar",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = onEliminar,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                            )
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Eliminar",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Texto de respuesta
            if (entrega.textoRespuesta.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Respuesta:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = entrega.textoRespuesta,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                }
            }

            // Archivos adjuntos
            if (entrega.archivos.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Archivos adjuntos (${entrega.archivos.size}):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    entrega.archivos.forEach { archivo ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AttachFile,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = archivo.substringAfterLast("/"),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Calificación si existe
            if (entrega.nota != null && entrega.nota != "null") {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Calificación:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = entrega.nota,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            // Comentario del profesor
            if (entrega.comentario != null && entrega.comentario != "null") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Comentario del Profesor:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = entrega.comentario,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun NoEntregaCard(estado: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Assignment,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )

            Text(
                text = if (estado == "cerrada")
                    "No Entregaste esta Tarea"
                else
                    "Pendiente de Entrega",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (estado == "cerrada")
                    "La fecha límite ha pasado"
                else
                    "Presiona el botón flotante para realizar tu entrega",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun EstadoBadge(estado: String) {
    val (bgColor, textColor, texto) = if (estado == "cerrada")
        Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.error,
            "CERRADA"
        )
    else
        Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.primary,
            "ACTIVA"
        )

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Text(
            text = texto,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun EstadoChip(estado: String) {
    val (bgColor, textColor, texto) = when (estado.lowercase()) {
        "calificada" -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.primary,
            "CALIFICADA"
        )
        "enviada" -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.primary,
            "ENVIADA"
        )
        "borrador" -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.secondary,
            "BORRADOR"
        )
        "tarde" -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.tertiary,
            "TARDE"
        )
        else -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.primary,
            "ENVIADA"
        )
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Text(
            text = texto,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun TareaLoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Cargando tarea...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun TareaErrorView(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = "Error al Cargar",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Volver")
                }
                Button(onClick = onRetry) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Reintentar")
                }
            }
        }
    }
}

@Composable
fun TareaEmptyView(onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Assignment,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )
            Text(
                text = "Tarea No Encontrada",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "No se pudo cargar la información de esta tarea",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Button(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Volver")
            }
        }
    }
}

fun formatFecha(fecha: String): String {
    return try {
        if (fecha.isEmpty()) return "Sin fecha"
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = inputFormat.parse(fecha)
        val outputFormat = SimpleDateFormat("dd/MM/yyyy 'a las' HH:mm", Locale.getDefault())
        date?.let { outputFormat.format(it) } ?: fecha
    } catch (e: Exception) {
        try {
            fecha.substring(0, 10).replace("-", "/")
        } catch (ex: Exception) {
            "Fecha inválida"
        }
    }
}

fun isVencida(fecha: String): Boolean {
    return try {
        if (fecha.isEmpty()) return false
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = inputFormat.parse(fecha)
        date?.before(Date()) ?: false
    } catch (e: Exception) {
        false
    }
}

fun getIconForTipo(tipo: String) = when (tipo.lowercase()) {
    "archivo" -> Icons.Default.AttachFile
    "multimedia" -> Icons.Default.Image
    "enlace" -> Icons.Default.Link
    "presencial" -> Icons.Default.Person
    "grupal" -> Icons.Default.Group
    else -> Icons.Default.Description
}

fun getTipoTexto(tipo: String) = when (tipo.lowercase()) {
    "texto" -> "Entrega de Texto"
    "archivo" -> "Entrega de Archivo"
    "multimedia" -> "Entrega Multimedia"
    "enlace" -> "Entrega por Enlace"
    "presencial" -> "Entrega Presencial"
    "grupal" -> "Entrega Grupal"
    else -> "Entrega de Texto"
}