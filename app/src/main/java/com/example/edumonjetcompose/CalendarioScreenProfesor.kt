package com.example.edumonjetcompose.screens.profesor

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.edumonjetcompose.*
import com.example.edumonjetcompose.network.ApiService
import com.example.edumonjetcompose.ui.*
import com.example.edumonjetcompose.ui.theme.AzulCielo
import com.example.edumonjetcompose.ui.theme.Fucsia
import com.example.edumonjetcompose.ui.theme.VerdeLima
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class EventoCalendario(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val fechaInicio: String,
    val fechaFin: String,
    val hora: String,
    val ubicacion: String,
    val categoria: String,
    val docenteNombre: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarioScreenProfesor(
    navController: NavController,
    cursoId: String,
    token: String

) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var eventos by remember { mutableStateOf<List<EventoCalendario>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var selectedYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var selectedDate by remember { mutableStateOf<Calendar?>(null) }
    var showEventDetails by remember { mutableStateOf<EventoCalendario?>(null) }

    val token = remember {
        context.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
            .getString("token", "") ?: ""
    }

    // Cargar eventos
    LaunchedEffect(cursoId) {
        isLoading = true
        try {
            val response = ApiService.getEventosByCurso(token, cursoId)
            if (response.isSuccessful) {
                val eventosArray = response.body()?.getAsJsonArray("eventos")
                eventos = eventosArray?.mapNotNull { element ->
                    try {
                        val obj = element.asJsonObject
                        val docenteObj = obj.getAsJsonObject("docenteId")
                        EventoCalendario(
                            id = obj.get("_id")?.asString ?: "",
                            titulo = obj.get("titulo")?.asString ?: "",
                            descripcion = obj.get("descripcion")?.asString ?: "",
                            fechaInicio = obj.get("fechaInicio")?.asString ?: "",
                            fechaFin = obj.get("fechaFin")?.asString ?: "",
                            hora = obj.get("hora")?.asString ?: "",
                            ubicacion = obj.get("ubicacion")?.asString ?: "",
                            categoria = obj.get("categoria")?.asString ?: "",
                            docenteNombre = "${docenteObj?.get("nombre")?.asString ?: ""} ${docenteObj?.get("apellido")?.asString ?: ""}"
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            scope.launch {
                snackbarHostState.showSnackbar("Error al cargar eventos")
            }
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendario del Curso", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate("crearEvento/$cursoId")
                    }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Crear evento",
                            tint = Celeste
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FondoCard,
                    titleContentColor = GrisOscuro
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = FondoClaro,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("crearEvento/$cursoId") },
                containerColor = Celeste,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, "Crear evento")
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = Celeste)
                    Text("Cargando calendario...", color = GrisMedio)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }

                // Estadísticas rápidas
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EstadisticaCard(
                            titulo = "Total",
                            valor = eventos.size.toString(),
                            icono = Icons.Default.Event,
                            color = Celeste,
                            modifier = Modifier.weight(1f)
                        )
                        EstadisticaCard(
                            titulo = "Este mes",
                            valor = eventos.count { evento ->
                                try {
                                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                                    val date = sdf.parse(evento.fechaInicio)
                                    val cal = Calendar.getInstance()
                                    cal.time = date!!
                                    cal.get(Calendar.MONTH) == selectedMonth && cal.get(Calendar.YEAR) == selectedYear
                                } catch (e: Exception) {
                                    false
                                }
                            }.toString(),
                            icono = Icons.Default.CalendarMonth,
                            color = VerdeLima,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Selector de mes/año
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = FondoCard,
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                if (selectedMonth == 0) {
                                    selectedMonth = 11
                                    selectedYear--
                                } else {
                                    selectedMonth--
                                }
                            }) {
                                Icon(Icons.Default.ChevronLeft, null, tint = Celeste)
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    getMonthName(selectedMonth),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = GrisOscuro
                                )
                                Text(
                                    selectedYear.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = GrisMedio
                                )
                            }

                            IconButton(onClick = {
                                if (selectedMonth == 11) {
                                    selectedMonth = 0
                                    selectedYear++
                                } else {
                                    selectedMonth++
                                }
                            }) {
                                Icon(Icons.Default.ChevronRight, null, tint = Celeste)
                            }
                        }
                    }
                }

                // Calendario visual
                item {
                    CalendarioGrid(
                        eventos = eventos,
                        selectedMonth = selectedMonth,
                        selectedYear = selectedYear,
                        selectedDate = selectedDate,
                        onDateSelected = { date ->
                            selectedDate = if (selectedDate?.timeInMillis == date.timeInMillis) null else date
                        }
                    )
                }

                // Lista de eventos
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (selectedDate != null) "Eventos del día" else "Todos los eventos",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GrisOscuro
                            )
                            if (selectedDate != null) {
                                TextButton(onClick = { selectedDate = null }) {
                                    Text("Ver todos", color = Celeste)
                                }
                            }
                        }
                    }
                }

                // Filtrar eventos según la fecha seleccionada
                val eventosFiltrados = if (selectedDate != null) {
                    eventos.filter { evento ->
                        try {
                            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                            val eventoDate = sdf.parse(evento.fechaInicio)
                            val eventoCal = Calendar.getInstance()
                            eventoCal.time = eventoDate!!

                            eventoCal.get(Calendar.YEAR) == selectedDate!!.get(Calendar.YEAR) &&
                                    eventoCal.get(Calendar.DAY_OF_YEAR) == selectedDate!!.get(Calendar.DAY_OF_YEAR)
                        } catch (e: Exception) {
                            false
                        }
                    }
                } else {
                    eventos
                }

                if (eventosFiltrados.isEmpty()) {
                    item {
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
                                    Icons.Default.EventBusy,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = GrisClaro
                                )
                                Text(
                                    if (selectedDate != null) "No hay eventos este día" else "No hay eventos creados",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = GrisMedio,
                                    textAlign = TextAlign.Center
                                )
                                if (selectedDate == null) {
                                    Button(
                                        onClick = { navController.navigate("crearEvento/$cursoId") },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Celeste
                                        )
                                    ) {
                                        Icon(Icons.Default.Add, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Crear primer evento")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(eventosFiltrados) { evento ->
                        EventoCard(
                            evento = evento,
                            onClick = { showEventDetails = evento }
                        )
                    }
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    // Diálogo de detalles del evento
    showEventDetails?.let { evento ->
        EventoDetalleDialog(
            evento = evento,
            onDismiss = { showEventDetails = null },
            onDelete = {
                scope.launch {
                    try {
                        val response = ApiService.deleteEvento(token, evento.id)
                        if (response.isSuccessful) {
                            eventos = eventos.filter { it.id != evento.id }
                            snackbarHostState.showSnackbar("Evento eliminado")
                            showEventDetails = null
                        } else {
                            snackbarHostState.showSnackbar("Error al eliminar")
                        }
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Error: ${e.message}")
                    }
                }
            }
        )
    }
}

// ==================== COMPONENTES ====================

@Composable
fun EstadisticaCard(
    titulo: String,
    valor: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                icono,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    valor,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    titulo,
                    style = MaterialTheme.typography.labelSmall,
                    color = GrisMedio
                )
            }
        }
    }
}

