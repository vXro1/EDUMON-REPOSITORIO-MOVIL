package com.example.edumonjetcompose

import android.util.Log
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.edumonjetcompose.models.Modulo
import com.example.edumonjetcompose.network.ApiService
import com.example.edumonjetcompose.ui.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListarModulosScreen(
    navController: NavController,
    cursoId: String,
    token: String
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var modulos by remember { mutableStateOf<List<Modulo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var cursoNombre by remember { mutableStateOf<String?>(null) }

    // Estado para diálogos
    var showDeleteDialog by remember { mutableStateOf(false) }
    var moduloToDelete by remember { mutableStateOf<Modulo?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var moduloToEdit by remember { mutableStateOf<Modulo?>(null) }

    // Función para cargar módulos
    fun cargarModulos() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                Log.d("ListarModulos", "📖 Cargando módulos del curso: $cursoId")

                // Cargar información del curso
                val cursoResponse = ApiService.getCursoById(token, cursoId)
                if (cursoResponse.isSuccessful) {
                    val cursoJson = cursoResponse.body()?.getAsJsonObject("curso")
                    cursoNombre = cursoJson?.get("nombre")?.asString
                }

                // Cargar módulos
                val response = ApiService.getModulosByCurso(token, cursoId)

                if (response.isSuccessful) {
                    val jsonResponse = response.body()
                    val modulosArray = jsonResponse?.getAsJsonArray("modulos")

                    modulos = modulosArray?.map { element ->
                        val moduloJson = element.asJsonObject
                        Modulo(
                            id = moduloJson.get("_id")?.asString ?: "",
                            nombre = moduloJson.get("titulo")?.asString ?: "",
                            descripcion = moduloJson.get("descripcion")?.asString ?: "",
                            cursoId = moduloJson.get("cursoId")?.asString ?: cursoId
                        )
                    } ?: emptyList()

                    Log.d("ListarModulos", "✅ ${modulos.size} módulo(s) cargado(s)")
                } else {
                    val errorBody = response.errorBody()?.string()
                    errorMessage = errorBody ?: "Error al cargar módulos"
                    Log.e("ListarModulos", "❌ Error: ${response.code()} - $errorBody")
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Error desconocido"
                Log.e("ListarModulos", "❌ Excepción al cargar módulos", e)
            } finally {
                isLoading = false
            }
        }
    }

    // Cargar módulos al iniciar
    LaunchedEffect(cursoId) {
        cargarModulos()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Módulos del Curso", fontWeight = FontWeight.Bold)
                        cursoNombre?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = GrisMedio
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
                    IconButton(onClick = { cargarModulos() }) {
                        Icon(Icons.Default.Refresh, "Actualizar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FondoCard,
                    titleContentColor = GrisOscuro
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("crearModulo/$cursoId") },
                containerColor = Fucsia,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, "Crear módulo")
                Spacer(Modifier.width(8.dp))
                Text("Nuevo Módulo")
            }
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
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = Fucsia)
                            Text("Cargando módulos...", color = GrisMedio)
                        }
                    }
                }

                errorMessage != null -> {
                    ModulosErrorView(
                        message = errorMessage!!,
                        onRetry = { cargarModulos() }
                    )
                }

                modulos.isEmpty() -> {
                    EmptyModulosView(
                        onCrearModulo = { navController.navigate("crearModulo/$cursoId") }
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header con estadísticas
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
                                            Icons.Default.LibraryBooks,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .padding(12.dp)
                                                .size(24.dp),
                                            tint = AzulCielo
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "${modulos.size} módulo(s)",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = AzulCielo
                                        )
                                        Text(
                                            "Organiza tu contenido educativo",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = GrisMedio
                                        )
                                    }
                                }
                            }
                        }

                        // Lista de módulos
                        items(modulos) { modulo ->
                            ModuloCardItem(
                                modulo = modulo,
                                onEdit = {
                                    moduloToEdit = modulo
                                    showEditDialog = true
                                },
                                onDelete = {
                                    moduloToDelete = modulo
                                    showDeleteDialog = true
                                },
                                onClick = {
                                    // Navegar a detalles del módulo si tienes esa pantalla
                                    Log.d("ListarModulos", "Click en módulo: ${modulo.nombre}")
                                }
                            )
                        }

                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }

    // Diálogo de eliminación
    if (showDeleteDialog && moduloToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = ErrorOscuro,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "Eliminar módulo",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("¿Estás seguro de que deseas eliminar este módulo?")
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ErrorClaro.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, ErrorClaro)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                moduloToDelete!!.nombre,
                                fontWeight = FontWeight.Bold,
                                color = ErrorOscuro
                            )
                            if (moduloToDelete!!.descripcion.isNotEmpty()) {
                                Text(
                                    moduloToDelete!!.descripcion,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GrisMedio
                                )
                            }
                        }
                    }
                    Text(
                        "Esta acción no se puede deshacer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GrisMedio,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                Log.d("ListarModulos", "🗑️ Eliminando módulo: ${moduloToDelete!!.id}")
                                val response = ApiService.deleteModulo(token, moduloToDelete!!.id)

                                if (response.isSuccessful) {
                                    Log.d("ListarModulos", "✅ Módulo eliminado")
                                    snackbarHostState.showSnackbar("Módulo eliminado exitosamente")
                                    cargarModulos()
                                } else {
                                    val errorBody = response.errorBody()?.string()
                                    Log.e("ListarModulos", "❌ Error: $errorBody")
                                    snackbarHostState.showSnackbar(
                                        errorBody ?: "Error al eliminar módulo"
                                    )
                                }
                            } catch (e: Exception) {
                                Log.e("ListarModulos", "❌ Excepción", e)
                                snackbarHostState.showSnackbar("Error: ${e.message}")
                            }
                        }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorOscuro
                    )
                ) {
                    Icon(Icons.Default.Delete, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Eliminar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de edición
    if (showEditDialog && moduloToEdit != null) {
        EditarModuloDialog(
            modulo = moduloToEdit!!,
            onDismiss = { showEditDialog = false },
            onConfirm = { titulo, descripcion ->
                scope.launch {
                    try {
                        Log.d("ListarModulos", "✏️ Editando módulo: ${moduloToEdit!!.id}")
                        val response = ApiService.updateModulo(
                            token = token,
                            moduloId = moduloToEdit!!.id,
                            titulo = titulo,
                            descripcion = descripcion.ifBlank { null },
                            orden = null
                        )

                        if (response.isSuccessful) {
                            Log.d("ListarModulos", "✅ Módulo actualizado")
                            snackbarHostState.showSnackbar("Módulo actualizado exitosamente")
                            cargarModulos()
                            showEditDialog = false
                        } else {
                            val errorBody = response.errorBody()?.string()
                            Log.e("ListarModulos", "❌ Error: $errorBody")
                            snackbarHostState.showSnackbar(
                                errorBody ?: "Error al actualizar módulo"
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("ListarModulos", "❌ Excepción", e)
                        snackbarHostState.showSnackbar("Error: ${e.message}")
                    }
                }
            }
        )
    }
}

// ==================== COMPONENTES ====================

@Composable
fun ModuloCardItem(
    modulo: Modulo,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = FondoCard,
        border = BorderStroke(1.dp, GrisClaro),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header con nombre y acciones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Fucsia.copy(alpha = 0.1f)
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp),
                            tint = Fucsia
                        )
                    }
                    Text(
                        modulo.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GrisOscuro
                    )
                }

                // Botones de acción
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = AzulCielo,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = ErrorOscuro,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Descripción
            if (modulo.descripcion.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    modulo.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GrisMedio,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun EditarModuloDialog(
    modulo: Modulo,
    onDismiss: () -> Unit,
    onConfirm: (titulo: String, descripcion: String) -> Unit
) {
    var titulo by remember { mutableStateOf(modulo.nombre) }
    var descripcion by remember { mutableStateOf(modulo.descripcion) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Edit,
                contentDescription = null,
                tint = AzulCielo,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text("Editar módulo", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Fucsia,
                        focusedLabelColor = Fucsia,
                        cursorColor = Fucsia
                    )
                )

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción (opcional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Fucsia,
                        focusedLabelColor = Fucsia,
                        cursorColor = Fucsia
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (titulo.isNotBlank() && titulo.length >= 3) {
                        onConfirm(titulo, descripcion)
                    }
                },
                enabled = titulo.isNotBlank() && titulo.length >= 3,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Fucsia
                )
            ) {
                Icon(Icons.Default.Check, null)
                Spacer(Modifier.width(8.dp))
                Text("Guardar")
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
fun EmptyModulosView(onCrearModulo: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Fucsia.copy(alpha = 0.1f),
                modifier = Modifier.size(120.dp)
            ) {
                Icon(
                    Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(32.dp)
                        .fillMaxSize(),
                    tint = Fucsia
                )
            }

            Text(
                "No hay módulos",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = GrisOscuro
            )

            Text(
                "Crea el primer módulo para organizar el contenido de tu curso",
                style = MaterialTheme.typography.bodyMedium,
                color = GrisMedio,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onCrearModulo,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Fucsia
                ),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Crear Módulo")
            }
        }
    }
}

@Composable
fun ModulosErrorView(message: String, onRetry: () -> Unit) {
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
                Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = ErrorOscuro
            )

            Text(
                "Error al cargar módulos",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = GrisOscuro
            )

            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = GrisMedio,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AzulCielo
                )
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Reintentar")
            }
        }
    }
}