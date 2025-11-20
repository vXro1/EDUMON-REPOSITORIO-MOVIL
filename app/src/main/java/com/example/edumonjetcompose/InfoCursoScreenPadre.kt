package com.example.edumonjetcompose.ui
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.edumonjetcompose.Fucsia
import com.example.edumonjetcompose.Naranja
import com.example.edumonjetcompose.R
import com.example.edumonjetcompose.VerdeLima
import com.example.edumonjetcompose.models.*
import com.example.edumonjetcompose.network.ApiService
import com.example.edumonjetcompose.ui.theme.AzulCielo
import com.example.edumonjetcompose.ui.theme.Blanco
import com.example.edumonjetcompose.ui.theme.GrisNeutral
import com.google.gson.JsonArray
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

// Colores del tema
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
                                nombre = m.get("titulo")?.asString ?: m.get("nombre")?.asString ?: "Sin nombre",
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
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
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
                                    Spacer(Modifier.height(12.dp))
                                    NavigationFeatureCard(
                                        icon = Icons.Default.Forum,
                                        titulo = "Foro del Curso",
                                        descripcion = "Participa en discusiones, comparte ideas y colabora con tus compañeros y docentes.",
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
                                    Spacer(Modifier.height(12.dp))
                                    NavigationFeatureCard(
                                        icon = Icons.Default.CalendarMonth,
                                        titulo = "Calendario del Curso",
                                        descripcion = "Visualiza todas las fechas importantes, entregas programadas y eventos del curso en un solo lugar.",
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
                            Spacer(Modifier.height(16.dp))
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
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isSmallScreen = screenWidth < 360.dp

    Column {
        // Header con diseño responsivo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isSmallScreen) 240.dp else 260.dp)
        ) {
            // Fondo con degradado
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

            // Imagen de portada
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

            // Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                AzulCielo.copy(alpha = 0.8f)
                            )
                        )
                    )
            )

            // Contenido
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = if (isSmallScreen) 16.dp else 20.dp,
                        vertical = if (isSmallScreen) 16.dp else 20.dp
                    ),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(if (isSmallScreen) 8.dp else 12.dp))

                // Título
                Text(
                    text = curso.nombre,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = if (isSmallScreen) 22.sp else 28.sp,
                        lineHeight = if (isSmallScreen) 28.sp else 34.sp
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = if (isSmallScreen) 8.dp else 12.dp)
                )

                // Info del docente
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isSmallScreen) 10.dp else 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isSmallScreen) 48.dp else 56.dp)
                            .shadow(if (isSmallScreen) 4.dp else 6.dp, CircleShape)
                    ) {
                        if (curso.docenteFotoUrl != null) {
                            AsyncImage(
                                model = curso.docenteFotoUrl,
                                contentDescription = "Foto del docente",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .border(
                                        width = if (isSmallScreen) 1.5.dp else 2.dp,
                                        color = Color.White,
                                        shape = CircleShape
                                    ),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = CircleShape,
                                color = Color.White,
                                border = BorderStroke(
                                    width = if (isSmallScreen) 1.5.dp else 2.dp,
                                    color = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.padding(if (isSmallScreen) 10.dp else 12.dp),
                                    tint = AzulCielo
                                )
                            }
                        }
                    }

                    Column {
                        Text(
                            text = "Docente",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = if (isSmallScreen) 10.sp else 11.sp
                            ),
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${curso.docenteNombre} ${curso.docenteApellido}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = if (isSmallScreen) 15.sp else 18.sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Tarjeta de información
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = if (isSmallScreen) (-20).dp else (-24).dp),
            shape = RoundedCornerShape(
                topStart = if (isSmallScreen) 20.dp else 24.dp,
                topEnd = if (isSmallScreen) 20.dp else 24.dp
            ),
            color = FondoClaro,
            shadowElevation = if (isSmallScreen) 4.dp else 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (isSmallScreen) 16.dp else 20.dp,
                        vertical = if (isSmallScreen) 14.dp else 20.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(if (isSmallScreen) 12.dp else 16.dp)
            ) {
                Text(
                    text = curso.descripcion,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = if (isSmallScreen) 13.sp else 14.sp,
                        lineHeight = if (isSmallScreen) 18.sp else 20.sp
                    ),
                    color = GrisMedio,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                HorizontalDivider(
                    color = GrisClaro,
                    thickness = 1.dp
                )
            }
        }
    }
}

