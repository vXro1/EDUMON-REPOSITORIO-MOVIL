package com.example.edumonjetcompose.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.edumonjetcompose.data.UserPreferences
import com.example.edumonjetcompose.network.ApiService
import com.example.edumonjetcompose.services.NotificationService
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val userPreferences = UserPreferences(context)
    private val notificationService = NotificationService(context)
    private val gson = Gson()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val token = userPreferences.getToken()

            if (token.isNullOrEmpty()) {
                android.util.Log.d("NotificationWorker", "No hay token, saltando sincronización")
                return@withContext Result.success()
            }

            // Obtener notificaciones no leídas desde el servidor
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

                    // Solo mostrar notificaciones nuevas
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

                        // Mostrar notificación
                        notificationService.mostrarNotificacion(notificacion)
                        nuevasNotificaciones++
                    }
                }

                // Guardar ID de la última notificación procesada
                if (notificacionesArray != null && notificacionesArray.size() > 0) {
                    val primeraNotif = notificacionesArray.get(0).asJsonObject
                    val primerNotifId = primeraNotif.get("_id")?.asString
                    primerNotifId?.let { userPreferences.saveUltimaNotificacionId(it) }
                }

                android.util.Log.d("NotificationWorker", "✅ Sincronización exitosa: $nuevasNotificaciones nuevas")
                Result.success()
            } else {
                android.util.Log.e("NotificationWorker", "❌ Error en respuesta: ${response.code()}")
                Result.retry()
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationWorker", "❌ Error en Worker", e)
            Result.retry()
        }
    }
}