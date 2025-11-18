package com.example.edumonjetcompose.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.edumonjetcompose.R
import com.example.edumonjetcompose.data.UserPreferences
import com.example.edumonjetcompose.network.ApiService
import kotlinx.coroutines.*

class NotificationForegroundService : Service() {

    companion object {
        private const val CHANNEL_ID = "edumon_foreground_service"
        private const val NOTIFICATION_ID = 9999
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var syncJob: Job? = null
    private lateinit var userPreferences: UserPreferences
    private lateinit var notificationService: NotificationService

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("ForegroundService", "🚀 Servicio iniciado")

        userPreferences = UserPreferences(this)
        notificationService = NotificationService(this)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        iniciarSincronizacionPeriodica()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("ForegroundService", "📨 onStartCommand")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sincronización Edumon",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantiene las notificaciones actualizadas"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Edumon")
            .setContentText("Sincronizando notificaciones...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun iniciarSincronizacionPeriodica() {
        syncJob?.cancel()

        syncJob = serviceScope.launch {
            while (isActive) {
                try {
                    sincronizarNotificaciones()
                } catch (e: Exception) {
                    android.util.Log.e("ForegroundService", "Error en sincronización: ${e.message}")
                }

                // Esperar 5 minutos antes de la próxima sincronización
                delay(5 * 60 * 1000)
            }
        }
    }

    private suspend fun sincronizarNotificaciones() {
        try {
            val token = userPreferences.getToken()
            if (token.isNullOrEmpty()) {
                android.util.Log.d("ForegroundService", "⚠️ No hay token")
                return
            }

            val response = ApiService.getMisNotificaciones(
                token = token,
                page = 1,
                limit = 50,
                leida = false
            )

            if (response.isSuccessful) {
                val body = response.body()
                val notificacionesArray = body?.getAsJsonArray("notificaciones")
                val ultimaNotificacionId = userPreferences.getUltimaNotificacionId()

                var nuevasNotificaciones = 0

                notificacionesArray?.forEach { element ->
                    val notifJson = element.asJsonObject
                    val notifId = notifJson.get("_id")?.asString ?: return@forEach

                    if (ultimaNotificacionId == null || notifId != ultimaNotificacionId) {
                        val notificacion = com.example.edumonjetcompose.models.Notificacion(
                            id = notifId,
                            usuarioId = notifJson.get("usuarioId")?.asString ?: "",
                            titulo = notifJson.get("titulo")?.asString ?: "Nueva notificación",
                            mensaje = notifJson.get("mensaje")?.asString ?: "",
                            tipo = notifJson.get("tipo")?.asString ?: "info",
                            leido = notifJson.get("leido")?.asBoolean ?: false,
                            fecha = notifJson.get("fecha")?.asString ?: "",
                            referenciaId = notifJson.get("referenciaId")?.asString,
                            referenciaModelo = notifJson.get("referenciaModelo")?.asString
                        )

                        notificationService.mostrarNotificacion(notificacion)
                        nuevasNotificaciones++
                    }
                }

                if (notificacionesArray != null && notificacionesArray.size() > 0) {
                    val primeraNotif = notificacionesArray.get(0).asJsonObject
                    val primerNotifId = primeraNotif.get("_id")?.asString
                    primerNotifId?.let { userPreferences.saveUltimaNotificacionId(it) }
                }

                android.util.Log.d("ForegroundService", "✅ Sync completo: $nuevasNotificaciones nuevas")
            }
        } catch (e: Exception) {
            android.util.Log.e("ForegroundService", "❌ Error en sync: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d("ForegroundService", "🛑 Servicio detenido")
        syncJob?.cancel()
        serviceScope.cancel()
    }
}