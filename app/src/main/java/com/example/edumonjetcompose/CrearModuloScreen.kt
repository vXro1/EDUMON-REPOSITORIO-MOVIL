package com.example.edumonjetcompose

import android.util.Log
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.edumonjetcompose.network.ApiService
import com.example.edumonjetcompose.ui.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearModuloScreen(
    navController: NavController,
    cursoId: String,
    token: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var orden by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }

    // Estado para la información del curso
    var cursoNombre by remember { mutableStateOf<String?>(null) }
    var docenteNombre by remember { mutableStateOf<String?>(null) }
    var isLoadingCurso by remember { mutableStateOf(true) }

    // ✅ Log de credenciales recibidas y cargar curso
    LaunchedEffect(Unit) {
        Log.d("CrearModulo", "📋 Credenciales recibidas:")
        Log.d("CrearModulo", "  - Token: ${if (token.isEmpty()) "VACÍO" else "OK (${token.length} chars)"}")
        Log.d("CrearModulo", "  - CursoId: $cursoId")

        // Cargar información del curso
        try {
            Log.d("CrearModulo", "📖 Cargando información del curso...")
            val response = ApiService.getCursoById(token, cursoId)

            if (response.isSuccessful) {
                val jsonResponse = response.body()
                val cursoJson = jsonResponse?.getAsJsonObject("curso")

                cursoJson?.let { curso ->
                    cursoNombre = curso.get("nombre")?.asString

                    val docente = curso.getAsJsonObject("docenteId")
                    if (docente != null) {
                        val nombre = docente.get("nombre")?.asString ?: ""
                        val apellido = docente.get("apellido")?.asString ?: ""
                        docenteNombre = "$nombre $apellido"
                    }

                    Log.d("CrearModulo", "✅ Curso cargado: $cursoNombre")
                    Log.d("CrearModulo", "   Docente: $docenteNombre")
                }
            } else {
                Log.e("CrearModulo", "❌ Error al cargar curso: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("CrearModulo", "❌ Excepción al cargar curso", e)
        } finally {
            isLoadingCurso = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear Nuevo Módulo", fontWeight = FontWeight.Bold) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            // Información del curso
            if (isLoadingCurso) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = AzulCielo
                        )
                    }
                }
            } else if (cursoNombre != null) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = AzulCielo.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, AzulCielo.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = AzulCielo.copy(alpha = 0.2f)
                            ) {
                                Icon(
                                    Icons.Default.School,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .size(24.dp),
                                    tint = AzulCielo
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    "Curso",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = GrisMedio
                                )
                                Text(
                                    cursoNombre!!,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AzulCielo
                                )
                                if (docenteNombre != null) {
                                    Text(
                                        "Docente: $docenteNombre",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = GrisMedio
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Header informativo
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Fucsia.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, Fucsia.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Fucsia.copy(alpha = 0.2f)
                        ) {
                            Icon(
                                Icons.Default.LibraryBooks,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(12.dp)
                                    .size(24.dp),
                                tint = Fucsia
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "Nuevo módulo de aprendizaje",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Fucsia
                            )
                            Text(
                                "Organiza el contenido del curso en módulos temáticos",
                                style = MaterialTheme.typography.bodySmall,
                                color = GrisMedio
                            )
                        }
                    }
                }
            }

            // Título del módulo
            item {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título del módulo") },
                    placeholder = { Text("Ej: Introducción a las bases de datos") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Title, null, tint = Fucsia)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Fucsia,
                        focusedLabelColor = Fucsia,
                        cursorColor = Fucsia
                    )
                )
            }

            // Descripción del módulo
            item {
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción (opcional)") },
                    placeholder = { Text("Describe los temas que se cubrirán en este módulo...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    maxLines = 8,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = Fucsia,
                            modifier = Modifier
                                .padding(top = 8.dp, start = 4.dp)
                                .size(24.dp)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Fucsia,
                        focusedLabelColor = Fucsia,
                        cursorColor = Fucsia
                    )
                )
            }

            // Orden del módulo
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Orden (opcional)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GrisOscuro
                    )
                    Text(
                        "Define la posición de este módulo en el curso",
                        style = MaterialTheme.typography.bodySmall,
                        color = GrisMedio
                    )

                    OutlinedTextField(
                        value = orden,
                        onValueChange = {
                            // Solo permitir números
                            if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                orden = it
                            }
                        },
                        label = { Text("Número de orden") },
                        placeholder = { Text("1, 2, 3...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Numbers, null, tint = AzulCielo)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AzulCielo,
                            focusedLabelColor = AzulCielo,
                            cursorColor = AzulCielo
                        )
                    )
                }
            }

            // Botones de acción
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            Log.d("CrearModulo", "🔵 Botón Crear presionado")
                            Log.d("CrearModulo", "📝 Datos - Título: '$titulo', Descripción: '${descripcion.take(50)}...'")

                            // Validaciones
                            if (titulo.isBlank()) {
                                Log.w("CrearModulo", "❌ Validación: título vacío")
                                scope.launch {
                                    snackbarHostState.showSnackbar("El título es obligatorio")
                                }
                                return@Button
                            }
                            if (titulo.length < 3) {
                                Log.w("CrearModulo", "❌ Validación: título muy corto (${titulo.length} chars)")
                                scope.launch {
                                    snackbarHostState.showSnackbar("El título debe tener al menos 3 caracteres")
                                }
                                return@Button
                            }
                            if (titulo.length > 200) {
                                Log.w("CrearModulo", "❌ Validación: título muy largo (${titulo.length} chars)")
                                scope.launch {
                                    snackbarHostState.showSnackbar("El título no puede exceder 200 caracteres")
                                }
                                return@Button
                            }
                            if (descripcion.length > 1000) {
                                Log.w("CrearModulo", "❌ Validación: descripción muy larga (${descripcion.length} chars)")
                                scope.launch {
                                    snackbarHostState.showSnackbar("La descripción no puede exceder 1000 caracteres")
                                }
                                return@Button
                            }

                            Log.d("CrearModulo", "✅ Validaciones OK, iniciando creación...")
                            Log.d("CrearModulo", "📊 Parámetros:")
                            Log.d("CrearModulo", "  - CursoId: $cursoId")
                            Log.d("CrearModulo", "  - Token: ${token.take(20)}...")
                            Log.d("CrearModulo", "  - Orden: ${orden.ifEmpty { "null" }}")

                            scope.launch {
                                isCreating = true
                                try {
                                    Log.d("CrearModulo", "📤 Enviando petición al servidor...")

                                    // Llamar a la API
                                    val response = ApiService.createModulo(
                                        token = token,
                                        cursoId = cursoId,
                                        nombre = titulo,
                                        descripcion = descripcion.ifBlank { null },
                                        orden = orden.toIntOrNull()
                                    )

                                    Log.d("CrearModulo", "📥 Respuesta recibida: ${response.code()}")

                                    if (response.isSuccessful) {
                                        val responseBody = response.body()
                                        Log.d("CrearModulo", "✅ Módulo creado exitosamente")
                                        Log.d("CrearModulo", "📄 Respuesta: $responseBody")
                                        snackbarHostState.showSnackbar("Módulo creado exitosamente")
                                        navController.popBackStack()
                                    } else {
                                        val errorBody = response.errorBody()?.string()
                                        Log.e("CrearModulo", "❌ Error del servidor:")
                                        Log.e("CrearModulo", "  - Código: ${response.code()}")
                                        Log.e("CrearModulo", "  - Mensaje: ${response.message()}")
                                        Log.e("CrearModulo", "  - Body: $errorBody")
                                        snackbarHostState.showSnackbar(
                                            errorBody?.let { "Error: $it" } ?: "Error al crear el módulo (${response.code()})"
                                        )
                                    }
                                } catch (e: Exception) {
                                    Log.e("CrearModulo", "❌ Excepción al crear módulo", e)
                                    e.printStackTrace()
                                    snackbarHostState.showSnackbar("Error: ${e.message ?: "Error desconocido"}")
                                } finally {
                                    isCreating = false
                                    Log.d("CrearModulo", "🏁 Proceso finalizado")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isCreating,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Fucsia
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
                                "Crear Módulo",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            Log.d("CrearModulo", "❌ Creación cancelada por el usuario")
                            navController.popBackStack()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
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
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}