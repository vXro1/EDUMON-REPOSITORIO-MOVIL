package com.example.edumonjetcompose.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import com.example.edumonjetcompose.data.UserPreferences
import com.example.edumonjetcompose.network.ApiService
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.launch

private const val BASE_URL = "https://backend-edumon.onrender.com"

data class CursoDetalle(
    val nombre: String,
    val descripcion: String,
    val fotoPortadaUrl: String?,
    val docenteNombre: String,
    val docenteApellido: String,
    val participantesCount: Int,
    val modulosCount: Int
)

data class ModuloConTareas(
    val id: String,
    val nombre: String,
    val tareas: List<TareaInfo>
)

data class TareaInfo(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val fechaEntrega: String,
    val estado: String,
    val estaVencida: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoCursoScreen(
    navController: NavController,
    cursoId: String,
    token: String,
    onNavigateToTarea: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var cursoDetalle by remember { mutableStateOf<CursoDetalle?>(null) }
    var modulos by remember { mutableStateOf<List<ModuloConTareas>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(0) }

    fun cargarDatosCurso() {
        scope.launch {
            isLoading = true
            errorMessage = null

            try {
                val cursoResponse = ApiService.getCursoById(token, cursoId)
                val modulosResponse = ApiService.getModulosByCurso(token, cursoId)
                val tareasResponse = ApiService.getTareas(token, cursoId = cursoId)

                if (cursoResponse.isSuccessful && modulosResponse.isSuccessful && tareasResponse.isSuccessful) {
                    val cursoBody = cursoResponse.body()
                    val modulosBody = modulosResponse.body()
                    val tareasBody = tareasResponse.body()

                    // Extraer curso
                    val cursoJson = cursoBody?.getAsJsonObject("curso")
                    val docenteJson = cursoJson?.getAsJsonObject("docenteId")
                    val participantesArray = cursoJson?.getAsJsonArray("participantes")

                    cursoDetalle = CursoDetalle(
                        nombre = cursoJson?.get("nombre")?.asString ?: "Sin nombre",
                        descripcion = cursoJson?.get("descripcion")?.asString ?: "Sin descripción",
                        fotoPortadaUrl = cursoJson?.get("fotoPortadaUrl")?.asString?.let {
                            if (it.startsWith("http")) it else BASE_URL + it
                        },
                        docenteNombre = docenteJson?.get("nombre")?.asString ?: "",
                        docenteApellido = docenteJson?.get("apellido")?.asString ?: "",
                        participantesCount = participantesArray?.size() ?: 0,
                        modulosCount = 0
                    )

                    // Extraer módulos
                    val modulosJson = when {
                        modulosBody?.has("modulos") == true -> modulosBody.getAsJsonArray("modulos")
                        modulosBody?.isJsonArray == true -> modulosBody.asJsonArray
                        else -> JsonArray()
                    }

                    // Extraer tareas
                    val tareasJson = when {
                        tareasBody?.has("tareas") == true -> {
                            val tareasElement = tareasBody.get("tareas")
                            if (tareasElement.isJsonArray) tareasElement.asJsonArray else JsonArray()
                        }
                        tareasBody?.isJsonArray == true -> tareasBody.asJsonArray
                        else -> JsonArray()
                    }

                    // Agrupar tareas por módulo
                    val modulosList = mutableListOf<ModuloConTareas>()

                    for (mElem in modulosJson) {
                        val m = mElem.asJsonObject
                        val moduloId = m.get("_id")?.asString.orEmpty()

                        val tareasModulo = mutableListOf<TareaInfo>()

                        for (tElem in tareasJson) {
                            val t = tElem.asJsonObject

                            val tareaModuloId = when {
                                t.has("moduloId") && t.get("moduloId").isJsonPrimitive ->
                                    t.get("moduloId").asString
                                t.has("moduloId") && t.get("moduloId").isJsonObject ->
                                    t.getAsJsonObject("moduloId").get("_id")?.asString
                                else -> null
                            }

                            if (tareaModuloId == moduloId) {
                                val fechaEntrega = t.get("fechaEntrega")?.asString ?: ""
                                val estadoTarea = t.get("estado")?.asString ?: "publicada"
                                val estaVencida = estaFechaVencida(fechaEntrega) && estadoTarea == "publicada"

                                tareasModulo.add(
                                    TareaInfo(
                                        id = t.get("_id")?.asString ?: "",
                                        titulo = t.get("titulo")?.asString ?: "Sin título",
                                        descripcion = t.get("descripcion")?.asString ?: "Sin descripción",
                                        fechaEntrega = fechaEntrega,
                                        estado = estadoTarea,
                                        estaVencida = estaVencida
                                    )
                                )
                            }
                        }

                        modulosList.add(
                            ModuloConTareas(
                                id = moduloId,
                                nombre = m.get("nombre")?.asString ?: "Sin nombre",
                                tareas = tareasModulo
                            )
                        )
                    }

                    modulos = modulosList
                    cursoDetalle = cursoDetalle?.copy(modulosCount = modulosList.size)

                } else {
                    errorMessage = "Error al cargar el curso"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "Error de conexión: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        cargarDatosCurso()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Información del Curso") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
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
                isLoading -> {
                    LoadingView()
                }
                errorMessage != null -> {
                    ErrorView(
                        message = errorMessage ?: "",
                        onRetry = { cargarDatosCurso() },
                        onBack = { navController.popBackStack() }
                    )
                }
                cursoDetalle != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Header con imagen
                        item {
                            CursoHeader(
                                curso = cursoDetalle!!,
                                modulos = modulos
                            )
                        }

                        // Tabs de navegación
                        item {
                            TabsSection(
                                selectedTab = selectedTab,
                                onTabSelected = { selectedTab = it }
                            )
                        }

                        // Contenido según tab seleccionada
                        when (selectedTab) {
                            0 -> {
                                // Tareas
                                if (modulos.isEmpty()) {
                                    item {
                                        EmptyTareasView()
                                    }
                                } else {
                                    items(modulos) { modulo ->
                                        ModuloSection(
                                            modulo = modulo,
                                            onTareaClick = onNavigateToTarea
                                        )
                                    }
                                }
                            }
                            1 -> {
                                // Foro - Navegación con botón
                                item {
                                    NavigationFeatureView(
                                        icon = Icons.Default.Forum,
                                        titulo = "Foro del Curso",
                                        descripcion = "Participa en discusiones con tus compañeros y docentes.",
                                        buttonText = "Ir al Foro",
                                        onClick = {
                                            navController.navigate("foro/$cursoId")
                                        }
                                    )
                                }
                            }
                            2 -> {
                                // Calendario - Navegación con botón
                                item {
                                    NavigationFeatureView(
                                        icon = Icons.Default.CalendarMonth,
                                        titulo = "Calendario del Curso",
                                        descripcion = "Vista de calendario con fechas importantes y entregas programadas.",
                                        buttonText = "Ver Calendario",
                                        onClick = {
                                            navController.navigate("calendario/$cursoId")
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CursoHeader(
    curso: CursoDetalle,
    modulos: List<ModuloConTareas>
) {
    Column {
        // Imagen de portada
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            if (curso.fotoPortadaUrl != null) {
                AsyncImage(
                    model = curso.fotoPortadaUrl,
                    contentDescription = "Portada del curso",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                )
            }

            // Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )

            // Título sobre la imagen
            Text(
                text = curso.nombre,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Información del curso
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Descripción
            Text(
                text = curso.descripcion,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            Divider()

            // Docente
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Column {
                    Text(
                        text = "Docente",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "${curso.docenteNombre} ${curso.docenteApellido}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Estadísticas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard(
                    icon = Icons.Default.People,
                    value = curso.participantesCount.toString(),
                    label = "Participantes"
                )
                StatCard(
                    icon = Icons.Default.Book,
                    value = curso.modulosCount.toString(),
                    label = "Módulos"
                )
                StatCard(
                    icon = Icons.Default.Assignment,
                    value = contarTareasPendientes(modulos).toString(),
                    label = "Tareas"
                )
            }
        }
    }
}

@Composable
fun StatCard(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    Card(
        modifier = Modifier.width(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TabsSection(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    TabRow(
        selectedTabIndex = selectedTab,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        Tab(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            text = { Text("Tareas") },
            icon = { Icon(Icons.Default.Assignment, null) }
        )
        Tab(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            text = { Text("Foro") },
            icon = { Icon(Icons.Default.Forum, null) }
        )
        Tab(
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            text = { Text("Calendario") },
            icon = { Icon(Icons.Default.CalendarMonth, null) }
        )
    }
}

@Composable
fun ModuloSection(modulo: ModuloConTareas, onTareaClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Header del módulo
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = modulo.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Tareas del módulo
        if (modulo.tareas.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "No hay tareas en este módulo",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            modulo.tareas.forEach { tarea ->
                TareaCardModerna(
                    tarea = tarea,
                    onClick = { onTareaClick(tarea.id) }
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun TareaCardModerna(tarea: TareaInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header con título y estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = tarea.titulo,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        tarea.estaVencida -> Color(0xFFFFEBEE)
                        tarea.estado == "cerrada" -> Color(0xFFFFF3E0)
                        else -> Color(0xFFE8F5E9)
                    }
                ) {
                    Text(
                        text = when {
                            tarea.estaVencida -> "VENCIDA"
                            tarea.estado == "cerrada" -> "CERRADA"
                            else -> "ACTIVA"
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            tarea.estaVencida -> Color(0xFFC62828)
                            tarea.estado == "cerrada" -> Color(0xFFEF6C00)
                            else -> Color(0xFF2E7D32)
                        }
                    )
                }
            }

            // Descripción
            Text(
                text = tarea.descripcion,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Divider()

            // Footer con fecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (tarea.estaVencida)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = formatFecha(tarea.fechaEntrega),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (tarea.estaVencida)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun NavigationFeatureView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    descripcion: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Icono principal
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Título
            Text(
                text = titulo,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            // Descripción
            Text(
                text = descripcion,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            // Botón de navegación
            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun EmptyTareasView() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Assignment,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Text(
                text = "No hay tareas disponibles",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Este curso aún no tiene tareas asignadas",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun LoadingView() {
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
                strokeWidth = 4.dp
            )
            Text(
                text = "Cargando curso...",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
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
                text = "Error al cargar",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Volver")
                }
                Button(onClick = onRetry) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Reintentar")
                }
            }
        }
    }
}

private fun formatFecha(fecha: String): String {
    return try {
        if (fecha.isNotEmpty()) {
            val partes = fecha.substring(0, 10).split("-")
            "${partes[2]}/${partes[1]}/${partes[0]}"
        } else {
            "Sin fecha"
        }
    } catch (e: Exception) {
        "Fecha inválida"
    }
}

private fun estaFechaVencida(fecha: String): Boolean {
    return try {
        val fechaTarea = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).parse(fecha)
        fechaTarea?.before(java.util.Date()) ?: false
    } catch (e: Exception) {
        false
    }
}

private fun contarTareasPendientes(modulos: List<ModuloConTareas>): Int {
    return modulos.sumOf { modulo ->
        modulo.tareas.count { it.estado == "publicada" && !it.estaVencida }
    }
}