@Composable
fun StatCardMejorada(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    isSmallScreen: Boolean = false
) {
    Surface(
        modifier = Modifier
            .width(if (isSmallScreen) 95.dp else 110.dp)
            .height(if (isSmallScreen) 95.dp else 110.dp),
        shape = RoundedCornerShape(if (isSmallScreen) 14.dp else 16.dp),
        color = FondoCard,
        shadowElevation = if (isSmallScreen) 2.dp else 3.dp,
        border = BorderStroke(1.dp, GrisClaro)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isSmallScreen) 8.dp else 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(if (isSmallScreen) 36.dp else 42.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(if (isSmallScreen) 7.dp else 9.dp),
                    tint = color
                )
            }
            Spacer(Modifier.height(if (isSmallScreen) 4.dp else 6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = if (isSmallScreen) 18.sp else 22.sp
                ),
                fontWeight = FontWeight.ExtraBold,
                color = GrisOscuro
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = if (isSmallScreen) 9.sp else 10.sp
                ),
                textAlign = TextAlign.Center,
                color = GrisMedio,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
fun TabsSection(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 360

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = FondoCard,
        shadowElevation = if (isSmallScreen) 2.dp else 3.dp
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = AzulCielo,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    height = if (isSmallScreen) 2.5.dp else 3.dp,
                    color = when (selectedTab) {
                        0 -> VerdeLima
                        1 -> Fucsia
                        2 -> Color(0xFF9C27B0)
                        else -> AzulCielo
                    }
                )
            },
            divider = {}
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { onTabSelected(0) },
                modifier = Modifier.padding(
                    vertical = if (isSmallScreen) 8.dp else 12.dp
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        if (isSmallScreen) 6.dp else 8.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(
                        horizontal = if (isSmallScreen) 10.dp else 14.dp,
                        vertical = if (isSmallScreen) 4.dp else 6.dp
                    )
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (selectedTab == 0)
                            VerdeLima.copy(alpha = 0.15f)
                        else
                            Color.Transparent
                    ) {
                        Icon(
                            Icons.Default.Assignment,
                            null,
                            tint = if (selectedTab == 0) VerdeLima else GrisMedio,
                            modifier = Modifier
                                .padding(if (isSmallScreen) 5.dp else 6.dp)
                                .size(if (isSmallScreen) 16.dp else 20.dp)
                        )
                    }
                    Text(
                        "Tareas",
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == 0) VerdeLima else GrisMedio,
                        fontSize = if (isSmallScreen) 12.sp else 14.sp
                    )
                }
            }

            Tab(
                selected = selectedTab == 1,
                onClick = { onTabSelected(1) },
                modifier = Modifier.padding(
                    vertical = if (isSmallScreen) 8.dp else 12.dp
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        if (isSmallScreen) 6.dp else 8.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(
                        horizontal = if (isSmallScreen) 10.dp else 14.dp,
                        vertical = if (isSmallScreen) 4.dp else 6.dp
                    )
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (selectedTab == 1)
                            Fucsia.copy(alpha = 0.15f)
                        else
                            Color.Transparent
                    ) {
                        Icon(
                            Icons.Default.Forum,
                            null,
                            tint = if (selectedTab == 1) Fucsia else GrisMedio,
                            modifier = Modifier
                                .padding(if (isSmallScreen) 5.dp else 6.dp)
                                .size(if (isSmallScreen) 16.dp else 20.dp)
                        )
                    }
                    Text(
                        "Foro",
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == 1) Fucsia else GrisMedio,
                        fontSize = if (isSmallScreen) 12.sp else 14.sp
                    )
                }
            }

            Tab(
                selected = selectedTab == 2,
                onClick = { onTabSelected(2) },
                modifier = Modifier.padding(
                    vertical = if (isSmallScreen) 8.dp else 12.dp
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        if (isSmallScreen) 6.dp else 8.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(
                        horizontal = if (isSmallScreen) 10.dp else 14.dp,
                        vertical = if (isSmallScreen) 4.dp else 6.dp
                    )
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (selectedTab == 2)
                            Color(0xFF9C27B0).copy(alpha = 0.15f)
                        else
                            Color.Transparent
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            null,
                            tint = if (selectedTab == 2) Color(0xFF9C27B0) else GrisMedio,
                            modifier = Modifier
                                .padding(if (isSmallScreen) 5.dp else 6.dp)
                                .size(if (isSmallScreen) 16.dp else 20.dp)
                        )
                    }
                    Text(
                        "Calendario",
                        fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == 2) Color(0xFF9C27B0) else GrisMedio,
                        fontSize = if (isSmallScreen) 12.sp else 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ModuloSection(modulo: ModuloConTareas, onTareaClick: (String) -> Unit) {
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 360

    // NO MOSTRAR SI NO HAY TAREAS
    if (modulo.tareas.isEmpty()) {
        return
    }

    // Ordenar tareas
    val tareasOrdenadas = modulo.tareas.sortedWith(
        compareBy<TareaInfo> { it.estaVencida }
            .thenBy { if (it.estado == "cerrada") 1 else 0 }
            .thenBy { it.fechaEntrega }
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (isSmallScreen) 12.dp else 16.dp,
                vertical = if (isSmallScreen) 6.dp else 8.dp
            ),
        verticalArrangement = Arrangement.spacedBy(if (isSmallScreen) 8.dp else 12.dp)
    ) {
        // Header del módulo
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(if (isSmallScreen) 12.dp else 14.dp),
            color = Color(0xFFF3E5F5),
            border = BorderStroke(
                width = if (isSmallScreen) 1.5.dp else 2.dp,
                color = Color(0xFFBA68C8)
            ),
            shadowElevation = if (isSmallScreen) 1.5.dp else 2.dp
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = if (isSmallScreen) 10.dp else 14.dp,
                    vertical = if (isSmallScreen) 10.dp else 12.dp
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (isSmallScreen) 10.dp else 12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFBA68C8),
                    modifier = Modifier.size(if (isSmallScreen) 38.dp else 44.dp)
                ) {
                    Icon(
                        Icons.Default.FolderOpen,
                        null,
                        modifier = Modifier.padding(if (isSmallScreen) 9.dp else 11.dp),
                        tint = Color.White
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        modulo.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GrisOscuro,
                        fontSize = if (isSmallScreen) 14.sp else 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(if (isSmallScreen) 2.dp else 3.dp))
                    Text(
                        "${modulo.tareas.size} tarea${if (modulo.tareas.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = GrisMedio,
                        fontWeight = FontWeight.Medium,
                        fontSize = if (isSmallScreen) 10.sp else 12.sp
                    )
                }
            }
        }

        // Lista de tareas
        tareasOrdenadas.forEach { tarea ->
            TareaCardMejorada(
                tarea = tarea,
                onClick = { onTareaClick(tarea.id) },
                isSmallScreen = isSmallScreen
            )
        }
    }
}

