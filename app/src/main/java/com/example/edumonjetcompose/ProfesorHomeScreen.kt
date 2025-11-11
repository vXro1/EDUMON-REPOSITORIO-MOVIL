package com.example.edumonjetcompose.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.edumonjetcompose.data.UserPreferences
import com.example.edumonjetcompose.network.ApiService
import com.example.edumonjetcompose.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CursoItem(
    val id: String,
    val nombre: String,
    val descripcion: String?,
    val fotoPortadaUrl: String?,
    val estado: String,
    val totalParticipantes: Int,
    val docenteNombre: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfesorHomeScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userPreferences = remember { UserPreferences(context) }

    var token by remember { mutableStateOf<String?>(null) }
    var userData by remember { mutableStateOf<com.example.edumonjetcompose.ui.UserData?>(null) }
    var cursos by remember { mutableStateOf<List<CursoItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedCurso by remember { mutableStateOf<CursoItem?>(null) }

    // Datos para crear/editar curso
    var nombreCurso by remember { mutableStateOf("") }
    var descripcionCurso by remember { mutableStateOf("") }

    // Cargar datos del usuario y token
    LaunchedEffect(Unit) {
        try {
            // Cargar token desde DataStore
            token = withContext(Dispatchers.IO) {
                userPreferences.getToken()
            }

            android.util.Log.d("ProfesorHome", """
                 Token cargado:
                - Token presente: ${!token.isNullOrEmpty()}
                - Token (primeros 20): ${token?.take(20)}...
            """.trimIndent())

            // Validar que tenemos token
            if (token.isNullOrEmpty()) {
                android.util.Log.e("ProfesorHome", " Token vacío o nulo")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Sesión inválida - Token no encontrado", Toast.LENGTH_LONG).show()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
                return@LaunchedEffect
            }

            // Cargar datos desde el servidor
            android.util.Log.d("ProfesorHome", "🔄 Obteniendo perfil desde servidor...")
            val response = withContext(Dispatchers.IO) {
                ApiService.getUserProfile(token!!)
            }

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    userData = com.example.edumonjetcompose.ui.UserData(
                        id = body.get("_id")?.asString ?: "",
                        nombre = body.get("nombre")?.asString ?: "",
                        apellido = body.get("apellido")?.asString ?: "",
                        correo = body.get("correo")?.asString ?: "",
                        rol = body.get("rol")?.asString ?: "",
                        telefono = body.get("telefono")?.asString ?: "",
                        cedula = body.get("cedula")?.takeIf { !it.isJsonNull }?.asString,
                        fotoPerfilUrl = body.get("fotoPerfilUrl")?.takeIf { !it.isJsonNull }?.asString,
                        estado = body.get("estado")?.asString ?: "activo" // ✅ agregado
                    )


                    android.util.Log.d("ProfesorHome", """
                        ✅ Datos del usuario cargados desde servidor:
                        - ID: ${userData?.id}
                        - Nombre: ${userData?.nombre} ${userData?.apellido}
                        - Rol: ${userData?.rol}
                        - Correo: ${userData?.correo}
                    """.trimIndent())

                    // Guardar en DataStore para futura referencia
                    withContext(Dispatchers.IO) {
                        userPreferences.saveUserData(userData!!)
                    }
                } else {
                    throw Exception("Respuesta del servidor vacía")
                }
            } else {
                throw Exception("Error al cargar perfil: ${response.code()}")
            }

            // Validar que tenemos los datos necesarios
            if (userData == null) {
                android.util.Log.e("ProfesorHome", "❌ UserData es null después de cargar desde servidor")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error al cargar datos de usuario", Toast.LENGTH_LONG).show()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
                return@LaunchedEffect
            }

            // Cargar cursos
            android.util.Log.d("ProfesorHome", "🔄 Iniciando carga de cursos...")
            cargarCursos(token!!, scope, context) { cursosResponse ->
                android.util.Log.d("ProfesorHome", "✅ Cursos cargados: ${cursosResponse.size}")
                cursos = cursosResponse
                isLoading = false
            }
        } catch (e: Exception) {
            android.util.Log.e("ProfesorHome", "❌ Error en LaunchedEffect", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error al cargar datos: ${e.message}", Toast.LENGTH_LONG).show()
                errorMessage = "Error: ${e.message}"
                isLoading = false
            }
        }
    }

    // Animaciones
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                contentColor = AzulCielo,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = "Inicio"
                        )
                    },
                    label = { Text("Inicio") },
                    selected = true,
                    onClick = { },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AzulCielo,
                        selectedTextColor = AzulCielo,
                        indicatorColor = AzulCielo.copy(alpha = 0.1f),
                        unselectedIconColor = GrisNeutral,
                        unselectedTextColor = GrisNeutral
                    )
                )

                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Notificaciones"
                        )
                    },
                    label = { Text("Notificaciones") },
                    selected = false,
                    onClick = {
                        Toast.makeText(context, "Notificaciones", Toast.LENGTH_SHORT).show()
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AzulCielo,
                        selectedTextColor = AzulCielo,
                        indicatorColor = AzulCielo.copy(alpha = 0.1f),
                        unselectedIconColor = GrisNeutral,
                        unselectedTextColor = GrisNeutral
                    )
                )

                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Configuración"
                        )
                    },
                    label = { Text("Ajustes") },
                    selected = false,
                    onClick = {
                        Toast.makeText(context, "Configuración", Toast.LENGTH_SHORT).show()
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AzulCielo,
                        selectedTextColor = AzulCielo,
                        indicatorColor = AzulCielo.copy(alpha = 0.1f),
                        unselectedIconColor = GrisNeutral,
                        unselectedTextColor = GrisNeutral
                    )
                )

                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = "Cerrar Sesión"
                        )
                    },
                    label = { Text("Salir") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            try {
                                token?.let {
                                    withContext(Dispatchers.IO) {
                                        ApiService.logout(it)
                                    }
                                }
                                withContext(Dispatchers.IO) {
                                    userPreferences.clearAll()
                                }
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Error al cerrar sesión", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Error,
                        selectedTextColor = Error,
                        indicatorColor = Error.copy(alpha = 0.1f),
                        unselectedIconColor = GrisNeutral,
                        unselectedTextColor = GrisNeutral
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF8FBFF),
                            Color(0xFFFFFFFF),
                            Color(0xFFFFFBF8)
                        )
                    )
                )
        ) {
            // Burbujas decorativas
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .offset(x = (-50).dp, y = 40.dp)
                    .scale(scale)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                AzulCielo.copy(alpha = 0.15f),
                                AzulCielo.copy(alpha = 0.05f)
                            )
                        ),
                        CircleShape
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.95f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hola, ${userData?.nombre ?: "Profesor"}",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp
                                ),
                                color = FondoOscuroPrimario
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Gestiona tus cursos",
                                style = MaterialTheme.typography.bodyMedium,
                                color = GrisNeutral
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            AzulCielo.copy(alpha = 0.2f),
                                            AzulCielo.copy(alpha = 0.08f)
                                        )
                                    ),
                                    CircleShape
                                )
                                .clickable {
                                    Toast
                                        .makeText(context, "Perfil", Toast.LENGTH_SHORT)
                                        .show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!userData?.fotoPerfilUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = userData?.fotoPerfilUrl,
                                    contentDescription = "Foto perfil",
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Perfil",
                                    tint = AzulCielo,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }

                // Botones de acciones rápidas
                LazyRow(
                    modifier = Modifier.padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        QuickActionButton(
                            icon = Icons.Default.PersonAdd,
                            label = "Agregar Padres",
                            color = VerdeLima,
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "Redirigiendo a Agregar Padres...",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                    item {
                        QuickActionButton(
                            icon = Icons.Default.LibraryBooks,
                            label = "Módulos",
                            color = AzulCielo,
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "Redirigiendo a Módulos...",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                    item {
                        QuickActionButton(
                            icon = Icons.Default.Info,
                            label = "Información",
                            color = Fucsia,
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "Redirigiendo a Información...",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                    item {
                        QuickActionButton(
                            icon = Icons.Default.CalendarToday,
                            label = "Calendario",
                            color = Naranja,
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "Redirigiendo a Calendario...",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                    item {
                        QuickActionButton(
                            icon = Icons.Default.Forum,
                            label = "Foro",
                            color = Color(0xFF9C27B0),
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "Redirigiendo a Foro...",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                    item {
                        QuickActionButton(
                            icon = Icons.Default.Assignment,
                            label = "Tareas",
                            color = Color(0xFF00BCD4),
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "Redirigiendo a Tareas...",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }

                // Título de sección
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mis Cursos",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = FondoOscuroPrimario
                    )

                    Button(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AzulCielo)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Crear Curso", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Lista de cursos
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AzulCielo)
                    }
                } else if (errorMessage != null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage ?: "Error desconocido",
                                color = Error,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else if (cursos.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = GrisNeutral,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No tienes cursos creados",
                                style = MaterialTheme.typography.titleMedium,
                                color = GrisNeutral
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Crea tu primer curso para comenzar",
                                style = MaterialTheme.typography.bodyMedium,
                                color = GrisClaro
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(cursos) { curso ->
                            CursoCard(
                                curso = curso,
                                onEdit = {
                                    selectedCurso = curso
                                    nombreCurso = curso.nombre
                                    descripcionCurso = curso.descripcion ?: ""
                                    showEditDialog = true
                                },
                                onDelete = {
                                    selectedCurso = curso
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo Crear Curso
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                nombreCurso = ""
                descripcionCurso = ""
            },
            title = {
                Text(
                    "Crear Nuevo Curso",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = nombreCurso,
                        onValueChange = { nombreCurso = it },
                        label = { Text("Nombre del curso") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AzulCielo,
                            focusedLabelColor = AzulCielo
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = descripcionCurso,
                        onValueChange = { descripcionCurso = it },
                        label = { Text("Descripción") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AzulCielo,
                            focusedLabelColor = AzulCielo
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nombreCurso.isNotBlank()) {
                            scope.launch {
                                try {
                                    val response = withContext(Dispatchers.IO) {
                                        ApiService.createCurso(
                                            token = token!!,
                                            nombre = nombreCurso,
                                            descripcion = descripcionCurso,
                                            docenteId = userData!!.id,
                                            fotoPortadaFile = null,
                                            archivoCSVFile = null
                                        )
                                    }

                                    if (response.isSuccessful) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                context,
                                                "Curso creado exitosamente",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            showCreateDialog = false
                                            nombreCurso = ""
                                            descripcionCurso = ""

                                            // Recargar cursos
                                            isLoading = true
                                            cargarCursos(token!!, scope, context) { cursosResponse ->
                                                cursos = cursosResponse
                                                isLoading = false
                                            }
                                        }
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                context,
                                                "Error al crear curso",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            "Error: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AzulCielo),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Crear", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateDialog = false
                        nombreCurso = ""
                        descripcionCurso = ""
                    }
                ) {
                    Text("Cancelar", color = GrisNeutral)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }

    // Diálogo Editar Curso
    if (showEditDialog && selectedCurso != null) {
        AlertDialog(
            onDismissRequest = {
                showEditDialog = false
                selectedCurso = null
                nombreCurso = ""
                descripcionCurso = ""
            },
            title = {
                Text(
                    "Editar Curso",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = nombreCurso,
                        onValueChange = { nombreCurso = it },
                        label = { Text("Nombre del curso") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VerdeLima,
                            focusedLabelColor = VerdeLima
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = descripcionCurso,
                        onValueChange = { descripcionCurso = it },
                        label = { Text("Descripción") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VerdeLima,
                            focusedLabelColor = VerdeLima
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nombreCurso.isNotBlank()) {
                            scope.launch {
                                try {
                                    val response = withContext(Dispatchers.IO) {
                                        ApiService.updateCurso(
                                            token = token!!,
                                            cursoId = selectedCurso!!.id,
                                            nombre = nombreCurso,
                                            descripcion = descripcionCurso,
                                            fotoPortadaFile = null
                                        )
                                    }

                                    if (response.isSuccessful) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                context,
                                                "Curso actualizado exitosamente",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            showEditDialog = false
                                            selectedCurso = null
                                            nombreCurso = ""
                                            descripcionCurso = ""

                                            // Recargar cursos
                                            isLoading = true
                                            cargarCursos(token!!, scope, context) { cursosResponse ->
                                                cursos = cursosResponse
                                                isLoading = false
                                            }
                                        }
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(
                                                context,
                                                "Error al actualizar curso",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            "Error: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VerdeLima),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Actualizar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEditDialog = false
                        selectedCurso = null
                        nombreCurso = ""
                        descripcionCurso = ""
                    }
                ) {
                    Text("Cancelar", color = GrisNeutral)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }

    // Diálogo Eliminar Curso
    if (showDeleteDialog && selectedCurso != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                selectedCurso = null
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Error,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "Eliminar Curso",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Text(
                    "¿Estás seguro de que deseas eliminar el curso '${selectedCurso?.nombre}'? Esta acción no se puede deshacer.",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val response = withContext(Dispatchers.IO) {
                                    ApiService.deleteCurso(
                                        token = token!!,
                                        cursoId = selectedCurso!!.id
                                    )
                                }

                                if (response.isSuccessful) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            "Curso eliminado exitosamente",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        showDeleteDialog = false
                                        selectedCurso = null

                                        // Recargar cursos
                                        isLoading = true
                                        cargarCursos(token!!, scope, context) { cursosResponse ->
                                            cursos = cursosResponse
                                            isLoading = false
                                        }
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            "Error al eliminar curso",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        "Error: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Eliminar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        selectedCurso = null
                    }
                ) {
                    Text("Cancelar", color = GrisNeutral)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }
}

// Función auxiliar para cargar cursos
private fun cargarCursos(
    token: String,
    scope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context,
    onResult: (List<CursoItem>) -> Unit
) {
    scope.launch {
        try {
            val response = withContext(Dispatchers.IO) {
                ApiService.getCursos(
                    token = token,
                    page = 1,
                    limit = 50
                )
            }

            if (response.isSuccessful) {
                val body = response.body()
                val cursosArray = body?.getAsJsonArray("cursos")

                val cursosList = cursosArray?.map { cursoElement ->
                    val cursoJson = cursoElement.asJsonObject

                    val docenteJson = cursoJson.getAsJsonObject("docenteId")
                    val docenteNombre = if (docenteJson != null) {
                        "${docenteJson.get("nombre")?.asString ?: ""} ${docenteJson.get("apellido")?.asString ?: ""}"
                    } else {
                        "Sin docente"
                    }

                    val participantesArray = cursoJson.getAsJsonArray("participantes")
                    val totalParticipantes = participantesArray?.size() ?: 0

                    CursoItem(
                        id = cursoJson.get("_id")?.asString ?: "",
                        nombre = cursoJson.get("nombre")?.asString ?: "Sin nombre",
                        descripcion = cursoJson.get("descripcion")?.takeIf { !it.isJsonNull }?.asString,
                        fotoPortadaUrl = cursoJson.get("fotoPortadaUrl")?.takeIf { !it.isJsonNull }?.asString,
                        estado = cursoJson.get("estado")?.asString ?: "activo",
                        totalParticipantes = totalParticipantes,
                        docenteNombre = docenteNombre
                    )
                } ?: emptyList()

                withContext(Dispatchers.Main) {
                    onResult(cursosList)
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Error al cargar cursos: ${response.code()}",
                        Toast.LENGTH_SHORT
                    ).show()
                    onResult(emptyList())
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                onResult(emptyList())
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(110.dp)
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                color.copy(alpha = 0.2f),
                                color.copy(alpha = 0.08f)
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = FondoOscuroPrimario,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CursoCard(
    curso: CursoItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = AzulCielo.copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = curso.nombre,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = FondoOscuroPrimario,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!curso.descripcion.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = curso.descripcion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = GrisNeutral,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                VerdeLima.copy(alpha = 0.1f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = VerdeLima,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                Error.copy(alpha = 0.1f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = Error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = AzulCielo,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${curso.totalParticipantes}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = GrisOscuro
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                if (curso.estado == "activo") VerdeLima.copy(alpha = 0.15f)
                                else GrisClaro.copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = curso.estado.uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (curso.estado == "activo") VerdeLima else GrisNeutral
                        )
                    }
                }
            }
        }
    }
}