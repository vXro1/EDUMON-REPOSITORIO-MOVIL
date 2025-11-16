package com.example.edumonjetcompose.screens.profesor

import android.util.Log
import android.widget.Toast
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

                // Extraer nombre del curso
                cursoNombre = body?.get("cursoNombre")?.asString ?: "Curso"

                // Extraer participantes
                val participantesArray = body?.getAsJsonArray("participantes")
                val listaParticipantes = mutableListOf<Participante>()

                participantesArray?.forEach { elem ->
                    try {
                        val pObj = elem.asJsonObject

                        // Procesar foto de perfil
                        val fotoPerfilRaw = pObj.get("fotoPerfilUrl")
                        val fotoPerfilUrl = if (fotoPerfilRaw != null && !fotoPerfilRaw.isJsonNull) {
                            val url = fotoPerfilRaw.asString
                            // Si la URL ya es completa (http/https), usarla directamente
                            // Si es una ruta relativa, agregar BASE_URL
                            if (url.startsWith("http://") || url.startsWith("https://")) {
                                url
                            } else {
                                BASE_URL + url
                            }
                        } else {
                            null
                        }

                        Log.d("ParticipantesCurso", "📸 Foto - Raw: $fotoPerfilRaw, Final: $fotoPerfilUrl")

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

                        Log.d("ParticipantesCurso", "👤 Participante: ${participante.nombre} ${participante.apellido} - Foto: ${participante.fotoPerfilUrl}")
                    } catch (e: Exception) {
                        Log.e("ParticipantesCurso", "❌ Error parseando participante", e)
                    }
                }

                // Ordenar: docentes primero, luego padres
                participantes = listaParticipantes.sortedBy { it.etiqueta != "docente" }
                Log.d("ParticipantesCurso", "✅ Total participantes cargados: ${participantes.size}")

            } else {
                errorMessage = "Error al cargar participantes: ${response.code()}"
                Log.e("ParticipantesCurso", "❌ Error HTTP: ${response.code()} - ${response.message()}")

                // Intentar leer el cuerpo del error
                try {
                    val errorBody = response.errorBody()?.string()
                    Log.e("ParticipantesCurso", "❌ Error body: $errorBody")
                } catch (e: Exception) {
                    Log.e("ParticipantesCurso", "❌ No se pudo leer error body", e)
                }
            }
        } catch (e: Exception) {
            errorMessage = "Error de conexión: ${e.message}"
            Log.e("ParticipantesCurso", "❌ Excepción al cargar participantes", e)
        } finally {
            isLoading = false
        }
    }

    // Cargar datos al inicio
    LaunchedEffect(Unit) {
        Log.d("ParticipantesCurso", "🔑 Token recibido: ${if (token.isNotEmpty()) "Sí" else "No"}")

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
                        Text("Participantes", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        if (cursoNombre.isNotEmpty()) {
                            Text(
                                cursoNombre,
                                fontSize = 14.sp,
                                color = GrisMedio,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showAgregarDialog = true }) {
                        Icon(Icons.Default.PersonAdd, "Agregar participante", tint = VerdeLima)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FondoCard,
                    titleContentColor = GrisOscuro
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = FondoClaro,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAgregarDialog = true },
                containerColor = VerdeLima,
                contentColor = Color.White
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            EstadisticasCard(participantes)
                            Spacer(Modifier.height(8.dp))
                        }

                        // Docentes
                        val docentes = participantes.filter { it.etiqueta == "docente" }
                        if (docentes.isNotEmpty()) {
                            item {
                                SeccionHeader("Docentes", docentes.size, Icons.Default.School)
                            }
                            items(docentes) { participante ->
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
                            item { Spacer(Modifier.height(8.dp)) }
                        }

                        // Padres
                        val padres = participantes.filter { it.etiqueta == "padre" }
                        if (padres.isNotEmpty()) {
                            item {
                                SeccionHeader("Padres de Familia", padres.size, Icons.Default.People)
                            }
                            items(padres) { participante ->
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

                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }

            // Dialog para agregar participante
            if (showAgregarDialog) {
                AgregarParticipanteDialog(
                    cursoId = cursoId,
                    token = token,
                    onDismiss = { showAgregarDialog = false },
                    onSuccess = {
                        showAgregarDialog = false
                        scope.launch {
                            cargarParticipantes()
                            snackbarHostState.showSnackbar("Participante agregado exitosamente")
                        }
                    }
                )
            }

            // Dialog para confirmar eliminación
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
                                    snackbarHostState.showSnackbar("Participante removido")
                                    cargarParticipantes()
                                } else {
                                    snackbarHostState.showSnackbar("Error al remover participante")
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error: ${e.message}")
                            }
                            showDeleteDialog = false
                            participanteToDelete = null
                        }
                    }
                )
            }

            // Dialog para mostrar información del participante
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