@Composable
fun TareaCardMejorada(
    tarea: TareaInfo,
    onClick: () -> Unit,
    isSmallScreen: Boolean = false
) {
    val (estadoColor, estadoFondo, estadoTexto, estadoIcon) = when {
        tarea.estaVencida -> TareaEstado(ErrorOscuro, ErrorClaro, "VENCIDA", Icons.Default.Warning)
        tarea.estado == "cerrada" -> TareaEstado(Advertencia, AdvertenciaClaro, "CERRADA", Icons.Default.Lock)
        else -> TareaEstado(Exito, ExitoClaro, "ACTIVA", Icons.Default.CheckCircle)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .shadow(
                elevation = if (isSmallScreen) 1.5.dp else 2.dp,
                shape = RoundedCornerShape(if (isSmallScreen) 12.dp else 14.dp)
            ),
        shape = RoundedCornerShape(if (isSmallScreen) 12.dp else 14.dp),
        color = FondoCard
    ) {
        Column {
            // Barra superior de color
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isSmallScreen) 2.5.dp else 3.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(estadoColor, estadoColor.copy(alpha = 0.4f))
                        )
                    )
            )

            Row(
                modifier = Modifier.padding(
                    horizontal = if (isSmallScreen) 12.dp else 16.dp,
                    vertical = if (isSmallScreen) 12.dp else 14.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(if (isSmallScreen) 10.dp else 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icono de tarea
                Surface(
                    shape = RoundedCornerShape(if (isSmallScreen) 8.dp else 10.dp),
                    color = estadoFondo,
                    modifier = Modifier.size(if (isSmallScreen) 44.dp else 52.dp)
                ) {
                    Icon(
                        Icons.Default.Assignment,
                        null,
                        modifier = Modifier.padding(if (isSmallScreen) 11.dp else 13.dp),
                        tint = estadoColor
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tarea.titulo,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = GrisOscuro,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = if (isSmallScreen) 13.sp else 14.sp,
                        lineHeight = if (isSmallScreen) 17.sp else 19.sp
                    )

                    Spacer(Modifier.height(if (isSmallScreen) 5.dp else 6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(if (isSmallScreen) 6.dp else 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Badge de fecha
                        Surface(
                            shape = RoundedCornerShape(if (isSmallScreen) 5.dp else 6.dp),
                            color = if (tarea.estaVencida) ErrorClaro else GrisExtraClaro
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = if (isSmallScreen) 6.dp else 8.dp,
                                    vertical = if (isSmallScreen) 3.dp else 4.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(if (isSmallScreen) 3.dp else 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    null,
                                    tint = if (tarea.estaVencida) ErrorOscuro else GrisMedio,
                                    modifier = Modifier.size(if (isSmallScreen) 11.dp else 13.dp)
                                )
                                Text(
                                    formatearFechaEntrega(tarea.fechaEntrega),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (tarea.estaVencida) ErrorOscuro else GrisMedio,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = if (isSmallScreen) 9.sp else 11.sp
                                )
                            }
                        }

                        // Badge de estado
                        Surface(
                            shape = RoundedCornerShape(if (isSmallScreen) 5.dp else 6.dp),
                            color = estadoFondo
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = if (isSmallScreen) 6.dp else 8.dp,
                                    vertical = if (isSmallScreen) 3.dp else 4.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(if (isSmallScreen) 3.dp else 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    estadoIcon,
                                    null,
                                    tint = estadoColor,
                                    modifier = Modifier.size(if (isSmallScreen) 11.dp else 13.dp)
                                )
                                Text(
                                    text = estadoTexto,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = estadoColor,
                                    fontSize = if (isSmallScreen) 8.sp else 10.sp
                                )
                            }
                        }
                    }
                }

                // Flecha
                Icon(
                    Icons.Default.ArrowForward,
                    null,
                    tint = estadoColor,
                    modifier = Modifier.size(if (isSmallScreen) 18.dp else 22.dp)
                )
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
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 360

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isSmallScreen) 12.dp else 16.dp),
        shape = RoundedCornerShape(if (isSmallScreen) 16.dp else 20.dp),
        shadowElevation = if (isSmallScreen) 4.dp else 6.dp
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
                    .padding(
                        horizontal = if (isSmallScreen) 18.dp else 24.dp,
                        vertical = if (isSmallScreen) 20.dp else 28.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (isSmallScreen) 14.dp else 20.dp)
            ) {
                // Icono
                Surface(
                    modifier = Modifier.size(if (isSmallScreen) 64.dp else 80.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.25f),
                    border = BorderStroke(
                        width = if (isSmallScreen) 1.5.dp else 2.dp,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(if (isSmallScreen) 30.dp else 38.dp),
                            tint = Color.White
                        )
                    }
                }

                Text(
                    text = titulo,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = if (isSmallScreen) 18.sp else 22.sp
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = descripcion,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = if (isSmallScreen) 12.sp else 14.sp,
                        lineHeight = if (isSmallScreen) 17.sp else 20.sp
                    ),
                    textAlign = TextAlign.Center,
                    color = Color.White.copy(alpha = 0.95f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Button(
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isSmallScreen) 46.dp else 54.dp),
                    shape = RoundedCornerShape(if (isSmallScreen) 12.dp else 14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = gradientColors[0]
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = if (isSmallScreen) 3.dp else 4.dp,
                        pressedElevation = if (isSmallScreen) 6.dp else 8.dp
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = if (isSmallScreen) 2.dp else 4.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(if (isSmallScreen) 18.dp else 22.dp)
                        )
                        Spacer(Modifier.width(if (isSmallScreen) 6.dp else 8.dp))
                        Text(
                            text = buttonText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = if (isSmallScreen) 13.sp else 15.sp
                            ),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(if (isSmallScreen) 3.dp else 4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(if (isSmallScreen) 16.dp else 20.dp)
                        )
                    }
                }
            }
        }
    }
}

