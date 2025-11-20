package com.example.edumonjetcompose.screens.profesor

import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.edumonjetcompose.Naranja
import com.example.edumonjetcompose.models.Participante
import com.example.edumonjetcompose.data.UserPreferences
import com.example.edumonjetcompose.network.ApiService
import com.example.edumonjetcompose.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


// ==================== PANTALLA PRINCIPAL ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticipantesCursoScreen(
    navController: NavController,
    cursoId: String,
    token: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var participantes by remember { mutableStateOf<List<Participante>>(emptyList()) }
    var cursoNombre by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAgregarDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var participanteToDelete by remember { mutableStateOf<Participante?>(null) }
    var participanteToShow by remember { mutableStateOf<Participante?>(null) }

    suspend fun cargarParticipantes() {
        isLoading = true
        errorMessage = null

        try {
            Log.d("ParticipantesCurso", "🔄 Cargando participantes del curso: $cursoId")

            val response = withContext(Dispatchers.IO) {
                ApiService.getParticipantesCurso(token, cursoId)
            }

            if (response.isSuccessful) {
                val body = response.body()
                Log.d("ParticipantesCurso", "✅ Respuesta exitosa: ${body.toString()}")

                cursoNombre = body?.get("cursoNombre")?.asString ?: "Curso"

                val participantesArray = body?.getAsJsonArray("participantes")
                val listaParticipantes = mutableListOf<Participante>()

                participantesArray?.forEach { elem ->
                    try {
                        val pObj = elem.asJsonObject

                        val fotoPerfilRaw = pObj.get("fotoPerfilUrl")
                        val fotoPerfilUrl = if (fotoPerfilRaw != null && !fotoPerfilRaw.isJsonNull) {
                            val url = fotoPerfilRaw.asString
                            if (url.startsWith("http://") || url.startsWith("https://")) {
                                url
                            } else {
                                BASE_URL + url
                            }
                        } else {
                            null
                        }

                        val participante = Participante(
                            id = pObj.get("_id")?.asString ?: "",
                            nombre = pObj.get("nombre")?.asString ?: "",
                            apellido = pObj.get("apellido")?.asString ?: "",
                            correo = pObj.get("correo")?.asString ?: "",
                            telefono = pObj.get("telefono")?.takeIf { !it.isJsonNull }?.asString,
                            cedula = pObj.get("cedula")?.takeIf { !it.isJsonNull }?.asString,
                            rol = pObj.get("rol")?.asString ?: "",
                            etiqueta = pObj.get("etiqueta")?.asString ?: "",
                            fotoPerfilUrl = fotoPerfilUrl
                        )
                        listaParticipantes.add(participante)
                    } catch (e: Exception) {
                        Log.e("ParticipantesCurso", "❌ Error parseando participante", e)
                    }
                }

                participantes = listaParticipantes.sortedBy { it.etiqueta != "docente" }
                Log.d("ParticipantesCurso", "✅ Total participantes cargados: ${participantes.size}")

            } else {
                errorMessage = "Error al cargar participantes: ${response.code()}"
                Log.e("ParticipantesCurso", "❌ Error HTTP: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            errorMessage = "Error de conexión: ${e.message}"
            Log.e("ParticipantesCurso", "❌ Excepción al cargar participantes", e)
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        if (token.isNotEmpty()) {
            cargarParticipantes()
        } else {
            errorMessage = "Token no disponible"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Participantes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = Color.White
                        )
                        if (cursoNombre.isNotEmpty()) {
                            Text(
                                cursoNombre,
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showAgregarDialog = true }) {
                        Icon(Icons.Default.PersonAdd, "Agregar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AzulCielo
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = FondoClaro,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAgregarDialog = true },
                containerColor = VerdeLima,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Default.Add, "Agregar")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> ParticipantesLoadingView()
                errorMessage != null -> ParticipantesErrorView(
                    message = errorMessage ?: "",
                    onRetry = { scope.launch { cargarParticipantes() } },
                    onBack = { navController.popBackStack() }
                )
                participantes.isEmpty() -> EmptyParticipantesView()
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            EstadisticasCard(participantes)
                        }

                        val docentes = participantes.filter { it.etiqueta == "docente" }
                        if (docentes.isNotEmpty()) {
                            item {
                                SeccionHeader("Docentes", docentes.size, Icons.Default.School)
                            }
                            items(docentes, key = { it.id }) { participante ->
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn() + slideInVertically()
                                ) {
                                    ParticipanteCard(
                                        participante = participante,
                                        onInfoClick = {
                                            participanteToShow = participante
                                            showInfoDialog = true
                                        },
                                        onDeleteClick = {
                                            participanteToDelete = participante
                                            showDeleteDialog = true
                                        }
                                    )
                                }
                            }
                        }

                        val padres = participantes.filter { it.etiqueta == "padre" }
                        if (padres.isNotEmpty()) {
                            item {
                                SeccionHeader("Padres de Familia", padres.size, Icons.Default.People)
                            }
                            items(padres, key = { it.id }) { participante ->
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn() + slideInVertically()
                                ) {
                                    ParticipanteCard(
                                        participante = participante,
                                        onInfoClick = {
                                            participanteToShow = participante
                                            showInfoDialog = true
                                        },
                                        onDeleteClick = {
                                            participanteToDelete = participante
                                            showDeleteDialog = true
                                        }
                                    )
                                }
                            }
                        }

                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }

            if (showAgregarDialog) {
                AgregarParticipanteDialog(
                    cursoId = cursoId,
                    token = token,
                    onDismiss = { showAgregarDialog = false },
                    onSuccess = {
                        showAgregarDialog = false
                        scope.launch {
                            cargarParticipantes()
                            snackbarHostState.showSnackbar("✅ Participante agregado exitosamente")
                        }
                    }
                )
            }

            if (showDeleteDialog && participanteToDelete != null) {
                ConfirmarEliminarDialog(
                    participante = participanteToDelete!!,
                    onDismiss = {
                        showDeleteDialog = false
                        participanteToDelete = null
                    },
                    onConfirm = {
                        scope.launch {
                            try {
                                val response = withContext(Dispatchers.IO) {
                                    ApiService.removerParticipante(token, cursoId, participanteToDelete!!.id)
                                }

                                if (response.isSuccessful) {
                                    snackbarHostState.showSnackbar("✅ Participante removido")
                                    cargarParticipantes()
                                } else {
                                    snackbarHostState.showSnackbar("❌ Error al remover")
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("❌ Error: ${e.message}")
                            }
                            showDeleteDialog = false
                            participanteToDelete = null
                        }
                    }
                )
            }

            if (showInfoDialog && participanteToShow != null) {
                InfoParticipanteDialog(
                    participante = participanteToShow!!,
                    onDismiss = {
                        showInfoDialog = false
                        participanteToShow = null
                    }
                )
            }
        }
    }
}

