package com.example.edumonjetcompose.screens.profesor

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
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
import java.util.*

// ==================== PANTALLA PRINCIPAL ====================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun InfoCursoScreenProfesor(
    navController: NavController,
    cursoId: String,
    token: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val pagerState = rememberPagerState(pageCount = { 3 })

    var cursoDetalle by remember { mutableStateOf<CursoDetalleProfesor?>(null) }
    var modulos by remember { mutableStateOf<List<ModuloConTareas>>(emptyList()) }
    var foros by remember { mutableStateOf<List<ForoInfo>>(emptyList()) }
    var proximosEventos by remember { mutableStateOf<List<EventoInfo>>(emptyList()) }
    var eventosDelMes by remember { mutableStateOf<Map<Int, List<EventoInfo>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showEditDialog by remember { mutableStateOf(false) }
    var showImagePicker by remember { mutableStateOf(false) }
    var selectedMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) }
    var selectedYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }

    suspend fun cargarDatosCurso() {
        isLoading = true
        errorMessage = null

        try {
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

            // 📅 CARGAR PRÓXIMOS EVENTOS CORREGIDO
            try {
                Log.d("InfoCursoProfesor", "🔍 Cargando próximos eventos del curso")

                val eventosResponse = withContext(Dispatchers.IO) {
                    ApiService.getProximosEventos(token, cursoId, limite = 5)
                }

                if (eventosResponse.isSuccessful) {
                    val eventosBody = eventosResponse.body()
                    Log.d("InfoCursoProfesor", "✅ Respuesta próximos eventos: ${eventosBody?.toString()}")

                    val proximosEventosArray = eventosBody?.getAsJsonArray("proximosEventos") ?: JsonArray()

                    val eventosList = mutableListOf<EventoInfo>()
                    for (eventoElem in proximosEventosArray) {
                        val e = eventoElem.asJsonObject
                        val tipo = e.get("tipo")?.asString ?: ""

                        // Agregar tanto tareas como eventos
                        eventosList.add(
                            EventoInfo(
                                id = e.get("id")?.asString ?: "",
                                titulo = e.get("titulo")?.asString ?: "Sin título",
                                descripcion = "",
                                fecha = e.get("fecha")?.asString ?: "",
                                hora = null,
                                categoria = if (tipo == "tarea") "tarea" else "evento"
                            )
                        )
                    }
                    proximosEventos = eventosList
                    Log.d("InfoCursoProfesor", "✅ Próximos eventos cargados: ${eventosList.size}")
                } else {
                    Log.e("InfoCursoProfesor", "❌ Error en respuesta: ${eventosResponse.code()}")
                }
            } catch (e: Exception) {
                Log.e("InfoCursoProfesor", "❌ Error cargando próximos eventos", e)
            }

            // 📅 CARGAR CALENDARIO DEL MES CORREGIDO
            try {
                Log.d("InfoCursoProfesor", "🔍 Cargando calendario: mes=$selectedMonth, año=$selectedYear")

                val calendarioResponse = withContext(Dispatchers.IO) {
                    ApiService.getCalendarioCurso(token, cursoId, selectedMonth, selectedYear)
                }

                if (calendarioResponse.isSuccessful) {
                    val calendarioBody = calendarioResponse.body()
                    Log.d("InfoCursoProfesor", "✅ Respuesta calendario: ${calendarioBody?.toString()}")

                    val itemsArray = calendarioBody?.getAsJsonArray("items") ?: JsonArray()

                    val eventosPorDia = mutableMapOf<Int, MutableList<EventoInfo>>()

                    for (itemElem in itemsArray) {
                        val item = itemElem.asJsonObject
                        val fechaInicio = item.get("fecha")?.asString ?: continue
                        val dia = obtenerDiaNumero(fechaInicio)
                        val tipo = item.get("tipo")?.asString ?: "evento"

                        val evento = EventoInfo(
                            id = item.get("id")?.asString ?: "",
                            titulo = item.get("titulo")?.asString ?: "Sin título",
                            descripcion = item.get("descripcion")?.asString ?: "",
                            fecha = fechaInicio,
                            hora = item.get("hora")?.asString,
                            categoria = if (tipo == "tarea") "tarea" else (item.get("categoria")?.asString ?: "general")
                        )

                        eventosPorDia.getOrPut(dia) { mutableListOf() }.add(evento)
                    }

                    eventosDelMes = eventosPorDia
                    Log.d("InfoCursoProfesor", "✅ Eventos del mes cargados: ${eventosPorDia.size} días con eventos")
                } else {
                    Log.e("InfoCursoProfesor", "❌ Error en respuesta calendario: ${calendarioResponse.code()}")
                }
            } catch (e: Exception) {
                Log.e("InfoCursoProfesor", "❌ Error cargando calendario", e)
            }

        } catch (e: Exception) {
            errorMessage = "Error de conexión: ${e.message}"
            Log.e("InfoCursoProfesor", "Error cargando datos", e)
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(cursoId, selectedMonth, selectedYear) {
        cargarDatosCurso()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Gestión del Curso",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Surface(
                            shape = CircleShape,
                            color = AzulCielo.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                "Editar",
                                tint = AzulCielo,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = { showImagePicker = true }) {
                        Surface(
                            shape = CircleShape,
                            color = Fucsia.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Image,
                                "Portada",
                                tint = Fucsia,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
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
                isLoading -> ModernCursoLoadingView()
                errorMessage != null -> ErrorView(
                    message = errorMessage ?: "",
                    onRetry = { scope.launch { cargarDatosCurso() } },
                    onBack = { navController.popBackStack() }
                )
                cursoDetalle != null -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                EnhancedCursoHeaderView(cursoDetalle!!)
                            }

                            item {
                                ModernQuickActionsRow(cursoId, navController, modulos.size, proximosEventos.size)
                            }

                            item {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 8.dp)
                                        .clickable { navController.navigate("calendarioProfesor/$cursoId") },
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF9C27B0).copy(alpha = 0.1f),
                                    border = BorderStroke(2.dp, Color(0xFF9C27B0).copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFF9C27B0),
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.CalendarMonth,
                                                    null,
                                                    tint = Color.White,
                                                    modifier = Modifier.padding(12.dp)
                                                )
                                            }

                                            Column {
                                                Text(
                                                    "Ver Calendario Completo",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF9C27B0)
                                                )
                                                Text(
                                                    "Gestiona eventos y fechas importantes",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = GrisMedio
                                                )
                                            }
                                        }

                                        Icon(
                                            Icons.Default.ArrowForward,
                                            null,
                                            tint = Color(0xFF9C27B0),
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }

                            item {
                                EnhancedTabsSectionView(
                                    pagerState = pagerState,
                                    modulos = modulos,
                                    foros = foros,
                                    eventos = proximosEventos,
                                    eventosDelMes = eventosDelMes,
                                    selectedMonth = selectedMonth,
                                    selectedYear = selectedYear,
                                    onMonthChange = { month, year ->
                                        selectedMonth = month
                                        selectedYear = year
                                    },
                                    onTareaClick = { tareaId ->
                                        navController.navigate("detalleTareaProfesor/$tareaId")
                                    },
                                    onForoClick = { foroId ->
                                        navController.navigate("foro_detalle/$foroId")
                                    },
                                    onVerCalendarioCompleto = {
                                        navController.navigate("calendarioProfesor/$cursoId")
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (showEditDialog && cursoDetalle != null) {
                EditCursoDialogMejorado(
                    curso = cursoDetalle!!,
                    token = token,
                    onDismiss = { showEditDialog = false },
                    onSuccess = {
                        showEditDialog = false
                        scope.launch {
                            cargarDatosCurso()
                            snackbarHostState.showSnackbar("✅ Curso actualizado")
                        }
                    }
                )
            }

            if (showImagePicker && cursoDetalle != null) {
                ImagePickerDialogMejorado(
                    cursoId = cursoDetalle!!.id,
                    token = token,
                    onDismiss = { showImagePicker = false },
                    onSuccess = {
                        showImagePicker = false
                        scope.launch {
                            cargarDatosCurso()
                            snackbarHostState.showSnackbar("✅ Portada actualizada")
                        }
                    }
                )
            }
        }
    }
}