// Data class para estados de tarea
data class TareaEstado(
    val color: Color,
    val fondo: Color,
    val texto: String,
    val icon: ImageVector
)

// Estado vacío para cuando no hay módulos con tareas
@Composable
fun EmptyStateView(
    icon: ImageVector,
    titulo: String,
    descripcion: String,
    color: Color
) {
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 360

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = if (isSmallScreen) 24.dp else 32.dp,
                vertical = if (isSmallScreen) 32.dp else 40.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(if (isSmallScreen) 70.dp else 80.dp),
            shape = CircleShape,
            color = color.copy(alpha = 0.15f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isSmallScreen) 18.dp else 20.dp),
                tint = color
            )
        }

        Spacer(Modifier.height(if (isSmallScreen) 14.dp else 16.dp))

        Text(
            text = titulo,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = if (isSmallScreen) 18.sp else 20.sp
            ),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = GrisOscuro
        )

        Spacer(Modifier.height(if (isSmallScreen) 6.dp else 8.dp))

        Text(
            text = descripcion,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = if (isSmallScreen) 13.sp else 14.sp,
                lineHeight = if (isSmallScreen) 18.sp else 20.sp
            ),
            textAlign = TextAlign.Center,
            color = GrisMedio,
            maxLines = 3
        )
    }
}

