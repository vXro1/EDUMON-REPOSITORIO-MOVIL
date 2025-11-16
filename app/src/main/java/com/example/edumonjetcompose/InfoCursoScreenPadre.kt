package com.example.edumonjetcompose.ui
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.edumonjetcompose.models.*
import com.example.edumonjetcompose.network.ApiService
import com.google.gson.JsonArray
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
// Colores del tema claro mejorado
 val AzulCielo = Color(0xFF42A5F5)
 val AzulCieloClaro = Color(0xFF90CAF9)
 val AzulCieloMuyClaro = Color(0xFFE3F2FD)
 val AzulOscuro = Color(0xFF1976D2)
 val Fucsia = Color(0xFFEC407A)
 val FucsiaClaro = Color(0xFFF48FB1)
 val VerdeLima = Color(0xFF66BB6A)
 val VerdeLimaClaro = Color(0xFFA5D6A7)
 val FondoClaro = Color(0xFFF8F9FA)
 val FondoCard = Color(0xFFFFFFFF)
 val FondoSecundario = Color(0xFFF5F7FA)
 val GrisOscuro = Color(0xFF212121)
 val GrisMedio = Color(0xFF757575)
 val GrisClaro = Color(0xFFE0E0E0)
 val GrisExtraClaro = Color(0xFFF5F5F5)
 val Error = Color(0xFFEF5350)
 val ErrorClaro = Color(0xFFFFEBEE)
 val ErrorOscuro = Color(0xFFC62828)
 val Exito = Color(0xFF66BB6A)
 val ExitoClaro = Color(0xFFE8F5E9)
 val Advertencia = Color(0xFFFF9800)
 val AdvertenciaClaro = Color(0xFFFFF3E0)

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
                        nombre = cursoJson?.get("nombre")?.asString ?: "Tareas",
                        descripcion = cursoJson?.get("descripcion")?.asString ?: "Sin descripción",
                        fotoPortadaUrl = cursoJson?.get("fotoPortadaUrl")?.asString?.let {
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

                            // Extraer moduloId de la tarea de forma más robusta
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

                            // Comparar IDs
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
                title = {
                    Text(
                        "Información del Curso",
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
                    titleContentColor = GrisOscuro,
                    navigationIconContentColor = AzulCielo
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = FondoClaro
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
                        // Header mejorado
                        item {
                            CursoHeaderMejorado(
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
                                if (modulos.isEmpty() || modulos.all { it.tareas.isEmpty() }) {
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
                                // Foro
                                item {
                                    Spacer(Modifier.height(16.dp))
                                    NavigationFeatureCard(
                                        icon = Icons.Default.Forum,
                                        titulo = "Foro del Curso",
                                        descripcion = "Participa en discusiones, comparte ideas y colabora con tus compañeros y docentes en un espacio diseñado para el aprendizaje interactivo.",
                                        buttonText = "Acceder al Foro",
                                        gradientColors = listOf(
                                            Color(0xFF667EEA),
                                            Color(0xFF764BA2)
                                        ),
                                        onClick = {
                                            navController.navigate("foro/$cursoId")
                                        }
                                    )
                                }
                            }
                            2 -> {
                                // Calendario
                                item {
                                    Spacer(Modifier.height(16.dp))
                                    NavigationFeatureCard(
                                        icon = Icons.Default.CalendarMonth,
                                        titulo = "Calendario del Curso",
                                        descripcion = "Visualiza todas las fechas importantes, entregas programadas y eventos del curso en un solo lugar. Mantente organizado y nunca pierdas una fecha límite.",
                                        buttonText = "Ver Calendario",
                                        gradientColors = listOf(
                                            Color(0xFFf093fb),
                                            Color(0xFFf5576c)
                                        ),
                                        onClick = {
                                            navController.navigate("calendario/$cursoId")
                                        }
                                    )
                                }
                            }
                        }

                        // Espaciado final
                        item {
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CursoHeaderMejorado(
    curso: CursoDetalle,
    modulos: List<ModuloConTareas>
) {
    Column {
        // Header con fondo azul claro y avatar del maestro
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            // Fondo azul claro con gradiente
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                AzulCieloClaro,
                                AzulCielo
                            )
                        )
                    )
            )

            // Imagen de portada con overlay
            if (curso.fotoPortadaUrl != null) {
                AsyncImage(
                    model = curso.fotoPortadaUrl,
                    contentDescription = "Portada del curso",
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.3f),
                    contentScale = ContentScale.Crop
                )
            }

            // Overlay para mejor legibilidad
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                AzulCielo.copy(alpha = 0.8f)
                            ),
                            startY = 0f,
                            endY = 900f
                        )
                    )
            )

            // Contenido del header
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // Título del curso
                Text(
                    text = curso.nombre,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 32.sp,
                        lineHeight = 38.sp
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Información del docente con avatar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Avatar del docente
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .shadow(8.dp, CircleShape)
                    ) {
                        if (curso.docenteFotoUrl != null) {
                            AsyncImage(
                                model = curso.docenteFotoUrl,
                                contentDescription = "Foto del docente",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .border(3.dp, Color.White, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = CircleShape,
                                color = Color.White,
                                border = BorderStroke(3.dp, Color.White)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.padding(14.dp),
                                    tint = AzulCielo
                                )
                            }
                        }
                    }

                    // Nombre del docente
                    Column {
                        Text(
                            text = "Docente",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 12.sp
                            ),
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${curso.docenteNombre} ${curso.docenteApellido}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 20.sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Tarjeta de información con estadísticas
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-30).dp),
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            color = FondoClaro,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Descripción
                Text(
                    text = curso.descripcion,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        lineHeight = 24.sp
                    ),
                    color = GrisMedio
                )

                HorizontalDivider(color = GrisClaro, thickness = 1.dp)

                // Estadísticas mejoradas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatCardMejorada(
                        icon = Icons.Default.People,
                        value = curso.participantesCount.toString(),
                        label = "Estudiantes",
                        color = Fucsia
                    )
                    StatCardMejorada(
                        icon = Icons.Default.Book,
                        value = curso.modulosCount.toString(),
                        label = "Módulos",
                        color = AzulCielo
                    )
                    StatCardMejorada(
                        icon = Icons.Default.Assignment,
                        value = contarTotalTareas(modulos).toString(),
                        label = "Tareas",
                        color = VerdeLima
                    )
                }
            }
        }
    }
}

