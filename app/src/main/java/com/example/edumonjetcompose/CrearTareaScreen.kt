package com.example.edumonjetcompose

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.edumonjetcompose.models.*
import com.example.edumonjetcompose.network.ApiService
import com.example.edumonjetcompose.ui.*
import com.example.edumonjetcompose.ui.theme.Amarillo
import com.google.gson.JsonArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearTareaScreen(
    navController: NavController,
    cursoId: String,
    token: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Estados del formulario
    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var selectedModuloId by remember { mutableStateOf("") }
    var fechaEntrega by remember { mutableStateOf("") }
    var tipoEntrega by remember { mutableStateOf("archivo") }
    var asignacionTipo by remember { mutableStateOf("todos") }
    var criterios by remember { mutableStateOf("") }
    var selectedFiles by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var enlaces by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }
    var etiquetas by remember { mutableStateOf<List<String>>(emptyList()) }

    // Estados de la UI
    var modulos by remember { mutableStateOf<List<Modulo>>(emptyList()) }
    var participantes by remember { mutableStateOf<List<Participante>>(emptyList()) }
    var participantesSeleccionados by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoadingModulos by remember { mutableStateOf(true) }
    var isLoadingParticipantes by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showEnlaceDialog by remember { mutableStateOf(false) }
    var showEtiquetaDialog by remember { mutableStateOf(false) }
    var showParticipantesDialog by remember { mutableStateOf(false) }
    var docenteId by remember { mutableStateOf("") }
    var expandedSections by remember { mutableStateOf(setOf("basico")) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 🔧 CORRECCIÓN: Launcher para seleccionar archivos (galería, documentos, etc.)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedFiles = uris
            Log.d("CrearTarea", "✅ ${uris.size} archivo(s) seleccionado(s)")
        }
    }

    // Cargar datos iniciales
    LaunchedEffect(Unit) {
        Log.d("CrearTarea", "🔄 Iniciando carga de datos...")
        Log.d("CrearTarea", "  - Token: ${if (token.isNotEmpty()) "OK (${token.length} chars)" else "VACÍO"}")
        Log.d("CrearTarea", "  - CursoId: $cursoId")

        try {
            // ✅ CORRECCIÓN 1: Obtener perfil del usuario
            isLoadingModulos = true
            val profileResponse = withContext(Dispatchers.IO) {
                ApiService.getProfile(token)
            }

            if (profileResponse.isSuccessful) {
                val profileBody = profileResponse.body()

                // 🔧 CORRECCIÓN: Obtener el ID del usuario correctamente
                val userObj = profileBody?.getAsJsonObject("user") ?: profileBody

                docenteId = userObj?.get("id")?.asString
                    ?: userObj?.get("_id")?.asString
                            ?: ""

                Log.d("CrearTarea", "✅ DocenteId obtenido: $docenteId")

                if (docenteId.isEmpty()) {
                    errorMessage = "Error: No se pudo obtener ID del docente"
                    Log.e("CrearTarea", "❌ DocenteId vacío. Body: $profileBody")
                }
            } else {
                errorMessage = "Error al obtener perfil del usuario"
                Log.e("CrearTarea", "❌ Error perfil: ${profileResponse.code()}")
            }

            // ✅ CORRECCIÓN 2: Cargar módulos del curso
            val modulosResponse = withContext(Dispatchers.IO) {
                ApiService.getModulosByCurso(token, cursoId)
            }

            Log.d("CrearTarea", "📦 Respuesta módulos: ${modulosResponse.code()}")

            if (modulosResponse.isSuccessful) {
                val body = modulosResponse.body()
                Log.d("CrearTarea", "📄 Body recibido: ${body?.toString()?.take(200)}")

                val modulosJson = when {
                    body?.has("modulos") == true -> body.getAsJsonArray("modulos")
                    body?.isJsonArray == true -> body.asJsonArray
                    else -> JsonArray()
                }

                val modulosList = mutableListOf<Modulo>()
                for (elem in modulosJson) {
                    val m = elem.asJsonObject
                    val modulo = Modulo(
                        id = m.get("_id")?.asString ?: m.get("id")?.asString ?: "",
                        nombre = m.get("nombre")?.asString ?: m.get("titulo")?.asString ?: "",
                        descripcion = m.get("descripcion")?.asString ?: "",
                        cursoId = cursoId
                    )
                    modulosList.add(modulo)
                    Log.d("CrearTarea", "  ✓ Módulo: ${modulo.nombre} (ID: ${modulo.id})")
                }

                modulos = modulosList
                Log.d("CrearTarea", "✅ ${modulos.size} módulo(s) cargado(s)")

                if (modulos.isEmpty()) {
                    errorMessage = "Este curso no tiene módulos. Crea uno primero."
                }
            } else {
                val errorBody = modulosResponse.errorBody()?.string()
                errorMessage = "Error al cargar módulos: ${modulosResponse.code()}"
                Log.e("CrearTarea", "❌ Error módulos: $errorBody")
            }

        } catch (e: Exception) {
            Log.e("CrearTarea", "❌ Excepción al cargar datos", e)
            errorMessage = "Error: ${e.message}"
            scope.launch {
                snackbarHostState.showSnackbar("Error al cargar datos: ${e.message}")
            }
        } finally {
            isLoadingModulos = false
        }
    }

    // Cargar participantes cuando se selecciona asignación específica
    LaunchedEffect(asignacionTipo) {
        if (asignacionTipo == "seleccionados" && participantes.isEmpty()) {
            isLoadingParticipantes = true
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiService.getParticipantesCurso(token, cursoId)
                }

                Log.d("CrearTarea", "📦 Respuesta participantes: ${response.code()}")

                if (response.isSuccessful) {
                    val body = response.body()
                    val participantesJson = body?.getAsJsonArray("participantes") ?: JsonArray()
                    val participantesList = mutableListOf<Participante>()

                    Log.d("CrearTarea", "📄 Total participantes: ${participantesJson.size()}")

                    for (elem in participantesJson) {
                        val p = elem.asJsonObject

                        // 🔧 CORRECCIÓN: Los datos vienen directamente, no dentro de usuarioId
                        val id = p.get("_id")?.asString ?: p.get("id")?.asString ?: ""
                        val nombre = p.get("nombre")?.asString ?: "Sin nombre"
                        val apellido = p.get("apellido")?.asString ?: ""
                        val correo = p.get("correo")?.asString ?: ""
                        val etiqueta = p.get("etiqueta")?.asString ?: ""

                        // Solo agregar padres/estudiantes, no docentes
                        if (etiqueta.equals("padre", true) || etiqueta.equals("estudiante", true)) {
                            participantesList.add(
                                Participante(
                                    id = id,
                                    nombre = "$nombre $apellido".trim(),
                                    email = correo
                                )
                            )
                            Log.d("CrearTarea", "  ✓ Participante: $nombre $apellido ($etiqueta)")
                        }
                    }

                    participantes = participantesList
                    Log.d("CrearTarea", "✅ ${participantes.size} participante(s) cargado(s)")
                }
            } catch (e: Exception) {
                Log.e("CrearTarea", "❌ Error al cargar participantes", e)
            } finally {
                isLoadingParticipantes = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Crear Nueva Tarea", fontWeight = FontWeight.Bold)
                        Text(
                            "Completa los campos requeridos",
                            style = MaterialTheme.typography.bodySmall,
                            color = GrisMedio
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Volver")
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
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }

                // 🚨 Mensaje de error si existe
                if (errorMessage != null) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = ErrorOscuro.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, ErrorOscuro)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Error, null, tint = ErrorOscuro)
                                Text(
                                    errorMessage!!,
                                    color = ErrorOscuro,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // Sección: Información Básica
                item {
                    ExpandableSection(
                        title = "Información Básica",
                        icon = Icons.Default.Info,
                        isExpanded = expandedSections.contains("basico"),
                        isRequired = true,
                        onToggle = {
                            expandedSections = if (expandedSections.contains("basico")) {
                                expandedSections - "basico"
                            } else {
                                expandedSections + "basico"
                            }
                        }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Título
                            OutlinedTextField(
                                value = titulo,
                                onValueChange = { titulo = it },
                                label = { Text("Título de la tarea *") },
                                placeholder = { Text("Ej: Investigación sobre...") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = {
                                    Icon(Icons.Default.Assignment, null, tint = AzulCielo)
                                },
                                isError = titulo.isBlank(),
                                supportingText = {
                                    if (titulo.isBlank()) {
                                        Text("El título es obligatorio", color = ErrorOscuro)
                                    } else {
                                        Text("${titulo.length}/100 caracteres")
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AzulCielo,
                                    focusedLabelColor = AzulCielo,
                                    cursorColor = AzulCielo
                                )
                            )

                            // Descripción
                            OutlinedTextField(
                                value = descripcion,
                                onValueChange = { descripcion = it },
                                label = { Text("Descripción *") },
                                placeholder = { Text("Describe la tarea en detalle...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp),
                                maxLines = 6,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = AzulCielo,
                                        modifier = Modifier
                                            .padding(top = 8.dp, start = 4.dp)
                                            .size(24.dp)
                                    )
                                },
                                isError = descripcion.isBlank(),
                                supportingText = {
                                    if (descripcion.isBlank()) {
                                        Text("La descripción es obligatoria", color = ErrorOscuro)
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AzulCielo,
                                    focusedLabelColor = AzulCielo,
                                    cursorColor = AzulCielo
                                )
                            )

                            // Selector de Módulo
                            Text(
                                "Módulo *",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GrisOscuro
                            )

                            if (isLoadingModulos) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = AzulCielo
                                    )
                                }
                            } else if (modulos.isEmpty()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = AdvertenciaClaro,
                                    border = BorderStroke(1.dp, Advertencia)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Warning, null, tint = Advertencia)
                                        Column {
                                            Text(
                                                "No hay módulos",
                                                color = Advertencia,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "Crea un módulo antes de crear tareas",
                                                color = Advertencia,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    modulos.forEach { modulo ->
                                        ModuloOptionCard(
                                            modulo = modulo,
                                            isSelected = selectedModuloId == modulo.id,
                                            onClick = {
                                                selectedModuloId = modulo.id
                                                Log.d("CrearTarea", "✅ Módulo seleccionado: ${modulo.nombre}")
                                            }
                                        )
                                    }
                                }
                            }

                            // Fecha límite
                            Text(
                                "Fecha de entrega *",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GrisOscuro
                            )

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDatePicker = true },
                                shape = RoundedCornerShape(12.dp),
                                color = FondoCard,
                                border = BorderStroke(
                                    1.dp,
                                    if (fechaEntrega.isEmpty()) ErrorOscuro.copy(alpha = 0.5f) else AzulCielo
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CalendarToday, null, tint = AzulCielo)
                                        Column {
                                            Text(
                                                if (fechaEntrega.isEmpty()) "Seleccionar fecha"
                                                else formatearFechaParaTarea(fechaEntrega),
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = if (fechaEntrega.isEmpty()) GrisMedio else GrisOscuro,
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (fechaEntrega.isNotEmpty()) {
                                                Text(
                                                    calcularDiasRestantesParaTarea(fechaEntrega),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = GrisMedio
                                                )
                                            }
                                        }
                                    }
                                    Icon(Icons.Default.ArrowDropDown, null, tint = GrisMedio)
                                }
                            }
                        }
                    }
                }

                // Sección: Tipo de Entrega
                item {
                    ExpandableSection(
                        title = "Tipo de Entrega",
                        icon = Icons.Default.Upload,
                        isExpanded = expandedSections.contains("entrega"),
                        onToggle = {
                            expandedSections = if (expandedSections.contains("entrega")) {
                                expandedSections - "entrega"
                            } else {
                                expandedSections + "entrega"
                            }
                        }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TipoEntregaChip(
                                    label = "Archivo",
                                    icon = Icons.Default.AttachFile,
                                    isSelected = tipoEntrega == "archivo",
                                    onClick = { tipoEntrega = "archivo" },
                                    modifier = Modifier.weight(1f)
                                )
                                TipoEntregaChip(
                                    label = "Enlace",
                                    icon = Icons.Default.Link,
                                    isSelected = tipoEntrega == "enlace",
                                    onClick = { tipoEntrega = "enlace" },
                                    modifier = Modifier.weight(1f)
                                )
                                TipoEntregaChip(
                                    label = "Ambos",
                                    icon = Icons.Default.ListAlt,
                                    isSelected = tipoEntrega == "ambos",
                                    onClick = { tipoEntrega = "ambos" },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // 🔧 CORRECCIÓN: Botón para seleccionar archivos
                            if (tipoEntrega == "archivo" || tipoEntrega == "ambos") {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            Log.d("CrearTarea", "📎 Abriendo selector de archivos...")
                                            filePickerLauncher.launch("*/*")
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = VerdeLima.copy(alpha = 0.1f),
                                    border = BorderStroke(2.dp, VerdeLima.copy(alpha = 0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CloudUpload,
                                            null,
                                            modifier = Modifier.size(48.dp),
                                            tint = VerdeLima
                                        )
                                        Text(
                                            "Material de apoyo (opcional)",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            color = VerdeLima
                                        )
                                        Text(
                                            if (selectedFiles.isEmpty())
                                                "Toca para seleccionar archivos"
                                            else
                                                "${selectedFiles.size} archivo(s) seleccionado(s)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = GrisMedio
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Lista de archivos seleccionados
                if (selectedFiles.isNotEmpty()) {
                    items(selectedFiles) { uri ->
                        FileItemCard(
                            uri = uri,
                            onRemove = {
                                selectedFiles = selectedFiles.filter { it != uri }
                                Log.d("CrearTarea", "🗑️ Archivo removido. Quedan: ${selectedFiles.size}")
                            }
                        )
                    }
                }

                // Sección: Asignación
                item {
                    ExpandableSection(
                        title = "Asignación de Estudiantes",
                        icon = Icons.Default.People,
                        isExpanded = expandedSections.contains("asignacion"),
                        onToggle = {
                            expandedSections = if (expandedSections.contains("asignacion")) {
                                expandedSections - "asignacion"
                            } else {
                                expandedSections + "asignacion"
                            }
                        }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TipoEntregaChip(
                                    label = "Todos",
                                    icon = Icons.Default.Groups,
                                    isSelected = asignacionTipo == "todos",
                                    onClick = { asignacionTipo = "todos" },
                                    modifier = Modifier.weight(1f)
                                )
                                TipoEntregaChip(
                                    label = "Seleccionar",
                                    icon = Icons.Default.PersonAdd,
                                    isSelected = asignacionTipo == "seleccionados",
                                    onClick = {
                                        asignacionTipo = "seleccionados"
                                        showParticipantesDialog = true
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (asignacionTipo == "seleccionados") {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showParticipantesDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    color = AzulCielo.copy(alpha = 0.1f),
                                    border = BorderStroke(1.dp, AzulCielo)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.PersonAdd, null, tint = AzulCielo)
                                            Text(
                                                "${participantesSeleccionados.size} estudiante(s) seleccionado(s)",
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Icon(Icons.Default.Edit, null, tint = AzulCielo)
                                    }
                                }
                            }
                        }
                    }
                }

                // Sección: Configuración Adicional
                item {
                    ExpandableSection(
                        title = "Configuración Adicional",
                        icon = Icons.Default.Settings,
                        isExpanded = expandedSections.contains("adicional"),
                        isOptional = true,
                        onToggle = {
                            expandedSections = if (expandedSections.contains("adicional")) {
                                expandedSections - "adicional"
                            } else {
                                expandedSections + "adicional"
                            }
                        }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Criterios de evaluación
                            OutlinedTextField(
                                value = criterios,
                                onValueChange = { criterios = it },
                                label = { Text("Criterios de evaluación") },
                                placeholder = { Text("Criterios que se evaluarán...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                maxLines = 5,
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Checklist,
                                        null,
                                        tint = AzulCielo,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AzulCielo,
                                    focusedLabelColor = AzulCielo,
                                    cursorColor = AzulCielo
                                )
                            )

                            // Enlaces
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Enlaces de referencia",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                FilledTonalButton(
                                    onClick = { showEnlaceDialog = true },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = Fucsia.copy(alpha = 0.1f),
                                        contentColor = Fucsia
                                    )
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Agregar enlace")
                                }
                            }

                            if (enlaces.isNotEmpty()) {
                                enlaces.forEachIndexed { index, enlace ->
                                    EnlaceItemCard(
                                        enlace = enlace,
                                        onRemove = { enlaces = enlaces.filterIndexed { i, _ -> i != index } }
                                    )
                                }
                            }

                            // Etiquetas
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Etiquetas",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                FilledTonalButton(
                                    onClick = { showEtiquetaDialog = true },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = Amarillo.copy(alpha = 0.2f),
                                        contentColor = GrisOscuro
                                    )
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Agregar etiqueta")
                                }
                            }

                            if (etiquetas.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    etiquetas.forEach { etiqueta ->
                                        EtiquetaChip(
                                            label = etiqueta,
                                            onRemove = { etiquetas = etiquetas.filter { it != etiqueta } }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }

            // Botones de acción flotantes
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = FondoCard,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        enabled = !isCreating,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(2.dp, GrisMedio)
                    ) {
                        Icon(Icons.Default.Close, null, tint = GrisMedio)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Cancelar",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = GrisMedio
                        )
                    }

                    Button(
                        onClick = {
                            Log.d("CrearTarea", "🚀 Intentando crear tarea...")

                            // Validaciones
                            when {
                                titulo.isBlank() -> {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("El título es obligatorio")
                                    }
                                }
                                descripcion.isBlank() -> {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("La descripción es obligatoria")
                                    }
                                }
                                selectedModuloId.isEmpty() -> {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Selecciona un módulo")
                                    }
                                }
                                fechaEntrega.isEmpty() -> {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Selecciona una fecha de entrega")
                                    }
                                }
                                asignacionTipo == "seleccionados" && participantesSeleccionados.isEmpty() -> {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Selecciona al menos un estudiante")
                                    }
                                }
                                docenteId.isEmpty() -> {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Error: No se pudo obtener ID del docente")
                                    }
                                }
                                else -> {
                                    scope.launch {
                                        isCreating = true
                                        try {
                                            Log.d("CrearTarea", "📤 Preparando archivos...")

                                            // Procesar archivos
                                            val archivos = selectedFiles.mapNotNull { uri ->
                                                try {
                                                    val inputStream = context.contentResolver.openInputStream(uri)
                                                    val fileName = obtenerNombreArchivoDesdeUri(context, uri)
                                                    val file = File(context.cacheDir, fileName)
                                                    file.outputStream().use { inputStream?.copyTo(it) }

                                                    val requestFile = file.asRequestBody("*/*".toMediaTypeOrNull())
                                                    val part = MultipartBody.Part.createFormData("archivos", fileName, requestFile)

                                                    Log.d("CrearTarea", "  ✓ Archivo preparado: $fileName")
                                                    part
                                                } catch (e: Exception) {
                                                    Log.e("CrearTarea", "  ✗ Error con archivo: ${e.message}")
                                                    null
                                                }
                                            }

                                            Log.d("CrearTarea", "📋 Datos de la tarea:")
                                            Log.d("CrearTarea", "  - Título: $titulo")
                                            Log.d("CrearTarea", "  - CursoId: $cursoId")
                                            Log.d("CrearTarea", "  - ModuloId: $selectedModuloId")
                                            Log.d("CrearTarea", "  - DocenteId: $docenteId")
                                            Log.d("CrearTarea", "  - Tipo Entrega: $tipoEntrega")
                                            Log.d("CrearTarea", "  - Asignación: $asignacionTipo")
                                            Log.d("CrearTarea", "  - Archivos: ${archivos.size}")

                                            val response = withContext(Dispatchers.IO) {
                                                ApiService.createTarea(
                                                    token = token,
                                                    cursoId = cursoId,
                                                    moduloId = selectedModuloId,
                                                    docenteId = docenteId,
                                                    titulo = titulo,
                                                    descripcion = descripcion.ifBlank { null },
                                                    fechaEntrega = fechaEntrega,
                                                    tipoEntrega = tipoEntrega,
                                                    asignacionTipo = asignacionTipo,
                                                    participantesSeleccionados = if (asignacionTipo == "seleccionados")
                                                        participantesSeleccionados.toList() else null,
                                                    etiquetas = etiquetas.ifEmpty { null },
                                                    criterios = criterios.ifBlank { null },
                                                    archivos = archivos.ifEmpty { null },
                                                    enlaces = enlaces.ifEmpty { null }
                                                )
                                            }

                                            if (response.isSuccessful) {
                                                Log.d("CrearTarea", "✅ Tarea creada exitosamente")
                                                snackbarHostState.showSnackbar("✓ Tarea creada exitosamente")
                                                navController.popBackStack()
                                            } else {
                                                val errorBody = response.errorBody()?.string()
                                                Log.e("CrearTarea", "❌ Error al crear: ${response.code()} - $errorBody")
                                                snackbarHostState.showSnackbar("Error: ${response.code()}")
                                            }
                                        } catch (e: Exception) {
                                            Log.e("CrearTarea", "❌ Excepción al crear tarea", e)
                                            snackbarHostState.showSnackbar("Error: ${e.message}")
                                        } finally {
                                            isCreating = false
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        enabled = !isCreating && modulos.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VerdeLima,
                            disabledContainerColor = GrisClaro
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Check, null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Crear Tarea",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Diálogos
        if (showDatePicker) {
            DatePickerDialog(
                currentDate = fechaEntrega,
                onDateSelected = { date ->
                    fechaEntrega = date
                    showDatePicker = false
                },
                onDismiss = { showDatePicker = false }
            )
        }

        if (showEnlaceDialog) {
            AgregarEnlaceDialog(
                onDismiss = { showEnlaceDialog = false },
                onAgregar = { enlace ->
                    enlaces = enlaces + enlace
                    showEnlaceDialog = false
                }
            )
        }

        if (showEtiquetaDialog) {
            AgregarEtiquetaDialog(
                onDismiss = { showEtiquetaDialog = false },
                onAgregar = { etiqueta ->
                    if (etiqueta.isNotBlank() && !etiquetas.contains(etiqueta)) {
                        etiquetas = etiquetas + etiqueta
                    }
                    showEtiquetaDialog = false
                }
            )
        }

        if (showParticipantesDialog) {
            SeleccionarParticipantesDialog(
                participantes = participantes,
                participantesSeleccionados = participantesSeleccionados,
                isLoading = isLoadingParticipantes,
                onDismiss = { showParticipantesDialog = false },
                onConfirmar = { seleccionados ->
                    participantesSeleccionados = seleccionados
                    showParticipantesDialog = false
                }
            )
        }
    }
}

// ==================== COMPONENTES ====================

@Composable
fun ExpandableSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isExpanded: Boolean,
    isRequired: Boolean = false,
    isOptional: Boolean = false,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = FondoCard,
        shadowElevation = 2.dp
    ) {
        Column {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isExpanded) AzulCielo else GrisClaro,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                icon,
                                null,
                                modifier = Modifier.padding(8.dp),
                                tint = if (isExpanded) Color.White else GrisMedio
                            )
                        }
                        Column {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GrisOscuro
                                )
                                if (isRequired) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = ErrorOscuro.copy(alpha = 0.1f)
                                    ) {
                                        Text(
                                            "Requerido",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ErrorOscuro,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                if (isOptional) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = GrisClaro
                                    ) {
                                        Text(
                                            "Opcional",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GrisMedio,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null,
                        tint = GrisMedio
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
fun ModuloOptionCard(
    modulo: Modulo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) AzulCielo.copy(alpha = 0.1f) else FondoCard,
        border = BorderStroke(
            width = 2.dp,
            color = if (isSelected) AzulCielo else GrisClaro
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSelected) AzulCielo else GrisClaro,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    if (isSelected) Icons.Default.CheckCircle else Icons.Default.FolderOpen,
                    null,
                    modifier = Modifier.padding(8.dp),
                    tint = if (isSelected) Color.White else GrisMedio
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    modulo.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) AzulCielo else GrisOscuro
                )
                if (modulo.descripcion.isNotBlank()) {
                    Text(
                        modulo.descripcion,
                        style = MaterialTheme.typography.bodySmall,
                        color = GrisMedio,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun TipoEntregaChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) AzulCielo else FondoCard,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) AzulCielo else GrisClaro
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(18.dp),
                tint = if (isSelected) Color.White else GrisMedio
            )
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else GrisOscuro
            )
        }
    }
}