// ==================== HEADER ====================

@Composable
private fun EnhancedCursoHeaderView(curso: CursoDetalleProfesor) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = FondoCard,
        shadowElevation = 8.dp
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF667EEA),
                                    Color(0xFF764BA2),
                                    Color(0xFFF093FB)
                                )
                            )
                        )
                )

                if (curso.fotoPortadaUrl != null) {
                    AsyncImage(
                        model = curso.fotoPortadaUrl,
                        contentDescription = "Portada",
                        modifier = Modifier.fillMaxSize().alpha(0.5f),
                        contentScale = ContentScale.Crop
                    )
                }

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

                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ModernBadge(
                            text = "Profesor",
                            icon = Icons.Default.School,
                            color = Color(0xFF4CAF50)
                        )
                        ModernBadge(
                            text = "${curso.participantesCount} estudiantes",
                            icon = Icons.Default.People,
                            color = Color(0xFF2196F3)
                        )
                    }

                    Text(
                        text = curso.nombre,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 28.sp,
                        lineHeight = 32.sp
                    )
                }
            }

            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = curso.descripcion,
                    style = MaterialTheme.typography.bodyLarge,
                    color = GrisMedio,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 24.sp
                )

                Divider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = GrisExtraClaro
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CursoStatItem(
                        icon = Icons.Default.FolderOpen,
                        value = "${curso.modulosCount}",
                        label = "Módulos",
                        color = Color(0xFFBA68C8)
                    )
                    CursoStatItem(
                        icon = Icons.Default.People,
                        value = "${curso.participantesCount}",
                        label = "Estudiantes",
                        color = AzulCielo
                    )
                    CursoStatItem(
                        icon = Icons.Default.Assignment,
                        value = "Activo",
                        label = "Estado",
                        color = VerdeLima
                    )
                }
            }
        }
    }
}

