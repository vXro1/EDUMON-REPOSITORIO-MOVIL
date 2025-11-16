package com.example.edumonjetcompose

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
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.edumonjetcompose.models.*
import com.example.edumonjetcompose.network.ApiService
import com.example.edumonjetcompose.ui.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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

    val token = remember {
        context.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
            .getString("token", "") ?: ""
    }

    // Cargar datos de la tarea y entregas
    LaunchedEffect(tareaId) {
        isLoading = true
        try {
            // Obtener detalle de la tarea
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

                    // Módulo
                    val moduloObj = json.getAsJsonObject("moduloId")
                    val moduloNombre = moduloObj?.get("nombre")?.asString ?: "Sin módulo"

                    // Archivos
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

            // Obtener entregas de la tarea
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
                        val fechaEntrega = entregaObj.get("fechaEntrega")?.asString
                        val nota = entregaObj.get("nota")?.asDouble
                        val comentario = entregaObj.get("comentario")?.asString

                        // Información del padre/estudiante
                        val padreObj = entregaObj.getAsJsonObject("padreId")
                        val padreNombre = padreObj?.get("nombre")?.asString ?: ""
                        val padreApellido = padreObj?.get("apellido")?.asString ?: ""
                        val padreFoto = padreObj?.get("fotoPerfilUrl")?.asString

                        entregasList.add(
                            EntregaEstudiante(
                                id = id,
                                estudianteNombre = "$padreNombre $padreApellido",
                                estudianteFoto = padreFoto,
                                estado = estado,
                                fechaEntrega = fechaEntrega,
                                nota = nota,
                                comentario = comentario
                            )
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                entregas = entregasList
            }
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
                // Tabs
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

                // Contenido según tab seleccionada
                when (selectedTab) {
                    0 -> InformacionTareaTab(tarea!!)
                    1 -> EntregasTab(
                        entregas = entregas,
                        onEntregaClick = { entregaId ->
                            navController.navigate("detalle_entrega_profesor/$entregaId")
                        }
                    )
                }
            }
        }
    }

    // Diálogo de confirmación para eliminar
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

// ==================== TABS ====================

@Composable
fun InformacionTareaTab(tarea: TareaDetalleProfesor) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header con estado
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

        // Fechas
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

        // Descripción
        item {
            InfoSection(
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

        // Archivos adjuntos
        if (tarea.archivos.isNotEmpty()) {
            item {
                InfoSection(
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    color = GrisMedio
                )
                Text(
                    "Las entregas aparecerán aquí",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GrisMedio
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Estadísticas
            item {
                EstadisticasEntregas(entregas)
            }

            // Lista de entregas
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

// ==================== COMPONENTES ====================

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
                formatearFechaEntrega(fecha),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

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
fun ArchivoAdjuntoCard(archivo: ArchivoAdjunto) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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

            IconButton(
                onClick = { /* Descargar/abrir archivo */ },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = "Descargar",
                    tint = Fucsia
                )
            }
        }
    }
}

@Composable
fun EstadisticasEntregas(entregas: List<EntregaEstudiante>) {
    val enviadas = entregas.count { it.estado == "enviada" }
    val calificadas = entregas.count { it.estado == "calificada" }
    val pendientes = entregas.count { it.estado == "pendiente" || it.estado == "borrador" }

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
                count = enviadas,
                label = "Enviadas",
                color = Advertencia
            )
            VerticalDivider(
                modifier = Modifier.height(40.dp),
                color = GrisClaro
            )
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
                count = pendientes,
                label = "Pendientes",
                color = GrisMedio
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
            color = GrisMedio
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
            // Avatar
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

            // Información
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
                    // Estado
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (entrega.estado) {
                            "calificada" -> VerdeLima.copy(alpha = 0.15f)
                            "enviada" -> Advertencia.copy(alpha = 0.15f)
                            else -> GrisClaro
                        }
                    ) {
                        Text(
                            when (entrega.estado) {
                                "calificada" -> "Calificada"
                                "enviada" -> "Enviada"
                                "borrador" -> "Borrador"
                                else -> "Pendiente"
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (entrega.estado) {
                                "calificada" -> VerdeLima
                                "enviada" -> Advertencia
                                else -> GrisMedio
                            }
                        )
                    }

                    // Nota si está calificada
                    if (entrega.nota != null) {
                        Text(
                            "★ ${String.format("%.1f", entrega.nota)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = VerdeLima
                        )
                    }
                }

                // Fecha de entrega
                if (entrega.fechaEntrega != null) {
                    Text(
                        "Entregado: ${formatearFechaEntrega(entrega.fechaEntrega)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = GrisMedio
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = GrisMedio
            )
        }
    }
}

// ==================== MODELOS ====================

data class TareaDetalleProfesor(val id: String,
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

// ==================== FUNCIONES AUXILIARES ====================

fun estaVencida(fechaLimite: String): Boolean {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val fecha = sdf.parse(fechaLimite)
        val ahora = Date()
        fecha?.before(ahora) ?: false
    } catch (e: Exception) {
        false
    }
}

fun formatearFechaEntrega(fecha: String): String {
    return try {
        val sdfInput = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        sdfInput.timeZone = TimeZone.getTimeZone("UTC")
        val date = sdfInput.parse(fecha)

        val sdfOutput = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("es", "ES"))
        sdfOutput.format(date!!)
    } catch (e: Exception) {
        fecha
    }
}