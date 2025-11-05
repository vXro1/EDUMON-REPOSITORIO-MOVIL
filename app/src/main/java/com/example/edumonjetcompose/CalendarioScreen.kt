package com.example.edumonjetcompose.ui.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.edumonjetcompose.AzulCielo
import com.example.edumonjetcompose.Celeste
import com.example.edumonjetcompose.Fucsia
import com.example.edumonjetcompose.Naranja
import com.example.edumonjetcompose.VerdeLima
import com.example.edumonjetcompose.network.ApiService
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class EventoCalendario(
    val id: String,
    val tipo: String, // "tarea" o "evento"
    val titulo: String,
    val descripcion: String?,
    val fecha: String,
    val fechaInicio: String,
    val fechaFin: String,
    val estado: String,
    val modulo: String? = null,
    val moduloId: String? = null,
    val categoria: String? = null,
    val ubicacion: String? = null,
    val hora: String? = null,
    val color: String,
    val icono: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarioScreen(
    navController: NavController,
    cursoId: String,
    token: String
) {
    var eventos by remember { mutableStateOf<List<EventoCalendario>>(emptyList()) }
    var eventosFiltrados by remember { mutableStateOf<List<EventoCalendario>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var cursoNombre by remember { mutableStateOf("Calendario") }
    var estadisticas by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    var filtroSeleccionado by remember { mutableStateOf("Todos") }
    var mesSeleccionado by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) }
    var anioSeleccionado by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var showDatePicker by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Cargar calendario
    fun cargarCalendario() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = ApiService.getCalendarioCurso(token, cursoId, mesSeleccionado, anioSeleccionado)

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!

                    // Extraer nombre del curso
                    val cursoObj = body.getAsJsonObject("curso")
                    cursoNombre = cursoObj.get("nombre")?.asString ?: "Calendario"

                    // Extraer eventos
                    val itemsArray = body.getAsJsonArray("items")
                    eventos = itemsArray.map { item ->
                        val obj = item.asJsonObject
                        EventoCalendario(
                            id = obj.get("id").asString,
                            tipo = obj.get("tipo").asString,
                            titulo = obj.get("titulo").asString,
                            descripcion = obj.get("descripcion")?.asString,
                            fecha = obj.get("fecha").asString,
                            fechaInicio = obj.get("fechaInicio").asString,
                            fechaFin = obj.get("fechaFin").asString,
                            estado = obj.get("estado").asString,
                            modulo = obj.get("modulo")?.asString,
                            moduloId = obj.get("moduloId")?.asString,
                            categoria = obj.get("categoria")?.asString,
                            ubicacion = obj.get("ubicacion")?.asString,
                            hora = obj.get("hora")?.asString,
                            color = obj.get("color").asString,
                            icono = obj.get("icono").asString
                        )
                    }

                    // Extraer estadísticas
                    val stats = body.getAsJsonObject("estadisticas")
                    estadisticas = mapOf(
                        "totalTareas" to stats.get("totalTareas").asInt,
                        "totalEventos" to stats.get("totalEventos").asInt,
                        "tareasVencidas" to stats.get("tareasVencidas").asInt,
                        "eventosProximos" to stats.get("eventosProximos").asInt
                    )

                    // Aplicar filtro inicial
                    eventosFiltrados = eventos

                    Log.d("CalendarioScreen", "✅ Calendario cargado: ${eventos.size} eventos")
                } else {
                    errorMessage = "Error al cargar calendario"
                    Log.e("CalendarioScreen", "Error: ${response.code()}")
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.message}"
                Log.e("CalendarioScreen", "❌ Error", e)
            } finally {
                isLoading = false
            }
        }
    }

    // Aplicar filtros
    fun aplicarFiltro() {
        eventosFiltrados = when (filtroSeleccionado) {
            "Tareas" -> eventos.filter { it.tipo == "tarea" }
            "Eventos" -> eventos.filter { it.tipo == "evento" }
            else -> eventos
        }
    }

    LaunchedEffect(cursoId, mesSeleccionado, anioSeleccionado) {
        cargarCalendario()
    }

    LaunchedEffect(filtroSeleccionado) {
        aplicarFiltro()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = cursoNombre,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = obtenerNombreMes(mesSeleccionado) + " $anioSeleccionado",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Cambiar mes",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AzulCielo,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            when {
                isLoading -> {
                    LoadingCalendar()
                }
                errorMessage != null -> {
                    ErrorMessage(errorMessage!!) { cargarCalendario() }
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Estadísticas
                        if (estadisticas.isNotEmpty()) {
                            EstadisticasCard(estadisticas)
                        }

                        // Filtros
                        FiltrosChips(
                            filtroSeleccionado = filtroSeleccionado,
                            onFiltroChange = { filtroSeleccionado = it }
                        )

                        // Lista de eventos
                        if (eventosFiltrados.isEmpty()) {
                            EmptyCalendar()
                        } else {
                            EventosLista(
                                eventos = eventosFiltrados,
                                navController = navController
                            )
                        }
                    }
                }
            }
        }

        // Selector de fecha
        if (showDatePicker) {
            DatePickerDialog(
                mesActual = mesSeleccionado,
                anioActual = anioSeleccionado,
                onDismiss = { showDatePicker = false },
                onDateSelected = { mes, anio ->
                    mesSeleccionado = mes
                    anioSeleccionado = anio
                    showDatePicker = false
                }
            )
        }
    }
}