@Composable
fun FileItemCard(
    uri: Uri,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val fileName = remember { obtenerNombreArchivoDesdeUri(context, uri) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = FondoCard,
        border = BorderStroke(1.dp, GrisClaro)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Fucsia.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.AttachFile,
                    null,
                    modifier = Modifier.padding(8.dp),
                    tint = Fucsia
                )
            }
            Text(
                fileName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = GrisOscuro,
                maxLines = 1
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, "Eliminar", tint = ErrorOscuro)
            }
        }
    }
}

@Composable
fun EnlaceItemCard(
    enlace: Map<String, String>,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = FondoCard,
        border = BorderStroke(1.dp, Fucsia.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Fucsia.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Link,
                    null,
                    modifier = Modifier.padding(8.dp),
                    tint = Fucsia
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    enlace["nombre"] ?: "Enlace",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = GrisOscuro
                )
                Text(
                    enlace["url"] ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = GrisMedio,
                    maxLines = 1
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, "Eliminar", tint = ErrorOscuro)
            }
        }
    }
}

@Composable
fun EtiquetaChip(
    label: String,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Amarillo.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, Amarillo)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Tag,
                null,
                modifier = Modifier.size(14.dp),
                tint = GrisOscuro
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = GrisOscuro
            )
            Icon(
                Icons.Default.Close,
                "Eliminar",
                modifier = Modifier
                    .size(16.dp)
                    .clickable(onClick = onRemove),
                tint = GrisOscuro
            )
        }
    }
}

