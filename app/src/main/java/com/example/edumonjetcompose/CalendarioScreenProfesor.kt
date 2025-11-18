package com.example.edumonjetcompose.ui.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import com.example.edumonjetcompose.models.EventoCalendario
import com.example.edumonjetcompose.network.ApiService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun CalendarioScreenProfesor(
    navController: NavController,
    cursoId: String,
    token: String,
    esProfesor: Boolean = true
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
    var showEventoDialog by remember { mutableStateOf(false) }
    var eventoSeleccionado by remember { mutableStateOf<EventoCalendario?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

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
                    errorMessage = "Error al cargar el calendario"
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.localizedMessage}"
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

    // Eliminar evento
    fun eliminarEvento() {
        scope.launch {
            try {
                val response = ApiService.deleteEvento(token, eventoSeleccionado!!.id)
                if (response.isSuccessful) {
                    showDeleteConfirmation = false
                    showEventoDialog = false
                    cargarCalendario()
                    snackbarHostState.showSnackbar(
                        message = "Evento eliminado exitosamente",
                        duration = SnackbarDuration.Short
                    )
                } else {
                    snackbarHostState.showSnackbar(
                        message = "Error al eliminar el evento",
                        duration = SnackbarDuration.Short
                    )
                }
            } catch (e: Exception) {
                Log.e("CalendarioScreen", "Error al eliminar", e)
                snackbarHostState.showSnackbar(
                    message = "Error de conexión",
                    duration = SnackbarDuration.Short
                )
            }
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
                            text = obtenerNombreMesCalendario(mesSeleccionado) + " $anioSeleccionado",
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
                            contentDescription = "Seleccionar fecha",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AzulCielo,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (esProfesor) {
                FloatingActionButton(
                    onClick = {
                        navController.navigate("crearEvento/$cursoId")
                    },
                    containerColor = Fucsia
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Crear evento",
                        tint = Color.White
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                        AnimatedVisibility(
                            visible = estadisticas.isNotEmpty(),
                            enter = fadeIn() + expandVertically()
                        ) {
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
                                diaSeleccionado = cal.get(Calendar.DAY_OF_MONTH)
                            }
                        )

                        // Filtros
                        FiltrosChips(
                            filtroSeleccionado = filtroSeleccionado,
                            onFiltroChange = { filtroSeleccionado = it },
                            totalEventos = eventosFiltrados.size
                        )

                        // Vista de calendario o lista
                        AnimatedContent(
                            targetState = vistaCalendario,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(300)) with
                                        fadeOut(animationSpec = tween(300))
                            },
                            label = "vista_transition"
                        ) { isCalendarioView ->
                            if (isCalendarioView) {
                                VistaCalendario(
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
                                    EventosLista(
                                        eventos = eventosFiltrados,
                                        onEventoClick = { evento ->
                                            eventoSeleccionado = evento
                                            showEventoDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selector de fecha
        if (showDatePicker) {
            CalendarioDatePickerDialog(
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

        // Dialog de detalle de evento
        if (showEventoDialog && eventoSeleccionado != null) {
            EventoDetalleDialog(
                evento = eventoSeleccionado!!,
                esProfesor = esProfesor,
                onDismiss = {
                    showEventoDialog = false
                    eventoSeleccionado = null
                },
                onVerDetalle = {
                    showEventoDialog = false
                    if (eventoSeleccionado!!.tipo == "tarea") {
                        navController.navigate("tarea_entrega/${eventoSeleccionado!!.id}")
                    }
                },
                onEditar = {
                    showEventoDialog = false
                    // TODO: Navegar a editar evento
                    // navController.navigate("editar_evento/${eventoSeleccionado!!.id}")
                },
                onEliminar = {
                    showDeleteConfirmation = true
                }
            )
        }

        // Confirmación de eliminación
        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Confirmar eliminación") },
                text = { Text("¿Estás seguro de que deseas eliminar este evento? Esta acción no se puede deshacer.") },
                confirmButton = {
                    TextButton(
                        onClick = { eliminarEvento() },
                        colors = ButtonDefaults.textButtonColors(contentColor = Naranja)
                    ) {
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

// ==================== COMPONENTES DE VISTA ====================

@Composable
fun VistaCalendario(
    mesSeleccionado: Int,
    anioSeleccionado: Int,
    eventos: List<EventoCalendario>,
    diaSeleccionado: Int?,
    onDiaClick: (Int) -> Unit,
    onEventoClick: (EventoCalendario) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Calendario
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column {
                // Encabezado días de la semana
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

                HorizontalDivider()

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
                                            when {
                                                diaSeleccionado == diaActual -> AzulCielo.copy(alpha = 0.15f)
                                                verificarEsHoyCalendario(
                                                    diaActual,
                                                    mesSeleccionado,
                                                    anioSeleccionado
                                                ) -> Celeste.copy(alpha = 0.1f)

                                                else -> Color.Transparent
                                            }
                                        )
                                        .border(
                                            width = 0.5.dp,
                                            color = Color.LightGray.copy(alpha = 0.3f)
                                        ),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    if (mostrarDia) {
                                        val diaParaMostrar = diaActual
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            // Número del día
                                            val esDiaHoy = verificarEsHoyCalendario(
                                                diaParaMostrar,
                                                mesSeleccionado,
                                                anioSeleccionado
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(if (esDiaHoy) AzulCielo else Color.Transparent),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = diaParaMostrar.toString(),
                                                    fontSize = 12.sp,
                                                    fontWeight = if (esDiaHoy) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (esDiaHoy) Color.White else Color.Black
                                                )
                                            }

                                            // Indicadores de eventos
                                            val eventosDia = obtenerEventosDiaCalendario(
                                                diaParaMostrar,
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
                                                                .size(5.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                    Color(
                                                                        android.graphics.Color.parseColor(
                                                                            evento.color
                                                                        )
                                                                    )
                                                                )
                                                        )
                                                        if (evento != eventosDia.take(3).last()) {
                                                            Spacer(modifier = Modifier.width(2.dp))
                                                        }
                                                    }
                                                }
                                                if (eventosDia.size > 3) {
                                                    Text(
                                                        text = "+${eventosDia.size - 3}",
                                                        fontSize = 8.sp,
                                                        color = Color.Gray,
                                                        modifier = Modifier.padding(top = 1.dp)
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
        AnimatedVisibility(
            visible = diaSeleccionado != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            if (diaSeleccionado != null) {
                val eventosDia = obtenerEventosDiaCalendario(
                    diaSeleccionado,
                    mesSeleccionado,
                    anioSeleccionado,
                    eventos
                )

                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Eventos del $diaSeleccionado de ${obtenerNombreMesCalendario(mesSeleccionado)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (eventosDia.isNotEmpty()) {
                            Surface(
                                shape = CircleShape,
                                color = AzulCielo
                            ) {
                                Text(
                                    text = eventosDia.size.toString(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (eventosDia.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No hay eventos para este día",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(eventosDia) { evento ->
                                EventoCardCompacto(
                                    evento = evento,
                                    onClick = { onEventoClick(evento) }
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
fun EventoCardCompacto(
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
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicador de color
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(evento.color)))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (evento.tipo == "tarea") Fucsia.copy(alpha = 0.15f) else AzulCielo.copy(
                            alpha = 0.15f
                        )
                    ) {
                        Text(
                            text = if (evento.tipo == "tarea") "Tarea" else "Evento",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (evento.tipo == "tarea") Fucsia else AzulCielo,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (!evento.hora.isNullOrBlank()) {
                        Text(
                            text = evento.hora,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = evento.titulo,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (!evento.ubicacion.isNullOrBlank()) {
                    Text(
                        text = "📍 ${evento.ubicacion}",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
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
    onFiltroChange: (String) -> Unit,
    totalEventos: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Filtrar:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )

        listOf("Todos", "Tareas", "Eventos").forEach { filtro ->
            FilterChip(
                selected = filtroSeleccionado == filtro,
                onClick = { onFiltroChange(filtro) },
                label = { Text(filtro) },
                leadingIcon = {
                    if (filtroSeleccionado == filtro) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AzulCielo,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White
                )
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF0F0F0)
        ) {
            Text(
                text = "$totalEventos",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun EventosLista(
    eventos: List<EventoCalendario>,
    onEventoClick: (EventoCalendario) -> Unit
) {
    // Agrupar eventos por fecha
    val eventosAgrupados = eventos.groupBy {
        formatearFechaCortaCalendario(it.fecha)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        eventosAgrupados.forEach { (fecha, eventosDelDia) ->
            item {
                Text(
                    text = fecha,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AzulCielo,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(eventosDelDia) { evento ->
                EventoCard(
                    evento = evento,
                    onClick = { onEventoClick(evento) }
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
                    .height(80.dp)
                    .clip(RoundedCornerShape(2.dp)).background(Color(android.graphics.Color.parseColor(evento.color)))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (evento.tipo == "tarea") Fucsia.copy(alpha = 0.15f) else AzulCielo.copy(
                            alpha = 0.15f
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (evento.tipo == "tarea") Icons.Default.Assignment else Icons.Default.Event,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (evento.tipo == "tarea") Fucsia else AzulCielo
                            )
                            Text(
                                text = if (evento.tipo == "tarea") "Tarea" else "Evento",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (evento.tipo == "tarea") Fucsia else AzulCielo
                            )
                        }
                    }

                    if (evento.estado == "vencida") {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Naranja.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Vencida",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Naranja,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = evento.titulo,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (!evento.descripcion.isNullOrBlank()) {
                    Text(
                        text = evento.descripcion,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Información adicional
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!evento.hora.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = evento.hora,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    if (!evento.ubicacion.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = evento.ubicacion,
                                fontSize = 12.sp,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (evento.tipo == "tarea" && !evento.modulo.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = evento.modulo,
                            fontSize = 12.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Icono de navegación
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Ver detalles",
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun EventoDetalleDialog(
    evento: EventoCalendario,
    esProfesor: Boolean,
    onDismiss: () -> Unit,
    onVerDetalle: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (evento.tipo == "tarea") Fucsia.copy(alpha = 0.15f) else AzulCielo.copy(
                                alpha = 0.15f
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (evento.tipo == "tarea") Icons.Default.Assignment else Icons.Default.Event,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (evento.tipo == "tarea") Fucsia else AzulCielo
                                )
                                Text(
                                    text = if (evento.tipo == "tarea") "Tarea" else "Evento",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (evento.tipo == "tarea") Fucsia else AzulCielo
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = evento.titulo,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(android.graphics.Color.parseColor(evento.color)))
                    )
                }
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Descripción
                if (!evento.descripcion.isNullOrBlank()) {
                    item {
                        Column {
                            Text(
                                text = "Descripción",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = evento.descripcion,
                                fontSize = 14.sp
                            )
                        }
                    }

                    item { HorizontalDivider() }
                }

                // Fecha y hora
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            tint = AzulCielo,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Fecha",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = formatearFechaCalendario(evento.fecha),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                if (!evento.hora.isNullOrBlank()) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = AzulCielo,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Hora",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = evento.hora,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                if (!evento.ubicacion.isNullOrBlank()) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = AzulCielo,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Ubicación",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = evento.ubicacion,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                if (!evento.categoria.isNullOrBlank()) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Category,
                                contentDescription = null,
                                tint = AzulCielo,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Categoría",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = evento.categoria,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                if (evento.tipo == "tarea" && !evento.modulo.isNullOrBlank()) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Book,
                                contentDescription = null,
                                tint = AzulCielo,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Módulo",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = evento.modulo,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Estado
                if (evento.estado == "vencida") {
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Naranja.copy(alpha = 0.1f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Naranja,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Esta tarea está vencida",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Naranja
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botón principal: Ver detalles
                if (evento.tipo == "tarea") {
                    Button(
                        onClick = onVerDetalle,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AzulCielo)
                    ) {
                        Icon(
                            Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ver tarea completa")
                    }
                }

                // Botones de profesor
                if (esProfesor && evento.tipo == "evento") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onEditar,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Editar")
                        }

                        OutlinedButton(
                            onClick = onEliminar,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Naranja
                            ),
                            border = BorderStroke(1.dp, Naranja)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Eliminar")
                        }
                    }
                }

                // Botón cerrar
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cerrar")
                }
            }
        }
    )
}

@Composable
fun CalendarioDatePickerDialog(
    mesActual: Int,
    anioActual: Int,
    onDismiss: () -> Unit,
    onDateSelected: (mes: Int, anio: Int) -> Unit
) {
    var selectedMes by remember { mutableStateOf(mesActual) }
    var selectedAnio by remember { mutableStateOf(anioActual) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text(
                "Seleccionar mes y año",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                // Selector de año
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedAnio-- }) {
                            Icon(Icons.Default.KeyboardArrowLeft, null, tint = AzulCielo)
                        }
                        Text(
                            selectedAnio.toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = AzulCielo
                        )
                        IconButton(onClick = { selectedAnio++ }) {
                            Icon(Icons.Default.KeyboardArrowRight, null, tint = AzulCielo)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                                        .background(
                                            if (selectedMes == mes) AzulCielo
                                            else Color.LightGray.copy(alpha = 0.2f)
                                        )
                                        .clickable { selectedMes = mes }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = obtenerNombreMesCalendario(mes).substring(0, 3),
                                        color = if (selectedMes == mes) Color.White else Color.Black,
                                        fontWeight = if (selectedMes == mes) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onDateSelected(selectedMes, selectedAnio) },
                colors = ButtonDefaults.buttonColors(containerColor = AzulCielo)
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}