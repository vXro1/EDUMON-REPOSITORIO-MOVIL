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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.edumonjetcompose.models.CursoItem
import com.example.edumonjetcompose.models.UserData
import com.example.edumonjetcompose.network.ApiService
import com.google.gson.JsonArray
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenPadre(
    navController: NavController,
    token: String,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var userData by remember { mutableStateOf<UserData?>(null) }
    var cursos by remember { mutableStateOf<List<CursoItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedCurso by remember { mutableStateOf<CursoItem?>(null) }

    fun cargarDatos() {
        scope.launch {
            isLoading = true
            errorMessage = null

            try {
                // Cargar perfil del usuario
                val profileResponse = ApiService.getUserProfile(token)
                if (profileResponse.isSuccessful) {
                    val body = profileResponse.body()

                    userData = UserData(
                        id = body?.get("_id")?.asString ?: "",
                        nombre = body?.get("nombre")?.asString ?: "",
                        apellido = body?.get("apellido")?.asString ?: "",
                        cedula = body?.get("cedula")?.asString,
                        correo = body?.get("correo")?.asString ?: "",
                        telefono = body?.get("telefono")?.asString ?: "",
                        rol = body?.get("rol")?.asString ?: "",
                        fotoPerfilUrl = body?.get("fotoPerfilUrl")?.asString,
                        estado = body?.get("estado")?.asString ?: ""
                    )
                }

                // Cargar cursos
                val cursosResponse = ApiService.getMisCursos(token, page = 1, limit = 50)
                if (cursosResponse.isSuccessful) {
                    val body = cursosResponse.body()
                    val cursosJson = when {
                        body?.has("cursos") == true -> body.getAsJsonArray("cursos")
                        body?.isJsonArray == true -> body.asJsonArray
                        else -> JsonArray()
                    }

                    cursos = cursosJson.mapNotNull { elemento ->
                        try {
                            val curso = elemento.asJsonObject
                            val docenteJson = curso.getAsJsonObject("docenteId")
                            val participantesArray = curso.getAsJsonArray("participantes")

                            CursoItem(
                                id = curso.get("_id")?.asString ?: return@mapNotNull null,
                                nombre = curso.get("nombre")?.asString ?: "Sin nombre",
                                descripcion = curso.get("descripcion")?.asString,
                                fotoPortadaUrl = curso.get("fotoPortadaUrl")?.asString,
                                estado = curso.get("estado")?.asString ?: "activo",
                                totalParticipantes = participantesArray?.size() ?: 0,
                                docenteNombre = docenteJson?.get("nombre")?.asString ?: "Docente"
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
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
        cargarDatos()
    }

    // Diálogo para editar perfil
    if (showEditDialog && userData != null) {
        EditProfileDialog(
            userData = userData!!,
            token = token,
            onDismiss = { showEditDialog = false },
            onSuccess = {
                showEditDialog = false
                cargarDatos()
                scope.launch {
                    snackbarHostState.showSnackbar("Perfil actualizado exitosamente")
                }
            },
            onError = { message ->
                scope.launch {
                    snackbarHostState.showSnackbar(message)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Edumon",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Bienvenido",
                            style = MaterialTheme.typography.labelMedium,
                            color = LocalContentColor.current.copy(alpha = 0.7f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
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
                    HomeLoadingView()
                }
                errorMessage != null -> {
                    HomeErrorView(
                        message = errorMessage ?: "",
                        onRetry = { cargarDatos() }
                    )
                }
                userData != null -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        // Header del usuario
                        item {
                            UserHeaderSection(
                                userData = userData!!,
                                onEditProfile = { showEditDialog = true }
                            )
                        }

                        // Sección de cursos
                        item {
                            SectionHeader(
                                title = "Mis Cursos",
                                subtitle = "${cursos.size} cursos disponibles"
                            )
                        }

                        if (cursos.isEmpty()) {
                            item {
                                EmptyCursosView()
                            }
                        } else {
                            items(cursos) { curso ->
                                CursoCard(
                                    curso = curso,
                                    onClick = {
                                        // CORRECCIÓN: Usar "infoCurso" en lugar de "curso_detalle"
                                        navController.navigate("infoCurso/${curso.id}")
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

@Composable
fun EditProfileDialog(
    userData: UserData,
    token: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var nombre by remember { mutableStateOf(userData.nombre) }
    var apellido by remember { mutableStateOf(userData.apellido) }
    var cedula by remember { mutableStateOf(userData.cedula ?: "") }
    var correo by remember { mutableStateOf(userData.correo) }
    var telefono by remember { mutableStateOf(userData.telefono) }
    var isUpdating by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isUpdating) onDismiss() },
        title = {
            Text(
                "Editar Perfil",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isUpdating
                )

                OutlinedTextField(
                    value = apellido,
                    onValueChange = { apellido = it },
                    label = { Text("Apellido") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isUpdating
                )

                OutlinedTextField(
                    value = cedula,
                    onValueChange = { cedula = it },
                    label = { Text("Cédula") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isUpdating
                )

                OutlinedTextField(
                    value = correo,
                    onValueChange = { correo = it },
                    label = { Text("Correo") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isUpdating
                )

                OutlinedTextField(
                    value = telefono,
                    onValueChange = { telefono = it },
                    label = { Text("Teléfono") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isUpdating
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        isUpdating = true
                        try {
                            val response = ApiService.updateUserWithCedula(
                                token = token,
                                id = userData.id,
                                nombre = nombre.trim(),
                                apellido = apellido.trim(),
                                cedula = cedula.trim(),
                                correo = correo.trim(),
                                telefono = telefono.trim()
                            )

                            if (response.isSuccessful) {
                                onSuccess()
                            } else {
                                onError("Error al actualizar el perfil")
                            }
                        } catch (e: Exception) {
                            onError("Error de conexión: ${e.message}")
                        } finally {
                            isUpdating = false
                        }
                    }
                },
                enabled = !isUpdating && nombre.isNotBlank() && apellido.isNotBlank()
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Guardar")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isUpdating
            ) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun UserHeaderSection(
    userData: UserData,
    onEditProfile: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box {
            // Fondo decorativo con gradiente suave
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF6366F1).copy(alpha = 0.1f),
                                Color(0xFFEC4899).copy(alpha = 0.1f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .shadow(8.dp, CircleShape)
                                .background(Color.White, CircleShape)
                                .padding(4.dp)
                        ) {
                            if (!userData.fotoPerfilUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(userData.fotoPerfilUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Color(0xFF6366F1).copy(alpha = 0.15f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${userData.nombre.firstOrNull()?.uppercase() ?: ""}${userData.apellido.firstOrNull()?.uppercase() ?: ""}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6366F1)
                                    )
                                }
                            }
                        }

                        // Información del usuario
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${userData.nombre} ${userData.apellido}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF6366F1).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = userData.rol.uppercase(),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6366F1)
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = userData.correo,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Botón de editar
                    FilledIconButton(
                        onClick = onEditProfile,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFF6366F1)
                        )
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar perfil",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun CursoCard(
    curso: CursoItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Imagen del curso
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(120.dp)
            ) {
                if (!curso.fotoPortadaUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(curso.fotoPortadaUrl)
                            .crossfade(true)
                            .build(),
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
                                        Color(0xFF6366F1),
                                        Color(0xFFEC4899)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.School,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.White
                        )
                    }
                }
            }

            // Información del curso
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = curso.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (!curso.descripcion.isNullOrBlank()) {
                    Text(
                        text = curso.descripcion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF6366F1)
                        )
                        Text(
                            text = curso.docenteNombre,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF6366F1)
                        )
                        Text(
                            text = "${curso.totalParticipantes}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Icono de navegación
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF6366F1)
                )
            }
        }
    }
}

@Composable
fun EmptyCursosView() {
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
                imageVector = Icons.Default.School,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF6366F1).copy(alpha = 0.5f)
            )
            Text(
                text = "No tienes cursos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Los cursos asignados aparecerán aquí",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun HomeLoadingView() {
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
                text = "Cargando...",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun HomeErrorView(message: String, onRetry: () -> Unit) {
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
            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Reintentar")
            }
        }
    }
}