// Composable adicional para estadísticas responsivas
@Composable
fun CursoEstadisticasSection(
    modulos: List<ModuloConTareas>,
    isSmallScreen: Boolean = false
) {
    val totalTareas = modulos.sumOf { it.tareas.size }
    val modulosConTareas = modulos.count { it.tareas.isNotEmpty() }
    val tareasActivas = modulos.flatMap { it.tareas }.count {
        !it.estaVencida && it.estado != "cerrada"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (isSmallScreen) 12.dp else 20.dp,
                vertical = if (isSmallScreen) 10.dp else 16.dp
            ),
        horizontalArrangement = Arrangement.spacedBy(if (isSmallScreen) 8.dp else 12.dp)
    ) {
        StatCardMejorada(
            icon = Icons.Default.Assignment,
            value = totalTareas.toString(),
            label = "Tareas",
            color = VerdeLima,
            isSmallScreen = isSmallScreen
        )

        StatCardMejorada(
            icon = Icons.Default.FolderOpen,
            value = modulosConTareas.toString(),
            label = "Módulos",
            color = Color(0xFFBA68C8),
            isSmallScreen = isSmallScreen
        )

        StatCardMejorada(
            icon = Icons.Default.CheckCircle,
            value = tareasActivas.toString(),
            label = "Activas",
            color = Exito,
            isSmallScreen = isSmallScreen
        )
    }
}

// Divider decorativo responsivo
@Composable
fun DecorativeDivider(isSmallScreen: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (isSmallScreen) 32.dp else 40.dp,
                vertical = if (isSmallScreen) 12.dp else 16.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        HorizontalDivider(
            color = GrisClaro.copy(alpha = 0.5f),
            thickness = 1.dp
        )
        Surface(
            shape = CircleShape,
            color = FondoClaro,
            modifier = Modifier.padding(horizontal = if (isSmallScreen) 6.dp else 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = null,
                modifier = Modifier
                    .padding(if (isSmallScreen) 3.dp else 4.dp)
                    .size(if (isSmallScreen) 18.dp else 20.dp),
                tint = GrisClaro
            )
        }
    }
}

