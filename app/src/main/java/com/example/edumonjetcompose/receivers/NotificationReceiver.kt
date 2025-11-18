package com.example.edumonjetcompose.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.edumonjetcompose.data.UserPreferences
import com.example.edumonjetcompose.network.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d("NotificationReceiver", "📬 Acción recibida: ${intent.action}")

        when (intent.action) {
            "com.example.edumonjetcompose.NOTIFICATION_ACTION" -> {
                val notificacionId = intent.getStringExtra("notificacion_id")
                val action = intent.getStringExtra("action")

                android.util.Log.d("NotificationReceiver", "   ID: $notificacionId")
                android.util.Log.d("NotificationReceiver", "   Acción: $action")

                when (action) {
                    "MARCAR_LEIDA" -> marcarComoLeida(context, notificacionId)
                    "ELIMINAR" -> eliminarNotificacion(context, notificacionId)
                }
            }
        }
    }

    private fun marcarComoLeida(context: Context, notificacionId: String?) {
        if (notificacionId == null) return

        val userPreferences = UserPreferences(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = userPreferences.getToken()
                if (!token.isNullOrEmpty()) {
                    ApiService.marcarComoLeida(token, notificacionId)
                    android.util.Log.d("NotificationReceiver", "✅ Notificación marcada como leída")
                }
            } catch (e: Exception) {
                android.util.Log.e("NotificationReceiver", "❌ Error al marcar leída: ${e.message}")
            }
        }
    }

    private fun eliminarNotificacion(context: Context, notificacionId: String?) {
        if (notificacionId == null) return

        val userPreferences = UserPreferences(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = userPreferences.getToken()
                if (!token.isNullOrEmpty()) {
                    ApiService.deleteNotificacion(token, notificacionId)
                    android.util.Log.d("NotificationReceiver", "✅ Notificación eliminada")
                }
            } catch (e: Exception) {
                android.util.Log.e("NotificationReceiver", "❌ Error al eliminar: ${e.message}")
            }
        }
    }
}