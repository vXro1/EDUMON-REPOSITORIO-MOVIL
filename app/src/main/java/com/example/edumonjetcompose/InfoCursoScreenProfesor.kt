package com.example.edumonjetcompose.screens.profesor

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.edumonjetcompose.Naranja
import com.example.edumonjetcompose.models.*
import com.example.edumonjetcompose.network.ApiService
import com.example.edumonjetcompose.ui.*
import com.google.gson.JsonArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


// ==================== PANTALLA PRINCIPAL ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoCursoScreenProfesor(
    navController: NavController,
    cursoId: String,
    token: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var cursoDetalle by remember { mutableStateOf<CursoDetalleProfesor?>(null) }
    var modulos by remember { mutableStateOf<List<ModuloConTareas>>(emptyList()) }
    var foros by remember { mutableStateOf<List<ForoInfo>>(emptyList()) }
    var proximosEventos by remember { mutableStateOf<List<EventoInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showEditDialog by remember { mutableStateOf(false) }
    var showImagePicker by remember { mutableStateOf(false) }

    suspend fun cargarDatosCurso() {
        isLoading = true
        errorMessage = null

        try {
            // Cargar datos del curso
            val cursoResponse = withContext(Dispatchers.IO) {
                ApiService.getCursoById(token, cursoId)
            }

            if (!cursoResponse.isSuccessful) {
                errorMessage = "Error al cargar curso: ${cursoResponse.code()}"
                return
            }

            val cursoJson = cursoResponse.body()?.getAsJsonObject("curso")
            if (cursoJson == null) {
                errorMessage = "Datos del curso no encontrados"
                return
            }

            val docenteJson = cursoJson.getAsJsonObject("docenteId")
            val participantesArray = cursoJson.getAsJsonArray("participantes")

            cursoDetalle = CursoDetalleProfesor(
                id = cursoJson.get("_id")?.asString ?: cursoId,
                nombre = cursoJson.get("nombre")?.asString ?: "Mi Curso",
                descripcion = cursoJson.get("descripcion")?.asString ?: "Sin descripción",
                fotoPortadaUrl = cursoJson.get("fotoPortadaUrl")?.asString?.let {
                    if (it.startsWith("http")) it else BASE_URL + it
                },
                docenteNombre = docenteJson?.get("nombre")?.asString ?: "",
                docenteApellido = docenteJson?.get("apellido")?.asString ?: "",
                docenteFotoUrl = docenteJson?.get("fotoPerfilUrl")?.asString?.let {
                    if (it.startsWith("http")) it else BASE_URL + it
                },
                participantesCount = participantesArray?.size() ?: 0,
                modulosCount = 0
            )

            // Cargar módulos y tareas
            val modulosResponse = withContext(Dispatchers.IO) {
                ApiService.getModulosByCurso(token, cursoId)
            }

            val tareasResponse = withContext(Dispatchers.IO) {
                ApiService.getTareas(token, cursoId = cursoId)
            }

            if (modulosResponse.isSuccessful && tareasResponse.isSuccessful) {
                val modulosBody = modulosResponse.body()
                val tareasBody = tareasResponse.body()

                val modulosJson = when {
                    modulosBody?.has("modulos") == true -> modulosBody.getAsJsonArray("modulos")
                    modulosBody?.isJsonArray == true -> modulosBody.asJsonArray
                    else -> JsonArray()
                }

                val tareasJson = when {
                    tareasBody?.has("tareas") == true -> {
                        val tareasElement = tareasBody.get("tareas")
                        if (tareasElement.isJsonArray) tareasElement.asJsonArray else JsonArray()
                    }
                    tareasBody?.isJsonArray == true -> tareasBody.asJsonArray
                    else -> JsonArray()
                }

                val modulosList = mutableListOf<ModuloConTareas>()

                for (mElem in modulosJson) {
                    val m = mElem.asJsonObject
                    val moduloId = m.get("_id")?.asString.orEmpty()
                    val tareasModulo = mutableListOf<TareaInfo>()

                    for (tElem in tareasJson) {
                        val t = tElem.asJsonObject
                        val tareaModuloId = when {
                            t.has("moduloId") && !t.get("moduloId").isJsonNull -> {
                                when {
                                    t.get("moduloId").isJsonPrimitive -> t.get("moduloId").asString
                                    t.get("moduloId").isJsonObject -> t.getAsJsonObject("moduloId").get("_id")?.asString
                                    else -> null
                                }
                            }
                            else -> null
                        }

                        if (tareaModuloId == moduloId) {
                            val fechaEntrega = t.get("fechaLimite")?.asString ?: t.get("fechaEntrega")?.asString ?: ""
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
            }

            // Cargar foros
            val forosResponse = withContext(Dispatchers.IO) {
                ApiService.getForosPorCurso(token, cursoId)
            }

            if (forosResponse.isSuccessful) {
                val forosBody = forosResponse.body()
                val forosJson = when {
                    forosBody?.has("foros") == true -> forosBody.getAsJsonArray("foros")
                    forosBody?.isJsonArray == true -> forosBody.asJsonArray
                    else -> JsonArray()
                }

                val forosList = mutableListOf<ForoInfo>()
                for (foroElem in forosJson) {
                    val f = foroElem.asJsonObject
                    val mensajesArray = f.getAsJsonArray("mensajes")

                    forosList.add(
                        ForoInfo(
                            id = f.get("_id")?.asString ?: "",
                            titulo = f.get("titulo")?.asString ?: "Sin título",
                            descripcion = f.get("descripcion")?.asString ?: "",
                            fechaCreacion = f.get("fechaCreacion")?.asString ?: "",
                            mensajesCount = mensajesArray?.size() ?: 0
                        )
                    )
                }
                foros = forosList
            }

            // Cargar próximos eventos
            val eventosResponse = withContext(Dispatchers.IO) {
                ApiService.getProximosEventos(token, cursoId, limite = 5)
            }

            if (eventosResponse.isSuccessful) {
                val eventosBody = eventosResponse.body()
                val eventosJson = when {
                    eventosBody?.has("eventos") == true -> eventosBody.getAsJsonArray("eventos")
                    eventosBody?.isJsonArray == true -> eventosBody.asJsonArray
                    else -> JsonArray()
                }

                val eventosList = mutableListOf<EventoInfo>()
                for (eventoElem in eventosJson) {
                    val e = eventoElem.asJsonObject
                    eventosList.add(
                        EventoInfo(
                            id = e.get("_id")?.asString ?: "",
                            titulo = e.get("titulo")?.asString ?: "Sin título",
                            descripcion = e.get("descripcion")?.asString ?: "",
                            fecha = e.get("fechaInicio")?.asString ?: "",
                            hora = e.get("hora")?.asString,
                            categoria = e.get("categoria")?.asString ?: "general"
                        )
                    )
                }
                proximosEventos = eventosList
            }

        } catch (e: Exception) {
            errorMessage = "Error de conexión: ${e.message}"
            Log.e("InfoCursoProfesor", "Error cargando datos", e)
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(cursoId) {
        cargarDatosCurso()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión del Curso", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, "Editar curso", tint = AzulCielo)
                    }
                    IconButton(onClick = { showImagePicker = true }) {
                        Icon(Icons.Default.Image, "Cambiar portada", tint = Fucsia)
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
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> LoadingView()
                errorMessage != null -> ErrorView(
                    message = errorMessage ?: "",
                    onRetry = { scope.launch { cargarDatosCurso() } },
                    onBack = { navController.popBackStack() }
                )
                cursoDetalle != null -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item { CursoHeader(cursoDetalle!!, modulos.size, cursoDetalle!!.participantesCount, calcularTotalTareas(modulos)) }
                        item { QuickActions(cursoId, navController) }
                        item {
                            Spacer(Modifier.height(16.dp))
                            AddTareaButton { navController.navigate("crearTarea/$cursoId") }
                        }

                        item {
                            Spacer(Modifier.height(24.dp))
                            SectionHeader(
                                icon = Icons.Default.Forum,
                                title = "Foros de Discusión",
                                subtitle = "${foros.size} foros activos",
                                color = Fucsia
                            )
                        }

                        if (foros.isEmpty()) {
                            item { EmptyForosView() }
                        } else {
                            items(foros) { foro ->
                                ForoCard(
                                    foro = foro,
                                    onClick = { navController.navigate("foro_detalle/${foro.id}") }
                                )
                                Spacer(Modifier.height(12.dp))
                            }
                        }

                        item {
                            Spacer(Modifier.height(24.dp))
                            SectionHeader(
                                icon = Icons.Default.CalendarMonth,
                                title = "Próximas Actividades",
                                subtitle = "${proximosEventos.size} eventos programados",
                                color = Naranja
                            )
                        }

                        if (proximosEventos.isEmpty()) {
                            item { EmptyEventosView() }
                        } else {
                            items(proximosEventos) { evento ->
                                EventoCard(evento = evento)
                                Spacer(Modifier.height(12.dp))
                            }
                        }

                        item {
                            Spacer(Modifier.height(24.dp))
                            SectionHeader(
                                icon = Icons.Default.Assignment,
                                title = "Tareas del Curso",
                                subtitle = "${calcularTotalTareas(modulos)} tareas en ${modulos.size} módulos",
                                color = VerdeLima
                            )
                        }

                        if (modulos.isEmpty() || modulos.all { it.tareas.isEmpty() }) {
                            item { EmptyTareasView() }
                        } else {
                            items(modulos) { modulo ->
                                ModuloSection(
                                    modulo = modulo,
                                    onTareaClick = { tareaId ->
                                        navController.navigate("detalleTareaProfesor/$tareaId")
                                    }
                                )
                            }
                        }

                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }

            if (showEditDialog && cursoDetalle != null) {
                EditCursoDialog(
                    curso = cursoDetalle!!,
                    token = token,
                    onDismiss = { showEditDialog = false },
                    onSuccess = {
                        showEditDialog = false
                        scope.launch {
                            cargarDatosCurso()
                            snackbarHostState.showSnackbar("Curso actualizado")
                        }
                    }
                )
            }

            if (showImagePicker && cursoDetalle != null) {
                ImagePickerDialog(
                    cursoId = cursoDetalle!!.id,
                    token = token,
                    onDismiss = { showImagePicker = false },
                    onSuccess = {
                        showImagePicker = false
                        scope.launch {
                            cargarDatosCurso()
                            snackbarHostState.showSnackbar("Portada actualizada")
                        }
                    }
                )
            }
        }
    }
}

// ==================== COMPONENTES REUTILIZABLES ====================

data class ActionButton(
    val icon: ImageVector,
    val label: String,
    val color: Color,
    val route: String
)

@Composable
fun CursoHeader(curso: CursoDetalleProfesor, modulosCount: Int, participantesCount: Int, tareasCount: Int) {
    Column {
        Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color(0xFF667EEA), Color(0xFF764BA2)))
                )
            )

            if (curso.fotoPortadaUrl != null) {
                AsyncImage(
                    model = curso.fotoPortadaUrl,
                    contentDescription = "Portada",
                    modifier = Modifier.fillMaxSize().alpha(0.4f),
                    contentScale = ContentScale.Crop
                )
            }

            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF667EEA).copy(alpha = 0.9f)),
                        startY = 0f,
                        endY = 900f
                    )
                )
            )

            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Badge(text = "Modo Profesor", icon = Icons.Default.School)
                Text(
                    text = curso.nombre,
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth().offset(y = (-30).dp),
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            color = FondoClaro,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = curso.descripcion,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
                    color = GrisMedio
                )

                HorizontalDivider(color = GrisClaro)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatsCard(Icons.Default.People, participantesCount.toString(), "Estudiantes", Fucsia)
                    StatsCard(Icons.Default.Book, modulosCount.toString(), "Módulos", AzulCielo)
                    StatsCard(Icons.Default.Assignment, tareasCount.toString(), "Tareas", VerdeLima)
                }
            }
        }
    }
}

