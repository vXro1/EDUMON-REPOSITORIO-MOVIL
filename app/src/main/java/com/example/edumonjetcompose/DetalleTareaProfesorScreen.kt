package com.example.edumonjetcompose

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.edumonjetcompose.models.*
import com.example.edumonjetcompose.network.ApiService
import com.example.edumonjetcompose.ui.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.util.Base64
import com.example.edumonjetcompose.utils.abrirUrl
import com.example.edumonjetcompose.utils.estaVencida

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleTareaProfesorScreen(
    navController: NavController,
    tareaId: String,
    token: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var tarea by remember { mutableStateOf<TareaDetalleProfesor?>(null) }
    var entregas by remember { mutableStateOf<List<EntregaEstudiante>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    var showCalificacionDialog by remember { mutableStateOf(false) }
    var selectedEntrega by remember { mutableStateOf<EntregaDetalleCompleta?>(null) }
    var isLoadingEntrega by remember { mutableStateOf(false) }

    fun cargarEntregas() {
        scope.launch {
            try {
                val entregasResponse = ApiService.getEntregasByTarea(token, tareaId)
                if (entregasResponse.isSuccessful) {
                    val body = entregasResponse.body()
                    val entregasArray = body?.getAsJsonArray("entregas")

                    val entregasList = mutableListOf<EntregaEstudiante>()
                    entregasArray?.forEach { element ->
                        try {
                            val entregaObj = element.asJsonObject
                            val id = entregaObj.get("_id")?.asString ?: ""
                            val estado = entregaObj.get("estado")?.asString ?: "pendiente"

                            if (estado == "enviada" || estado == "tarde") {
                                val fechaEntrega = entregaObj.get("fechaEntrega")?.asString

                                val calificacionObj = entregaObj.getAsJsonObject("calificacion")
                                val nota = calificacionObj?.get("nota")?.asDouble
                                val comentario = calificacionObj?.get("comentario")?.asString

                                val padreObj = entregaObj.getAsJsonObject("padreId")
                                val padreNombre = padreObj?.get("nombre")?.asString ?: ""
                                val padreApellido = padreObj?.get("apellido")?.asString ?: ""
                                val padreFoto = padreObj?.get("fotoPerfilUrl")?.asString

                                val estadoFinal = if (nota != null) "calificada" else estado

                                entregasList.add(
                                    EntregaEstudiante(
                                        id = id,
                                        estudianteNombre = "$padreNombre $padreApellido",
                                        estudianteFoto = padreFoto,
                                        estado = estadoFinal,
                                        fechaEntrega = fechaEntrega,
                                        nota = nota,
                                        comentario = comentario
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    entregas = entregasList
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(tareaId) {
        isLoading = true
        try {
            val tareaResponse = ApiService.getTareaById(token, tareaId)
            if (tareaResponse.isSuccessful) {
                val body = tareaResponse.body()
                body?.let { json ->
                    val id = json.get("_id")?.asString ?: ""
                    val titulo = json.get("titulo")?.asString ?: ""
                    val descripcion = json.get("descripcion")?.asString ?: ""
                    val fechaCreacion = json.get("fechaCreacion")?.asString ?: ""
                    val fechaLimite = json.get("fechaLimite")?.asString ?: ""
                    val estado = json.get("estado")?.asString ?: "activa"

                    val moduloObj = json.getAsJsonObject("moduloId")
                    val moduloNombre = moduloObj?.get("nombre")?.asString ?: "Sin módulo"

                    val archivosArray = json.getAsJsonArray("archivos")
                    val archivos = mutableListOf<ArchivoAdjunto>()
                    archivosArray?.forEach { element ->
                        val archivoObj = element.asJsonObject
                        archivos.add(
                            ArchivoAdjunto(
                                tipo = archivoObj.get("tipo")?.asString ?: "",
                                url = archivoObj.get("url")?.asString ?: "",
                                nombre = archivoObj.get("nombre")?.asString ?: "Archivo"
                            )
                        )
                    }

                    tarea = TareaDetalleProfesor(
                        id = id,
                        titulo = titulo,
                        descripcion = descripcion,
                        fechaCreacion = fechaCreacion,
                        fechaLimite = fechaLimite,
                        estado = estado,
                        moduloNombre = moduloNombre,
                        archivos = archivos
                    )
                }
            }

            cargarEntregas()
        } catch (e: Exception) {
            e.printStackTrace()
            scope.launch {
                snackbarHostState.showSnackbar("Error al cargar la tarea")
            }
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Detalle de Tarea",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showOptionsMenu = true }) {
                        Icon(Icons.Default.MoreVert, "Opciones")
                    }

                    DropdownMenu(
                        expanded = showOptionsMenu,
                        onDismissRequest = { showOptionsMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Editar tarea") },
                            onClick = {
                                showOptionsMenu = false
                                showEditDialog = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, null, tint = AzulCielo)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Cerrar tarea") },
                            onClick = {
                                showOptionsMenu = false
                                scope.launch {
                                    try {
                                        val response = ApiService.closeTarea(token, tareaId)
                                        if (response.isSuccessful) {
                                            snackbarHostState.showSnackbar("Tarea cerrada")
                                            navController.popBackStack()
                                        } else {
                                            snackbarHostState.showSnackbar("Error al cerrar tarea")
                                        }
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Error: ${e.message}")
                                    }
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, null, tint = Advertencia)
                            }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text("Eliminar tarea", color = ErrorOscuro) },
                            onClick = {
                                showOptionsMenu = false
                                showDeleteDialog = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, null, tint = ErrorOscuro)
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FondoCard,
                    titleContentColor = GrisOscuro
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
        } else if (tarea == null) {
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
                        "No se pudo cargar la tarea",
                        style = MaterialTheme.typography.bodyLarge,
                        color = GrisMedio
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = FondoCard,
                    contentColor = AzulCielo
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Información") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Entregas")
                                if (entregas.isNotEmpty()) {
                                    Surface(
                                        shape = CircleShape,
                                        color = AzulCielo
                                    ) {
                                        Text(
                                            entregas.size.toString(),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    )
                }

                when (selectedTab) {
                    0 -> InformacionTareaTab(tarea!!)
                    1 -> EntregasTab(
                        entregas = entregas,
                        onEntregaClick = { entregaId ->
                            isLoadingEntrega = true
                            showCalificacionDialog = true
                            scope.launch {
                                try {
                                    val response = ApiService.getEntregaById(token, entregaId)
                                    if (response.isSuccessful) {
                                        val body = response.body()
                                        body?.let { json ->
                                            val id = json.get("_id")?.asString ?: ""
                                            val textoRespuesta = json.get("textoRespuesta")?.asString
                                            val estado = json.get("estado")?.asString ?: ""
                                            val fechaEntrega = json.get("fechaEntrega")?.asString

                                            val padreObj = json.getAsJsonObject("padreId")
                                            val padreNombre = padreObj?.get("nombre")?.asString ?: ""
                                            val padreApellido = padreObj?.get("apellido")?.asString ?: ""
                                            val padreFoto = padreObj?.get("fotoPerfilUrl")?.asString

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

                                            val calificacionObj = json.getAsJsonObject("calificacion")
                                            var notaActual: Double? = null
                                            var comentarioActual: String? = null

                                            if (calificacionObj != null) {
                                                notaActual = calificacionObj.get("nota")?.asDouble
                                                comentarioActual = calificacionObj.get("comentario")?.asString
                                            }

                                            selectedEntrega = EntregaDetalleCompleta(
                                                id = id,
                                                estudianteNombre = "$padreNombre $padreApellido",
                                                estudianteFoto = padreFoto,
                                                estado = estado,
                                                fechaEntrega = fechaEntrega,
                                                textoRespuesta = textoRespuesta,
                                                archivos = archivos,
                                                notaActual = notaActual,
                                                comentarioActual = comentarioActual
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    snackbarHostState.showSnackbar("Error al cargar la entrega")
                                    showCalificacionDialog = false
                                } finally {
                                    isLoadingEntrega = false
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (showCalificacionDialog) {
        CalificacionDialog(
            entrega = selectedEntrega,
            isLoading = isLoadingEntrega,
            onDismiss = {
                showCalificacionDialog = false
                selectedEntrega = null
            },
            onCalificar = { nota, comentario ->
                scope.launch {
                    try {
                        val docenteId = getUserIdFromToken(token)

                        if (docenteId.isEmpty()) {
                            snackbarHostState.showSnackbar("Error: No se pudo obtener el ID del docente")
                            return@launch
                        }

                        val response = ApiService.calificarEntrega(
                            token = token,
                            entregaId = selectedEntrega!!.id,
                            nota = nota,
                            comentario = if (comentario.isBlank()) null else comentario,
                            docenteId = docenteId
                        )

                        if (response.isSuccessful) {
                            snackbarHostState.showSnackbar("Entrega calificada exitosamente")
                            showCalificacionDialog = false
                            selectedEntrega = null
                            cargarEntregas()
                        } else {
                            val errorBody = response.errorBody()?.string()
                            snackbarHostState.showSnackbar("Error al calificar: ${response.code()}")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        snackbarHostState.showSnackbar("Error: ${e.message}")
                    }
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = ErrorOscuro,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "¿Eliminar tarea?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Esta acción no se puede deshacer. Todas las entregas asociadas también se eliminarán.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        scope.launch {
                            try {
                                val response = ApiService.deleteTarea(token, tareaId)
                                if (response.isSuccessful) {
                                    snackbarHostState.showSnackbar("Tarea eliminada")
                                    navController.popBackStack()
                                } else {
                                    snackbarHostState.showSnackbar("Error al eliminar")
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error: ${e.message}")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorOscuro
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar", color = GrisMedio)
                }
            }
        )
    }
}

@Composable
fun CalificacionDialog(
    entrega: EntregaDetalleCompleta?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onCalificar: (Double, String) -> Unit
) {
    var nota by remember { mutableStateOf(entrega?.notaActual?.toString() ?: "") }
    var comentario by remember { mutableStateOf(entrega?.comentarioActual ?: "") }
    var notaError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            color = FondoClaro
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AzulCielo)
                }
            } else if (entrega == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Error al cargar la entrega", color = ErrorOscuro)
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Surface(
                        color = AzulCielo,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (entrega.estudianteFoto != null) {
                                    AsyncImage(
                                        model = entrega.estudianteFoto,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Color.White),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Surface(
                                        modifier = Modifier.size(48.dp),
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.3f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                entrega.estudianteNombre.firstOrNull()?.uppercase() ?: "?",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                Column {
                                    Text(
                                        entrega.estudianteNombre,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    if (entrega.fechaEntrega != null) {
                                        Text(
                                            formatearFecha(entrega.fechaEntrega),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }

                            IconButton(onClick = onDismiss) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Cerrar",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = when (entrega.estado) {
                                    "tarde" -> ErrorOscuro.copy(alpha = 0.1f)
                                    "enviada" -> VerdeLima.copy(alpha = 0.1f)
                                    else -> GrisClaro
                                },
                                border = BorderStroke(
                                    1.dp,
                                    when (entrega.estado) {
                                        "tarde" -> ErrorOscuro
                                        "enviada" -> VerdeLima
                                        else -> GrisMedio
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (entrega.estado == "tarde") Icons.Default.Warning else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = when (entrega.estado) {
                                            "tarde" -> ErrorOscuro
                                            "enviada" -> VerdeLima
                                            else -> GrisMedio
                                        }
                                    )
                                    Text(
                                        if (entrega.estado == "tarde") "Entrega tardía" else "Entregado a tiempo",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = when (entrega.estado) {
                                            "tarde" -> ErrorOscuro
                                            "enviada" -> VerdeLima
                                            else -> GrisMedio
                                        }
                                    )
                                }
                            }
                        }

                        if (!entrega.textoRespuesta.isNullOrBlank()) {
                            item {
                                TareaInfoSection(
                                    title = "Respuesta",
                                    icon = Icons.Default.Description
                                ) {
                                    Text(
                                        entrega.textoRespuesta,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = GrisOscuro,
                                        lineHeight = 24.sp
                                    )
                                }
                            }
                        }

                        if (entrega.archivos.isNotEmpty()) {
                            item {
                                TareaInfoSection(
                                    title = "Archivos adjuntos (${entrega.archivos.size})",
                                    icon = Icons.Default.AttachFile
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        entrega.archivos.forEach { archivo ->
                                            ArchivoEntregaCard(archivo)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Divider(color = GrisClaro, thickness = 1.dp)
                        }

                        item {
                            Text(
                                "Calificación",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = GrisOscuro
                            )
                        }

                        item {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Nota (0-100)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GrisOscuro
                                )

                                OutlinedTextField(
                                    value = nota,
                                    onValueChange = {
                                        nota = it
                                        notaError = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Ingrese la nota") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Star, null, tint = AzulCielo)
                                    },
                                    isError = notaError,
                                    supportingText = if (notaError) {
                                        { Text("Ingrese una nota válida entre 0 y 100") }
                                    } else null,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AzulCielo,
                                        unfocusedBorderColor = GrisClaro
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }

                        item { Spacer(Modifier.height(80.dp)) }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = FondoCard,
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = GrisMedio
                                ),
                                border = BorderStroke(1.dp, GrisClaro)
                            ) {
                                Text("Cancelar")
                            }

                            Button(
                                onClick = {
                                    val notaDouble = nota.toDoubleOrNull()
                                    if (notaDouble == null || notaDouble < 0 || notaDouble > 100) {
                                        notaError = true
                                    } else {
                                        onCalificar(notaDouble, comentario)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AzulCielo
                                )
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Calificar")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArchivoEntregaCard(archivo: ArchivoAdjuntoEntrega) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                abrirUrl(context, archivo.url)
            },
        shape = RoundedCornerShape(8.dp),
        color = Fucsia.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Fucsia.copy(alpha = 0.2f))
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
                color = Fucsia.copy(alpha = 0.15f)
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
                    tint = Fucsia
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
                tint = Fucsia,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun InformacionTareaTab(tarea: TareaDetalleProfesor) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = when (tarea.estado) {
                    "activa" -> VerdeLima.copy(alpha = 0.1f)
                    "cerrada" -> GrisClaro
                    else -> Advertencia.copy(alpha = 0.1f)
                },
                border = BorderStroke(
                    2.dp,
                    when (tarea.estado) {
                        "activa" -> VerdeLima
                        "cerrada" -> GrisMedio
                        else -> Advertencia
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            tarea.titulo,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = GrisOscuro,
                            modifier = Modifier.weight(1f)
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (tarea.estado) {
                                "activa" -> VerdeLima
                                "cerrada" -> GrisMedio
                                else -> Advertencia
                            }
                        ) {
                            Text(
                                when (tarea.estado) {
                                    "activa" -> "Activa"
                                    "cerrada" -> "Cerrada"
                                    else -> "Inactiva"
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = AzulCielo
                        )
                        Text(
                            tarea.moduloNombre,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AzulCielo,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FechaCard(
                    icon = Icons.Default.CalendarToday,
                    label = "Creación",
                    fecha = tarea.fechaCreacion,
                    color = AzulCielo,
                    modifier = Modifier.weight(1f)
                )

                FechaCard(
                    icon = Icons.Default.Event,
                    label = "Fecha límite",
                    fecha = tarea.fechaLimite,
                    color = if (estaVencida(tarea.fechaLimite)) ErrorOscuro else VerdeLima,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            TareaInfoSection(
                title = "Descripción",
                icon = Icons.Default.Description
            ) {
                Text(
                    tarea.descripcion,
                    style = MaterialTheme.typography.bodyLarge,
                    color = GrisOscuro,
                    lineHeight = 24.sp
                )
            }
        }

        if (tarea.archivos.isNotEmpty()) {
            item {
                TareaInfoSection(
                    title = "Archivos adjuntos",
                    icon = Icons.Default.AttachFile
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tarea.archivos.forEach { archivo ->
                            ArchivoAdjuntoCard(archivo)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun EntregasTab(
    entregas: List<EntregaEstudiante>,
    onEntregaClick: (String) -> Unit
) {
    if (entregas.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.Default.Assignment,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = GrisClaro
                )
                Text(
                    "No hay entregas aún",
                    style = MaterialTheme.typography.titleMedium,
                    color = GrisMedio,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Las entregas enviadas aparecerán aquí",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GrisMedio,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                EstadisticasEntregas(entregas)
            }

            items(entregas) { entrega ->
                EntregaEstudianteCard(
                    entrega = entrega,
                    onClick = { onEntregaClick(entrega.id) }
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun FechaCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    fecha: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = FondoCard,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = color
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = GrisMedio,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                formatearFecha(fecha),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun TareaInfoSection(
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
fun ArchivoAdjuntoCard(archivo: ArchivoAdjunto) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                abrirUrl(context, archivo.url)
            },
        shape = RoundedCornerShape(8.dp),
        color = Fucsia.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Fucsia.copy(alpha = 0.2f))
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
                color = Fucsia.copy(alpha = 0.15f)
            ) {
                Icon(
                    Icons.Default.AttachFile,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp),
                    tint = Fucsia
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    archivo.nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = GrisOscuro,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    archivo.tipo.uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    color = GrisMedio
                )
            }

            Icon(
                Icons.Default.Download,
                contentDescription = "Descargar",
                tint = Fucsia,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun EstadisticasEntregas(entregas: List<EntregaEstudiante>) {
    val enviadas = entregas.count { it.estado == "enviada" }
    val tarde = entregas.count { it.estado == "tarde" }
    val calificadas = entregas.count { it.estado == "calificada" }
    val pendientesCalificar = entregas.count { it.estado == "enviada" || it.estado == "tarde" }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = AzulCielo.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, AzulCielo.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            EstadisticaItem(
                count = calificadas,
                label = "Calificadas",
                color = VerdeLima
            )
            VerticalDivider(
                modifier = Modifier.height(40.dp),
                color = GrisClaro
            )
            EstadisticaItem(
                count = pendientesCalificar,
                label = "Pendientes",
                color = Advertencia
            )
            VerticalDivider(
                modifier = Modifier.height(40.dp),
                color = GrisClaro
            )
            EstadisticaItem(
                count = tarde,
                label = "Tarde",
                color = ErrorOscuro
            )
        }
    }
}

@Composable
fun EstadisticaItem(
    count: Int,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = GrisMedio,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun EntregaEstudianteCard(
    entrega: EntregaEstudiante,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = FondoCard,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (entrega.estudianteFoto != null) {
                AsyncImage(
                    model = entrega.estudianteFoto,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(GrisClaro),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = AzulCielo.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            entrega.estudianteNombre.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.titleLarge,
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
                    entrega.estudianteNombre,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = GrisOscuro,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (entrega.estado) {
                            "calificada" -> VerdeLima.copy(alpha = 0.15f)
                            "enviada" -> Advertencia.copy(alpha = 0.15f)
                            "tarde" -> ErrorOscuro.copy(alpha = 0.15f)
                            else -> GrisClaro
                        }
                    ) {
                        Text(
                            when (entrega.estado) {
                                "calificada" -> "Calificada"
                                "enviada" -> "Enviada"
                                "tarde" -> "Entrega tardía"
                                else -> "Pendiente"
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (entrega.estado) {
                                "calificada" -> VerdeLima
                                "enviada" -> Advertencia
                                "tarde" -> ErrorOscuro
                                else -> GrisMedio
                            }
                        )
                    }

                    if (entrega.nota != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = VerdeLima
                            )
                            Text(
                                String.format("%.1f", entrega.nota),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = VerdeLima
                            )
                        }
                    }
                }

                if (entrega.fechaEntrega != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = GrisMedio
                        )
                        Text(
                            formatearFecha(entrega.fechaEntrega),
                            style = MaterialTheme.typography.bodySmall,
                            color = GrisMedio
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (entrega.estado != "calificada") {
                    Surface(
                        shape = CircleShape,
                        color = AzulCielo.copy(alpha = 0.15f)
                    ) {
                        Icon(
                            Icons.Default.RateReview,
                            contentDescription = "Calificar",
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp),
                            tint = AzulCielo
                        )
                    }
                } else {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Calificada",
                        tint = VerdeLima,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

data class TareaDetalleProfesor(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val fechaCreacion: String,
    val fechaLimite: String,
    val estado: String,
    val moduloNombre: String,
    val archivos: List<ArchivoAdjunto>
)

data class EntregaEstudiante(
    val id: String,
    val estudianteNombre: String,
    val estudianteFoto: String?,
    val estado: String,
    val fechaEntrega: String?,
    val nota: Double?,
    val comentario: String?
)

data class EntregaDetalleCompleta(
    val id: String,
    val estudianteNombre: String,
    val estudianteFoto: String?,
    val estado: String,
    val fechaEntrega: String?,
    val textoRespuesta: String?,
    val archivos: List<ArchivoAdjuntoEntrega>,
    val notaActual: Double?,
    val comentarioActual: String?
)

data class ArchivoAdjuntoEntrega(
    val url: String,
    val nombreOriginal: String,
    val tipoArchivo: String,
    val tamano: Long
)