// ==================== COMPONENTES UI MEJORADOS ====================

@Composable
fun EstadisticasCard(participantes: List<Participante>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                Icon(
                    Icons.Default.BarChart,
                    contentDescription = null,
                    tint = AzulCielo,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "Estadísticas del Curso",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = GrisOscuro
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Default.Groups,
                    value = participantes.size.toString(),
                    label = "Total",
                    color = AzulCielo,
                    gradient = Brush.linearGradient(
                        colors = listOf(AzulCielo.copy(alpha = 0.2f), AzulCielo.copy(alpha = 0.05f))
                    )
                )
                StatItem(
                    icon = Icons.Default.School,
                    value = participantes.count { it.etiqueta == "docente" }.toString(),
                    label = "Docentes",
                    color = Fucsia,
                    gradient = Brush.linearGradient(
                        colors = listOf(Fucsia.copy(alpha = 0.2f), Fucsia.copy(alpha = 0.05f))
                    )
                )
                StatItem(
                    icon = Icons.Default.FamilyRestroom,
                    value = participantes.count { it.etiqueta == "padre" }.toString(),
                    label = "Padres",
                    color = VerdeLima,
                    gradient = Brush.linearGradient(
                        colors = listOf(VerdeLima.copy(alpha = 0.2f), VerdeLima.copy(alpha = 0.05f))
                    )
                )
            }
        }
    }
}