@Composable
fun SectionHeader(icon: ImageVector, title: String, subtitle: String, color: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(2.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(shape = CircleShape, color = color, modifier = Modifier.size(48.dp)) {
                Icon(icon, null, modifier = Modifier.padding(12.dp), tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.Bold,
                    color = GrisOscuro
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GrisMedio,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun Badge(text: String, icon: ImageVector) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.25f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun StatsCard(icon: ImageVector, value: String, label: String, color: Color) {
    Surface(
        modifier = Modifier.width(110.dp).height(110.dp),
        shape = RoundedCornerShape(20.dp),
        color = FondoCard,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, GrisClaro)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(icon, null, modifier = Modifier.padding(10.dp), tint = color)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                fontWeight = FontWeight.ExtraBold,
                color = GrisOscuro
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                textAlign = TextAlign.Center,
                color = GrisMedio,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun QuickActions(cursoId: String, navController: NavController) {
    val actions = listOf(
        ActionButton(Icons.Default.PersonAdd, "Participantes", Color(0xFF667EEA), "participantesCurso/$cursoId"),
        ActionButton(Icons.Default.Forum, "Crear Foro", Color(0xFFf093fb), "crearForo/$cursoId"),
        ActionButton(Icons.Default.CalendarMonth, "Crear Evento", Color(0xFFf5576c), "crearEvento/$cursoId"),
        ActionButton(Icons.Default.Assignment, "Nueva Tarea", Color(0xFF38ef7d), "crearTarea/$cursoId")
    )

    Surface(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        color = FondoCard,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Accesos Rápidos",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = GrisOscuro
            )

            actions.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { action ->
                        ActionCard(
                            modifier = Modifier.weight(1f),
                            action = action,
                            onClick = { navController.navigate(action.route) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActionCard(
    modifier: Modifier = Modifier,
    action: ActionButton,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.height(100.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = action.color.copy(alpha = 0.1f),
        border = BorderStroke(2.dp, action.color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(shape = CircleShape, color = action.color, modifier = Modifier.size(48.dp)) {
                Icon(action.icon, null, modifier = Modifier.padding(12.dp), tint = Color.White)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = action.label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = action.color,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ForoCard(foro: ForoInfo, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = FondoCard,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Fucsia.copy(alpha = 0.15f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Forum,
                            null,
                            modifier = Modifier.padding(12.dp),
                            tint = Fucsia
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = foro.titulo,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                            fontWeight = FontWeight.Bold,
                            color = GrisOscuro,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${foro.mensajesCount} mensajes",
                            style = MaterialTheme.typography.bodySmall,
                            color = GrisMedio
                        )
                    }
                }

                Surface(shape = CircleShape, color = Fucsia, modifier = Modifier.size(36.dp)) {
                    IconButton(onClick = onClick) {
                        Icon(Icons.Default.ArrowForward, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (foro.descripcion.isNotBlank()) {
                Text(
                    text = foro.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GrisMedio,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun EventoCard(evento: EventoInfo) {
    val categoriaColor = when (evento.categoria) {
        "examen" -> ErrorOscuro
        "clase" -> AzulCielo
        "entrega" -> Advertencia
        else -> VerdeLima
    }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = FondoCard,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = categoriaColor.copy(alpha = 0.15f),
                modifier = Modifier.size(60.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = obtenerDiaEvento(evento.fecha),
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = categoriaColor
                    )
                    Text(
                        text = obtenerMesEvento(evento.fecha),
                        style = MaterialTheme.typography.labelSmall,
                        color = categoriaColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = evento.titulo,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                        fontWeight = FontWeight.Bold,
                        color = GrisOscuro,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = categoriaColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = evento.categoria.uppercase(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold,
                            color = categoriaColor
                        )
                    }
                }

                if (evento.descripcion.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = evento.descripcion,
                        style = MaterialTheme.typography.bodySmall,
                        color = GrisMedio,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (evento.hora != null) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            null,
                            tint = AzulCielo,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = evento.hora,
                            style = MaterialTheme.typography.bodySmall,
                            color = AzulCielo,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddTareaButton(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = VerdeLima,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Text("Crear Nueva Tarea", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
fun ModuloSection(modulo: ModuloConTareas, onTareaClick: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF3E5F5),
            border = BorderStroke(2.dp, Color(0xFFBA68C8))
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(shape = CircleShape, color = Color(0xFFBA68C8), modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.FolderOpen, null, modifier = Modifier.padding(12.dp), tint = Color.White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        modulo.nombre,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.Bold,
                        color = GrisOscuro
                    )
                    Text(
                        "${modulo.tareas.size} tareas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GrisMedio,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (modulo.tareas.isEmpty()) {
            ModuloVacioCard()
        } else {
            modulo.tareas.forEach { tarea ->
                TareaCard(tarea = tarea, onClick = { onTareaClick(tarea.id) })
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun ModuloVacioCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = GrisExtraClaro
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Info, null, tint = GrisMedio)
            Text("No hay tareas en este módulo", style = MaterialTheme.typography.bodyMedium, color = GrisMedio)
        }
    }
}

@Composable
fun TareaCard(tarea: TareaInfo, onClick: () -> Unit) {
    val (estadoColor, estadoFondo, estadoTexto) = when {
        tarea.estaVencida -> Triple(ErrorOscuro, ErrorClaro, "VENCIDA")
        tarea.estado == "cerrada" -> Triple(Advertencia, AdvertenciaClaro, "CERRADA")
        else -> Triple(Exito, ExitoClaro, "ACTIVA")
    }

    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = FondoCard,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = tarea.titulo,
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = GrisOscuro
                )

                Surface(shape = RoundedCornerShape(12.dp), color = estadoFondo) {
                    Text(
                        text = estadoTexto,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = estadoColor
                    )
                }
            }

            Text(
                text = tarea.descripcion,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp),
                color = GrisMedio,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            HorizontalDivider(color = GrisClaro)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (tarea.estaVencida) ErrorClaro else AzulCieloMuyClaro,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            null,
                            modifier = Modifier.padding(10.dp),
                            tint = if (tarea.estaVencida) ErrorOscuro else AzulCielo
                        )
                    }
                    Column {
                        Text(
                            "Fecha límite",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = GrisMedio
                        )
                        Text(
                            formatearFechaEntrega(tarea.fechaEntrega),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            fontWeight = FontWeight.Bold,
                            color = if (tarea.estaVencida) ErrorOscuro else GrisOscuro
                        )
                    }
                }

                Surface(shape = CircleShape, color = AzulCielo, modifier = Modifier.size(44.dp)) {
                    IconButton(onClick = onClick) {
                        Icon(Icons.Default.ArrowForward, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyForosView() {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = FondoCard,
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(shape = CircleShape, color = Fucsia.copy(alpha = 0.15f), modifier = Modifier.size(80.dp)) {
                    Icon(Icons.Default.Forum, null, modifier = Modifier.padding(20.dp), tint = Fucsia)
                }
                Text(
                    "No hay foros creados",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GrisOscuro
                )
                Text(
                    "Crea un foro para iniciar discusiones con tus estudiantes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GrisMedio,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun EmptyEventosView() {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = FondoCard,
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(shape = CircleShape, color = Naranja.copy(alpha = 0.15f), modifier = Modifier.size(80.dp)) {
                    Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.padding(20.dp), tint = Naranja)
                }
                Text(
                    "No hay eventos próximos",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = GrisOscuro
                )
                Text(
                    "Programa eventos y actividades para tu curso",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GrisMedio,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun EmptyTareasView() {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = FondoCard,
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Surface(shape = CircleShape, color = VerdeLima.copy(alpha = 0.15f), modifier = Modifier.size(100.dp)) {
                    Icon(Icons.Default.Assignment, null, modifier = Modifier.padding(24.dp), tint = VerdeLima)
                }
                Text(
                    "No hay tareas creadas",
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                    fontWeight = FontWeight.Bold,
                    color = GrisOscuro
                )
                Text(
                    "Comienza creando tu primera tarea para este curso",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp),
                    color = GrisMedio,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun LoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(64.dp), strokeWidth = 6.dp, color = Color(0xFF667EEA))
            Text("Cargando curso...", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

// ==================== DIÁLOGOS ====================

@Composable
fun EditCursoDialog(curso: CursoDetalleProfesor, token: String, onDismiss: () -> Unit, onSuccess: () -> Unit) {
    var nombre by remember { mutableStateOf(curso.nombre) }
    var descripcion by remember { mutableStateOf(curso.descripcion) }
    var isUpdating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = FondoCard, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Editar Curso", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Cerrar", tint = GrisMedio)
                    }
                }

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre del curso") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AzulCielo,
                        focusedLabelColor = AzulCielo
                    )
                )

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AzulCielo,
                        focusedLabelColor = AzulCielo
                    )
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), enabled = !isUpdating) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                isUpdating = true
                                try {
                                    val response = ApiService.updateCurso(
                                        token = token,
                                        cursoId = curso.id,
                                        nombre = nombre,
                                        descripcion = descripcion,
                                        fotoPortadaFile = null
                                    )
                                    if (response.isSuccessful) onSuccess()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    isUpdating = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isUpdating && nombre.isNotBlank() && descripcion.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = AzulCielo)
                    ) {
                        if (isUpdating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Guardar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImagePickerDialog(cursoId: String, token: String, onDismiss: () -> Unit, onSuccess: () -> Unit) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> selectedImageUri = uri }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = FondoCard, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cambiar Portada", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Cerrar", tint = GrisMedio)
                    }
                }

                if (selectedImageUri != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = GrisExtraClaro
                    ) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Vista previa",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(200.dp).clickable { imagePickerLauncher.launch("image/*") },
                        shape = RoundedCornerShape(16.dp),
                        color = GrisExtraClaro,
                        border = BorderStroke(2.dp, AzulCielo.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(64.dp), tint = AzulCielo.copy(alpha = 0.6f))
                            Spacer(Modifier.height(12.dp))
                            Text("Toca para seleccionar imagen", color = GrisMedio, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (selectedImageUri != null) {
                        OutlinedButton(onClick = { selectedImageUri = null }, modifier = Modifier.weight(1f), enabled = !isUploading) {
                            Text("Cambiar")
                        }
                    } else {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), enabled = !isUploading) {
                            Text("Cancelar")
                        }
                    }

                    Button(
                        onClick = {
                            selectedImageUri?.let { uri ->
                                scope.launch {
                                    isUploading = true
                                    try {
                                        val inputStream = context.contentResolver.openInputStream(uri)
                                        val file = File(context.cacheDir, "portada_${System.currentTimeMillis()}.jpg")
                                        file.outputStream().use { inputStream?.copyTo(it) }

                                        val response = ApiService.updateCurso(
                                            token = token,
                                            cursoId = cursoId,
                                            nombre = null,
                                            descripcion = null,
                                            fotoPortadaFile = file
                                        )

                                        file.delete()
                                        if (response.isSuccessful) onSuccess()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        isUploading = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isUploading && selectedImageUri != null,
                        colors = ButtonDefaults.buttonColors(containerColor = Fucsia)
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Subir")
                        }
                    }
                }
            }
        }
    }
}

// ==================== FUNCIONES AUXILIARES ====================

fun calcularTotalTareas(modulos: List<ModuloConTareas>): Int = modulos.sumOf { it.tareas.size }

fun obtenerDiaEvento(fecha: String): String {
    return try {
        fecha.split("-").getOrNull(2)?.take(2) ?: "?"
    } catch (e: Exception) {
        "?"
    }
}

fun obtenerMesEvento(fecha: String): String {
    return try {
        val mes = fecha.split("-").getOrNull(1)?.toIntOrNull() ?: return "?"
        val meses = listOf("", "ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SEP", "OCT", "NOV", "DIC")
        meses.getOrNull(mes) ?: "?"
    } catch (e: Exception) {
        "?"
    }
}