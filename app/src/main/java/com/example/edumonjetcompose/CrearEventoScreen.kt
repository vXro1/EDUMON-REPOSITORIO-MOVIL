package com.example.edumonjetcompose

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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearEventoScreen(
    navController: NavController,
    cursoId: String,
    token: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var fechaInicio by remember { mutableStateOf("") }
    var fechaFin by remember { mutableStateOf("") }
    var hora by remember { mutableStateOf("") }
    var ubicacion by remember { mutableStateOf("") }
    var selectedCategoria by remember { mutableStateOf("tarea") }
    var isCreating by remember { mutableStateOf(false) }
    var docenteId by remember { mutableStateOf("") }

    var showDatePickerInicio by remember { mutableStateOf(false) }
    var showDatePickerFin by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }


    val categorias = listOf(
        CategoriaEvento("escuela_padres", "Escuela de Padres", Icons.Default.School, AzulCielo),
        CategoriaEvento("tarea", "Tarea", Icons.Default.Assignment, Naranja),
        CategoriaEvento("institucional", "Institucional", Icons.Default.Business, Celeste)
    )

    // Obtener el ID del docente
    LaunchedEffect(Unit) {
        try {
            val profileResponse = ApiService.getProfile(token)
            if (profileResponse.isSuccessful) {
                val profileBody = profileResponse.body()
                docenteId = profileBody?.get("_id")?.asString ?: ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear Nuevo Evento", fontWeight = FontWeight.Bold) },
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
                    color = Celeste.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, Celeste.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Celeste.copy(alpha = 0.2f)
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(12.dp)
                                    .size(24.dp),
                                tint = Celeste
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "Planifica un evento",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Celeste
                            )
                            Text(
                                "Los eventos aparecerán en el calendario del curso",
                                style = MaterialTheme.typography.bodySmall,
                                color = GrisMedio
                            )
                        }
                    }
                }
            }

            // Título del evento
            item {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { if (it.length <= 200) titulo = it },
                    label = { Text("Título del evento") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Title, null, tint = Celeste)
                    },
                    supportingText = {
                        Text("${titulo.length}/200 caracteres")
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Celeste,
                        focusedLabelColor = Celeste,
                        cursorColor = Celeste
                    )
                )
            }

            // Descripción
            item {
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    placeholder = { Text("Describe los detalles del evento...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    maxLines = 6,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = Celeste,
                            modifier = Modifier
                                .padding(top = 8.dp, start = 4.dp)
                                .size(24.dp)
                        )
                    },
                    supportingText = {
                        Text(if (descripcion.length < 10) "Mínimo 10 caracteres" else "✓ Descripción válida")
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Celeste,
                        focusedLabelColor = Celeste,
                        cursorColor = Celeste
                    )
                )
            }

            // Categoría del evento
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Categoría",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GrisOscuro
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categorias.forEach { categoria ->
                            CategoriaEventoCard(
                                categoria = categoria,
                                isSelected = selectedCategoria == categoria.id,
                                onClick = { selectedCategoria = categoria.id },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Fechas
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Fechas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GrisOscuro
                    )

                    // Fecha de inicio
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePickerInicio = true },
                        shape = RoundedCornerShape(12.dp),
                        color = FondoCard,
                        border = BorderStroke(1.dp, if (fechaInicio.isEmpty()) GrisClaro else Celeste)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    null,
                                    tint = Celeste
                                )
                                Column {
                                    Text(
                                        "Fecha de inicio",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GrisMedio
                                    )
                                    Text(
                                        if (fechaInicio.isEmpty()) "Seleccionar"
                                        else formatearFechaEvento(fechaInicio),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (fechaInicio.isEmpty()) GrisMedio else GrisOscuro,
                                        fontWeight = if (fechaInicio.isEmpty()) FontWeight.Normal else FontWeight.Medium
                                    )
                                }
                            }
                            Icon(
                                Icons.Default.ArrowDropDown,
                                null,
                                tint = GrisMedio
                            )
                        }
                    }

                    // Fecha de fin
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePickerFin = true },
                        shape = RoundedCornerShape(12.dp),
                        color = FondoCard,
                        border = BorderStroke(1.dp, if (fechaFin.isEmpty()) GrisClaro else Celeste)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Event,
                                    null,
                                    tint = Celeste
                                )
                                Column {
                                    Text(
                                        "Fecha de fin",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GrisMedio
                                    )
                                    Text(
                                        if (fechaFin.isEmpty()) "Seleccionar"
                                        else formatearFechaEvento(fechaFin),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (fechaFin.isEmpty()) GrisMedio else GrisOscuro,
                                        fontWeight = if (fechaFin.isEmpty()) FontWeight.Normal else FontWeight.Medium
                                    )
                                }
                            }
                            Icon(
                                Icons.Default.ArrowDropDown,
                                null,
                                tint = GrisMedio
                            )
                        }
                    }
                }
            }

            // Hora (OBLIGATORIA)
            item {
                OutlinedTextField(
                    value = hora,
                    onValueChange = { },
                    label = { Text("Hora *") },
                    placeholder = { Text("Ej: 10:30") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimePicker = true },
                    enabled = false,
                    leadingIcon = {
                        Icon(Icons.Default.Schedule, null, tint = Celeste)
                    },
                    trailingIcon = {
                        if (hora.isNotEmpty()) {
                            IconButton(onClick = { hora = "" }) {
                                Icon(Icons.Default.Close, "Limpiar", tint = GrisMedio)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = if (hora.isEmpty()) GrisClaro else Celeste,
                        disabledLabelColor = GrisMedio,
                        disabledLeadingIconColor = Celeste,
                        disabledTextColor = GrisOscuro
                    )
                )
            }

            // Ubicación (OBLIGATORIA)
            item {
                OutlinedTextField(
                    value = ubicacion,
                    onValueChange = { if (it.length <= 200) ubicacion = it },
                    label = { Text("Ubicación *") },
                    placeholder = { Text("Ej: Salón 201, Auditorio...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, null, tint = Celeste)
                    },
                    supportingText = {
                        Text("${ubicacion.length}/200 caracteres")
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Celeste,
                        focusedLabelColor = Celeste,
                        cursorColor = Celeste
                    )
                )
            }

            // Botones de acción
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            // Validaciones
                            when {
                                titulo.isBlank() -> {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("El título es obligatorio")
                                    }
                                    return@Button
                                }
                                titulo.length < 3 -> {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("El título debe tener al menos 3 caracteres")
                                    }
                                    return@Button
                                }
                                descripcion.isBlank() -> {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("La descripción es obligatoria")
                                    }
                                    return@Button
                                }
                                descripcion.length < 10 -> {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("La descripción debe tener al menos 10 caracteres")
                                    }
                                    return@Button
                                }
                                fechaInicio.isEmpty() -> {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Selecciona la fecha de inicio")
                                    }
                                    return@Button
                                }
                                fechaFin.isEmpty() -> {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Selecciona la fecha de fin")
                                    }
                                    return@Button
                                }
                                hora.isEmpty() -> {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("La hora es obligatoria")
                                    }
                                    return@Button
                                }
                                ubicacion.isBlank() -> {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("La ubicación es obligatoria")
                                    }
                                    return@Button
                                }
                                ubicacion.length < 3 -> {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("La ubicación debe tener al menos 3 caracteres")
                                    }
                                    return@Button
                                }
                                docenteId.isEmpty() -> {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Error al obtener datos del usuario")
                                    }
                                    return@Button
                                }
                            }

                            // Validar que fechaFin sea posterior a fechaInicio
                            try {
                                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                                val dateInicio = sdf.parse(fechaInicio)
                                val dateFin = sdf.parse(fechaFin)

                                if (dateFin != null && dateInicio != null && dateFin <= dateInicio) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("La fecha de fin debe ser posterior a la de inicio")
                                    }
                                    return@Button
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            scope.launch {
                                isCreating = true
                                try {
                                    // Convertir hora de formato 12h a 24h
                                    val hora24h = convertirHoraA24h(hora)

                                    val response = ApiService.createEvento(
                                        token = token,
                                        titulo = titulo.trim(),
                                        descripcion = descripcion.trim(),
                                        fechaInicio = fechaInicio,
                                        fechaFin = fechaFin,
                                        hora = hora24h,
                                        ubicacion = ubicacion.trim(),
                                        categoria = selectedCategoria,
                                        cursosIds = listOf(cursoId),
                                        docenteId = docenteId
                                    )

                                    if (response.isSuccessful) {
                                        snackbarHostState.showSnackbar("✓ Evento creado exitosamente")
                                        kotlinx.coroutines.delay(500)
                                        navController.navigate("calendarioProfesor/$cursoId") {
                                            popUpTo("crearEvento/$cursoId") { inclusive = true }
                                        }
                                    } else {
                                        val errorBody = response.errorBody()?.string()
                                        snackbarHostState.showSnackbar(
                                            errorBody?.let {
                                                "Error: ${it.take(100)}"
                                            } ?: "Error al crear el evento"
                                        )
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    snackbarHostState.showSnackbar("Error: ${e.message}")
                                } finally {
                                    isCreating = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isCreating,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Celeste
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
                                "Crear Evento",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { navController.popBackStack() },
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

    // Diálogo para fecha de inicio
    if (showDatePickerInicio) {
        DatePickerDialogEvento(
            currentDate = fechaInicio,
            onDateSelected = { date ->
                fechaInicio = date
                showDatePickerInicio = false
            },
            onDismiss = { showDatePickerInicio = false },
            minDate = System.currentTimeMillis() // Solo fechas futuras
        )
    }

    // Diálogo para fecha de fin
    if (showDatePickerFin) {
        DatePickerDialogEvento(
            currentDate = fechaFin,
            onDateSelected = { date ->
                fechaFin = date
                showDatePickerFin = false
            },
            onDismiss = { showDatePickerFin = false },
            minDate = if (fechaInicio.isNotEmpty()) {
                try {
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                        .parse(fechaInicio)?.time ?: System.currentTimeMillis()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }
            } else {
                System.currentTimeMillis()
            }
        )
    }

    // Diálogo para hora
    if (showTimePicker) {
        TimePickerDialog(
            currentTime = hora,
            onTimeSelected = { time ->
                hora = time
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

// ==================== COMPONENTES ====================

@Composable
fun CategoriaEventoCard(
    categoria: CategoriaEvento,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(90.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) categoria.color.copy(alpha = 0.15f) else FondoCard,
        border = BorderStroke(
            width = 2.dp,
            color = if (isSelected) categoria.color else GrisClaro
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                categoria.icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isSelected) categoria.color else GrisMedio
            )
            Spacer(Modifier.height(6.dp))
            Text(
                categoria.nombre,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) categoria.color else GrisMedio,
                maxLines = 2
            )
        }
    }
}

@Composable
fun DatePickerDialogEvento(
    currentDate: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    minDate: Long = System.currentTimeMillis()
) {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = minDate

    if (currentDate.isNotEmpty()) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            sdf.parse(currentDate)?.let { calendar.time = it }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    var selectedYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Seleccionar fecha",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Selector de año
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedYear-- }) {
                        Icon(Icons.Default.ChevronLeft, null)
                    }
                    Text(
                        "$selectedYear",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { selectedYear++ }) {
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }

                // Selector de mes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        selectedMonth = if (selectedMonth == 0) 11 else selectedMonth - 1
                    }) {
                        Icon(Icons.Default.ChevronLeft, null)
                    }
                    Text(
                        getMonthName(selectedMonth),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = {
                        selectedMonth = if (selectedMonth == 11) 0 else selectedMonth + 1
                    }) {
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }

                // Selector de día
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (selectedDay > 1) selectedDay--
                    }) {
                        Icon(Icons.Default.ChevronLeft, null)
                    }
                    Text(
                        "$selectedDay",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = {
                        val maxDay = calendar.apply {
                            set(selectedYear, selectedMonth, 1)
                        }.getActualMaximum(Calendar.DAY_OF_MONTH)
                        if (selectedDay < maxDay) selectedDay++
                    }) {
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    calendar.set(selectedYear, selectedMonth, selectedDay, 0, 0, 0)
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                    onDateSelected(sdf.format(calendar.time))
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Celeste
                )
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = GrisMedio)
            }
        }
    )
}