@Composable
fun DatePickerDialog(
    currentDate: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val calendar = Calendar.getInstance()
    if (currentDate.isNotEmpty()) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            sdf.parse(currentDate)?.let { calendar.time = it }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    var selectedYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Seleccionar fecha límite", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Año
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedYear-- }) {
                        Icon(Icons.Default.ChevronLeft, null)
                    }
                    Text(
                        "$selectedYear",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { selectedYear++ }) {
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }

                // Mes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        selectedMonth = if (selectedMonth == 0) 11 else selectedMonth - 1
                    }) {
                        Icon(Icons.Default.ChevronLeft, null)
                    }
                    Text(
                        obtenerNombreMesParaTarea(selectedMonth),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = {
                        selectedMonth = if (selectedMonth == 11) 0 else selectedMonth + 1
                    }) {
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }

                // Día
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (selectedDay > 1) selectedDay--
                    }) {
                        Icon(Icons.Default.ChevronLeft, null)
                    }
                    Text(
                        "$selectedDay",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = {
                        val maxDay = calendar.apply {
                            set(selectedYear, selectedMonth, 1)
                        }.getActualMaximum(Calendar.DAY_OF_MONTH)
                        if (selectedDay < maxDay) selectedDay++
                    }) {
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    calendar.set(selectedYear, selectedMonth, selectedDay, 23, 59, 59)
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                    onDateSelected(sdf.format(calendar.time))
                },
                colors = ButtonDefaults.buttonColors(containerColor = AzulCielo)
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = GrisMedio)
            }
        }
    )
}

