package com.example.edumonjetcompose.ui.screens

import android.util.Log
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.edumonjetcompose.AzulCielo
import com.example.edumonjetcompose.Fucsia
import com.example.edumonjetcompose.Naranja
import com.example.edumonjetcompose.VerdeLima
import com.example.edumonjetcompose.models.EventoCalendario
import com.example.edumonjetcompose.network.ApiService
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarioScreenPadre(
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
    var vistaCalendario by remember { mutableStateOf(true) }

    var diaSeleccionado by remember { mutableStateOf<Int?>(null) }

    // Estados para el diálogo de evento
    var showEventoDialog by remember { mutableStateOf(false) }
    var eventoSeleccionado by remember { mutableStateOf<EventoCalendario?>(null) }

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

                    val cursoObj = body.getAsJsonObject("curso")
                    cursoNombre = cursoObj.get("nombre")?.asString ?: "Calendario"

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

                    val stats = body.getAsJsonObject("estadisticas")
                    estadisticas = mapOf(
                        "totalTareas" to stats.get("totalTareas").asInt,
                        "totalEventos" to stats.get("totalEventos").asInt,
                        "tareasVencidas" to stats.get("tareasVencidas").asInt,
                        "eventosProximos" to stats.get("eventosProximos").asInt
                    )

                    eventosFiltrados = eventos
                    Log.d("CalendarioScreen", "✅ Calendario cargado: ${eventos.size} eventos")
                } else {
                    errorMessage = "Error al cargar calendario"
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
                            text = "${obtenerNombreMesCalendario(mesSeleccionado)} $anioSeleccionado",
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
                    IconButton(onClick = { vistaCalendario = !vistaCalendario }) {
                        Icon(
                            imageVector = if (vistaCalendario) Icons.Default.List else Icons.Default.CalendarMonth,
                            contentDescription = "Cambiar vista",
                            tint = Color.White
                        )
                    }
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
                    LoadingCalendarioComun()
                }
                errorMessage != null -> {
                    ErrorCalendarioComun(errorMessage!!) { cargarCalendario() }
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Estadísticas
                        if (estadisticas.isNotEmpty()) {
                            EstadisticasCardComun(estadisticas)
                        }

                        // Navegación de mes
                        MesNavigatorComun(
                            mesSeleccionado = mesSeleccionado,
                            anioSeleccionado = anioSeleccionado,
                            onMesAnterior = {
                                if (mesSeleccionado == 1) {
                                    mesSeleccionado = 12
                                    anioSeleccionado--
                                } else {
                                    mesSeleccionado--
                                }
                            },
                            onMesSiguiente = {
                                if (mesSeleccionado == 12) {
                                    mesSeleccionado = 1
                                    anioSeleccionado++
                                } else {
                                    mesSeleccionado++
                                }
                            },
                            onHoyClick = {
                                val cal = Calendar.getInstance()
                                mesSeleccionado = cal.get(Calendar.MONTH) + 1
                                anioSeleccionado = cal.get(Calendar.YEAR)
                            }
                        )

                        // Filtros
                        FiltrosChips(
                            filtroSeleccionado = filtroSeleccionado,
                            onFiltroChange = { filtroSeleccionado = it }
                        )

                        // Vista de calendario o lista
                        if (vistaCalendario) {
                            VistaCalendarioPadre(
                                mesSeleccionado = mesSeleccionado,
                                anioSeleccionado = anioSeleccionado,
                                eventos = eventosFiltrados,
                                diaSeleccionado = diaSeleccionado,
                                onDiaClick = { dia ->
                                    diaSeleccionado = if (diaSeleccionado == dia) null else dia
                                },
                                onEventoClick = { evento ->
                                    eventoSeleccionado = evento
                                    showEventoDialog = true
                                }
                            )
                        } else {
                            if (eventosFiltrados.isEmpty()) {
                                EmptyCalendarioComun()
                            } else {
                                EventosListaPadre(
                                    eventos = eventosFiltrados,
                                    navController = navController,
                                    onEventoClick = { evento ->
                                        if (evento.tipo == "tarea") {
                                            navController.navigate("tarea_entrega/${evento.id}")
                                        } else {
                                            eventoSeleccionado = evento
                                            showEventoDialog = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Selector de fecha
        if (showDatePicker) {
            DatePickerDialogCalendario(
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

        // Diálogo de detalle de evento
        if (showEventoDialog && eventoSeleccionado != null) {
            EventoDetalleDialog(
                evento = eventoSeleccionado!!,
                onDismiss = {
                    showEventoDialog = false
                    eventoSeleccionado = null
                },
                onIrATarea = {
                    showEventoDialog = false
                    navController.navigate("tarea_entrega/${eventoSeleccionado!!.id}")
                }
            )
        }
    }
}

// ==================== COMPONENTES ESPECÍFICOS DE CALENDARIO PADRE ====================

@Composable
fun VistaCalendarioPadre(
    mesSeleccionado: Int,
    anioSeleccionado: Int,
    eventos: List<EventoCalendario>,
    diaSeleccionado: Int?,
    onDiaClick: (Int) -> Unit,
    onEventoClick: (EventoCalendario) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Días de la semana
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column {
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb").forEach { dia ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dia,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (dia == "Dom" || dia == "Sáb") Naranja else Color.Gray,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                Divider()

                // Grid de días
                val diasDelMes = obtenerDiasDelMesCalendario(mesSeleccionado, anioSeleccionado)
                val primerDia = obtenerPrimerDiaSemanaCalendario(mesSeleccionado, anioSeleccionado)

                Column {
                    var diaActual = 1
                    var semana = 0

                    while (diaActual <= diasDelMes) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (diaSemana in 0..6) {
                                val mostrarDia = if (semana == 0) {
                                    diaSemana >= primerDia && diaActual <= diasDelMes
                                } else {
                                    diaActual <= diasDelMes
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clickable(enabled = mostrarDia) {
                                            if (mostrarDia) onDiaClick(diaActual)
                                        }
                                        .background(
                                            if (diaSeleccionado == diaActual) AzulCielo.copy(alpha = 0.1f)
                                            else Color.Transparent
                                        )
                                        .border(
                                            width = 0.5.dp,
                                            color = Color.LightGray.copy(alpha = 0.3f)
                                        ),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    if (mostrarDia) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            // Número del día
                                            val esHoy = verificarEsHoyCalendario(diaActual, mesSeleccionado, anioSeleccionado)
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(if (esHoy) AzulCielo else Color.Transparent),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = diaActual.toString(),
                                                    fontSize = 12.sp,
                                                    fontWeight = if (esHoy) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (esHoy) Color.White else Color.Black
                                                )
                                            }

                                            // Indicadores de eventos
                                            val eventosDia = obtenerEventosDiaCalendario(
                                                diaActual,
                                                mesSeleccionado,
                                                anioSeleccionado,
                                                eventos
                                            )

                                            if (eventosDia.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(
                                                    horizontalArrangement = Arrangement.Center,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    eventosDia.take(3).forEach { evento ->
                                                        Box(
                                                            modifier = Modifier
                                                                .size(4.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                    Color(
                                                                        android.graphics.Color.parseColor(
                                                                            evento.color
                                                                        )
                                                                    )
                                                                )
                                                        )
                                                        if (evento != eventosDia.last()) {
                                                            Spacer(modifier = Modifier.width(2.dp))
                                                        }
                                                    }
                                                }
                                                if (eventosDia.size > 3) {
                                                    Text(
                                                        text = "+${eventosDia.size - 3}",
                                                        fontSize = 8.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                            }
                                        }
                                        diaActual++
                                    }
                                }
                            }
                        }
                        semana++
                    }
                }
            }
        }

        // Lista de eventos del día seleccionado
        if (diaSeleccionado != null) {
            val eventosDia = obtenerEventosDiaCalendario(diaSeleccionado, mesSeleccionado, anioSeleccionado, eventos)
            if (eventosDia.isNotEmpty()) {
                Text(
                    text = "Eventos del día $diaSeleccionado",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(eventosDia) { evento ->
                        EventoCardCompactoPadre(
                            evento = evento,
                            onClick = { onEventoClick(evento) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EventoCardCompactoPadre(
    evento: EventoCalendario,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(evento.color)))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = evento.titulo,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!evento.hora.isNullOrBlank()) {
                    Text(
                        text = evento.hora,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
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
fun EventosListaPadre(
    eventos: List<EventoCalendario>,
    navController: NavController,
    onEventoClick: (EventoCalendario) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(eventos) { evento ->
            EventoCardPadre(
                evento = evento,
                onClick = { onEventoClick(evento) }
            )
        }
    }
}

@Composable
fun EventoCardPadre(
    evento: EventoCalendario,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

                    Text(
                        text = formatearFechaCalendario(evento.fecha),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = evento.titulo,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

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

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}

// ==================== DIÁLOGO DE DETALLE DE EVENTO ====================

@Composable
fun EventoDetalleDialog(
    evento: EventoCalendario,
    onDismiss: () -> Unit,
    onIrATarea: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (evento.tipo == "tarea") "Detalle de Tarea" else "Detalle de Evento",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (evento.tipo == "tarea") Fucsia.copy(alpha = 0.15f) else AzulCielo.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (evento.tipo == "tarea") "TAREA" else "EVENTO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (evento.tipo == "tarea") Fucsia else AzulCielo,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Título
                item {
                    Column {
                        Text(
                            text = evento.titulo,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Divider(modifier = Modifier.padding(top = 8.dp))
                    }
                }

                // Descripción
                if (!evento.descripcion.isNullOrBlank()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    tint = AzulCielo,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Descripción",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                            }
                            Text(
                                text = evento.descripcion,
                                fontSize = 14.sp,
                                color = Color.Black,
                                modifier = Modifier.padding(start = 28.dp)
                            )
                        }
                    }
                }

                // Fecha
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = AzulCielo,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Fecha",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Text(
                                text = formatearFechaCortaCalendario(evento.fecha),
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                        }
                    }
                }

                // Hora (si existe)
                if (!evento.hora.isNullOrBlank()) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = AzulCielo,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Hora",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                                Text(
                                    text = evento.hora,
                                    fontSize = 14.sp,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }

                // Ubicación (si existe)
                if (!evento.ubicacion.isNullOrBlank()) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = AzulCielo,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Ubicación",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                                Text(
                                    text = evento.ubicacion,
                                    fontSize=12.sp,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }

                // Categoría (si existe)
                if (!evento.categoria.isNullOrBlank()) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Category,
                                contentDescription = null,
                                tint = AzulCielo,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Categoría",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                                Text(
                                    text = evento.categoria,
                                    fontSize = 14.sp,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }

                // Módulo (si es tarea)
                if (evento.tipo == "tarea" && !evento.modulo.isNullOrBlank()) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = Fucsia,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Módulo",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                                Text(
                                    text = evento.modulo,
                                    fontSize = 14.sp,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }

                // Estado
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            when (evento.estado) {
                                "pendiente" -> Icons.Default.Schedule
                                "completado" -> Icons.Default.CheckCircle
                                "vencido" -> Icons.Default.Warning
                                else -> Icons.Default.Info
                            },
                            contentDescription = null,
                            tint = when (evento.estado) {
                                "pendiente" -> Naranja
                                "completado" -> VerdeLima
                                "vencido" -> Color.Red
                                else -> Color.Gray
                            },
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Estado",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Text(
                                text = when (evento.estado) {
                                    "pendiente" -> "Pendiente"
                                    "completado" -> "Completado"
                                    "vencido" -> "Vencido"
                                    "proximo" -> "Próximo"
                                    else -> evento.estado.replaceFirstChar {
                                        if (it.isLowerCase()) it.titlecase(Locale.getDefault())
                                        else it.toString()
                                    }
                                },
                                fontSize = 14.sp,
                                color = Color.Black,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (evento.tipo == "tarea") {
                Button(
                    onClick = onIrATarea,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Fucsia
                    )
                ) {
                    Icon(
                        Icons.Default.Assignment,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ver Tarea")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Cerrar", color = AzulCielo, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (evento.tipo == "tarea") {
                TextButton(onClick = onDismiss) {
                    Text("Cerrar", color = Color.Gray)
                }
            }
        }
    )
}

@Composable
fun DatePickerDialogCalendario(
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
                                        text = obtenerNombreMesCalendario(mes).substring(0, 3),
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