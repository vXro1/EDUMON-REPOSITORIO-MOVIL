package com.example.edumonjetcompose.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.edumonjetcompose.Fucsia
import com.example.edumonjetcompose.Naranja
import com.example.edumonjetcompose.R
import com.example.edumonjetcompose.VerdeLima
import com.example.edumonjetcompose.models.AvatarOption
import com.example.edumonjetcompose.models.CursoItem
import com.example.edumonjetcompose.models.UserData
import com.example.edumonjetcompose.network.ApiService
import com.example.edumonjetcompose.ui.theme.AzulCielo
import com.example.edumonjetcompose.ui.theme.AzulCieloClaro
import com.example.edumonjetcompose.ui.theme.Blanco
import com.example.edumonjetcompose.ui.theme.Celeste
import com.example.edumonjetcompose.ui.theme.FondoOscuroTerciario
import com.example.edumonjetcompose.ui.theme.GradienteOcean
import com.example.edumonjetcompose.ui.theme.GrisNeutral
import com.google.gson.JsonArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


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
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    var userData by remember { mutableStateOf<UserData?>(null) }
    var cursos by remember { mutableStateOf<List<CursoItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showAvatarDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    fun cargarDatos() {
        scope.launch {
            isLoading = true
            errorMessage = null

            try {
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

    if (showAvatarDialog && userData != null) {
        AvatarSelectionDialog(
            currentAvatarUrl = userData!!.fotoPerfilUrl,
            token = token,
            onDismiss = { showAvatarDialog = false },
            onSuccess = { newAvatarUrl ->
                showAvatarDialog = false
                cargarDatos()
                scope.launch {
                    snackbarHostState.showSnackbar("Avatar actualizado exitosamente")
                }
            },
            onError = { message ->
                scope.launch {
                    snackbarHostState.showSnackbar(message)
                }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(
                    Icons.Default.ExitToApp,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "Cerrar Sesión",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "¿Estás seguro de que deseas cerrar sesión?",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444)
                    )
                ) {
                    Text("Cerrar Sesión")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar")
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
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = Blanco
                        )
                        Text(
                            "Plataforma Educativa",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Blanco
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = "Cerrar Sesión",
                            tint = Color(0xFFEF4444)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AzulCieloClaro,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFFFFFFF),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> HomeLoadingView()
                errorMessage != null -> HomeErrorView(
                    message = errorMessage ?: "",
                    onRetry = { cargarDatos() }
                )
                userData != null -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFFFFFFF)),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        item {
                            UserHeaderSectionImproved(
                                userData = userData!!,
                                onEditProfile = { showEditDialog = true },
                                onChangeAvatar = { showAvatarDialog = true },
                                screenWidth = screenWidth
                            )
                        }

                        item {
                            SectionHeaderImproved(
                                title = "Mis Cursos",
                                totalCursos = cursos.size
                            )
                        }

                        if (cursos.isEmpty()) {
                            item { EmptyCursosViewImproved() }
                        } else {
                            items(cursos) { curso ->
                                CursoCardImproved(
                                    curso = curso,
                                    onClick = {
                                        navController.navigate("infoCurso/${curso.id}")
                                    },
                                    screenWidth = screenWidth
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
fun UserHeaderSectionImproved(
    userData: UserData,
    onEditProfile: () -> Unit,
    onChangeAvatar: () -> Unit,
    screenWidth: Dp
) {
    val context = LocalContext.current
    val isSmallScreen = screenWidth < 360.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = AzulCieloClaro
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box {
                        Box(
                            modifier = Modifier
                                .size(if (isSmallScreen) 80.dp else 90.dp)
                                .background(
                                    brush = Brush.linearGradient(GradienteOcean),
                                    shape = CircleShape
                                )
                                .padding(3.dp)
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
                                        .clip(CircleShape)
                                        .background(FondoOscuroTerciario)
                                        .clickable { onChangeAvatar() },
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(FondoOscuroTerciario, CircleShape)
                                        .clickable { onChangeAvatar() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${userData.nombre.firstOrNull()?.uppercase() ?: ""}${userData.apellido.firstOrNull()?.uppercase() ?: ""}",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontSize = if (isSmallScreen) 24.sp else 28.sp
                                        ),
                                        fontWeight = FontWeight.Bold,
                                        color = Blanco
                                    )
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(30.dp)
                                .clickable { onChangeAvatar() },
                            shape = CircleShape,
                            color = AzulCielo,
                            shadowElevation = 4.dp
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Cambiar avatar",
                                tint = Blanco,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "${userData.nombre} ${userData.apellido}",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = if (isSmallScreen) 18.sp else 22.sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = Blanco,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AzulCielo.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = userData.rol.uppercase(),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 12.sp
                                ),
                                fontWeight = FontWeight.Bold,
                                color = Blanco
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onEditProfile,
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            AzulCielo,
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar perfil",
                        tint = Blanco,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    iconColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFF334155).copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    iconColor.copy(alpha = 0.2f),
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = Color(0xFF94A3B8)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SectionHeaderImproved(
    title: String,
    totalCursos: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = "$totalCursos ${if (totalCursos == 1) "curso disponible" else "cursos disponibles"}",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = AzulCielo
            )
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF00B9F0).copy(alpha = 0.2f)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.School,
                    contentDescription = null,
                    tint = Color(0xFF00B9F0),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun CursoCardImproved(
    curso: CursoItem,
    onClick: () -> Unit,
    screenWidth: Dp
) {
    val context = LocalContext.current
    val isSmallScreen = screenWidth < 360.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = AzulCieloClaro
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isSmallScreen) 180.dp else 200.dp)
            ) {
                if (!curso.fotoPortadaUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(curso.fotoPortadaUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Portada del curso",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        AzulCielo,
                                        Celeste
                                    )
                                )
                            )
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.School,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Blanco.copy(alpha = 0.7f)
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = when (curso.estado.lowercase()) {
                        "activo" -> Color(0xFF10B981)
                        "inactivo" -> Color(0xFFEF4444)
                        else -> Color(0xFFF59E0B)
                    }
                ) {
                    Text(
                        text = curso.estado.uppercase(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.Bold,
                        color = Blanco
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = curso.nombre,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Blanco
                )

                if (!curso.descripcion.isNullOrBlank()) {
                    Text(
                        text = curso.descripcion,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = Blanco.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                Blanco.copy(alpha = 0.12f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    Blanco.copy(alpha = 0.25f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = Blanco,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Docente",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = Blanco.copy(alpha = 0.6f)
                            )
                            Text(
                                text = curso.docenteNombre,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                fontWeight = FontWeight.SemiBold,
                                color = Blanco,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                Blanco.copy(alpha = 0.12f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    Blanco.copy(alpha = 0.25f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.People,
                                contentDescription = null,
                                tint = Blanco,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Estudiantes",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = Blanco.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "${curso.totalParticipantes}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                fontWeight = FontWeight.SemiBold,
                                color = Blanco
                            )
                        }
                    }
                }

                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Fucsia
                    ),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Text(
                        "Ver Curso",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                        fontWeight = FontWeight.Bold,
                        color = Blanco
                    )

                    Spacer(Modifier.width(8.dp))

                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Blanco
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyCursosViewImproved() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(
                        Color(0xFF00B9F0).copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.School,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = Color(0xFF00B9F0)
                )
            }

            Text(
                text = "No tienes cursos",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Text(
                text = "Los cursos asignados aparecerán aquí.\nContacta a tu docente para más información.",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}
@Composable
fun HomeLoadingView() {
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
fun HomeErrorView(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFFFF)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        Color(0xFFEF4444).copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    tint = Color(0xFFEF4444)
                )
            }

            Text(
                text = "Error al cargar",
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp),
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                textAlign = TextAlign.Center,
                color = Color(0xFF94A3B8),
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00B9F0)
                ),
                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 14.dp)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Reintentar",
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                    fontWeight = FontWeight.Bold
                )
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color(0xFF00B9F0)
                )

                Text(
                    "Editar Perfil",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isUpdating,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = apellido,
                    onValueChange = { apellido = it },
                    label = { Text("Apellido") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isUpdating,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = cedula,
                    onValueChange = { cedula = it },
                    label = { Text("Cédula") },
                    leadingIcon = {
                        Icon(Icons.Default.Badge, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isUpdating,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = correo,
                    onValueChange = { correo = it },
                    label = { Text("Correo") },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isUpdating,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = telefono,
                    onValueChange = { telefono = it },
                    label = { Text("Teléfono") },
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isUpdating,
                    shape = RoundedCornerShape(12.dp)
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
                enabled = !isUpdating && nombre.isNotBlank() && apellido.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00B9F0)
                )
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
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
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}

@Composable
fun AvatarSelectionDialog(
    token: String,
    currentAvatarUrl: String?,
    onSuccess: (String) -> Unit,
    onDismiss: () -> Unit,
    onError: (String) -> Unit,
    maxHeight: Dp = 420.dp
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var avatarList by remember { mutableStateOf<List<AvatarOption>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isUpdating by remember { mutableStateOf(false) }
    var selectedAvatar by remember { mutableStateOf<String?>(currentAvatarUrl) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(key1 = token) {
        isLoading = true
        errorMessage = null
        try {
            val response = withContext(Dispatchers.IO) {
                ApiService.getFotosPredeterminadas(token)
            }

            if (response.isSuccessful) {
                val body = response.body()
                val fotosArray = body?.getAsJsonArray("fotos")

                avatarList = fotosArray?.mapNotNull { elem ->
                    try {
                        val obj = elem.asJsonObject
                        val url = obj.get("url").asString
                        val nombre = if (obj.has("nombre")) obj.get("nombre").asString else url.substringAfterLast('/')
                        AvatarOption(url = url, name = nombre)
                    } catch (_: Exception) {
                        null
                    }
                } ?: emptyList()
            } else {
                errorMessage = "Error al cargar avatares (${response.code()})"
            }
        } catch (e: Exception) {
            errorMessage = "Error de conexión: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!isUpdating) onDismiss()
        },
        containerColor = Color.White,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Avatar",
                    tint = Color(0xFF00B9F0)
                )
                Text(
                    text = "Seleccionar avatar",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color.Red,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF00B9F0))
                    }
                } else {
                    if (avatarList.isEmpty()) {
                        Text(text = "No hay avatares disponibles.", color = Color.Gray)
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(avatarList) { avatar ->
                                val isSelected = selectedAvatar == avatar.url

                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(CircleShape)
                                        .clickable {
                                            selectedAvatar = avatar.url
                                        }
                                        .background(
                                            if (isSelected) Color(0xFF00B9F0).copy(alpha = 0.12f)
                                            else Color.Transparent,
                                            CircleShape
                                        )
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF00B9F0) else Color(0xFFDDDDDD),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(avatar.url)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = avatar.name,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(6.dp)
                                            .clip(CircleShape)
                                    )

                                    if (isSelected) {
                                        Surface(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(6.dp)
                                                .size(26.dp),
                                            shape = CircleShape,
                                            color = Color(0xFF00B9F0)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Seleccionado",
                                                tint = Color.White,
                                                modifier = Modifier.padding(4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val urlSeleccionada = selectedAvatar
                    if (urlSeleccionada != null && !isUpdating && urlSeleccionada != currentAvatarUrl) {
                        scope.launch {
                            isUpdating = true
                            try {
                                val response = withContext(Dispatchers.IO) {
                                    ApiService.updateFotoPerfilPredeterminada(
                                        token = token,
                                        fotoPredeterminadaUrl = urlSeleccionada
                                    )
                                }

                                if (response.isSuccessful) {
                                    onSuccess(urlSeleccionada)
                                } else {
                                    onError("Error al actualizar avatar")
                                }
                            } catch (e: Exception) {
                                onError("Error de conexión: ${e.message}")
                            } finally {
                                isUpdating = false
                            }
                        }
                    } else {
                        onDismiss()
                    }
                },
                enabled = !isUpdating && selectedAvatar != null && selectedAvatar != currentAvatarUrl
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFF00B9F0))
                } else {
                    Text(text = "Guardar", color = Color(0xFF00B9F0), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (!isUpdating) onDismiss()
            }) {
                Text(text = "Cancelar", color = Color.Black)
            }
        },
        shape = RoundedCornerShape(18.dp)
    )
}