@Composable
private fun CursoStatItem(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.15f),
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.padding(12.dp)
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = GrisOscuro
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = GrisMedio
        )
    }
}

@Composable
private fun ModernBadge(text: String, icon: ImageVector, color: Color) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

// ==================== QUICK ACTIONS ====================

private data class ProfesorActionButton(
    val icon: ImageVector,
    val label: String,
    val color: Color,
    val route: String,
    val badge: String? = null
)

@Composable
private fun ModernQuickActionsRow(
    cursoId: String,
    navController: NavController,
    modulosCount: Int,
    eventosCount: Int
) {
    val actions = listOf(
        ProfesorActionButton(
            Icons.Default.PersonAdd,
            "Participantes",
            Color(0xFF3F51B5),
            "participantesCurso/$cursoId"
        ),
        ProfesorActionButton(
            Icons.Default.FolderOpen,
            "Módulos",
            Color(0xFFBA68C8),
            "modulosCurso/$cursoId",
            badge = "$modulosCount"
        ),
        ProfesorActionButton(
            Icons.Default.Add,
            "Nuevo Módulo",
            VerdeLima,
            "crearModulo/$cursoId"
        ),
        ProfesorActionButton(
            Icons.Default.Assignment,
            "Nueva Tarea",
            Naranja,
            "crearTarea/$cursoId"
        ),
        ProfesorActionButton(
            Icons.Default.Forum,
            "Crear Foro",
            Fucsia,
            "crearForo/$cursoId"
        ),
        ProfesorActionButton(
            Icons.Default.CalendarMonth,
            "Nuevo Evento",
            Color(0xFFF44336),
            "crearEvento/$cursoId",
            badge = if (eventosCount > 0) "$eventosCount" else null
        )
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(actions.size) { index ->
            EnhancedActionCardView(actions[index]) {
                navController.navigate(actions[index].route)
            }
        }
    }
}

