package com.example.edumonjetcompose.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.edumonjetcompose.MainActivity
import com.example.edumonjetcompose.R
import com.example.edumonjetcompose.models.Notificacion

class NotificationService(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "edumon_notifications"
        private const val CHANNEL_NAME = "Notificaciones Edumon"
        private const val CHANNEL_DESCRIPTION = "Notificaciones de cursos, tareas y mensajes"

        // IDs de notificación
        private const val NOTIFICATION_ID_BASE = 1000
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun mostrarNotificacion(notificacion: Notificacion) {
        // Intent para abrir la app al tocar la notificación
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notificacion_id", notificacion.id)
            putExtra("tipo", notificacion.tipo)
            putExtra("referencia_id", notificacion.referenciaId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificacion.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Icono según tipo
        val iconoNotificacion = when (notificacion.tipo) {
            "curso" -> R.drawable.ic_launcher_foreground
            "tarea" -> R.drawable.ic_launcher_foreground
            "mensaje" -> R.drawable.ic_launcher_foreground
            else -> R.drawable.ic_launcher_foreground
        }

        // Construir notificación
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(iconoNotificacion)
            .setContentTitle(notificacion.titulo)
            .setContentText(notificacion.mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificacion.mensaje))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        // Mostrar notificación
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(NOTIFICATION_ID_BASE + notificacion.id.hashCode(), notification)
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationService", "Error al mostrar notificación: ${e.message}")
        }
    }

    fun cancelarNotificacion(notificacionId: String) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(NOTIFICATION_ID_BASE + notificacionId.hashCode())
    }

    fun cancelarTodasNotificaciones() {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancelAll()
    }
}