// Sección de módulos con scroll responsivo
@Composable
fun ModulosListSection(
    modulos: List<ModuloConTareas>,
    onTareaClick: (String) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 360

    // Filtrar módulos con tareas
    val modulosConTareas = modulos.filter { it.tareas.isNotEmpty() }

    if (modulosConTareas.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.Assignment,
            titulo = "No hay tareas",
            descripcion = "Los módulos con tareas aparecerán aquí cuando el profesor las publique",
            color = VerdeLima
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = 0.dp,
                vertical = if (isSmallScreen) 8.dp else 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(if (isSmallScreen) 12.dp else 16.dp)
        ) {
            items(modulosConTareas.size) { index ->
                ModuloSection(
                    modulo = modulosConTareas[index],
                    onTareaClick = onTareaClick
                )
            }

            // Espaciado final
            item {
                Spacer(Modifier.height(if (isSmallScreen) 12.dp else 16.dp))
            }
        }
    }
}

// Card de información adicional responsiva
@Composable
fun InfoCard(
    icon: ImageVector,
    titulo: String,
    descripcion: String,
    color: Color,
    onClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 360

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = if (isSmallScreen) 12.dp else 16.dp),
        shape = RoundedCornerShape(if (isSmallScreen) 14.dp else 16.dp),
        color = FondoCard,
        shadowElevation = if (isSmallScreen) 2.dp else 3.dp,
        border = BorderStroke(
            width = if (isSmallScreen) 1.dp else 1.5.dp,
            color = color.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (isSmallScreen) 14.dp else 16.dp,
                vertical = if (isSmallScreen) 14.dp else 16.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(if (isSmallScreen) 12.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(if (isSmallScreen) 44.dp else 48.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(if (isSmallScreen) 11.dp else 12.dp),
                    tint = color
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = if (isSmallScreen) 15.sp else 16.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = GrisOscuro
                )
                Spacer(Modifier.height(if (isSmallScreen) 3.dp else 4.dp))
                Text(
                    text = descripcion,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = if (isSmallScreen) 12.sp else 13.sp,
                        lineHeight = if (isSmallScreen) 16.sp else 18.sp
                    ),
                    color = GrisMedio,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(if (isSmallScreen) 20.dp else 22.dp)
            )
        }
    }
}

// Header de sección responsivo
@Composable
fun SectionHeader(
    titulo: String,
    subtitulo: String? = null,
    icon: ImageVector? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 360

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (isSmallScreen) 12.dp else 16.dp,
                vertical = if (isSmallScreen) 10.dp else 12.dp
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(if (isSmallScreen) 10.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (icon != null) {
                Surface(
                    shape = CircleShape,
                    color = AzulCielo.copy(alpha = 0.15f),
                    modifier = Modifier.size(if (isSmallScreen) 36.dp else 40.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.padding(if (isSmallScreen) 9.dp else 10.dp),
                        tint = AzulCielo
                    )
                }
            }

            Column {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = if (isSmallScreen) 18.sp else 20.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = GrisOscuro
                )
                if (subtitulo != null) {
                    Spacer(Modifier.height(if (isSmallScreen) 2.dp else 3.dp))
                    Text(
                        text = subtitulo,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = if (isSmallScreen) 11.sp else 12.sp
                        ),
                        color = GrisMedio
                    )
                }
            }
        }

        if (actionText != null && onActionClick != null) {
            TextButton(
                onClick = onActionClick,
                contentPadding = PaddingValues(
                    horizontal = if (isSmallScreen) 8.dp else 12.dp,
                    vertical = if (isSmallScreen) 4.dp else 6.dp
                )
            ) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = if (isSmallScreen) 12.sp else 13.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = AzulCielo
                )
                Spacer(Modifier.width(if (isSmallScreen) 3.dp else 4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(if (isSmallScreen) 14.dp else 16.dp),
                    tint = AzulCielo
                )
            }
        }
    }
}