// ==================== COMPONENTES UI ====================

@Composable
fun EstadisticasCard(participantes: List<Participante>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = FondoCard,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                icon = Icons.Default.People,
                value = participantes.size.toString(),
                label = "Total",
                color = AzulCielo
            )
            StatItem(
                icon = Icons.Default.School,
                value = participantes.count { it.etiqueta == "docente" }.toString(),
                label = "Docentes",
                color = Fucsia
            )
            StatItem(
                icon = Icons.Default.Person,
                value = participantes.count { it.etiqueta == "padre" }.toString(),
                label = "Padres",
                color = VerdeLima
            )
        }
    }
}

@Composable
fun StatItem(icon: ImageVector, value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.15f),
            modifier = Modifier.size(56.dp)
        ) {
            Icon(icon, null, modifier = Modifier.padding(14.dp), tint = color)
        }
        Spacer(Modifier.height(8.dp))
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = GrisOscuro)
        Text(label, fontSize = 12.sp, color = GrisMedio)
    }
}

@Composable
fun SeccionHeader(titulo: String, cantidad: Int, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = AzulCielo.copy(alpha = 0.15f),
            modifier = Modifier.size(40.dp)
        ) {
            Icon(icon, null, modifier = Modifier.padding(10.dp), tint = AzulCielo)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GrisOscuro)
            Text("$cantidad participante${if (cantidad != 1) "s" else ""}", fontSize = 13.sp, color = GrisMedio)
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

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = FondoCard,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar con AsyncImage de Coil
            if (!participante.fotoPerfilUrl.isNullOrBlank()) {
                AsyncImage(
                    model = participante.fotoPerfilUrl,
                    contentDescription = "Avatar de ${participante.nombre}",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(GrisExtraClaro),
                    contentScale = ContentScale.Crop,
                    error = painterResource(android.R.drawable.ic_menu_report_image),
                    placeholder = painterResource(android.R.drawable.ic_menu_report_image)
                )
            } else {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = colorTag.copy(alpha = 0.15f)
                ) {
                    Icon(
                        Icons.Default.Person,
                        null,
                        modifier = Modifier.padding(14.dp),
                        tint = colorTag
                    )
                }
            }

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${participante.nombre} ${participante.apellido}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = GrisOscuro,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colorTag.copy(alpha = 0.15f)
                    ) {
                        Text(
                            participante.etiqueta.uppercase(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorTag
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Email, null, modifier = Modifier.size(14.dp), tint = GrisMedio)
                    Text(
                        participante.correo,
                        fontSize = 13.sp,
                        color = GrisMedio,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!participante.telefono.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Phone, null, modifier = Modifier.size(14.dp), tint = GrisMedio)
                        Text(participante.telefono, fontSize = 13.sp, color = GrisMedio)
                    }
                }
            }

            // Acciones
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = AzulCielo.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Info, null, modifier = Modifier.padding(8.dp), tint = AzulCielo)
                    }
                }

                if (participante.etiqueta != "docente") {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = ErrorOscuro.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.padding(8.dp), tint = ErrorOscuro)
                        }
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
            shape = RoundedCornerShape(24.dp),
            color = FondoCard,
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Agregar Padre", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GrisOscuro)
                        Text("Completa los datos del nuevo participante", fontSize = 13.sp, color = GrisMedio)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Cerrar", tint = GrisMedio)
                    }
                }

                if (errorMsg != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = ErrorClaro
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, null, tint = ErrorOscuro, modifier = Modifier.size(20.dp))
                            Text(errorMsg!!, fontSize = 13.sp, color = ErrorOscuro)
                        }
                    }
                }

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AzulCielo,
                        focusedLabelColor = AzulCielo
                    )
                )

                OutlinedTextField(
                    value = apellido,
                    onValueChange = { apellido = it },
                    label = { Text("Apellido *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AzulCielo,
                        focusedLabelColor = AzulCielo
                    )
                )

                OutlinedTextField(
                    value = cedula,
                    onValueChange = { cedula = it },
                    label = { Text("Cédula *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Badge, null) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AzulCielo,
                        focusedLabelColor = AzulCielo
                    )
                )

                OutlinedTextField(
                    value = telefono,
                    onValueChange = { telefono = it },
                    label = { Text("Teléfono *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Phone, null) },
                    placeholder = { Text("+57 300 123 4567") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AzulCielo,
                        focusedLabelColor = AzulCielo
                    )
                )

                Text(
                    "* Campos obligatorios",
                    fontSize = 12.sp,
                    color = GrisMedio,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            errorMsg = null

                            if (nombre.isBlank() || apellido.isBlank() || cedula.isBlank() || telefono.isBlank()) {
                                errorMsg = "Completa todos los campos obligatorios"
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
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Participante agregado exitosamente", Toast.LENGTH_SHORT).show()
                                        }
                                        onSuccess()
                                    } else {
                                        val errorBody = response.errorBody()?.string()
                                        errorMsg = "Error: ${response.code()} - ${errorBody ?: "Error desconocido"}"
                                        Log.e("ParticipantesDialog", "Error: ${errorMsg}")
                                    }
                                } catch (e: Exception) {
                                    errorMsg = "Error: ${e.message}"
                                    Log.e("ParticipantesDialog", "Excepción", e)
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = VerdeLima)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Agregar")
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
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, null, tint = Advertencia, modifier = Modifier.size(48.dp)) },
        title = { Text("Remover participante", fontWeight = FontWeight.Bold) },
        text = {
            Text("¿Estás seguro de remover a ${participante.nombre} ${participante.apellido} del curso?")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = ErrorOscuro)
            ) {
                Text("Remover")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
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
                // Header con botón cerrar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Información del Participante",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = GrisOscuro
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Cerrar", tint = GrisMedio)
                    }
                }

                // Avatar y nombre
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!participante.fotoPerfilUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = participante.fotoPerfilUrl,
                            contentDescription = "Avatar de ${participante.nombre}",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(GrisExtraClaro),
                            contentScale = ContentScale.Crop,
                            error = painterResource(android.R.drawable.ic_menu_report_image),
                            placeholder = painterResource(android.R.drawable.ic_menu_report_image)
                        )
                    } else {
                        Surface(
                            modifier = Modifier.size(100.dp),
                            shape = CircleShape,
                            color = colorTag.copy(alpha = 0.15f)
                        ) {
                            Icon(
                                Icons.Default.Person,
                                null,
                                modifier = Modifier.padding(24.dp),
                                tint = colorTag
                            )
                        }
                    }

                    Text(
                        "${participante.nombre} ${participante.apellido}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = GrisOscuro,
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colorTag.copy(alpha = 0.15f)
                    ) {
                        Text(
                            participante.etiqueta.uppercase(),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorTag
                        )
                    }
                }

                HorizontalDivider(color = GrisClaro)

                // Información detallada
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    InfoRow(
                        icon = Icons.Default.Badge,
                        label = "ID",
                        value = participante.id,
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
                        label = "Correo",
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
                        icon = Icons.Default.Assignment,
                        label = "Rol",
                        value = participante.rol.uppercase(),
                        color = colorTag
                    )
                }

                // Botón cerrar
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AzulCielo)
                ) {
                    Text("Cerrar")
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
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.15f),
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                icon,
                null,
                modifier = Modifier.padding(10.dp),
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
            Text(
                value,
                fontSize = 15.sp,
                color = GrisOscuro,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun EmptyParticipantesView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = GrisMedio.copy(alpha = 0.15f),
                modifier = Modifier.size(100.dp)
            ) {
                Icon(Icons.Default.People, null, modifier = Modifier.padding(24.dp), tint = GrisMedio)
            }
            Text("Sin participantes", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GrisOscuro)
            Text("Agrega participantes para comenzar", fontSize = 14.sp, color = GrisMedio, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun ParticipantesLoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp), color = AzulCielo)
            Text("Cargando participantes...", fontSize = 16.sp, color = GrisMedio)
        }
    }
}

@Composable
fun ParticipantesErrorView(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(Icons.Default.Error, null, modifier = Modifier.size(64.dp), tint = ErrorOscuro)
            Text(message, fontSize = 16.sp, color = GrisOscuro, textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack) {
                    Text("Volver")
                }
                Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = AzulCielo)) {
                    Text("Reintentar")
                }
            }
        }
    }
}