@Composable
fun StatCardMejorada(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Surface(
        modifier = Modifier
            .width(110.dp)
            .height(110.dp),
        shape = RoundedCornerShape(20.dp),
        color = FondoCard,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, GrisClaro)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp),
                    tint = color
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 24.sp
                ),
                fontWeight = FontWeight.ExtraBold,
                color = GrisOscuro
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp
                ),
                textAlign = TextAlign.Center,
                color = GrisMedio,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun TabsSection(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = FondoCard,
        shadowElevation = 4.dp
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = FondoCard,
            contentColor = AzulCielo,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = AzulCielo,
                    height = 3.dp
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { onTabSelected(0) },
                selectedContentColor = AzulCielo,
                unselectedContentColor = GrisMedio
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Assignment,
                        null,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "Tareas",
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
            Tab(
                selected = selectedTab == 1,
                onClick = { onTabSelected(1) },
                selectedContentColor = AzulCielo,
                unselectedContentColor = GrisMedio
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Forum,
                        null,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "Foro",
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
            Tab(
                selected = selectedTab == 2,
                onClick = { onTabSelected(2) },
                selectedContentColor = AzulCielo,
                unselectedContentColor = GrisMedio
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        null,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "Calendario",
                        fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ModuloSection(modulo: ModuloConTareas, onTareaClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Header del módulo mejorado
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = AzulCieloMuyClaro,
            border = BorderStroke(2.dp, AzulCieloClaro)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = AzulCielo,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp),
                        tint = Color.White
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = modulo.nombre,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 18.sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = GrisOscuro
                    )
                    Text(
                        text = "${modulo.tareas.size} tareas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GrisMedio,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Tareas del módulo
        if (modulo.tareas.isEmpty()) {
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
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = GrisMedio
                    )
                    Text(
                        text = "No hay tareas en este módulo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GrisMedio
                    )
                }
            }
        } else {
            modulo.tareas.forEach { tarea ->
                TareaCardMejorada(
                    tarea = tarea,
                    onClick = { onTareaClick(tarea.id) }
                )
                Spacer(Modifier.height(12.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun TareaCardMejorada(tarea: TareaInfo, onClick: () -> Unit) {
    val (estadoColor, estadoFondo, estadoTexto) = when {
        tarea.estaVencida -> Triple(ErrorOscuro, ErrorClaro, "VENCIDA")
        tarea.estado == "cerrada" -> Triple(Advertencia, AdvertenciaClaro, "CERRADA")
        else -> Triple(Exito, ExitoClaro, "ACTIVA")
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = FondoCard,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header con título y badge de estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = tarea.titulo,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 18.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = GrisOscuro
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = estadoFondo
                ) {
                    Text(
                        text = estadoTexto,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        color = estadoColor,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Descripción
            Text(
                text = tarea.descripcion,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                ),
                color = GrisMedio,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            HorizontalDivider(color = GrisClaro, thickness = 1.dp)

            // Footer con fecha y botón
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (tarea.estaVencida) ErrorClaro else AzulCieloMuyClaro,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.padding(10.dp),
                            tint = if (tarea.estaVencida) ErrorOscuro else AzulCielo
                        )
                    }
                    Column {
                        Text(
                            text = "Fecha límite",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp
                            ),
                            color = GrisMedio,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = formatearFechaEntrega(tarea.fechaEntrega),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = if (tarea.estaVencida) ErrorOscuro else GrisOscuro
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = AzulCielo,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun NavigationFeatureCard(
    icon: ImageVector,
    titulo: String,
    descripcion: String,
    buttonText: String,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = gradientColors
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Icono principal
                Surface(
                    modifier = Modifier.size(90.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.25f),
                    border = BorderStroke(3.dp, Color.White.copy(alpha = 0.4f))
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = Color.White
                        )
                    }
                }

                // Título
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 26.sp
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    color = Color.White
                )

                // Descripción
                Text(
                    text = descripcion,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp,
                        lineHeight = 23.sp
                    ),
                    textAlign = TextAlign.Center,
                    color = Color.White.copy(alpha = 0.95f)
                )

                // Botón de acción
                Button(
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = gradientColors[0]
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 10.dp
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = buttonText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 17.sp
                            ),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
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
                Surface(
                    shape = CircleShape,
                    color = AzulCieloMuyClaro,
                    modifier = Modifier.size(100.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = null,
                        modifier = Modifier.padding(24.dp),
                        tint = AzulCielo
                    )
                }
                Text(
                    text = "No hay tareas disponibles",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 22.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = GrisOscuro
                )
                Text(
                    text = "Este curso aún no tiene tareas asignadas. Las tareas aparecerán aquí cuando el docente las publique.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    ),
                    color = GrisMedio,
                    textAlign = TextAlign.Center
                )
            }
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                strokeWidth = 6.dp,
                color = AzulCielo
            )
            Text(
                text = "Cargando curso...",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 18.sp
                ),
                fontWeight = FontWeight.Bold,
                color = GrisOscuro
            )
        }
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoClaro),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            shape = RoundedCornerShape(24.dp),
            color = FondoCard,
            shadowElevation = 6.dp
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(36.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = ErrorClaro,
                    modifier = Modifier.size(90.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.padding(22.dp),
                        tint = ErrorOscuro
                    )
                }
                Text(
                    text = "Error al cargar",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 24.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = GrisOscuro
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    ),
                    textAlign = TextAlign.Center,
                    color = GrisMedio
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AzulCielo
                        ),
                        border = BorderStroke(2.dp, AzulCielo)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Volver",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Button(
                        onClick = onRetry,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AzulCielo
                        )
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Reintentar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

