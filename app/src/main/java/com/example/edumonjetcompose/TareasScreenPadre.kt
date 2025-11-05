@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.edumonjetcompose.ui

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.edumonjetcompose.network.ApiService
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.*

private const val BASE_URL = "https://backend-edumon.onrender.com"
private const val TAG = "TareaEntregaScreen"

data class TareaDetalle(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val fechaCreacion: String,
    val fechaEntrega: String,
    val estado: String,
    val tipoEntrega: String,
    val criterios: String,
    val archivosAdjuntos: List<ArchivoAdjunto>
)

data class ArchivoAdjunto(
    val tipo: String,
    val url: String,
    val nombre: String,
    val descripcion: String? = null
)

data class Entrega(
    val id: String,
    val textoRespuesta: String,
    val fechaEntrega: String,
    val estado: String,
    val nota: String?,
    val comentario: String?,
    val archivos: List<String>
)

data class ArchivoSeleccionado(
    val uri: Uri,
    val nombre: String,
    val tipo: String,
    val tamano: Long
)

@Composable
fun TareaEntregaScreen(
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
    var showEntregaDialog by remember { mutableStateOf(false) }
    var textoRespuesta by remember { mutableStateOf("") }
    var enlaceRespuesta by remember { mutableStateOf("") }
    var archivosSeleccionados by remember { mutableStateOf<List<ArchivoSeleccionado>>(emptyList()) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val nuevosArchivos = uris.mapNotNull { uri ->
            try {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                val nombre = cursor?.use {
                    if (it.moveToFirst()) {
                        val nombreIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        val tamanoIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        val nombre = if (nombreIndex >= 0) it.getString(nombreIndex) else "archivo_${System.currentTimeMillis()}"
                        val tamano = if (tamanoIndex >= 0) it.getLong(tamanoIndex) else 0L
                        nombre to tamano
                    } else null
                } ?: ("archivo_${System.currentTimeMillis()}" to 0L)

                val tipo = context.contentResolver.getType(uri) ?: "application/octet-stream"

                ArchivoSeleccionado(
                    uri = uri,
                    nombre = nombre.first,
                    tipo = tipo,
                    tamano = nombre.second
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error procesando archivo", e)
                null
            }
        }
        archivosSeleccionados = archivosSeleccionados + nuevosArchivos
    }

    LaunchedEffect(padreId) {
        if (padreId.isEmpty() || padreId == "null") {
            Log.e(TAG, "❌ ERROR: padreId inválido: '$padreId'")
            errorMessage = "Error: No se pudo identificar tu usuario"
            isLoading = false
            return@LaunchedEffect
        }
        Log.d(TAG, "✅ padreId válido: $padreId")
    }

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

                        Log.d(TAG, "✅ Tarea cargada: ${tarea?.titulo}, tipo: ${tarea?.tipoEntrega}")
                    }

                    try {
                        Log.d(TAG, "📡 Buscando entrega para tarea: $tareaId")
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
                                Log.d(TAG, "Entrega encontrada: estado=${miEntrega?.estado}")
                            } ?: Log.d(TAG, "No hay entrega previa")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error cargando entrega", e)
                    }

                } else {
                    errorMessage = "Error ${tareaResponse.code()}: No se pudo cargar la tarea"
                    Log.e(TAG, "Error HTTP: ${tareaResponse.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error general", e)
                errorMessage = "Error de conexión: ${e.message}"
            } finally {
                isLoading = false
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
            val puedeEntregar = tareaActiva && miEntrega == null
            val esEditable = miEntrega?.estado == "borrador"

            if (puedeEntregar && !isLoading) {
                FloatingActionButton(
                    onClick = {
                        textoRespuesta = ""
                        enlaceRespuesta = ""
                        archivosSeleccionados = emptyList()
                        showEntregaDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Send, "Entregar")
                }
            } else if (esEditable && !isLoading) {
                FloatingActionButton(
                    onClick = {
                        textoRespuesta = miEntrega?.textoRespuesta ?: ""
                        enlaceRespuesta = ""
                        archivosSeleccionados = emptyList()
                        showEntregaDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Default.Edit, "Editar")
                }
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
                else -> TareaContent(
                    tarea = tarea!!,
                    miEntrega = miEntrega,
                    onOpenFile = { url ->
                        try {
                            val fullUrl = if (url.startsWith("http")) url else BASE_URL + url
                            Log.d(TAG, "🔗 Abriendo URL: $fullUrl")
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error abriendo archivo", e)
                            scope.launch {
                                snackbarHostState.showSnackbar("No se pudo abrir el archivo")
                            }
                        }
                    }
                )
            }
        }

        if (showEntregaDialog && tarea != null) {
            DialogoEntrega(
                tipoEntrega = tarea!!.tipoEntrega,
                textoRespuesta = textoRespuesta,
                enlaceRespuesta = enlaceRespuesta,
                archivosSeleccionados = archivosSeleccionados,
                esBorrador = miEntrega?.estado == "borrador",
                onTextoChange = { textoRespuesta = it },
                onEnlaceChange = { enlaceRespuesta = it },
                onAgregarArchivo = {
                    when (tarea!!.tipoEntrega.lowercase()) {
                        "multimedia" -> filePickerLauncher.launch("image/*,video/*,audio/*")
                        "archivo" -> filePickerLauncher.launch("*/*")
                        else -> filePickerLauncher.launch("*/*")
                    }
                },
                onEliminarArchivo = { archivo ->
                    archivosSeleccionados = archivosSeleccionados.filter { it != archivo }
                },
                onDismiss = {
                    showEntregaDialog = false
                    textoRespuesta = ""
                    enlaceRespuesta = ""
                    archivosSeleccionados = emptyList()
                },
                onGuardarBorrador = {
                    scope.launch {
                        try {
                            Log.d(TAG, "💾 Guardando borrador...")

                            val tieneContenido = textoRespuesta.isNotEmpty() ||
                                    enlaceRespuesta.isNotEmpty() ||
                                    archivosSeleccionados.isNotEmpty()

                            if (!tieneContenido) {
                                snackbarHostState.showSnackbar("Agrega contenido para guardar")
                                showEntregaDialog = false
                                return@launch
                            }

                            val response = withTimeoutOrNull(45000) {
                                if (miEntrega?.estado == "borrador") {
                                    Log.d(TAG, "Actualizando borrador: ${miEntrega!!.id}")

                                    if (archivosSeleccionados.isEmpty()) {
                                        ApiService.updateEntrega(
                                            token = token,
                                            entregaId = miEntrega!!.id,
                                            textoRespuesta = textoRespuesta.takeIf { it.isNotEmpty() },
                                            archivos = null
                                        )
                                    } else {
                                        ApiService.updateEntregaMultipart(
                                            token = token,
                                            entregaId = miEntrega!!.id,
                                            textoRespuesta = textoRespuesta.takeIf { it.isNotEmpty() },
                                            enlace = enlaceRespuesta.takeIf { it.isNotEmpty() },
                                            archivos = archivosSeleccionados.map { it.uri },
                                            context = context
                                        )
                                    }
                                } else {
                                    Log.d(TAG, "Creando nuevo borrador")

                                    if (archivosSeleccionados.isEmpty()) {
                                        Log.d(TAG, "🔄 Usando endpoint JSON")
                                        ApiService.crearEntrega(
                                            token = token,
                                            tareaId = tareaId,
                                            padreId = padreId,
                                            textoRespuesta = textoRespuesta.takeIf { it.isNotEmpty() },
                                            archivos = null,
                                            estado = "borrador"
                                        )
                                    } else {
                                        Log.d(TAG, "📎 Usando endpoint multipart")
                                        ApiService.crearEntregaMultipart(
                                            token = token,
                                            tareaId = tareaId,
                                            padreId = padreId,
                                            textoRespuesta = textoRespuesta.takeIf { it.isNotEmpty() },
                                            enlace = enlaceRespuesta.takeIf { it.isNotEmpty() },
                                            archivos = archivosSeleccionados.map { it.uri },
                                            estado = "borrador",
                                            context = context
                                        )
                                    }
                                }
                            }

                            if (response == null) {
                                snackbarHostState.showSnackbar("⏱️ Tiempo de espera agotado. Intenta nuevamente.")
                                showEntregaDialog = false
                                return@launch
                            }

                            if (response.isSuccessful) {
                                showEntregaDialog = false
                                textoRespuesta = ""
                                enlaceRespuesta = ""
                                archivosSeleccionados = emptyList()
                                snackbarHostState.showSnackbar("✅ Borrador guardado")
                                cargarDatos()
                            } else {
                                val errorBody = response.errorBody()?.string()
                                Log.e(TAG, "❌ Error: ${response.code()}")
                                Log.e(TAG, "❌ Respuesta: $errorBody")

                                val mensaje = try {
                                    val json = com.google.gson.JsonParser.parseString(errorBody).asJsonObject
                                    json.get("message")?.asString ?: "Error desconocido"
                                } catch (e: Exception) {
                                    "Error ${response.code()}"
                                }

                                snackbarHostState.showSnackbar(mensaje)
                                showEntregaDialog = false
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Excepción guardando", e)
                            val errorMsg = when (e) {
                                is java.net.SocketTimeoutException -> "⏱️ Tiempo de espera agotado. Intenta nuevamente."
                                is java.net.UnknownHostException -> "📡 Sin conexión a internet"
                                else -> "Error: ${e.localizedMessage}"
                            }
                            snackbarHostState.showSnackbar(errorMsg)
                            showEntregaDialog = false
                        }
                    }
                },
                onEnviar = {
                    scope.launch {
                        try {
                            Log.d(TAG, "📤 Enviando entrega...")

                            val tieneContenido = textoRespuesta.isNotEmpty() ||
                                    enlaceRespuesta.isNotEmpty() ||
                                    archivosSeleccionados.isNotEmpty()

                            if (!tieneContenido) {
                                snackbarHostState.showSnackbar("Agrega contenido antes de enviar")
                                showEntregaDialog = false
                                return@launch
                            }

                            // PASO 1: Crear o actualizar borrador
                            val entregaId = withTimeoutOrNull(45000) {
                                if (miEntrega?.estado == "borrador") {
                                    Log.d(TAG, "Actualizando borrador existente: ${miEntrega!!.id}")

                                    val updateResponse = if (archivosSeleccionados.isEmpty()) {
                                        ApiService.updateEntrega(
                                            token = token,
                                            entregaId = miEntrega!!.id,
                                            textoRespuesta = textoRespuesta.takeIf { it.isNotEmpty() },
                                            archivos = null
                                        )
                                    } else {
                                        ApiService.updateEntregaMultipart(
                                            token = token,
                                            entregaId = miEntrega!!.id,
                                            textoRespuesta = textoRespuesta.takeIf { it.isNotEmpty() },
                                            enlace = enlaceRespuesta.takeIf { it.isNotEmpty() },
                                            archivos = archivosSeleccionados.map { it.uri },
                                            context = context
                                        )
                                    }

                                    if (updateResponse.isSuccessful) {
                                        Log.d(TAG, "Borrador actualizado")
                                        miEntrega!!.id
                                    } else {
                                        Log.e(TAG, "Error actualizando: ${updateResponse.errorBody()?.string()}")
                                        null
                                    }
                                } else {
                                    Log.d(TAG, "Creando nuevo borrador para enviar")

                                    val createResponse = if (archivosSeleccionados.isEmpty()) {
                                        Log.d(TAG, "Creando con JSON")
                                        ApiService.crearEntrega(
                                            token = token,
                                            tareaId = tareaId,
                                            padreId = padreId,
                                            textoRespuesta = textoRespuesta.takeIf { it.isNotEmpty() },
                                            archivos = null,
                                            estado = "borrador"
                                        )
                                    } else {
                                        Log.d(TAG, "📎 Creando con multipart")
                                        ApiService.crearEntregaMultipart(
                                            token = token,
                                            tareaId = tareaId,
                                            padreId = padreId,
                                            textoRespuesta = textoRespuesta.takeIf { it.isNotEmpty() },
                                            enlace = enlaceRespuesta.takeIf { it.isNotEmpty() },
                                            archivos = archivosSeleccionados.map { it.uri },
                                            estado = "borrador",
                                            context = context
                                        )
                                    }

                                    if (createResponse.isSuccessful) {
                                        val entregaIdCreada = createResponse.body()
                                            ?.getAsJsonObject("entrega")
                                            ?.get("_id")?.asString

                                        Log.d(TAG, "✅ Borrador creado: $entregaIdCreada")
                                        entregaIdCreada
                                    } else {
                                        val errorBody = createResponse.errorBody()?.string()
                                        Log.e(TAG, "❌ Error creando: ${createResponse.code()}")
                                        Log.e(TAG, "❌ Respuesta: $errorBody")

                                        val mensaje = try {
                                            val json = com.google.gson.JsonParser.parseString(errorBody).asJsonObject
                                            json.get("message")?.asString ?: "Error desconocido"
                                        } catch (e: Exception) {
                                            "Error ${createResponse.code()}"
                                        }

                                        snackbarHostState.showSnackbar(mensaje)
                                        null
                                    }
                                }
                            }

                            if (entregaId == null) {
                                snackbarHostState.showSnackbar("⏱️ Tiempo de espera agotado al preparar la entrega")
                                showEntregaDialog = false
                                return@launch
                            }

                            // PASO 2: Enviar la entrega
                            Log.d(TAG, "📤 Enviando entrega ID: $entregaId")

                            val enviarResponse = withTimeoutOrNull(45000) {
                                ApiService.enviarEntrega(token, entregaId)
                            }

                            if (enviarResponse == null) {
                                snackbarHostState.showSnackbar("⏱️ Tiempo de espera agotado al enviar. Revisa tu conexión.")
                                showEntregaDialog = false
                                return@launch
                            }

                            if (enviarResponse.isSuccessful) {
                                showEntregaDialog = false
                                textoRespuesta = ""
                                enlaceRespuesta = ""
                                archivosSeleccionados = emptyList()
                                snackbarHostState.showSnackbar("✅ Tarea entregada exitosamente")
                                cargarDatos()
                            } else {
                                val errorBody = enviarResponse.errorBody()?.string()
                                Log.e(TAG, "❌ Error enviando: ${enviarResponse.code()}")
                                Log.e(TAG, "❌ Respuesta: $errorBody")

                                val mensaje = try {
                                    val json = com.google.gson.JsonParser.parseString(errorBody).asJsonObject
                                    json.get("message")?.asString ?: "Error al enviar"
                                } catch (e: Exception) {
                                    "Error ${enviarResponse.code()}"
                                }

                                snackbarHostState.showSnackbar(mensaje)
                                showEntregaDialog = false
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Excepción", e)
                            val errorMsg = when (e) {
                                is java.net.SocketTimeoutException -> "⏱️ Tiempo de espera agotado. Intenta nuevamente."
                                is java.net.UnknownHostException -> "📡 Sin conexión a internet"
                                else -> "Error: ${e.localizedMessage}"
                            }
                            snackbarHostState.showSnackbar(errorMsg)
                            showEntregaDialog = false
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun DialogoEntrega(
    tipoEntrega: String,
    textoRespuesta: String,
    enlaceRespuesta: String,
    archivosSeleccionados: List<ArchivoSeleccionado>,
    esBorrador: Boolean,
    onTextoChange: (String) -> Unit,
    onEnlaceChange: (String) -> Unit,
    onAgregarArchivo: () -> Unit,
    onEliminarArchivo: (ArchivoSeleccionado) -> Unit,
    onDismiss: () -> Unit,
    onGuardarBorrador: () -> Unit,
    onEnviar: () -> Unit
) {
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Text(
                text = if (esBorrador) "Editar Entrega" else "Entregar Tarea",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = getTipoTexto(tipoEntrega),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                if (tipoEntrega.lowercase() != "enlace" || archivosSeleccionados.isNotEmpty()) {
                    item {
                        OutlinedTextField(
                            value = textoRespuesta,
                            onValueChange = onTextoChange,
                            label = { Text("Descripción o respuesta") },
                            placeholder = { Text("Escribe aquí...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            maxLines = 8,
                            enabled = !isSubmitting
                        )
                    }
                }

                if (tipoEntrega.lowercase() == "enlace") {
                    item {
                        OutlinedTextField(
                            value = enlaceRespuesta,
                            onValueChange = onEnlaceChange,
                            label = { Text("Enlace *") },
                            placeholder = { Text("https://...") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isSubmitting,
                            leadingIcon = {
                                Icon(Icons.Default.Link, contentDescription = null)
                            }
                        )
                    }
                }

                if (tipoEntrega.lowercase() in listOf("archivo", "multimedia")) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Archivos adjuntos",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = onAgregarArchivo,
                                enabled = !isSubmitting
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Agregar")
                            }
                        }
                    }

                    if (archivosSeleccionados.isEmpty()) {
                        item {
                            Text(
                                text = "No hay archivos adjuntos",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    items(archivosSeleccionados) { archivo ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when {
                                    archivo.tipo.startsWith("image/") -> Icons.Default.Image
                                    archivo.tipo.startsWith("video/") -> Icons.Default.VideoLibrary
                                    archivo.tipo.startsWith("audio/") -> Icons.Default.AudioFile
                                    else -> Icons.Default.AttachFile
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = archivo.nombre,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                                Text(
                                    text = formatFileSize(archivo.tamano),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }

                            IconButton(
                                onClick = { onEliminarArchivo(archivo) },
                                enabled = !isSubmitting
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
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (!isSubmitting) {
                            isSubmitting = true
                            onEnviar()
                        }
                    },
                    enabled = !isSubmitting && when (tipoEntrega.lowercase()) {
                        "texto" -> textoRespuesta.isNotEmpty()
                        "enlace" -> enlaceRespuesta.isNotEmpty()
                        "archivo", "multimedia" -> archivosSeleccionados.isNotEmpty() || textoRespuesta.isNotEmpty()
                        else -> true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Enviar Definitivamente")
                }

                OutlinedButton(
                    onClick = {
                        if (!isSubmitting) {
                            isSubmitting = true
                            onGuardarBorrador()
                        }
                    },
                    enabled = !isSubmitting && when (tipoEntrega.lowercase()) {
                        "texto" -> textoRespuesta.isNotEmpty()
                        "enlace" -> enlaceRespuesta.isNotEmpty()
                        "archivo", "multimedia" -> archivosSeleccionados.isNotEmpty() || textoRespuesta.isNotEmpty()
                        else -> true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Guardar como Borrador")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting
            ) {
                Text("Cancelar")
            }
        }
    )
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

@Composable
fun TareaContent(
    tarea: TareaDetalle,
    miEntrega: Entrega?,
    onOpenFile: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header con título y estado
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tarea.titulo,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    EstadoBadge(estado = tarea.estado)
                }

                // Fecha de entrega
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isVencida(tarea.fechaEntrega) && tarea.estado == "publicada")
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Entrega: ${formatFecha(tarea.fechaEntrega)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isVencida(tarea.fechaEntrega) && tarea.estado == "publicada")
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Tipo de entrega
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            ) {
                Icon(
                    getIconForTipo(tarea.tipoEntrega),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = getTipoTexto(tarea.tipoEntrega),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Descripción
        if (tarea.descripcion.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Descripción",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = tarea.descripcion,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 24.sp
                    )
                }
            }
        }

        // Criterios
        if (tarea.criterios.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Criterios de Evaluación",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = tarea.criterios,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp
                    )
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

            items(tarea.archivosAdjuntos) { archivo ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenFile(archivo.url) }
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
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
                        archivo.descripcion?.let {
                            Text(
                                text = it,
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

        // Divider
        item {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }

        // Mi entrega
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
                EntregaCard(entrega = miEntrega, onOpenFile = onOpenFile)
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun EntregaCard(entrega: Entrega, onOpenFile: (String) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        // Estado y fecha
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
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
                modifier = Modifier.size(24.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
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

        // Respuesta de texto
        if (entrega.textoRespuesta.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Tu Respuesta:",
                    style = MaterialTheme.typography.labelLarge,
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
                    text = "Archivos Adjuntos:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                entrega.archivos.forEach { archivoUrl ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenFile(archivoUrl) }
                            .background(
                                MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AttachFile,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = archivoUrl.substringAfterLast("/"),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Default.OpenInNew,
                            contentDescription = "Abrir",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Calificación
        if (entrega.nota != null && entrega.nota != "null") {
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = "Calificación:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = entrega.nota,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        // Comentario del docente
        if (entrega.comentario != null && entrega.comentario.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Comment,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Comentarios del Docente:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = entrega.comentario,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun NoEntregaCard(estado: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            )
            .padding(32.dp),
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
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
                Icon(Icons.Default.ArrowBack, null)
                Spacer(Modifier.width(8.dp))
                Text("Volver")
            }
        }
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
            CircularProgressIndicator()
            Text(
                text = "Cargando tarea...",
                style = MaterialTheme.typography.bodyLarge
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
                    Icon(Icons.Default.ArrowBack, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Volver")
                }

                Button(onClick = onRetry) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Reintentar")
                }
            }
        }
    }
}

private fun formatFecha(fecha: String): String {
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

private fun isVencida(fecha: String): Boolean {
    return try {
        if (fecha.isEmpty()) return false
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = inputFormat.parse(fecha)
        date?.before(Date()) ?: false
    } catch (e: Exception) {
        false
    }
}

private fun getIconForTipo(tipo: String) = when (tipo.lowercase()) {
    "archivo" -> Icons.Default.AttachFile
    "multimedia" -> Icons.Default.Image
    "enlace" -> Icons.Default.Link
    "presencial" -> Icons.Default.Person
    "grupal" -> Icons.Default.Group
    else -> Icons.Default.Description
}

private fun getTipoTexto(tipo: String) = when (tipo.lowercase()) {
    "texto" -> "Entrega de Texto"
    "archivo" -> "Entrega de Archivo"
    "multimedia" -> "Entrega Multimedia"
    "enlace" -> "Entrega por Enlace"
    "presencial" -> "Entrega Presencial"
    "grupal" -> "Entrega Grupal"
    else -> "Entrega de Texto"}