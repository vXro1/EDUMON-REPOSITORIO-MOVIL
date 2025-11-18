package com.example.edumonjetcompose.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.edumonjetcompose.MainActivity
import com.example.edumonjetcompose.R
import com.example.edumonjetcompose.data.UserPreferences
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "edumon_notificaciones"
        const val CHANNEL_NAME = "Notificaciones EduMon"
        const val CHANNEL_DESCRIPTION = "Notificaciones de tareas, entregas y eventos"
        const val ACTION_NUEVA_NOTIFICACION = "com.example.edumonjetcompose.NUEVA_NOTIFICACION"
        private const val TAG = "FCMService"
    }

    private val userPreferences by lazy { UserPreferences(applicationContext) }

    // ═══════════════════════════════════════════════════════════════
    // 🚀 INICIALIZACIÓN
    // ═══════════════════════════════════════════════════════════════

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🚀 MyFirebaseMessagingService creado")
        crearCanalNotificacion()
    }

    // ═══════════════════════════════════════════════════════════════
    // 🔥 NUEVO TOKEN FCM GENERADO
    // ═══════════════════════════════════════════════════════════════

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "🔥 Nuevo FCM Token generado: ${token.take(30)}...")

        userPreferences.saveFcmToken(token)
        userPreferences.markFcmTokenEnviado(false)

        // TODO: Implementar cuando el backend tenga el endpoint
        // enviarTokenAlBackend(token)
    }

    // ═══════════════════════════════════════════════════════════════
    // 📩 MENSAJE RECIBIDO (FOREGROUND Y BACKGROUND)
    // ═══════════════════════════════════════════════════════════════

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, """
            📩 Mensaje FCM recibido
               From: ${remoteMessage.from}
               MessageId: ${remoteMessage.messageId}
               Priority: ${remoteMessage.priority}
               Data: ${remoteMessage.data}
               Notification: ${remoteMessage.notification?.let {
            "title=${it.title}, body=${it.body}"
        } ?: "null"}
        """.trimIndent())

        // ✅ IMPORTANTE: Siempre procesar el mensaje, sin importar el estado de la app
        procesarMensaje(remoteMessage)
    }

    // ═══════════════════════════════════════════════════════════════
    // 🔧 PROCESAR MENSAJE
    // ═══════════════════════════════════════════════════════════════

    private fun procesarMensaje(remoteMessage: RemoteMessage) {
        // Verificar si las notificaciones están habilitadas
        if (!userPreferences.areNotificationsEnabled()) {
            Log.d(TAG, "🔕 Notificaciones deshabilitadas por el usuario")
            return
        }

        // Extraer datos (priorizar 'data' sobre 'notification')
        val titulo = remoteMessage.data["titulo"]
            ?: remoteMessage.notification?.title
            ?: "Nueva notificación"

        val mensaje = remoteMessage.data["mensaje"]
            ?: remoteMessage.notification?.body
            ?: ""

        val tipo = remoteMessage.data["tipo"] ?: "info"
        val notificacionId = remoteMessage.data["notificacionId"]
        val referenciaId = remoteMessage.data["referenciaId"]
        val referenciaModelo = remoteMessage.data["referenciaModelo"]

        Log.d(TAG, """
            📋 Contenido procesado:
               Título: $titulo
               Mensaje: $mensaje
               Tipo: $tipo
               NotificacionId: $notificacionId
               ReferenciaId: $referenciaId
               ReferenciaModelo: $referenciaModelo
        """.trimIndent())

        // Mostrar notificación
        mostrarNotificacion(
            titulo = titulo,
            mensaje = mensaje,
            tipo = tipo,
            notificacionId = notificacionId,
            referenciaId = referenciaId,
            referenciaModelo = referenciaModelo
        )

        // Emitir broadcast si la app está en foreground
        emitirBroadcastNuevaNotificacion()

        // Guardar ID de última notificación
        notificacionId?.let {
            userPreferences.saveUltimaNotificacionId(it)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 🔔 MOSTRAR NOTIFICACIÓN
    // ═══════════════════════════════════════════════════════════════

    private fun mostrarNotificacion(
        titulo: String,
        mensaje: String,
        tipo: String,
        notificacionId: String?,
        referenciaId: String?,
        referenciaModelo: String?
    ) {
        try {
            // Asegurar que el canal existe
            crearCanalNotificacion()

            // Intent para abrir la app
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("openNotificaciones", true)
                putExtra("notificacionId", notificacionId)
                putExtra("tipo", tipo)
                putExtra("referenciaId", referenciaId)
                putExtra("referenciaModelo", referenciaModelo)
            }

            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val pendingIntent = PendingIntent.getActivity(
                this,
                System.currentTimeMillis().toInt(),
                intent,
                pendingIntentFlags
            )

            // Sonido de notificación
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            // Construir notificación con configuración completa
            val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.edumonavatar1)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setColor(getColor(R.color.colorAzulCielo))
                .apply {
                    // Icono de notificación según tipo
                    when (tipo) {
                        "tarea" -> setSubText("📚 Tarea")
                        "entrega" -> setSubText("📤 Entrega")
                        "calificacion" -> setSubText("⭐ Calificación")
                        "recordatorio" -> setSubText("⏰ Recordatorio")
                        "evento" -> setSubText("📅 Evento")
                        else -> setSubText("📢 Notificación")
                    }
                }

            // Mostrar notificación
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationIdInt = notificacionId?.hashCode() ?: System.currentTimeMillis().toInt()

            notificationManager.notify(notificationIdInt, notificationBuilder.build())

            Log.d(TAG, "✅ Notificación mostrada con ID: $notificationIdInt")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al mostrar notificación: ${e.message}", e)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 📢 EMITIR BROADCAST PARA ACTUALIZAR UI
    // ═══════════════════════════════════════════════════════════════

    private fun emitirBroadcastNuevaNotificacion() {
        try {
            val intent = Intent(ACTION_NUEVA_NOTIFICACION)
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
            Log.d(TAG, "📢 Broadcast emitido: $ACTION_NUEVA_NOTIFICACION")
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Error al emitir broadcast: ${e.message}")
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 🔧 CANAL DE NOTIFICACIÓN (Android 8.0+)
    // ═══════════════════════════════════════════════════════════════

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                // Verificar si el canal ya existe
                if (notificationManager.getNotificationChannel(CHANNEL_ID) != null) {
                    Log.d(TAG, "✓ Canal de notificación ya existe")
                    return
                }

                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESCRIPTION
                    enableLights(true)
                    enableVibration(true)
                    setShowBadge(true)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }

                notificationManager.createNotificationChannel(channel)
                Log.d(TAG, "✅ Canal de notificación creado: $CHANNEL_ID")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error al crear canal: ${e.message}", e)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 🛑 DESTRUCCIÓN
    // ═══════════════════════════════════════════════════════════════

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "🛑 MyFirebaseMessagingService destruido")
    }
}