// Shimmer loading responsivo para tarjetas
@Composable
fun TareaCardShimmer(isSmallScreen: Boolean = false) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (isSmallScreen) 12.dp else 16.dp,
                vertical = if (isSmallScreen) 6.dp else 8.dp
            ),
        shape = RoundedCornerShape(if (isSmallScreen) 12.dp else 14.dp),
        color = FondoCard,
        shadowElevation = if (isSmallScreen) 1.5.dp else 2.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isSmallScreen) 2.5.dp else 3.dp)
                    .background(GrisClaro)
            )

            Row(
                modifier = Modifier.padding(
                    horizontal = if (isSmallScreen) 12.dp else 16.dp,
                    vertical = if (isSmallScreen) 12.dp else 14.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(if (isSmallScreen) 10.dp else 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isSmallScreen) 44.dp else 52.dp)
                        .background(GrisExtraClaro, RoundedCornerShape(if (isSmallScreen) 8.dp else 10.dp))
                )

                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(if (isSmallScreen) 14.dp else 16.dp)
                            .background(GrisExtraClaro, RoundedCornerShape(4.dp))
                    )
                    Spacer(Modifier.height(if (isSmallScreen) 6.dp else 8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(if (isSmallScreen) 10.dp else 12.dp)
                            .background(GrisExtraClaro, RoundedCornerShape(4.dp))
                    )
                }

                Box(
                    modifier = Modifier
                        .size(if (isSmallScreen) 18.dp else 22.dp)
                        .background(GrisExtraClaro, CircleShape)
                )
            }
        }
    }
}