@Composable
fun AgregarEnlaceDialog(
    onDismiss: () -> Unit,
    onAgregar: (Map<String, String>) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar enlace", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL *") },
                    placeholder = { Text("https://ejemplo.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre *") },
                    placeholder = { Text("Nombre del recurso") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (url.isNotBlank() && nombre.isNotBlank()) {
                        onAgregar(
                            mapOf(
                                "url" to url,
                                "nombre" to nombre,
                                "descripcion" to descripcion
                            )
                        )
                    }
                },
                enabled = url.isNotBlank() && nombre.isNotBlank()
            ) {
                Text("Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun AgregarEtiquetaDialog(
    onDismiss: () -> Unit,
    onAgregar: (String) -> Unit
) {
    var etiqueta by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar etiqueta", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = etiqueta,
                onValueChange = { etiqueta = it },
                label = { Text("Etiqueta") },
                placeholder = { Text("Ej: Importante, Urgente") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onAgregar(etiqueta.trim()) },
                enabled = etiqueta.isNotBlank()
            ) {
                Text("Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun SeleccionarParticipantesDialog(
    participantes: List<Participante>,
    participantesSeleccionados: Set<String>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirmar: (Set<String>) -> Unit
) {
    var seleccionTemp by remember { mutableStateOf(participantesSeleccionados) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Seleccionar estudiantes",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AzulCielo)
                }
            } else if (participantes.isEmpty()) {
                Text(
                    "No hay estudiantes en este curso",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GrisMedio
                )
            } else {
                LazyColumn(
                    modifier = Modifier.height(400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(participantes) { participante ->
                        ParticipanteCheckItem(
                            participante = participante,
                            isSelected = seleccionTemp.contains(participante.id),
                            onToggle = {
                                seleccionTemp = if (seleccionTemp.contains(participante.id)) {
                                    seleccionTemp - participante.id
                                } else {
                                    seleccionTemp + participante.id
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmar(seleccionTemp) },
                enabled = seleccionTemp.isNotEmpty()
            ) {
                Text("Confirmar (${seleccionTemp.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun ParticipanteCheckItem(
    participante: Participante,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) AzulCielo.copy(alpha = 0.1f) else FondoCard,
        border = BorderStroke(1.dp, if (isSelected) AzulCielo else GrisClaro)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = AzulCielo)
            )
            Column {
                Text(
                    participante.nombre,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = GrisOscuro
                )
                Text(
                    participante.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = GrisMedio
                )
            }
        }
    }
}

// ==================== FUNCIONES AUXILIARES ====================

private fun obtenerNombreMesParaTarea(month: Int): String {
    return when (month) {
        0 -> "Enero"; 1 -> "Febrero"; 2 -> "Marzo"; 3 -> "Abril"
        4 -> "Mayo"; 5 -> "Junio"; 6 -> "Julio"; 7 -> "Agosto"
        8 -> "Septiembre"; 9 -> "Octubre"; 10 -> "Noviembre"; 11 -> "Diciembre"
        else -> ""
    }
}

private fun obtenerNombreArchivoDesdeUri(context: android.content.Context, uri: Uri): String {
    var result = "archivo_${System.currentTimeMillis()}"
    try {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = it.getString(index)
                }
            }
        }
    } catch (e: Exception) {
        Log.e("CrearTarea", "Error obteniendo nombre de archivo", e)
    }
    return result
}

private fun formatearFechaParaTarea(fecha: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val date = sdf.parse(fecha)
        val outputFormat = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
        date?.let { outputFormat.format(it) } ?: fecha
    } catch (e: Exception) {
        fecha
    }
}

private fun calcularDiasRestantesParaTarea(fecha: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val fechaLimite = sdf.parse(fecha)
        val hoy = Calendar.getInstance().time
        val diff = fechaLimite.time - hoy.time
        val dias = (diff / (1000 * 60 * 60 * 24)).toInt()
        when {
            dias < 0 -> "Fecha vencida"
            dias == 0 -> "Vence hoy"
            dias == 1 -> "Vence mañana"
            dias <= 7 -> "Vence en $dias días"
            else -> "Faltan $dias días"
        }
    } catch (e: Exception) {
        ""
    }
}

// Modelo auxiliar
data class Participante(
    val id: String,
    val nombre: String,
    val email: String
)