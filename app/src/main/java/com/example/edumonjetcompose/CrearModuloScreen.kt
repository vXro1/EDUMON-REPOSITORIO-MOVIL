package com.example.edumonjetcompose.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.edumonjetcompose.network.ApiService
import com.example.edumonjetcompose.ui.*
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearModuloScreen(
    navController: NavController,
    cursoId: String,
    token: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Crear Módulo",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AzulCielo
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(FondoClaro, Color.White)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Mensaje de error si existe
                errorMessage?.let { message ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = ErrorClaro.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = message,
                            color = ErrorOscuro,
                            modifier = Modifier.padding(16.dp),
                            fontSize = 14.sp
                        )
                    }
                }

                // Campo: Título
                OutlinedTextField(
                    value = titulo,
                    onValueChange = {
                        titulo = it
                        errorMessage = null
                    },
                    label = { Text("Título del módulo *") },
                    placeholder = { Text("Ej: Módulo 1 - Introducción") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AzulCielo,
                        focusedLabelColor = AzulCielo,
                        cursorColor = AzulCielo
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Campo: Descripción
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = {
                        descripcion = it
                        errorMessage = null
                    },
                    label = { Text("Descripción") },
                    placeholder = { Text("Describe el contenido del módulo...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    enabled = !isLoading,
                    maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AzulCielo,
                        focusedLabelColor = AzulCielo,
                        cursorColor = AzulCielo
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Botón Crear
                Button(
                    onClick = {
                        Log.d("CrearModulo", "🔵 Botón Crear presionado")
                        Log.d("CrearModulo", "📝 Datos - Título: '$titulo', Descripción: '${descripcion.take(20)}...'")

                        when {
                            titulo.isBlank() -> {
                                Log.w("CrearModulo", "⚠️ Validación fallida: título vacío")
                                errorMessage = "El título es obligatorio"
                            }
                            titulo.length < 3 -> {
                                Log.w("CrearModulo", "⚠️ Validación fallida: título muy corto")
                                errorMessage = "El título debe tener al menos 3 caracteres"
                            }
                            titulo.length > 200 -> {
                                Log.w("CrearModulo", "⚠️ Validación fallida: título muy largo")
                                errorMessage = "El título no puede exceder 200 caracteres"
                            }
                            descripcion.length > 1000 -> {
                                Log.w("CrearModulo", "⚠️ Validación fallida: descripción muy larga")
                                errorMessage = "La descripción no puede exceder 1000 caracteres"
                            }
                            else -> {
                                Log.d("CrearModulo", "✅ Validaciones OK, iniciando creación...")
                                errorMessage = null
                                isLoading = true

                                scope.launch {
                                    try {
                                        Log.d("CrearModulo", "📊 Parámetros:")
                                        Log.d("CrearModulo", "  - CursoId: $cursoId")
                                        Log.d("CrearModulo", "  - Token: ${token.take(20)}...")
                                        Log.d("CrearModulo", "  - Orden: null")

                                        Log.d("CrearModulo", "📤 Enviando petición al servidor...")

                                        val response = withContext(Dispatchers.IO) {
                                            ApiService.createModulo(
                                                token = token,
                                                cursoId = cursoId,
                                                titulo = titulo.trim(),
                                                descripcion = descripcion.trim().takeIf { it.isNotBlank() },
                                                orden = null
                                            )
                                        }

                                        Log.d("CrearModulo", "📥 Respuesta recibida: ${response.code()}")

                                        withContext(Dispatchers.Main) {
                                            isLoading = false

                                            if (response.isSuccessful) {
                                                val body = response.body()
                                                Log.d("CrearModulo", "✅ Módulo creado exitosamente")
                                                Log.d("CrearModulo", "Response body: $body")

                                                Toast.makeText(
                                                    context,
                                                    "Módulo creado exitosamente",
                                                    Toast.LENGTH_SHORT
                                                ).show()

                                                delay(300)
                                                navController.popBackStack()
                                            } else {
                                                val errorBody = response.errorBody()?.string()
                                                Log.e("CrearModulo", "❌ Error del servidor:")
                                                Log.e("CrearModulo", "  - Código: ${response.code()}")
                                                Log.e("CrearModulo", "  - Mensaje: ${response.message()}")
                                                Log.e("CrearModulo", "  - Body: $errorBody")

                                                errorMessage = when (response.code()) {
                                                    400 -> "Datos inválidos. Verifica el título."
                                                    401 -> "Sesión expirada. Inicia sesión nuevamente."
                                                    403 -> "No tienes permisos para crear módulos."
                                                    404 -> "Curso no encontrado."
                                                    else -> "Error al crear módulo (${response.code()})"
                                                }

                                                Toast.makeText(
                                                    context,
                                                    errorMessage,
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }

                                    } catch (e: Exception) {
                                        Log.e("CrearModulo", "❌ Excepción capturada", e)
                                        withContext(Dispatchers.Main) {
                                            isLoading = false
                                            errorMessage = "Error de conexión: ${e.localizedMessage}"
                                            Toast.makeText(
                                                context,
                                                "Error de conexión",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    } finally {
                                        Log.d("CrearModulo", "🏁 Proceso finalizado")
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AzulCielo,
                        disabledContainerColor = AzulCielo.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Creando...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Crear Módulo",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Nota informativa
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = AzulCielo.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "📌 Información",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = AzulCielo
                        )
                        Text(
                            "• El título debe tener entre 3 y 200 caracteres",
                            fontSize = 13.sp,
                            color = TextoGris
                        )
                        Text(
                            "• La descripción es opcional (máx. 1000 caracteres)",
                            fontSize = 13.sp,
                            color = TextoGris
                        )
                        Text(
                            "• Los módulos organizan el contenido del curso",
                            fontSize = 13.sp,
                            color = TextoGris
                        )
                    }
                }
            }
        }
    }
}

// Colores (asegúrate de tenerlos definidos en tu tema)
val AzulCielo = Color(0xFF00B9F0)
val FondoClaro = Color(0xFFF8F9FA)
val ErrorClaro = Color(0xFFFFEBEE)
val ErrorOscuro = Color(0xFFD32F2F)
val TextoGris = Color(0xFF757575)