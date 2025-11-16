package com.example.edumonjetcompose.ui.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import com.example.edumonjetcompose.network.ApiService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HacerEntregaScreen(
    navController: NavController,
    tareaId: String,
    padreId: String,
    token: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var textoRespuesta by remember { mutableStateOf("") }
    var archivosSeleccionados by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var tituloTarea by remember { mutableStateOf("Tarea") }

    // Cargar información de la tarea
    LaunchedEffect(tareaId) {
        try {
            val response = ApiService.getTareaById(token, tareaId)
            if (response.isSuccessful) {
                val body = response.body()
                val tareaJson = body?.getAsJsonObject("tarea") ?: body
                tituloTarea = tareaJson?.get("titulo")?.asString ?: "Tarea"
            }
        } catch (e: Exception) {
            Log.e("HacerEntrega", "Error cargando tarea", e)
        }
    }

    // Launcher para seleccionar archivos
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        archivosSeleccionados = uris
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Nueva Entrega",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            tituloTarea,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = {
            if (showError) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { showError = false }) {
                            Text("OK")
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ) {
                    Text(errorMessage)
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sección de texto de respuesta
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Respuesta",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            OutlinedTextField(
                                value = textoRespuesta,
                                onValueChange = { textoRespuesta = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 150.dp),
                                placeholder = {
                                    Text("Escribe tu respuesta aquí...")
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                // Sección de archivos
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.AttachFile,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Archivos Adjuntos",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (archivosSeleccionados.isNotEmpty()) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text("${archivosSeleccionados.size}")
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Botón para agregar archivos
                            Button(
                                onClick = { filePickerLauncher.launch("*/*") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, "Agregar")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Seleccionar Archivos")
                            }
                        }
                    }
                }

                // Lista de archivos seleccionados
                items(archivosSeleccionados) { uri ->
                    ArchivoItem(
                        uri = uri,
                        context = context,
                        onRemove = {
                            archivosSeleccionados = archivosSeleccionados.filter { it != uri }
                        }
                    )
                }

                // Botones de acción
                item {
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Guardar como borrador
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    guardarEntrega(
                                        context = context,
                                        token = token,
                                        tareaId = tareaId,
                                        padreId = padreId,
                                        textoRespuesta = textoRespuesta,
                                        archivos = archivosSeleccionados,
                                        estado = "borrador",
                                        onLoading = { isLoading = it },
                                        onSuccess = {
                                            navController.popBackStack()
                                        },
                                        onError = { error ->
                                            errorMessage = error
                                            showError = true
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading && (textoRespuesta.isNotBlank() || archivosSeleccionados.isNotEmpty()),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Save, "Borrador")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Borrador")
                        }

                        // Enviar
                        Button(
                            onClick = {
                                scope.launch {
                                    guardarEntrega(
                                        context = context,
                                        token = token,
                                        tareaId = tareaId,
                                        padreId = padreId,
                                        textoRespuesta = textoRespuesta,
                                        archivos = archivosSeleccionados,
                                        estado = "enviada",
                                        onLoading = { isLoading = it },
                                        onSuccess = {
                                            navController.popBackStack()
                                        },
                                        onError = { error ->
                                            errorMessage = error
                                            showError = true
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading && (textoRespuesta.isNotBlank() || archivosSeleccionados.isNotEmpty()),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Send, "Enviar")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enviar")
                        }
                    }
                }

                // Espacio inferior
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // Loading overlay
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Subiendo archivos...",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArchivoItem(
    uri: Uri,
    context: android.content.Context,
    onRemove: () -> Unit
) {
    val fileName = remember(uri) {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) cursor.getString(nameIndex) else "Archivo"
            } else "Archivo"
        } ?: "Archivo"
    }

    val fileSize = remember(uri) {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (sizeIndex >= 0) {
                    val bytes = cursor.getLong(sizeIndex)
                    formatFileSize(bytes)
                } else ""
            } else ""
        } ?: ""
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono del archivo
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.InsertDriveFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Información del archivo
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    fileName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1
                )
                if (fileSize.isNotEmpty()) {
                    Text(
                        fileSize,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            // Botón eliminar
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Eliminar",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024f * 1024f))} MB"
        else -> "${"%.1f".format(bytes / (1024f * 1024f * 1024f))} GB"
    }
}

private suspend fun guardarEntrega(
    context: android.content.Context,
    token: String,
    tareaId: String,
    padreId: String,
    textoRespuesta: String,
    archivos: List<Uri>,
    estado: String,
    onLoading: (Boolean) -> Unit,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    try {
        onLoading(true)

        Log.d("HacerEntrega", """
            📤 Guardando entrega:
            - Estado: $estado
            - TareaId: $tareaId
            - PadreId: $padreId
            - Texto: ${textoRespuesta.isNotBlank()}
            - Archivos: ${archivos.size}
        """.trimIndent())

        val response = ApiService.crearEntregaConArchivos(
            token = token,
            tareaId = tareaId,
            padreId = padreId,
            textoRespuesta = textoRespuesta.takeIf { it.isNotBlank() },
            archivos = archivos,
            estado = estado,
            context = context
        )

        if (response.isSuccessful) {
            Log.d("HacerEntrega", "✅ Entrega guardada exitosamente")
            onSuccess()
        } else {
            val errorBody = response.errorBody()?.string()
            val errorMsg = errorBody ?: "Error al guardar la entrega (${response.code()})"
            Log.e("HacerEntrega", "❌ Error en respuesta: $errorMsg")
            onError(errorMsg)
        }
    } catch (e: Exception) {
        Log.e("HacerEntrega", "❌ Excepción al guardar", e)
        onError(e.message ?: "Error desconocido")
    } finally {
        onLoading(false)
    }
}