// Mensaje de error responsivo
@Composable
fun ErrorStateView(
    mensaje: String,
    onRetry: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 360

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = if (isSmallScreen) 24.dp else 32.dp,
                vertical = if (isSmallScreen) 32.dp else 40.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(if (isSmallScreen) 70.dp else 80.dp),
            shape = CircleShape,
            color = ErrorClaro
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isSmallScreen) 18.dp else 20.dp),
                tint = ErrorOscuro
            )
        }

        Spacer(Modifier.height(if (isSmallScreen) 14.dp else 16.dp))

        Text(
            text = "¡Oops! Algo salió mal",
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = if (isSmallScreen) 18.sp else 20.sp
            ),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = GrisOscuro
        )

        Spacer(Modifier.height(if (isSmallScreen) 6.dp else 8.dp))

        Text(
            text = mensaje,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = if (isSmallScreen) 13.sp else 14.sp,
                lineHeight = if (isSmallScreen) 18.sp else 20.sp
            ),
            textAlign = TextAlign.Center,
            color = GrisMedio,
            maxLines = 3
        )

        Spacer(Modifier.height(if (isSmallScreen) 20.dp else 24.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth(if (isSmallScreen) 0.8f else 0.6f)
                .height(if (isSmallScreen) 44.dp else 48.dp),
            shape = RoundedCornerShape(if (isSmallScreen) 12.dp else 14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AzulCielo
            )
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(if (isSmallScreen) 18.dp else 20.dp)
            )
            Spacer(Modifier.width(if (isSmallScreen) 6.dp else 8.dp))
            Text(
                text = "Reintentar",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = if (isSmallScreen) 14.sp else 15.sp
                ),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EmptyTareasView() {
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 360

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (isSmallScreen) 20.dp else 24.dp,
                vertical = if (isSmallScreen) 24.dp else 28.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = FondoCard,
            shadowElevation = 3.dp
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = if (isSmallScreen) 24.dp else 32.dp,
                    vertical = if (isSmallScreen) 28.dp else 36.dp
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = AzulCieloMuyClaro,
                    modifier = Modifier.size(if (isSmallScreen) 80.dp else 90.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = null,
                        modifier = Modifier.padding(if (isSmallScreen) 20.dp else 22.dp),
                        tint = AzulCielo
                    )
                }
                Text(
                    text = "No hay tareas disponibles",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = if (isSmallScreen) 18.sp else 20.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = GrisOscuro,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Este curso aún no tiene tareas asignadas. Las tareas aparecerán aquí cuando el docente las publique.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = if (isSmallScreen) 13.sp else 14.sp,
                        lineHeight = if (isSmallScreen) 19.sp else 20.sp
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
    val infiniteTransition = rememberInfiniteTransition(label = "modernLoading")

    // Rotación del gradiente circular
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Pulsación del logo
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Alpha del resplandor
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Blanco),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                // Anillo exterior rotatorio con gradiente
                Surface(
                    modifier = Modifier
                        .size(120.dp)
                        .graphicsLayer {
                            rotationZ = rotation
                        },
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(
                        4.dp,
                        Brush.sweepGradient(
                            colors = listOf(
                                AzulCielo,
                                VerdeLima,
                                Fucsia,
                                Naranja,
                                AzulCielo
                            )
                        )
                    )
                ) {}

                // Resplandor intermedio
                Surface(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(scale),
                    shape = CircleShape,
                    color = AzulCielo.copy(alpha = glowAlpha * 0.2f)
                ) {}

                // Logo central - AQUÍ VA TU IMAGEN
                Surface(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(scale),
                    shape = CircleShape,
                    color = Blanco,
                    shadowElevation = 16.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        AzulCielo.copy(alpha = 0.1f),
                                        Blanco
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // REEMPLAZA ESTO CON TU IMAGEN DEL LOGO
                        Image(
                            painter = painterResource(id = R.drawable.edumonavatar1), // <-- PON AQUÍ TU LOGO
                            contentDescription = "Logo Edumon",
                            modifier = Modifier
                                .size(60.dp)
                                .padding(12.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                // Puntos orbitales
                for (i in 0..2) {
                    val orbitRotation by infiniteTransition.animateFloat(
                        initialValue = i * 120f,
                        targetValue = i * 120f + 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(3000 + i * 500, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "orbit$i"
                    )

                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .graphicsLayer {
                                rotationZ = orbitRotation
                            }
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(8.dp)
                                .offset(y = (-60).dp)
                                .align(Alignment.TopCenter),
                            shape = CircleShape,
                            color = listOf(VerdeLima, Fucsia, Naranja)[i],
                            shadowElevation = 4.dp
                        ) {}
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Puntos animados de carga
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0..2) {
                    val dotScale by infiniteTransition.animateFloat(
                        initialValue = 0.5f,
                        targetValue = 1.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                600,
                                easing = FastOutSlowInEasing,
                                delayMillis = i * 200
                            ),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot$i"
                    )

                    Surface(
                        modifier = Modifier
                            .size(10.dp)
                            .scale(dotScale),
                        shape = CircleShape,
                        color = listOf(AzulCielo, VerdeLima, Fucsia)[i]
                    ) {}
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Cargando",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AzulCielo
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Preparando tu experiencia educativa",
                fontSize = 14.sp,
                color = GrisNeutral,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
@Composable
fun ErrorView(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    val configuration = LocalConfiguration.current
    val isSmallScreen = configuration.screenWidthDp < 360

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoClaro),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isSmallScreen) 20.dp else 24.dp),
            shape = RoundedCornerShape(20.dp),
            color = FondoCard,
            shadowElevation = 4.dp
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.padding(
                    horizontal = if (isSmallScreen) 24.dp else 28.dp,
                    vertical = if (isSmallScreen) 28.dp else 32.dp
                )
            ) {
                Surface(
                    shape = CircleShape,
                    color = ErrorClaro,
                    modifier = Modifier.size(if (isSmallScreen) 70.dp else 80.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.padding(if (isSmallScreen) 18.dp else 20.dp),
                        tint = ErrorOscuro
                    )
                }
                Text(
                    text = "Error al cargar",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = if (isSmallScreen) 19.sp else 21.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = GrisOscuro
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = if (isSmallScreen) 13.sp else 14.sp,
                        lineHeight = if (isSmallScreen) 19.sp else 20.sp
                    ),
                    textAlign = TextAlign.Center,
                    color = GrisMedio
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier
                            .weight(1f)
                            .height(if (isSmallScreen) 46.dp else 50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AzulCielo
                        ),
                        border = BorderStroke(1.5.dp, AzulCielo)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            null,
                            modifier = Modifier.size(if (isSmallScreen) 18.dp else 20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Volver",
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isSmallScreen) 13.sp else 14.sp
                        )
                    }
                    Button(
                        onClick = onRetry,
                        modifier = Modifier
                            .weight(1f)
                            .height(if (isSmallScreen) 46.dp else 50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AzulCielo
                        )
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            null,
                            modifier = Modifier.size(if (isSmallScreen) 18.dp else 20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Reintentar",
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isSmallScreen) 13.sp else 14.sp
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