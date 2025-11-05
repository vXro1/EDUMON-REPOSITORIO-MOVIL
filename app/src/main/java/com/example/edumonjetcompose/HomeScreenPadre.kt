package com.example.edumonjetcompose.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.edumonjetcompose.network.ApiService
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

data class Curso(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val fotoPortadaUrl: String?,
    val docenteNombre: String,
    val docenteApellido: String,
    val participantesCount: Int,
    val fechaCreacion: String,
    val estado: String
)

data class UserProfile(
    val id: String,
    val nombre: String,
    val apellido: String,
    val cedula: String,
    val correo: String,
    val telefono: String,
    val fotoPerfilUrl: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenPadre(
    token: String,
    onNavigateToCurso: (String) -> Unit,
    onLogout: () -> Unit
) {
    var cursos by remember { mutableStateOf<List<Curso>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentPage by remember { mutableStateOf(1) }
    var hasNextPage by remember { mutableStateOf(false) }
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Cargar perfil del usuario
    fun loadUserProfile() {
        scope.launch {
            try {
                val response = ApiService.getUserProfile(token)
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        // Limpiar teléfono: remover +57 si existe
                        val telefonoRaw = body.get("telefono")?.asString ?: ""
                        val telefonoLimpio = telefonoRaw.replace("+57", "").trim()

                        userProfile = UserProfile(
                            id = body.get("_id")?.asString ?: "",
                            nombre = body.get("nombre")?.asString ?: "",
                            apellido = body.get("apellido")?.asString ?: "",
                            cedula = body.get("cedula")?.asString ?: "",
                            correo = body.get("correo")?.asString ?: "",
                            telefono = telefonoLimpio,
                            fotoPerfilUrl = body.get("fotoPerfilUrl")?.asString
                        )
                    }
                }
            } catch (e: Exception) {
                // Error silencioso
            }
        }
    }

    LaunchedEffect(Unit) {
        loadUserProfile()
    }

    // Cargar cursos
    fun loadCursos(page: Int = 1) {
        scope.launch {
            isLoading = true
            errorMessage = null

            try {
                val response = ApiService.getMisCursos(token, page = page, limit = 10)

                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        val cursosArray = body.getAsJsonArray("cursos")
                        val pagination = body.getAsJsonObject("pagination")

                        val nuevoCursos = cursosArray.map { element ->
                            val curso = element.asJsonObject
                            val docente = curso.getAsJsonObject("docenteId")
                            val participantes = curso.getAsJsonArray("participantes")

                            Curso(
                                id = curso.get("_id").asString,
                                nombre = curso.get("nombre").asString,
                                descripcion = curso.get("descripcion")?.asString ?: "",
                                fotoPortadaUrl = curso.get("fotoPortadaUrl")?.asString,
                                docenteNombre = docente?.get("nombre")?.asString ?: "",
                                docenteApellido = docente?.get("apellido")?.asString ?: "",
                                participantesCount = participantes?.size() ?: 0,
                                fechaCreacion = curso.get("fechaCreacion")?.asString ?: "",
                                estado = curso.get("estado")?.asString ?: "activo"
                            )
                        }

                        if (page == 1) {
                            cursos = nuevoCursos
                        } else {
                            cursos = cursos + nuevoCursos
                        }

                        currentPage = page
                        hasNextPage = pagination.get("hasNextPage")?.asBoolean ?: false
                    }
                } else {
                    errorMessage = "Error al cargar cursos: ${response.code()}"
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadCursos(1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Avatar clickeable
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { showProfileDialog = true }
                        ) {
                            if (userProfile?.fotoPerfilUrl != null) {
                                AsyncImage(
                                    model = userProfile?.fotoPerfilUrl,
                                    contentDescription = "Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(24.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Column {
                            Text(
                                "Mis Cursos",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Bienvenido, ${userProfile?.nombre ?: "Usuario"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { loadCursos(1) }) {
                        Icon(Icons.Default.Refresh, "Actualizar")
                    }
                    Box {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Default.MoreVert, "Menú")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Mi Perfil") },
                                onClick = {
                                    showMenu = false
                                    showProfileDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.AccountCircle, null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Cerrar Sesión") },
                                onClick = {
                                    showMenu = false
                                    onLogout()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.ExitToApp, null)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                isLoading && cursos.isEmpty() -> {
                    LoadingView()
                }
                errorMessage != null && cursos.isEmpty() -> {
                    ErrorView(
                        message = errorMessage ?: "Error desconocido",
                        onRetry = { loadCursos(1) }
                    )
                }
                cursos.isEmpty() -> {
                    EmptyStateView()
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(cursos) { curso ->
                            CursoCard(
                                curso = curso,
                                onClick = { onNavigateToCurso(curso.id) }
                            )
                        }

                        if (hasNextPage) {
                            item {
                                LoadMoreButton(
                                    isLoading = isLoading,
                                    onClick = { loadCursos(currentPage + 1) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo de perfil
    if (showProfileDialog && userProfile != null) {
        ProfileDialog(
            token = token,
            userProfile = userProfile!!,
            onDismiss = { showProfileDialog = false },
            onProfileUpdated = {
                loadUserProfile()
                scope.launch {
                    snackbarHostState.showSnackbar("Perfil actualizado exitosamente")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDialog(
    token: String,
    userProfile: UserProfile,
    onDismiss: () -> Unit,
    onProfileUpdated: () -> Unit
) {
    var nombre by remember { mutableStateOf(userProfile.nombre) }
    var apellido by remember { mutableStateOf(userProfile.apellido) }
    var cedula by remember { mutableStateOf(userProfile.cedula) }
    var correo by remember { mutableStateOf(userProfile.correo) }
    var telefono by remember { mutableStateOf(userProfile.telefono) }
    var currentAvatarUrl by remember { mutableStateOf(userProfile.fotoPerfilUrl) }
    var isEditing by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showAvatarSelector by remember { mutableStateOf(false) }
    var fotosPredeterminadas by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoadingFotos by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Launcher para seleccionar imagen del dispositivo
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isSaving = true
                try {
                    // Convertir Uri a File
                    val file = uriToFile(context, uri)
                    val response = ApiService.updateFotoPerfilConArchivo(token, file)

                    if (response.isSuccessful) {
                        response.body()?.let { body ->
                            currentAvatarUrl = body.get("fotoPerfilUrl")?.asString
                            onProfileUpdated()
                        }
                    }
                } catch (e: Exception) {
                    // Error manejado
                } finally {
                    isSaving = false
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "Editar Perfil" else "Mi Perfil",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Cerrar")
                    }
                }

                Divider()

                // Avatar
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable(enabled = isEditing) { showAvatarSelector = true }
                    ) {
                        if (currentAvatarUrl != null) {
                            AsyncImage(
                                model = currentAvatarUrl,
                                contentDescription = "Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(60.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        if (isEditing) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Editar avatar",
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(20.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }

                    if (isEditing) {
                        Text(
                            text = "Toca para cambiar foto",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Campos de información
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    enabled = isEditing,
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = apellido,
                    onValueChange = { apellido = it },
                    label = { Text("Apellido") },
                    enabled = isEditing,
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = cedula,
                    onValueChange = { cedula = it },
                    label = { Text("Cédula") },
                    enabled = isEditing,
                    leadingIcon = { Icon(Icons.Default.Badge, null) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = correo,
                    onValueChange = { correo = it },
                    label = { Text("Correo") },
                    enabled = isEditing,
                    leadingIcon = { Icon(Icons.Default.Email, null) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = telefono,
                    onValueChange = {
                        // Permitir solo números y el símbolo +
                        if (it.all { char -> char.isDigit() || char == '+' }) {
                            telefono = it
                        }
                    },
                    label = { Text("Teléfono") },
                    enabled = isEditing,
                    leadingIcon = { Icon(Icons.Default.Phone, null) },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text(
                            text = "Formato: 3001234567 (sin +57)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                )

                Divider()

                // Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isEditing) {
                        OutlinedButton(
                            onClick = {
                                nombre = userProfile.nombre
                                apellido = userProfile.apellido
                                cedula = userProfile.cedula
                                correo = userProfile.correo
                                telefono = userProfile.telefono
                                isEditing = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar")
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    isSaving = true
                                    try {
                                        // Validar teléfono antes de enviar
                                        val telefonoLimpio = telefono.replace("+57", "").trim()

                                        if (telefonoLimpio.length != 10) {
                                            scope.launch {
                                                // Mostrar error en snackbar si hay uno disponible
                                            }
                                            return@launch
                                        }

                                        val response = ApiService.updateUserWithCedula(
                                            token = token,
                                            id = userProfile.id,
                                            nombre = nombre,
                                            apellido = apellido,
                                            cedula = cedula,
                                            correo = correo,
                                            telefono = telefonoLimpio
                                        )

                                        if (response.isSuccessful) {
                                            onProfileUpdated()
                                            isEditing = false
                                        } else {
                                            // Manejar error de respuesta
                                            val errorBody = response.errorBody()?.string()
                                            println("Error al actualizar: $errorBody")
                                        }
                                    } catch (e: Exception) {
                                        println("Excepción al actualizar: ${e.message}")
                                    } finally {
                                        isSaving = false
                                    }
                                }
                            },
                            enabled = !isSaving,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Guardar")
                            }
                        }
                    } else {
                        Button(
                            onClick = { isEditing = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Edit, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Editar Perfil")
                        }
                    }
                }
            }
        }
    }

    // Diálogo de selección de avatar
    if (showAvatarSelector) {
        AvatarSelectorDialog(
            token = token,
            currentUrl = currentAvatarUrl,
            onDismiss = { showAvatarSelector = false },
            onAvatarSelected = { url ->
                scope.launch {
                    isSaving = true
                    try {
                        val response = ApiService.updateFotoPerfilPredeterminada(token, url)
                        if (response.isSuccessful) {
                            currentAvatarUrl = url
                            onProfileUpdated()
                        }
                    } catch (e: Exception) {
                        // Error manejado
                    } finally {
                        isSaving = false
                        showAvatarSelector = false
                    }
                }
            },
            onSelectFromDevice = {
                showAvatarSelector = false
                imagePickerLauncher.launch("image/*")
            }
        )
    }
}

@Composable
fun AvatarSelectorDialog(
    token: String,
    currentUrl: String?,
    onDismiss: () -> Unit,
    onAvatarSelected: (String) -> Unit,
    onSelectFromDevice: () -> Unit
) {
    var fotosPredeterminadas by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val response = ApiService.getFotosPredeterminadas(token)
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        val fotosArray = body.getAsJsonArray("fotos")
                        // Extraer las URLs de los objetos
                        fotosPredeterminadas = fotosArray.mapNotNull { element ->
                            val fotoObj = element.asJsonObject
                            fotoObj.get("url")?.asString
                        }
                    }
                }
            } catch (e: Exception) {
                println("Error al cargar fotos: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Selecciona tu avatar",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = onSelectFromDevice,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Upload, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Subir desde dispositivo")
                }

                Divider()

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(fotosPredeterminadas) { url ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .background(
                                        if (url == currentUrl)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { onAvatarSelected(url) }
                                    .padding(4.dp)
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )

                                if (url == currentUrl) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Seleccionado",
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(24.dp),
                                        tint = MaterialTheme.colorScheme.primary
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

// Función auxiliar para convertir Uri a File
private fun uriToFile(context: android.content.Context, uri: Uri): File {
    val contentResolver = context.contentResolver
    val file = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")

    contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(file).use { output ->
            input.copyTo(output)
        }
    }

    return file
}

@Composable
fun CursoCard(
    curso: Curso,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // Imagen de portada con gradiente
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                if (curso.fotoPortadaUrl != null) {
                    AsyncImage(
                        model = curso.fotoPortadaUrl,
                        contentDescription = "Portada ${curso.nombre}",
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

                if (curso.estado == "archivado") {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Red.copy(alpha = 0.9f)
                    ) {
                        Text(
                            text = "Archivado",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = curso.nombre,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = curso.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.padding(8.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Column {
                        Text(
                            text = "Docente",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "${curso.docenteNombre} ${curso.docenteApellido}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        icon = Icons.Default.People,
                        value = curso.participantesCount.toString(),
                        label = "Participantes"
                    )

                    StatItem(
                        icon = Icons.Default.DateRange,
                        value = formatDate(curso.fechaCreacion),
                        label = "Creado"
                    )
                }
            }
        }
    }
}

@Composable
fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
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
                text = "Cargando cursos...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun ErrorView(
    message: String,
    onRetry: () -> Unit
) {
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
                imageVector = Icons.Default.Warning,
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
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Button(
                onClick = onRetry,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Reintentar")
            }
        }
    }
}

@Composable
fun EmptyStateView() {
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
                imageVector = Icons.Default.School,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Text(
                text = "No tienes cursos",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Aún no estás inscrito en ningún curso. Contacta a tu docente para unirte.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun LoadMoreButton(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
        } else {
            OutlinedButton(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Icon(Icons.Default.KeyboardArrowDown, null)
                Spacer(Modifier.width(8.dp))
                Text("Cargar más")
            }
        }
    }
}

fun formatDate(dateString: String): String {
    return try {
        val date = dateString.substring(0, 10)
        val parts = date.split("-")
        "${parts[2]}/${parts[1]}/${parts[0].substring(2)}"
    } catch (e: Exception) {
        "N/A"
    }
}