@Composable
fun CalendarioGrid(
    eventos: List<EventoCalendario>,
    selectedMonth: Int,
    selectedYear: Int,
    selectedDate: Calendar?,
    onDateSelected: (Calendar) -> Unit
) {
    val calendar = Calendar.getInstance()
    calendar.set(selectedYear, selectedMonth, 1)
    val firstDayOfMonth = calendar.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = FondoCard,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Días de la semana
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("D", "L", "M", "M", "J", "V", "S").forEach { day ->
                    Text(
                        day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = GrisMedio
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Grid de días
            var dayCounter = 1
            for (week in 0..5) {
                if (dayCounter > daysInMonth) break

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (dayOfWeek in 0..6) {
                        if (week == 0 && dayOfWeek < firstDayOfMonth) {
                            Spacer(Modifier.weight(1f))
                        } else if (dayCounter <= daysInMonth) {
                            val currentDay = dayCounter++
                            val currentCal = Calendar.getInstance()
                            currentCal.set(selectedYear, selectedMonth, currentDay)

                            val hasEvents = eventos.any { evento ->
                                try {
                                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                                    val eventoDate = sdf.parse(evento.fechaInicio)
                                    val eventoCal = Calendar.getInstance()
                                    eventoCal.time = eventoDate!!
                                    eventoCal.get(Calendar.YEAR) == selectedYear &&
                                            eventoCal.get(Calendar.MONTH) == selectedMonth &&
                                            eventoCal.get(Calendar.DAY_OF_MONTH) == currentDay
                                } catch (e: Exception) {
                                    false
                                }
                            }

                            val isSelected = selectedDate?.let {
                                it.get(Calendar.YEAR) == selectedYear &&
                                        it.get(Calendar.MONTH) == selectedMonth &&
                                        it.get(Calendar.DAY_OF_MONTH) == currentDay
                            } ?: false

                            val isToday = Calendar.getInstance().let {
                                it.get(Calendar.YEAR) == selectedYear &&
                                        it.get(Calendar.MONTH) == selectedMonth &&
                                        it.get(Calendar.DAY_OF_MONTH) == currentDay
                            }

                            DiaCalendario(
                                dia = currentDay,
                                hasEvents = hasEvents,
                                isSelected = isSelected,
                                isToday = isToday,
                                onClick = { onDateSelected(currentCal) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun DiaCalendario(
    dia: Int,
    hasEvents: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> Celeste
                    isToday -> VerdeLima.copy(alpha = 0.2f)
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                dia.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> Color.White
                    isToday -> VerdeLima
                    else -> GrisOscuro
                }
            )
            if (hasEvents) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(
                            if (isSelected) Color.White else Fucsia,
                            CircleShape
                        )
                )
            }
        }
    }
}

@Composable
fun EventoCard(
    evento: EventoCalendario,
    onClick: () -> Unit
) {
    val color = when (evento.categoria) {
        "escuela_padres" -> AzulCielo
        "tarea" -> Naranja
        "institucional" -> Celeste
        else -> GrisMedio
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = FondoCard,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Indicador de categoría
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(60.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    evento.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = GrisOscuro
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = GrisMedio
                    )
                    Text(
                        evento.hora,
                        style = MaterialTheme.typography.bodySmall,
                        color = GrisMedio
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = GrisMedio
                    )
                    Text(
                        evento.ubicacion,
                        style = MaterialTheme.typography.bodySmall,
                        color = GrisMedio
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = GrisMedio
            )
        }
    }
}

@Composable
fun EventoDetalleDialog(
    evento: EventoCalendario,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val color = when (evento.categoria) {
        "escuela_padres" -> AzulCielo
        "tarea" -> Naranja
        "institucional" -> Celeste
        else -> GrisMedio
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    evento.titulo,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    when (evento.categoria) {
                        "escuela_padres" -> "Escuela de Padres"
                        "tarea" -> "Tarea"
                        "institucional" -> "Institucional"
                        else -> evento.categoria
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = color
                )
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Descripción",
                            style = MaterialTheme.typography.labelSmall,
                            color = GrisMedio
                        )
                        Text(
                            evento.descripcion,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Schedule, null, tint = color)
                        Column {
                            Text(
                                "Hora",
                                style = MaterialTheme.typography.labelSmall,
                                color = GrisMedio
                            )
                            Text(evento.hora, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationOn, null, tint = color)
                        Column {
                            Text(
                                "Ubicación",
                                style = MaterialTheme.typography.labelSmall,
                                color = GrisMedio
                            )
                            Text(evento.ubicacion, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, null, tint = color)
                        Column {
                            Text(
                                "Docente",
                                style = MaterialTheme.typography.labelSmall,
                                color = GrisMedio
                            )
                            Text(evento.docenteNombre, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Fecha inicio",
                            style = MaterialTheme.typography.labelSmall,
                            color = GrisMedio
                        )
                        Text(
                            formatearFechaEvento(evento.fechaInicio),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Fecha fin",
                            style = MaterialTheme.typography.labelSmall,
                            color = GrisMedio
                        )
                        Text(
                            formatearFechaEvento(evento.fechaFin),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { showDeleteConfirm = true },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = ErrorOscuro
                )
            ) {
                Text("Eliminar")
            }
        }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("¿Eliminar evento?") },
            text = { Text("Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorOscuro
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}