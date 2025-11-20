package com.example.edumonjetcompose.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edumonjetcompose.Fucsia
import com.example.edumonjetcompose.Naranja
import com.example.edumonjetcompose.R
import com.example.edumonjetcompose.VerdeLima
import com.example.edumonjetcompose.ui.theme.AzulCielo
import com.example.edumonjetcompose.ui.theme.Blanco
import com.example.edumonjetcompose.ui.theme.GrisNeutral
import java.text.SimpleDateFormat
import java.util.*

/**
 * Abre una URL en el navegador del dispositivo
 */
fun abrirUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
        // Intentar abrir con Chrome específicamente
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                setPackage("com.android.chrome")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * Extrae el userId del token JWT
 */
fun getUserIdFromToken(token: String): String {
    return try {
        // Limpiar el token (remover "Bearer " si existe)
        val cleanToken = token.removePrefix("Bearer ").trim()

        // Dividir el token en sus partes
        val parts = cleanToken.split(".")

        if (parts.size == 3) {
            // Decodificar la parte del payload (segunda parte)
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP))

            // Parsear el JSON
            val json = com.google.gson.JsonParser.parseString(payload).asJsonObject

            // Extraer el userId
            val userId = json.get("userId")?.asString ?: ""

            // Log para debugging
            android.util.Log.d("JWT_DECODE", "Token payload: $payload")
            android.util.Log.d("JWT_DECODE", "Extracted userId: $userId")

            userId
        } else {
            android.util.Log.e("JWT_DECODE", "Invalid token format - parts: ${parts.size}")
            ""
        }
    } catch (e: Exception) {
        android.util.Log.e("JWT_DECODE", "Error decoding token: ${e.message}", e)
        e.printStackTrace()
        ""
    }
}

/**
 * Verifica si una fecha ya pasó (está vencida)
 */
fun estaVencida(fechaLimite: String): Boolean {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val fecha = sdf.parse(fechaLimite)
        val ahora = Date()
        fecha?.before(ahora) ?: false
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

/**
 * Formatea una fecha ISO a formato legible en español
 * Ejemplo: "2024-01-15T10:30:00.000Z" -> "15 Ene 2024, 10:30"
 */
fun formatearFecha(fechaStr: String): String {
    return try {
        val sdfInput = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        sdfInput.timeZone = TimeZone.getTimeZone("UTC")
        val date = sdfInput.parse(fechaStr)

        val sdfOutput = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("es", "ES"))
        sdfOutput.format(date!!)
    } catch (e: Exception) {
        e.printStackTrace()
        fechaStr
    }
}

/**
 * Formatea el tamaño de un archivo en bytes a formato legible
 * Ejemplo: 1024 -> "1 KB", 1048576 -> "1.0 MB"
 */
fun formatearTamano(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }
}

/**
 * Formatea una fecha ISO a formato corto
 * Ejemplo: "2024-01-15T10:30:00.000Z" -> "15 Ene 2024"
 */
fun formatearFechaCorta(fechaStr: String): String {
    return try {
        val sdfInput = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        sdfInput.timeZone = TimeZone.getTimeZone("UTC")
        val date = sdfInput.parse(fechaStr)

        val sdfOutput = SimpleDateFormat("dd MMM yyyy", Locale("es", "ES"))
        sdfOutput.format(date!!)
    } catch (e: Exception) {
        e.printStackTrace()
        fechaStr
    }
}

/**
 * Calcula el tiempo transcurrido desde una fecha
 * Ejemplo: "Hace 2 horas", "Hace 3 días"
 */
fun tiempoTranscurrido(fechaStr: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val fecha = sdf.parse(fechaStr)
        val ahora = Date()

        val diff = ahora.time - (fecha?.time ?: 0)
        val segundos = diff / 1000
        val minutos = segundos / 60
        val horas = minutos / 60
        val dias = horas / 24

        when {
            dias > 0 -> "Hace ${dias.toInt()} día${if (dias > 1) "s" else ""}"
            horas > 0 -> "Hace ${horas.toInt()} hora${if (horas > 1) "s" else ""}"
            minutos > 0 -> "Hace ${minutos.toInt()} minuto${if (minutos > 1) "s" else ""}"
            else -> "Hace un momento"
        }
    } catch (e: Exception) {
        e.printStackTrace()
        "Fecha desconocida"
    }
}

/**
 * Valida si un email tiene formato correcto
 */
fun esEmailValido(email: String): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

/**
 * Valida si una contraseña cumple requisitos mínimos
 */
fun esContrasenaValida(password: String): Boolean {
    return password.length >= 6
}

/**
 * Obtiene las iniciales de un nombre
 * Ejemplo: "Juan Pérez" -> "JP"
 */
fun obtenerIniciales(nombre: String): String {
    return nombre.split(" ")
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .take(2)
        .joinToString("")
        .ifEmpty { "?" }
}