@Composable
fun TimePickerDialog(
    currentTime: String,
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var hour by remember { mutableStateOf(10) }
    var minute by remember { mutableStateOf(0) }
    var isAM by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Seleccionar hora",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Selector de hora
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(onClick = {
                            hour = if (hour == 12) 1 else hour + 1
                        }) {
                            Icon(Icons.Default.KeyboardArrowUp, null)
                        }
                        Text(
                            String.format("%02d", hour),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = {
                            hour = if (hour == 1) 12 else hour - 1
                        }) {
                            Icon(Icons.Default.KeyboardArrowDown, null)
                        }
                    }

                    Text(":", style = MaterialTheme.typography.displaySmall)

                    // Selector de minutos
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(onClick = {
                            minute = (minute + 5) % 60
                        }) {
                            Icon(Icons.Default.KeyboardArrowUp, null)
                        }
                        Text(
                            String.format("%02d", minute),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = {
                            minute = if (minute < 5) 55 else minute - 5
                        }) {
                            Icon(Icons.Default.KeyboardArrowDown, null)
                        }
                    }

                    // AM/PM
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(onClick = { isAM = !isAM }) {
                            Icon(Icons.Default.KeyboardArrowUp, null)
                        }
                        Text(
                            if (isAM) "AM" else "PM",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { isAM = !isAM }) {
                            Icon(Icons.Default.KeyboardArrowDown, null)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val timeString = String.format("%02d:%02d %s", hour, minute, if (isAM) "AM" else "PM")
                    onTimeSelected(timeString)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Celeste
                )
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = GrisMedio)
            }
        }
    )
}

// ==================== MODELOS ====================

data class CategoriaEvento(
    val id: String,
    val nombre: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

// ==================== FUNCIONES AUXILIARES ====================

fun formatearFechaEvento(fecha: String): String {
    return try {
        val sdfInput = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        sdfInput.timeZone = TimeZone.getTimeZone("UTC")
        val date = sdfInput.parse(fecha)

        val sdfOutput = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es", "ES"))
        sdfOutput.format(date!!)
    } catch (e: Exception) {
        fecha
    }
}

fun getMonthName(month: Int): String {
    val months = arrayOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )
    return months[month]
}

fun convertirHoraA24h(hora12h: String): String {
    return try {
        val sdf12 = SimpleDateFormat("hh:mm a", Locale.US)
        val sdf24 = SimpleDateFormat("HH:mm", Locale.US)
        val date = sdf12.parse(hora12h)
        sdf24.format(date!!)
    } catch (e: Exception) {
        hora12h
    }
}