@Composable
fun EstadisticasCard(estadisticas: Map<String, Int>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            EstadisticaItem(
                titulo = "Tareas",
                valor = estadisticas["totalTareas"] ?: 0,
                color = Fucsia
            )
            EstadisticaItem(
                titulo = "Eventos",
                valor = estadisticas["totalEventos"] ?: 0,
                color = AzulCielo
            )
            EstadisticaItem(
                titulo = "Vencidas",
                valor = estadisticas["tareasVencidas"] ?: 0,
                color = Naranja
            )
        }
    }
}

@Composable
fun EstadisticaItem(titulo: String, valor: Int, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Text(
            text = valor.toString(),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = titulo,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun FiltrosChips(
    filtroSeleccionado: String,
    onFiltroChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("Todos", "Tareas", "Eventos").forEach { filtro ->
            FilterChip(
                selected = filtroSeleccionado == filtro,
                onClick = { onFiltroChange(filtro) },
                label = { Text(filtro) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AzulCielo,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
fun EventosLista(
    eventos: List<EventoCalendario>,
    navController: NavController
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(eventos) { evento ->
            EventoCard(
                evento = evento,
                onClick = {
                    if (evento.tipo == "tarea") {
                        navController.navigate("tarea_entrega/${evento.id}")
                    }
                }
            )
        }
    }
}

@Composable
fun EventoCard(
    evento: EventoCalendario,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = evento.tipo == "tarea") { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Indicador de color
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(60.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(android.graphics.Color.parseColor(evento.color)))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Tipo y fecha
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Badge de tipo
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (evento.tipo == "tarea") Fucsia.copy(alpha = 0.15f) else AzulCielo.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (evento.tipo == "tarea") "Tarea" else "Evento",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (evento.tipo == "tarea") Fucsia else AzulCielo,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Fecha
                    Text(
                        text = formatearFecha(evento.fecha),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Título
                Text(
                    text = evento.titulo,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Descripción o módulo
                if (evento.tipo == "tarea" && !evento.modulo.isNullOrBlank()) {
                    Text(
                        text = "📚 ${evento.modulo}",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else if (evento.tipo == "evento") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        if (!evento.hora.isNullOrBlank()) {
                            Text(
                                text = "🕐 ${evento.hora}",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                        if (!evento.ubicacion.isNullOrBlank()) {
                            Text(
                                text = " • 📍 ${evento.ubicacion}",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            // Icono de acción
            if (evento.tipo == "tarea") {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }
        }
    }
}

@Composable
fun LoadingCalendar() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = AzulCielo)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Cargando calendario...", color = Color.Gray)
        }
    }
}

@Composable
fun EmptyCalendar() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.EventAvailable,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color.LightGray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No hay eventos este mes",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ErrorMessage(mensaje: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Naranja
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = mensaje,
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = AzulCielo)) {
                Text("Reintentar")
            }
        }
    }
}

@Composable
fun DatePickerDialog(
    mesActual: Int,
    anioActual: Int,
    onDismiss: () -> Unit,
    onDateSelected: (Int, Int) -> Unit
) {
    var selectedMes by remember { mutableStateOf(mesActual) }
    var selectedAnio by remember { mutableStateOf(anioActual) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar mes") },
        text = {
            Column {
                // Selector de año
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedAnio-- }) {
                        Icon(Icons.Default.KeyboardArrowLeft, null)
                    }
                    Text(selectedAnio.toString(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = { selectedAnio++ }) {
                        Icon(Icons.Default.KeyboardArrowRight, null)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Grid de meses
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..12 step 3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            (row until row + 3).forEach { mes ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selectedMes == mes) AzulCielo else Color.LightGray.copy(alpha = 0.2f))
                                        .clickable { selectedMes = mes }
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = obtenerNombreMes(mes).substring(0, 3),
                                        color = if (selectedMes == mes) Color.White else Color.Black,
                                        fontWeight = if (selectedMes == mes) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onDateSelected(selectedMes, selectedAnio) }) {
                Text("Aceptar", color = AzulCielo)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// Funciones auxiliares
fun formatearFecha(fechaISO: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale("es", "ES"))
        val date = inputFormat.parse(fechaISO)
        date?.let { outputFormat.format(it) } ?: fechaISO
    } catch (e: Exception) {
        fechaISO
    }
}

fun obtenerNombreMes(mes: Int): String {
    val meses = listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )
    return meses.getOrNull(mes - 1) ?: "Mes $mes"
}