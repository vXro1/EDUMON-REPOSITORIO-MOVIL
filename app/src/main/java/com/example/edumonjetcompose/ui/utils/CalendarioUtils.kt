package com.example.edumonjetcompose.ui.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edumonjetcompose.AzulCielo
import com.example.edumonjetcompose.Naranja
import com.example.edumonjetcompose.models.EventoCalendario
import java.text.SimpleDateFormat
import java.util.*

// ==================== FUNCIONES DE FORMATEO ====================

fun formatearFechaCalendario(fechaISO: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
        val date = inputFormat.parse(fechaISO)
        date?.let { outputFormat.format(it) } ?: fechaISO
    } catch (e: Exception) {
        fechaISO
    }
}

fun formatearFechaCortaCalendario(fechaISO: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("EEEE, dd 'de' MMMM", Locale("es", "ES"))
        val date = inputFormat.parse(fechaISO)
        date?.let {
            outputFormat.format(it).replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
            }
        } ?: fechaISO
    } catch (e: Exception) {
        fechaISO
    }
}

fun obtenerNombreMesCalendario(mes: Int): String {
    val meses = listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )
    return meses.getOrNull(mes - 1) ?: "Mes $mes"
}

// ==================== FUNCIONES DE CÁLCULO ====================

fun obtenerDiasDelMesCalendario(mes: Int, anio: Int): Int {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.YEAR, anio)
    calendar.set(Calendar.MONTH, mes - 1)
    return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
}

fun obtenerPrimerDiaSemanaCalendario(mes: Int, anio: Int): Int {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.YEAR, anio)
    calendar.set(Calendar.MONTH, mes - 1)
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    return calendar.get(Calendar.DAY_OF_WEEK) - 1
}

fun verificarEsHoyCalendario(dia: Int, mes: Int, anio: Int): Boolean {
    val hoy = Calendar.getInstance()
    return dia == hoy.get(Calendar.DAY_OF_MONTH) &&
            mes == hoy.get(Calendar.MONTH) + 1 &&
            anio == hoy.get(Calendar.YEAR)
}

fun obtenerEventosDiaCalendario(
    dia: Int,
    mes: Int,
    anio: Int,
    eventos: List<EventoCalendario>
): List<EventoCalendario> {
    return eventos.filter { evento ->
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val fecha = inputFormat.parse(evento.fecha)
            val calendar = Calendar.getInstance()
            fecha?.let { calendar.time = it }

            calendar.get(Calendar.DAY_OF_MONTH) == dia &&
                    calendar.get(Calendar.MONTH) + 1 == mes &&
                    calendar.get(Calendar.YEAR) == anio
        } catch (e: Exception) {
            false
        }
    }.sortedBy { it.hora ?: "" }
}

// ==================== COMPONENTES COMUNES ====================

@Composable
fun LoadingCalendarioComun() {
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
fun EmptyCalendarioComun() {
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
                text = "No hay eventos",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
            Text(
                text = "Los eventos y tareas aparecerán aquí",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun ErrorCalendarioComun(mensaje: String, onRetry: () -> Unit) {
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
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = AzulCielo)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reintentar")
            }
        }
    }
}

@Composable
fun EstadisticasCardComun(estadisticas: Map<String, Int>) {
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
            EstadisticaItemComun(
                titulo = "Tareas",
                valor = estadisticas["totalTareas"] ?: 0,
                color = com.example.edumonjetcompose.Fucsia,
                icono = Icons.Default.Assignment
            )
            VerticalDivider(modifier = Modifier.height(50.dp))
            EstadisticaItemComun(
                titulo = "Eventos",
                valor = estadisticas["totalEventos"] ?: 0,
                color = AzulCielo,
                icono = Icons.Default.Event
            )
            VerticalDivider(modifier = Modifier.height(50.dp))
            EstadisticaItemComun(
                titulo = "Vencidas",
                valor = estadisticas["tareasVencidas"] ?: 0,
                color = Naranja,
                icono = Icons.Default.Warning
            )
        }
    }
}

@Composable
fun EstadisticaItemComun(
    titulo: String,
    valor: Int,
    color: Color,
    icono: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = valor.toString(),
            fontSize = 24.sp,
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
fun MesNavigatorComun(
    mesSeleccionado: Int,
    anioSeleccionado: Int,
    onMesAnterior: () -> Unit,
    onMesSiguiente: () -> Unit,
    onHoyClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMesAnterior) {
                Icon(
                    Icons.Default.KeyboardArrowLeft,
                    "Mes anterior",
                    tint = AzulCielo
                )
            }

            TextButton(onClick = onHoyClick) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Today,
                        contentDescription = null,
                        tint = AzulCielo,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Hoy",
                        color = AzulCielo,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            IconButton(onClick = onMesSiguiente) {
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    "Mes siguiente",
                    tint = AzulCielo
                )
            }
        }
    }
}