@Composable
fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    gradient: Brush
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.Transparent,
            modifier = Modifier.size(70.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(gradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Text(
            value,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Text(
            label,
            fontSize = 13.sp,
            color = GrisMedio,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SeccionHeader(titulo: String, cantidad: Int, icon: ImageVector) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = AzulCielo.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = AzulCielo.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = AzulCielo
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    titulo,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = GrisOscuro
                )
                Text(
                    "$cantidad participante${if (cantidad != 1) "s" else ""}",
                    fontSize = 14.sp,
                    color = GrisMedio,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ParticipanteCard(
    participante: Participante,
    onInfoClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val colorTag = when (participante.etiqueta) {
        "docente" -> Fucsia
        "padre" -> VerdeLima
        else -> GrisMedio
    }

    val tagText = when (participante.etiqueta) {
        "docente" -> "Docente"
        "padre" -> "Padre de Familia"
        else -> participante.etiqueta
    }

    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar mejorado
            Box {
                if (!participante.fotoPerfilUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = participante.fotoPerfilUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(colorTag.copy(0.2f), colorTag.copy(0.05f))
                                )
                            )
                            .border(
                                width = 2.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(colorTag.copy(0.5f), colorTag.copy(0.2f))
                                ),
                                shape = RoundedCornerShape(18.dp)
                            ),
                        contentScale = ContentScale.Crop,
                        error = painterResource(android.R.drawable.ic_menu_report_image),
                        placeholder = painterResource(android.R.drawable.ic_menu_report_image)
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(68.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(colorTag.copy(0.2f), colorTag.copy(0.05f))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = colorTag
                            )
                        }
                    }
                }

                // Badge de rol
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp),
                    shape = CircleShape,
                    color = colorTag,
                    shadowElevation = 4.dp
                ) {
                    Icon(
                        if (participante.etiqueta == "docente") Icons.Default.School else Icons.Default.FamilyRestroom,
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(4.dp),
                        tint = Color.White
                    )
                }
            }

            // Info mejorada
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "${participante.nombre} ${participante.apellido}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = GrisOscuro,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colorTag.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(colorTag, CircleShape)
                        )
                        Text(
                            tagText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colorTag
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = GrisMedio
                    )
                    Text(
                        participante.correo,
                        fontSize = 13.sp,
                        color = GrisMedio,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!participante.telefono.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = GrisMedio
                        )
                        Text(
                            participante.telefono,
                            fontSize = 13.sp,
                            color = GrisMedio
                        )
                    }
                }
            }

            // Botones de acción mejorados
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    onClick = onInfoClick,
                    shape = RoundedCornerShape(12.dp),
                    color = AzulCielo.copy(alpha = 0.12f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Info",
                        modifier = Modifier.padding(10.dp),
                        tint = AzulCielo
                    )
                }

                if (participante.etiqueta != "docente") {
                    Surface(
                        onClick = onDeleteClick,
                        shape = RoundedCornerShape(12.dp),
                        color = ErrorOscuro.copy(alpha = 0.12f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            modifier = Modifier.padding(10.dp),
                            tint = ErrorOscuro
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AgregarParticipanteDialog(
    cursoId: String,
    token: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var apellido by remember { mutableStateOf("") }
    var cedula by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header mejorado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = VerdeLima.copy(alpha = 0.15f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.padding(12.dp),
                                tint = VerdeLima
                            )
                        }
                        Column {
                            Text(
                                "Nuevo Participante",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = GrisOscuro
                            )
                            Text(
                                "Agrega un padre de familia",
                                fontSize = 13.sp,
                                color = GrisMedio
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Cerrar", tint = GrisMedio)
                    }
                }

                if (errorMsg != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = ErrorClaro
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = ErrorOscuro,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(errorMsg!!, fontSize = 14.sp, color = ErrorOscuro)
                        }
                    }
                }

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = VerdeLima) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VerdeLima,
                        focusedLabelColor = VerdeLima,
                        cursorColor = VerdeLima
                    )
                )

                OutlinedTextField(
                    value = apellido,
                    onValueChange = { apellido = it },
                    label = { Text("Apellido") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = VerdeLima) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VerdeLima,
                        focusedLabelColor = VerdeLima,
                        cursorColor = VerdeLima
                    )
                )

                OutlinedTextField(
                    value = cedula,
                    onValueChange = { cedula = it },
                    label = { Text("Cédula") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Badge, null, tint = VerdeLima) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VerdeLima,
                        focusedLabelColor = VerdeLima,
                        cursorColor = VerdeLima
                    )
                )

                OutlinedTextField(
                    value = telefono,
                    onValueChange = { telefono = it },
                    label = { Text("Teléfono") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Phone, null, tint = VerdeLima) },
                    placeholder = { Text("+57 300 123 4567") },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VerdeLima,
                        focusedLabelColor = VerdeLima,
                        cursorColor = VerdeLima
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(52.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancelar", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            errorMsg = null

                            if (nombre.isBlank() || apellido.isBlank() || cedula.isBlank() || telefono.isBlank()) {
                                errorMsg = "Todos los campos son obligatorios"
                                return@Button
                            }

                            scope.launch {
                                isLoading = true
                                try {
                                    val response = withContext(Dispatchers.IO) {
                                        ApiService.agregarParticipante(
                                            token = token,
                                            cursoId = cursoId,
                                            nombre = nombre,
                                            apellido = apellido,
                                            cedula = cedula,
                                            telefono = telefono,
                                            contraseña = null
                                        )
                                    }

                                    if (response.isSuccessful) {
                                        onSuccess()
                                    } else {
                                        val errorBody = response.errorBody()?.string()
                                        errorMsg = errorBody ?: "Error desconocido"
                                    }
                                } catch (e: Exception) {
                                    errorMsg = e.message
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VerdeLima)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
                                Text("Agregar", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmarEliminarDialog(
    participante: Participante,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ErrorOscuro.copy(alpha = 0.1f),
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.padding(20.dp),
                        tint = ErrorOscuro
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "¿Remover Participante?",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = GrisOscuro,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Esta acción no se puede deshacer",
                        fontSize = 14.sp,
                        color = GrisMedio,
                        textAlign = TextAlign.Center
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = GrisExtraClaro,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "${participante.nombre} ${participante.apellido}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrisOscuro
                        )
                        Text(
                            participante.correo,
                            fontSize = 14.sp,
                            color = GrisMedio
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancelar", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorOscuro)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(20.dp))
                            Text("Remover", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoParticipanteDialog(
    participante: Participante,
    onDismiss: () -> Unit
) {
    val colorTag = when (participante.etiqueta) {
        "docente" -> Fucsia
        "padre" -> VerdeLima
        else -> GrisMedio
    }

    val tagText = when (participante.etiqueta) {
        "docente" -> "Docente"
        "padre" -> "Padre de Familia"
        else -> participante.etiqueta
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(28.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Información Completa",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = GrisOscuro
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Cerrar", tint = GrisMedio)
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Avatar y nombre mejorados
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        if (!participante.fotoPerfilUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = participante.fotoPerfilUrl,
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                colorTag.copy(0.2f),
                                                colorTag.copy(0.05f)
                                            )
                                        )
                                    )
                                    .border(
                                        width = 3.dp,
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                colorTag.copy(0.5f),
                                                colorTag.copy(0.2f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(30.dp)
                                    ),
                                contentScale = ContentScale.Crop,
                                error = painterResource(android.R.drawable.ic_menu_report_image),
                                placeholder = painterResource(android.R.drawable.ic_menu_report_image)
                            )
                        } else {
                            Surface(
                                modifier = Modifier.size(120.dp),
                                shape = RoundedCornerShape(30.dp),
                                color = Color.Transparent
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    colorTag.copy(0.2f),
                                                    colorTag.copy(0.05f)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(50.dp),
                                        tint = colorTag
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = colorTag,
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .size(36.dp)
                                .offset(x = 4.dp, y = 4.dp)
                        ) {
                            Icon(
                                if (participante.etiqueta == "docente") Icons.Default.School else Icons.Default.FamilyRestroom,
                                contentDescription = null,
                                modifier = Modifier.padding(8.dp),
                                tint = Color.White
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "${participante.nombre} ${participante.apellido}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrisOscuro,
                            textAlign = TextAlign.Center
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colorTag.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(colorTag, CircleShape)
                                )
                                Text(
                                    tagText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorTag
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                HorizontalDivider(color = GrisClaro)

                Spacer(Modifier.height(20.dp))

                // Información detallada
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    InfoRow(
                        icon = Icons.Default.Fingerprint,
                        label = "ID de Usuario",
                        value = participante.id.takeLast(12) + "...",
                        color = AzulCielo
                    )

                    if (!participante.cedula.isNullOrBlank()) {
                        InfoRow(
                            icon = Icons.Default.Badge,
                            label = "Cédula",
                            value = participante.cedula,
                            color = Fucsia
                        )
                    }

                    InfoRow(
                        icon = Icons.Default.Email,
                        label = "Correo Electrónico",
                        value = participante.correo,
                        color = VerdeLima
                    )

                    if (!participante.telefono.isNullOrBlank()) {
                        InfoRow(
                            icon = Icons.Default.Phone,
                            label = "Teléfono",
                            value = participante.telefono,
                            color = Naranja
                        )
                    }

                    InfoRow(
                        icon = Icons.Default.WorkspacePremium,
                        label = "Rol en el Sistema",
                        value = participante.rol.replaceFirstChar { it.uppercase() },
                        color = colorTag
                    )
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AzulCielo)
                ) {
                    Text("Cerrar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = color.copy(alpha = 0.12f),
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.padding(12.dp),
                tint = color
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                fontSize = 12.sp,
                color = GrisMedio,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                fontSize = 16.sp,
                color = GrisOscuro,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun EmptyParticipantesView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = AzulCielo.copy(alpha = 0.1f),
                modifier = Modifier.size(120.dp)
            ) {
                Icon(
                    Icons.Default.Groups,
                    contentDescription = null,
                    modifier = Modifier.padding(30.dp),
                    tint = AzulCielo
                )
            }
            Text(
                "Sin Participantes",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = GrisOscuro
            )
            Text(
                "Agrega el primer participante\npresionando el botón +",
                fontSize = 15.sp,
                color = GrisMedio,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
fun ParticipantesLoadingView() {
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
            .background(FondoClaro),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                // Anillo exterior rotatorio con gradiente
                Surface(
                    modifier = Modifier
                        .size(140.dp)
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
                        .size(110.dp)
                        .scale(scale),
                    shape = CircleShape,
                    color = AzulCielo.copy(alpha = glowAlpha * 0.2f)
                ) {}

                // Logo central
                Surface(
                    modifier = Modifier
                        .size(90.dp)
                        .scale(scale),
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 16.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        AzulCielo.copy(alpha = 0.1f),
                                        Color.White
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Groups,
                            contentDescription = null,
                            modifier = Modifier.size(50.dp),
                            tint = AzulCielo
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
                            .size(140.dp)
                            .graphicsLayer {
                                rotationZ = orbitRotation
                            }
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(10.dp)
                                .offset(y = (-70).dp)
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
                text = "Cargando Participantes",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = AzulCielo
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Obteniendo información del curso",
                fontSize = 15.sp,
                color = GrisMedio,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ParticipantesErrorView(
    message: String,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = ErrorOscuro.copy(alpha = 0.1f),
                modifier = Modifier.size(100.dp)
            ) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.padding(25.dp),
                    tint = ErrorOscuro
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Error al Cargar",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = GrisOscuro
                )
                Text(
                    message,
                    fontSize = 14.sp,
                    color = GrisMedio,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text("Volver", fontWeight = FontWeight.SemiBold)
                    }
                }

                Button(
                    onClick = onRetry,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AzulCielo)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text("Reintentar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}