// ==================== FUNCIONES AUXILIARES ====================

/**
 * Formatea una fecha de entrega de formato ISO a un formato legible
 * Ejemplo: "2025-11-15T23:59:00Z" -> "15/11/2025"
 */
 fun formatearFechaEntrega(fecha: String): String {
    return try {
        if (fecha.isEmpty()) return "Sin fecha"

        val formatoEntrada = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val formatoSalida = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val date = formatoEntrada.parse(fecha)
        date?.let { formatoSalida.format(it) } ?: "Fecha inválida"
    } catch (e: Exception) {
        // Intenta con formato alternativo sin hora
        try {
            val partes = fecha.substring(0, 10).split("-")
            "${partes[2]}/${partes[1]}/${partes[0]}"
        } catch (e2: Exception) {
            "Fecha inválida"
        }
    }
}

/**
 * Verifica si una fecha ya ha vencido comparándola con la fecha actual
 */
 fun estaFechaVencida(fecha: String): Boolean {
    return try {
        if (fecha.isEmpty()) return false

        val formato = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val fechaTarea = formato.parse(fecha)
        val fechaActual = Date()

        fechaTarea?.before(fechaActual) ?: false
    } catch (e: Exception) {
        false
    }
}

/**
 * Cuenta el total de tareas en todos los módulos
 */
fun contarTotalTareas(modulos: List<ModuloConTareas>): Int {
    return modulos.sumOf { it.tareas.size }
}