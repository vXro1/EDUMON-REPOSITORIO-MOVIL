package com.example.edumonjetcompose

import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.edumonjetcompose.network.ApiService
import com.example.edumonjetcompose.ui.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearForoScreen(
    navController: NavController,
    cursoId: String,
    token: String,
    userId: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var selectedFiles by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isCreating by remember { mutableStateOf(false) }

    // ✅ Log de credenciales recibidas
    LaunchedEffect(Unit) {
        Log.d("CrearForo", "📋 Credenciales recibidas:")
        Log.d("CrearForo", "  - Token: ${if (token.isEmpty()) "VACÍO" else "OK (${token.length} chars)"}")
        Log.d("CrearForo", "  - UserId: ${if (userId.isEmpty()) "VACÍO" else userId}")
        Log.d("CrearForo", "  - CursoId: $cursoId")
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        selectedFiles = uris
        Log.d("CrearForo", "📎 ${uris.size} archivo(s) seleccionado(s)")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear Nuevo Foro", fontWeight = FontWeight.Bold) },
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

            // Header informativo
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
                                Icons.Default.Info,
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
                                "Nuevo espacio de discusión",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AzulCielo
                            )
                            Text(
                                "Crea un foro para que los participantes del curso puedan comunicarse",
                                style = MaterialTheme.typography.bodySmall,
                                color = GrisMedio
                            )
                        }
                    }
                }
            }

            // Título del foro
            item {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título del foro") },
                    placeholder = { Text("Ej: Dudas sobre el módulo 1") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Forum, null, tint = Fucsia)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Fucsia,
                        focusedLabelColor = Fucsia,
                        cursorColor = Fucsia
                    )
                )
            }

            // Descripción del foro
            item {
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    placeholder = { Text("Describe el propósito de este foro...") },
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

            // Archivos de referencia (opcional)
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Archivos de referencia (opcional)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GrisOscuro
                    )
                    Text(
                        "Puedes adjuntar materiales de apoyo para la discusión",
                        style = MaterialTheme.typography.bodySmall,
                        color = GrisMedio
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { filePickerLauncher.launch("*/*") },
                        shape = RoundedCornerShape(12.dp),
                        color = Fucsia.copy(alpha = 0.1f),
                        border = BorderStroke(2.dp, Fucsia.copy(alpha = 0.5f))
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
                                tint = Fucsia
                            )
                            Text(
                                "Toca para seleccionar archivos",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = Fucsia
                            )
                            Text(
                                "${selectedFiles.size} archivo(s) seleccionado(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = GrisMedio
                            )
                        }
                    }
                }
            }

            // Lista de archivos seleccionados
            if (selectedFiles.isNotEmpty()) {
                items(selectedFiles) { uri ->
                    FileItemCardForo(
                        uri = uri,
                        onRemove = {
                            selectedFiles = selectedFiles.filter { it != uri }
                            Log.d("CrearForo", "🗑️ Archivo eliminado. Total: ${selectedFiles.size}")
                        }
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
                            Log.d("CrearForo", "🔵 Botón Crear presionado")
                            Log.d("CrearForo", "📝 Datos - Título: '$titulo', Descripción: '${descripcion.take(50)}...'")

                            // Validaciones
                            if (titulo.isBlank()) {
                                Log.w("CrearForo", "❌ Validación: título vacío")
                                scope.launch {
                                    snackbarHostState.showSnackbar("El título es obligatorio")
                                }
                                return@Button
                            }
                            if (titulo.length < 5) {
                                Log.w("CrearForo", "❌ Validación: título muy corto (${titulo.length} chars)")
                                scope.launch {
                                    snackbarHostState.showSnackbar("El título debe tener al menos 5 caracteres")
                                }
                                return@Button
                            }
                            if (descripcion.isBlank()) {
                                Log.w("CrearForo", "❌ Validación: descripción vacía")
                                scope.launch {
                                    snackbarHostState.showSnackbar("La descripción es obligatoria")
                                }
                                return@Button
                            }
                            if (descripcion.length < 10) {
                                Log.w("CrearForo", "❌ Validación: descripción muy corta (${descripcion.length} chars)")
                                scope.launch {
                                    snackbarHostState.showSnackbar("La descripción debe tener al menos 10 caracteres")
                                }
                                return@Button
                            }

                            Log.d("CrearForo", "✅ Validaciones OK, iniciando creación...")
                            Log.d("CrearForo", "📊 Parámetros:")
                            Log.d("CrearForo", "  - CursoId: $cursoId")
                            Log.d("CrearForo", "  - UserId (DocenteId): $userId")
                            Log.d("CrearForo", "  - Token: ${token.take(20)}...")
                            Log.d("CrearForo", "  - Archivos: ${selectedFiles.size}")

                            scope.launch {
                                isCreating = true
                                try {
                                    // Preparar archivos si existen
                                    val archivosParts: List<MultipartBody.Part>? = if (selectedFiles.isNotEmpty()) {
                                        Log.d("CrearForo", "📎 Procesando ${selectedFiles.size} archivo(s)...")
                                        selectedFiles.mapNotNull { uri ->
                                            try {
                                                val inputStream = context.contentResolver.openInputStream(uri)
                                                val fileName = getFileNameFromUri(context, uri)
                                                val file = File(context.cacheDir, fileName)
                                                file.outputStream().use { inputStream?.copyTo(it) }

                                                val mimeType = context.contentResolver.getType(uri) ?: "*/*"
                                                val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                                                Log.d("CrearForo", "  ✓ Archivo: $fileName ($mimeType, ${file.length()} bytes)")
                                                MultipartBody.Part.createFormData("archivos", fileName, requestFile)
                                            } catch (e: Exception) {
                                                Log.e("CrearForo", "  ✗ Error procesando archivo: ${e.message}", e)
                                                null
                                            }
                                        }
                                    } else {
                                        Log.d("CrearForo", "📎 Sin archivos adjuntos")
                                        null
                                    }

                                    Log.d("CrearForo", "📤 Enviando petición al servidor...")

                                    // Llamar a la API con userId como docenteId
                                    val response = ApiService.createForo(
                                        token = token,
                                        cursoId = cursoId,
                                        docenteId = userId,  // Usar userId del parámetro
                                        titulo = titulo,
                                        descripcion = descripcion,
                                        archivos = archivosParts
                                    )

                                    Log.d("CrearForo", "📥 Respuesta recibida: ${response.code()}")

                                    if (response.isSuccessful) {
                                        val responseBody = response.body()
                                        Log.d("CrearForo", "✅ Foro creado exitosamente")
                                        Log.d("CrearForo", "📄 Respuesta: $responseBody")
                                        snackbarHostState.showSnackbar("Foro creado exitosamente")
                                        navController.popBackStack()
                                    } else {
                                        val errorBody = response.errorBody()?.string()
                                        Log.e("CrearForo", "❌ Error del servidor:")
                                        Log.e("CrearForo", "  - Código: ${response.code()}")
                                        Log.e("CrearForo", "  - Mensaje: ${response.message()}")
                                        Log.e("CrearForo", "  - Body: $errorBody")
                                        snackbarHostState.showSnackbar(
                                            errorBody?.let { "Error: $it" } ?: "Error al crear el foro (${response.code()})"
                                        )
                                    }
                                } catch (e: Exception) {
                                    Log.e("CrearForo", "❌ Excepción al crear foro", e)
                                    e.printStackTrace()
                                    snackbarHostState.showSnackbar("Error: ${e.message ?: "Error desconocido"}")
                                } finally {
                                    isCreating = false
                                    Log.d("CrearForo", "🏁 Proceso finalizado")
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
                                "Crear Foro",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            Log.d("CrearForo", "❌ Creación cancelada por el usuario")
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

// ==================== FUNCIONES AUXILIARES ====================

/**
 * Obtener nombre del archivo desde Uri
 */
fun getFileNameFromUri(context: android.content.Context, uri: Uri): String {
    var result = "archivo_${System.currentTimeMillis()}"

    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0) {
                result = cursor.getString(nameIndex)
            }
        }
    }

    return result
}

// ==================== COMPONENTES ====================

@Composable
fun FileItemCardForo(
    uri: Uri,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val fileName = remember { getFileNameFromUri(context, uri) }

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
                Icon(
                    Icons.Default.Close,
                    "Eliminar",
                    tint = ErrorOscuro
                )
            }
        }
    }
}