@Composable
private fun EnhancedActionCardView(action: ProfesorActionButton, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(120.dp)
            .height(110.dp)
            .clickable(onClick = onClick)
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = FondoCard
    ) {
        Box {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                action.color.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box {
                    Surface(
                        shape = CircleShape,
                        color = action.color,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            action.icon,
                            null,
                            modifier = Modifier.padding(12.dp),
                            tint = Color.White
                        )
                    }

                    action.badge?.let { badge ->
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp),
                            shape = CircleShape,
                            color = Color(0xFFF44336),
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                text = badge,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = action.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = action.color,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

// ==================== TABS ====================

private data class ProfesorTabItem(
    val title: String,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EnhancedTabsSectionView(
    pagerState: PagerState,
    modulos: List<ModuloConTareas>,
    foros: List<ForoInfo>,
    eventos: List<EventoInfo>,
    eventosDelMes: Map<Int, List<EventoInfo>>,
    selectedMonth: Int,
    selectedYear: Int,
    onMonthChange: (Int, Int) -> Unit,
    onTareaClick: (String) -> Unit,
    onForoClick: (String) -> Unit,
    onVerCalendarioCompleto: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val tabs = listOf(
        ProfesorTabItem("Tareas", Icons.Default.Assignment, VerdeLima),
        ProfesorTabItem("Foros", Icons.Default.Forum, Fucsia),
        ProfesorTabItem("Calendario", Icons.Default.CalendarToday, Color(0xFF9C27B0))
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(600.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = FondoCard,
            shadowElevation = 4.dp
        ) {
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = Color.Transparent,
                contentColor = GrisOscuro,
                edgePadding = 0.dp,
                indicator = { tabPositions ->
                    if (tabPositions.isNotEmpty() && pagerState.currentPage < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                            height = 4.dp,
                            color = tabs[pagerState.currentPage].color
                        )
                    }
                },
                divider = {}
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (pagerState.currentPage == index)
                                    tab.color.copy(alpha = 0.15f)
                                else
                                    Color.Transparent
                            ) {
                                Icon(
                                    tab.icon,
                                    null,
                                    tint = if (pagerState.currentPage == index) tab.color else GrisMedio,
                                    modifier = Modifier.padding(8.dp).size(20.dp)
                                )
                            }

                            Text(
                                tab.title,
                                fontWeight = if (pagerState.currentPage == index)
                                    FontWeight.Bold
                                else
                                    FontWeight.Medium,
                                color = if (pagerState.currentPage == index) tab.color else GrisMedio,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            when (page) {
                0 -> TareasTabView(modulos, onTareaClick)
                1 -> ForosTabView(foros, onForoClick)
                2 -> CalendarioTabView(
                    eventosDelMes = eventosDelMes,
                    selectedMonth = selectedMonth,
                    selectedYear = selectedYear,
                    onMonthChange = onMonthChange,
                    onVerCalendarioCompleto = onVerCalendarioCompleto
                )
            }
        }
    }
}

@Composable
private fun TareasTabView(modulos: List<ModuloConTareas>, onTareaClick: (String) -> Unit) {
    if (modulos.isEmpty() || modulos.all { it.tareas.isEmpty() }) {
        EmptyStateView(
            Icons.Default.Assignment,
            "No hay tareas",
            "Crea una tarea para empezar a gestionar las entregas",
            VerdeLima
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(modulos.size) { index ->
                if (modulos[index].tareas.isNotEmpty()) {
                    ModuloEnhancedSectionView(modulos[index], onTareaClick)
                }
            }
        }
    }
}

@Composable
private fun ModuloEnhancedSectionView(modulo: ModuloConTareas, onTareaClick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF3E5F5),
            border = BorderStroke(2.dp, Color(0xFFBA68C8)),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFBA68C8),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.FolderOpen,
                        null,
                        modifier = Modifier.padding(12.dp),
                        tint = Color.White
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        modulo.nombre,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = GrisOscuro
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${modulo.tareas.size} tarea${if (modulo.tareas.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GrisMedio,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        modulo.tareas.forEach { tarea ->
            EnhancedTareaCardView(tarea) { onTareaClick(tarea.id) }
        }
    }
}

@Composable
private fun EnhancedTareaCardView(tarea: TareaInfo, onClick: () -> Unit) {
    val (estadoColor, estadoFondo, estadoTexto, estadoIcon) = when {
        tarea.estaVencida -> TareaEstado(ErrorOscuro, ErrorClaro, "VENCIDA", Icons.Default.Warning)
        tarea.estado == "cerrada" -> TareaEstado(Advertencia, AdvertenciaClaro, "CERRADA", Icons.Default.Lock)
        else -> TareaEstado(Exito, ExitoClaro, "ACTIVA", Icons.Default.CheckCircle)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .shadow(3.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = FondoCard
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(estadoColor, estadoColor.copy(alpha = 0.5f))
                        )
                    )
            )

            Row(
                modifier = Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = estadoFondo,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        Icons.Default.Assignment,
                        null,
                        modifier = Modifier.padding(14.dp),
                        tint = estadoColor
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tarea.titulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GrisOscuro,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (tarea.estaVencida) ErrorClaro else GrisExtraClaro
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    null,
                                    tint = if (tarea.estaVencida) ErrorOscuro else GrisMedio,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    formatearFechaEntrega(tarea.fechaEntrega),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (tarea.estaVencida) ErrorOscuro else GrisMedio,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = estadoFondo
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    estadoIcon,
                                    null,
                                    tint = estadoColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = estadoTexto,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = estadoColor
                                )
                            }
                        }
                    }
                }

                Icon(
                    Icons.Default.ArrowForward,
                    null,
                    tint = estadoColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun ForosTabView(foros: List<ForoInfo>, onForoClick: (String) -> Unit) {
    if (foros.isEmpty()) {
        EmptyStateView(
            Icons.Default.Forum,
            "No hay foros",
            "Crea un foro para iniciar discusiones con tus estudiantes",
            Fucsia
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(foros.size) { index ->
                EnhancedForoCardView(foros[index]) { onForoClick(foros[index].id) }
            }
        }
    }
}

@Composable
private fun EnhancedForoCardView(foro: ForoInfo, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .shadow(3.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = FondoCard
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Fucsia, Fucsia.copy(alpha = 0.5f))
                        )
                    )
            )

            Row(
                modifier = Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Fucsia.copy(alpha = 0.15f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        Icons.Default.Forum,
                        null,
                        modifier = Modifier.padding(14.dp),
                        tint = Fucsia
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = foro.titulo,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GrisOscuro,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GrisExtraClaro
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.ChatBubble,
                                    null,
                                    tint = Fucsia,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    "${foro.mensajesCount} mensaje${if (foro.mensajesCount != 1) "s" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GrisMedio,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (foro.fechaCreacion.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = GrisExtraClaro
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        null,
                                        tint = GrisMedio,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        formatearFechaEntrega(foro.fechaCreacion),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = GrisMedio,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                Icon(
                    Icons.Default.ArrowForward,
                    null,
                    tint = Fucsia,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun CalendarioTabView(
    eventosDelMes: Map<Int, List<EventoInfo>>,
    selectedMonth: Int,
    selectedYear: Int,
    onMonthChange: (Int, Int) -> Unit,
    onVerCalendarioCompleto: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(FondoClaro)) {
        CalendarioHeaderView(
            selectedMonth = selectedMonth,
            selectedYear = selectedYear,
            onMonthChange = onMonthChange
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CalendarioGridView(
                    month = selectedMonth,
                    year = selectedYear,
                    eventosDelMes = eventosDelMes
                )
            }

            item { Spacer(Modifier.height(8.dp)) }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Eventos del mes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = GrisOscuro
                    )

                    TextButton(onClick = onVerCalendarioCompleto) {
                        Text(
                            "Ver completo",
                            color = Color(0xFF9C27B0),
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            Icons.Default.ArrowForward,
                            null,
                            tint = Color(0xFF9C27B0),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            val todosLosEventos = eventosDelMes.values.flatten().sortedBy { it.fecha }

            if (todosLosEventos.isEmpty()) {
                item {
                    EmptyStateView(
                        Icons.Default.CalendarMonth,
                        "Sin eventos este mes",
                        "No hay eventos programados para ${obtenerNombreMes(selectedMonth)} $selectedYear",
                        Color(0xFF9C27B0)
                    )
                }
            } else {
                items(todosLosEventos.size) { index ->
                    EnhancedEventoCardView(todosLosEventos[index])
                }
            }
        }
    }
}

@Composable
private fun EnhancedEventoCardView(evento: EventoInfo) {
    val categoriaColor = when (evento.categoria) {
        "tarea" -> Color(0xFFFF9800)
        "examen" -> Color(0xFFE53935)
        "clase" -> Color(0xFF1E88E5)
        "entrega" -> Color(0xFFFB8C00)
        "reunion" -> Color(0xFF8E24AA)
        "escuela_padres" -> Color(0xFF2196F3)
        "institucional" -> Color(0xFF00BCD4)
        else -> Color(0xFF43A047)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = FondoCard
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(categoriaColor, categoriaColor.copy(alpha = 0.5f))
                        )
                    )
            )

            Row(
                modifier = Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = categoriaColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(70.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = obtenerDiaDeEvento(evento.fecha),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = categoriaColor
                        )
                        Text(
                            text = obtenerMesDeEvento(evento.fecha),
                            style = MaterialTheme.typography.labelMedium,
                            color = categoriaColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = evento.titulo,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GrisOscuro,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = categoriaColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = when(evento.categoria) {
                                    "tarea" -> "TAREA"
                                    "escuela_padres" -> "ESCUELA PADRES"
                                    "institucional" -> "INSTITUCIONAL"
                                    else -> evento.categoria.uppercase()
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = categoriaColor
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    if (evento.hora != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = categoriaColor.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AccessTime,
                                    null,
                                    tint = categoriaColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = evento.hora,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = categoriaColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (evento.descripcion.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = evento.descripcion,
                            style = MaterialTheme.typography.bodySmall,
                            color = GrisMedio,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarioHeaderView(
    selectedMonth: Int,
    selectedYear: Int,
    onMonthChange: (Int, Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = FondoCard,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = obtenerNombreMes(selectedMonth),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9C27B0)
                )
                Text(
                    text = selectedYear.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = GrisMedio,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = {
                        val (newMonth, newYear) = if (selectedMonth == 1) {
                            Pair(12, selectedYear - 1)
                        } else {
                            Pair(selectedMonth - 1, selectedYear)
                        }
                        onMonthChange(newMonth, newYear)
                    }
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF9C27B0).copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            "Anterior",
                            tint = Color(0xFF9C27B0),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        val (newMonth, newYear) = if (selectedMonth == 12) {
                            Pair(1, selectedYear + 1)
                        } else {
                            Pair(selectedMonth + 1, selectedYear)
                        }
                        onMonthChange(newMonth, newYear)
                    }
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF9C27B0).copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowForward,
                            "Siguiente",
                            tint = Color(0xFF9C27B0),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarioGridView(
    month: Int,
    year: Int,
    eventosDelMes: Map<Int, List<EventoInfo>>
) {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, 1)
    }

    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = FondoCard,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("D", "L", "M", "M", "J", "V", "S").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9C27B0)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            var dayCounter = 1
            val rows = ((daysInMonth + firstDayOfWeek) / 7.0).toInt() + 1

            for (week in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (dayOfWeek in 0 until 7) {
                        val position = week * 7 + dayOfWeek

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (position >= firstDayOfWeek && dayCounter <= daysInMonth) {
                                val hasEvents = eventosDelMes.containsKey(dayCounter)
                                val eventCount = eventosDelMes[dayCounter]?.size ?: 0

                                CalendarioDayCellView(
                                    day = dayCounter,
                                    hasEvents = hasEvents,
                                    eventCount = eventCount
                                )
                                dayCounter++
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarioDayCellView(
    day: Int,
    hasEvents: Boolean,
    eventCount: Int
) {
    val isToday = remember {
        val today = Calendar.getInstance()
        today.get(Calendar.DAY_OF_MONTH) == day
    }

    Surface(
        shape = CircleShape,
        color = when {
            isToday -> Color(0xFF9C27B0)
            hasEvents -> Color(0xFF9C27B0).copy(alpha = 0.15f)
            else -> Color.Transparent
        },
        modifier = Modifier.size(40.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = day.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (hasEvents || isToday) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        isToday -> Color.White
                        hasEvents -> Color(0xFF9C27B0)
                        else -> GrisOscuro
                    }
                )

                if (hasEvents && !isToday) {
                    Spacer(Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(minOf(eventCount, 3)) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(Color(0xFF9C27B0), CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== COMPONENTES AUXILIARES ====================

@Composable
private fun EmptyStateView(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(100.dp)
            ) {
                Icon(
                    icon,
                    null,
                    modifier = Modifier.padding(24.dp),
                    tint = color
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = GrisOscuro,
                textAlign = TextAlign.Center
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = GrisMedio,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        }
    }
}

@Composable
private fun ModernCursoLoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                strokeWidth = 6.dp,
                color = Color(0xFF667EEA)
            )
            Text(
                "Cargando curso...",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = GrisOscuro
            )
            Text(
                "Preparando información",
                style = MaterialTheme.typography.bodyMedium,
                color = GrisMedio
            )
        }
    }
}

// ==================== DIÁLOGOS ====================

@Composable
private fun EditCursoDialogMejorado(
    curso: CursoDetalleProfesor,
    token: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var nombre by remember { mutableStateOf(curso.nombre) }
    var descripcion by remember { mutableStateOf(curso.descripcion) }
    var isUpdating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = FondoCard,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Editar Curso",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = GrisOscuro
                        )
                        Text(
                            "Actualiza la información del curso",
                            style = MaterialTheme.typography.bodySmall,
                            color = GrisMedio
                        )
                    }
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
                    leadingIcon = {
                        Icon(Icons.Default.School, null, tint = AzulCielo)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AzulCielo,
                        focusedLabelColor = AzulCielo,
                        focusedLeadingIconColor = AzulCielo
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    maxLines = 5,
                    leadingIcon = {
                        Icon(Icons.Default.Description, null, tint = AzulCielo)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AzulCielo,
                        focusedLabelColor = AzulCielo,
                        focusedLeadingIconColor = AzulCielo
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(50.dp),
                        enabled = !isUpdating,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancelar", fontWeight = FontWeight.Bold)
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
                                    if (response.isSuccessful) {
                                        onSuccess()
                                    }
                                } catch (e: Exception) {
                                    Log.e("EditCurso", "Error", e)
                                } finally {
                                    isUpdating = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        enabled = !isUpdating && nombre.isNotBlank() && descripcion.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AzulCielo,
                            disabledContainerColor = GrisExtraClaro
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isUpdating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 3.dp
                            )
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Save, null, modifier = Modifier.size(20.dp))
                                Text("Guardar", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImagePickerDialogMejorado(
    cursoId: String,
    token: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> selectedImageUri = uri }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = FondoCard,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Cambiar Portada",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = GrisOscuro
                        )
                        Text(
                            "Selecciona una nueva imagen",
                            style = MaterialTheme.typography.bodySmall,
                            color = GrisMedio
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Cerrar", tint = GrisMedio)
                    }
                }

                if (selectedImageUri != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = GrisExtraClaro
                    ) {
                        Box {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Vista previa",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp),
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.6f)
                            ) {
                                IconButton(
                                    onClick = { selectedImageUri = null },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        "Eliminar",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clickable { imagePickerLauncher.launch("image/*") },
                        shape = RoundedCornerShape(16.dp),
                        color = Fucsia.copy(alpha = 0.08f),
                        border = BorderStroke(2.dp, Fucsia.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Fucsia.copy(alpha = 0.15f),
                                modifier = Modifier.size(80.dp)
                            ) {
                                Icon(
                                    Icons.Default.CloudUpload,
                                    null,
                                    modifier = Modifier.padding(20.dp),
                                    tint = Fucsia
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Toca para seleccionar imagen",
                                color = Fucsia,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                "JPG, PNG o WEBP",
                                color = GrisMedio,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (selectedImageUri != null) {
                        OutlinedButton(
                            onClick = { selectedImageUri = null },
                            modifier = Modifier.weight(1f).height(50.dp),
                            enabled = !isUploading,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cambiar", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(50.dp),
                            enabled = !isUploading,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancelar", fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            selectedImageUri?.let { uri ->
                                scope.launch {
                                    isUploading = true
                                    try {
                                        val inputStream = context.contentResolver.openInputStream(uri)
                                        val file = File(
                                            context.cacheDir,
                                            "portada_${System.currentTimeMillis()}.jpg"
                                        )
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
                                        Log.e("ImagePicker", "Error", e)
                                    } finally {
                                        isUploading = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        enabled = !isUploading && selectedImageUri != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Fucsia,
                            disabledContainerColor = GrisExtraClaro
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 3.dp
                            )
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Upload, null, modifier = Modifier.size(20.dp))
                                Text("Subir", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== DATA CLASSES ====================

private data class TareaEstado(
    val color: Color,
    val fondo: Color,
    val texto: String,
    val icon: ImageVector
)

// ==================== FUNCIONES AUXILIARES ====================

private fun obtenerDiaDeEvento(fecha: String): String {
    return try {
        fecha.split("-").getOrNull(2)?.take(2) ?: "?"
    } catch (e: Exception) {
        "?"
    }
}

private fun obtenerDiaNumero(fecha: String): Int {
    return try {
        fecha.split("-").getOrNull(2)?.take(2)?.toIntOrNull() ?: 0
    } catch (e: Exception) {
        0
    }
}

private fun obtenerMesDeEvento(fecha: String): String {
    return try {
        val mes = fecha.split("-").getOrNull(1)?.toIntOrNull() ?: return "?"
        val meses = listOf("", "ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SEP", "OCT", "NOV", "DIC")
        meses.getOrNull(mes) ?: "?"
    } catch (e: Exception) {
        "?"
    }
}

private fun obtenerNombreMes(mes: Int): String {
    val meses = listOf(
        "", "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )
    return meses.getOrNull(mes) ?: "Mes $mes"
}