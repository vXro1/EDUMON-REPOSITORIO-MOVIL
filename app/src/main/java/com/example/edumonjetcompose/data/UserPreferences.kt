package com.example.edumonjetcompose.data

import android.content.Context
import android.content.SharedPreferences
import com.example.edumonjetcompose.models.UserData
import com.google.gson.Gson

class UserPreferences(context: Context) {

    companion object {
        private const val PREFS_NAME = "edumon_preferences"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_PADRE_ID = "padre_id"
        private const val KEY_USER_DATA = "user_data"
        private const val KEY_ULTIMA_NOTIFICACION_ID = "ultima_notificacion_id"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val KEY_FCM_TOKEN_ENVIADO = "fcm_token_enviado"
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val gson = Gson()

    // ═══════════════════════════════════════════════════════════════
    // 🔐 TOKEN DE AUTENTICACIÓN
    // ═══════════════════════════════════════════════════════════════

    /**
     * Guarda el token de autenticación
     */
    fun saveToken(token: String) {
        sharedPreferences.edit().putString(KEY_TOKEN, token).apply()
        android.util.Log.d("UserPreferences", "✅ Token guardado")
    }

    /**
     * Obtiene el token de autenticación
     */
    fun getToken(): String? {
        return sharedPreferences.getString(KEY_TOKEN, null)
    }

    /**
     * Verifica si existe un token válido
     */
    fun hasValidToken(): Boolean {
        return !getToken().isNullOrEmpty()
    }

    /**
     * ✅ NUEVO: Limpia solo el token de autenticación
     */
    fun clearToken() {
        sharedPreferences.edit().remove(KEY_TOKEN).apply()
        android.util.Log.d("UserPreferences", "🗑️ Token limpiado")
    }

    // ═══════════════════════════════════════════════════════════════
    // 👤 DATOS DEL USUARIO
    // ═══════════════════════════════════════════════════════════════

    /**
     * Guarda el ID del usuario
     */
    fun saveUserId(userId: String) {
        sharedPreferences.edit().putString(KEY_USER_ID, userId).apply()
        android.util.Log.d("UserPreferences", "✅ UserId guardado: $userId")
    }

    /**
     * Obtiene el ID del usuario
     */
    fun getUserId(): String? {
        return sharedPreferences.getString(KEY_USER_ID, null)
    }

    /**
     * Guarda el ID del padre
     */
    fun savePadreId(padreId: String) {
        sharedPreferences.edit().putString(KEY_PADRE_ID, padreId).apply()
        android.util.Log.d("UserPreferences", "✅ PadreId guardado: $padreId")
    }

    /**
     * Obtiene el ID del padre
     */
    fun getPadreId(): String? {
        return sharedPreferences.getString(KEY_PADRE_ID, null)
    }

    /**
     * Guarda los datos completos del usuario
     */
    fun saveUserData(userData: UserData) {
        val userDataJson = gson.toJson(userData)
        sharedPreferences.edit().putString(KEY_USER_DATA, userDataJson).apply()
        android.util.Log.d("UserPreferences", "✅ UserData guardado: ${userData.nombre}")
    }

    /**
     * Obtiene los datos completos del usuario
     */
    fun getUserData(): UserData? {
        val userDataJson = sharedPreferences.getString(KEY_USER_DATA, null)
        return if (!userDataJson.isNullOrEmpty()) {
            try {
                gson.fromJson(userDataJson, UserData::class.java)
            } catch (e: Exception) {
                android.util.Log.e("UserPreferences", "Error al parsear UserData: ${e.message}")
                null
            }
        } else {
            null
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 🔥 FCM TOKEN (FIREBASE CLOUD MESSAGING)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Guarda el token FCM localmente
     */
    fun saveFcmToken(token: String) {
        sharedPreferences.edit().putString(KEY_FCM_TOKEN, token).apply()
        android.util.Log.d("UserPreferences", "✅ FCM Token guardado localmente")
    }

    /**
     * Obtiene el token FCM guardado
     */
    fun getFcmToken(): String? {
        return sharedPreferences.getString(KEY_FCM_TOKEN, null)
    }

    /**
     * Marca el token FCM como enviado al backend
     */
    fun markFcmTokenEnviado(enviado: Boolean = true) {
        sharedPreferences.edit().putBoolean(KEY_FCM_TOKEN_ENVIADO, enviado).apply()
        android.util.Log.d("UserPreferences", "✅ FCM Token marcado como ${if (enviado) "enviado" else "no enviado"}")
    }

    /**
     * Verifica si el token FCM ya fue enviado al backend
     */
    fun isFcmTokenEnviado(): Boolean {
        return sharedPreferences.getBoolean(KEY_FCM_TOKEN_ENVIADO, false)
    }

    /**
     * Limpia el token FCM y su estado
     */
    fun clearFcmToken() {
        sharedPreferences.edit().apply {
            remove(KEY_FCM_TOKEN)
            remove(KEY_FCM_TOKEN_ENVIADO)
            apply()
        }
        android.util.Log.d("UserPreferences", "🗑️ FCM Token limpiado")
    }

    /**
     * Verifica si hay un token FCM pendiente de enviar
     * (existe token pero no se ha enviado al backend)
     */
    fun hasPendingFcmToken(): Boolean {
        val token = getFcmToken()
        val enviado = isFcmTokenEnviado()
        return !token.isNullOrEmpty() && !enviado
    }

    // ═══════════════════════════════════════════════════════════════
    // 🔔 GESTIÓN DE NOTIFICACIONES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Guarda el ID de la última notificación recibida
     * (usado para detectar nuevas notificaciones)
     */
    fun saveUltimaNotificacionId(notificacionId: String) {
        sharedPreferences.edit().putString(KEY_ULTIMA_NOTIFICACION_ID, notificacionId).apply()
        android.util.Log.d("UserPreferences", "✅ Última notificación guardada: $notificacionId")
    }

    /**
     * Obtiene el ID de la última notificación recibida
     */
    fun getUltimaNotificacionId(): String? {
        return sharedPreferences.getString(KEY_ULTIMA_NOTIFICACION_ID, null)
    }

    /**
     * Limpia el ID de la última notificación
     */
    fun clearUltimaNotificacionId() {
        sharedPreferences.edit().remove(KEY_ULTIMA_NOTIFICACION_ID).apply()
        android.util.Log.d("UserPreferences", "🗑️ Última notificación limpiada")
    }

    /**
     * Habilita o deshabilita las notificaciones
     */
    fun setNotificationsEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
        android.util.Log.d("UserPreferences", "🔔 Notificaciones: ${if (enabled) "Habilitadas" else "Deshabilitadas"}")
    }

    /**
     * Verifica si las notificaciones están habilitadas
     */
    fun areNotificationsEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    /**
     * Guarda el timestamp de la última sincronización de notificaciones
     */
    fun saveLastSyncTime(timestamp: Long = System.currentTimeMillis()) {
        sharedPreferences.edit().putLong(KEY_LAST_SYNC_TIME, timestamp).apply()
        android.util.Log.d("UserPreferences", "⏰ Última sincronización: $timestamp")
    }

    /**
     * Obtiene el timestamp de la última sincronización
     */
    fun getLastSyncTime(): Long {
        return sharedPreferences.getLong(KEY_LAST_SYNC_TIME, 0L)
    }

    /**
     * Verifica si es necesario sincronizar (han pasado más de X minutos)
     */
    fun needsSync(intervalMinutes: Int = 5): Boolean {
        val lastSync = getLastSyncTime()
        val currentTime = System.currentTimeMillis()
        val diffMinutes = (currentTime - lastSync) / (1000 * 60)
        return diffMinutes >= intervalMinutes
    }

    // ═══════════════════════════════════════════════════════════════
    // 🗑️ LIMPIEZA DE DATOS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Limpia solo los datos de sesión (mantiene preferencias de notificaciones y FCM token)
     */
    fun clearSessionData() {
        val notificationsEnabled = areNotificationsEnabled()
        val fcmToken = getFcmToken()

        sharedPreferences.edit().apply {
            remove(KEY_TOKEN)
            remove(KEY_USER_ID)
            remove(KEY_PADRE_ID)
            remove(KEY_USER_DATA)
            remove(KEY_ULTIMA_NOTIFICACION_ID)
            remove(KEY_LAST_SYNC_TIME)
            remove(KEY_FCM_TOKEN_ENVIADO)
            apply()
        }

        // Restaurar preferencias
        setNotificationsEnabled(notificationsEnabled)

        // Restaurar FCM token
        if (!fcmToken.isNullOrEmpty()) {
            saveFcmToken(fcmToken)
        }

        android.util.Log.d("UserPreferences", "🗑️ Datos de sesión limpiados (FCM token preservado)")
    }

    /**
     * Limpia todos los datos almacenados (incluye preferencias y FCM token)
     */
    fun clearAll() {
        sharedPreferences.edit().clear().apply()
        android.util.Log.d("UserPreferences", "🗑️ Todos los datos limpiados (incluido FCM token)")
    }

    /**
     * Limpia solo los datos relacionados con notificaciones
     */
    fun clearNotificationData() {
        sharedPreferences.edit().apply {
            remove(KEY_ULTIMA_NOTIFICACION_ID)
            remove(KEY_LAST_SYNC_TIME)
            apply()
        }
        android.util.Log.d("UserPreferences", "🗑️ Datos de notificaciones limpiados")
    }

    // ═══════════════════════════════════════════════════════════════
    // 📊 INFORMACIÓN DE DEBUG
    // ═══════════════════════════════════════════════════════════════

    /**
     * Imprime toda la información almacenada (para debug)
     */
    fun printDebugInfo() {
        android.util.Log.d("UserPreferences", """
            ═══════════════════════════════════════════════════
            📊 UserPreferences Debug Info
            ═══════════════════════════════════════════════════
            🔐 Token: ${if (hasValidToken()) "✅ Presente" else "❌ Ausente"}
            👤 UserId: ${getUserId() ?: "❌ No guardado"}
            👨‍👩‍👧 PadreId: ${getPadreId() ?: "❌ No guardado"}
            📧 UserData: ${getUserData()?.correo ?: "❌ No guardado"}
            🔥 FCM Token: ${if (!getFcmToken().isNullOrEmpty()) "✅ Presente (${getFcmToken()?.take(20)}...)" else "❌ Ausente"}
            📤 FCM Enviado: ${if (isFcmTokenEnviado()) "✅ Sí" else "❌ No"}
            🔄 FCM Pendiente: ${if (hasPendingFcmToken()) "⚠️ Sí (debe enviarse)" else "✅ No"}
            🔔 Última Notif: ${getUltimaNotificacionId() ?: "❌ No hay"}
            ⏰ Última Sync: ${getLastSyncTime()}
            🔔 Notif Enabled: ${areNotificationsEnabled()}
            🔄 Needs Sync: ${needsSync()}
            ═══════════════════════════════════════════════════
        """.trimIndent())
    }

    /**
     * Obtiene un resumen de la información almacenada
     */
    fun getSummary(): String {
        return """
            Token: ${if (hasValidToken()) "✓" else "✗"}
            UserId: ${getUserId()?.take(8) ?: "null"}
            FCM Token: ${if (!getFcmToken().isNullOrEmpty()) "✓" else "✗"}
            FCM Enviado: ${if (isFcmTokenEnviado()) "✓" else "✗"}
            Notificaciones: ${if (areNotificationsEnabled()) "✓" else "✗"}
            Última sync: ${if (getLastSyncTime() > 0) "✓" else "✗"}
        """.trimIndent()
    }

    // ═══════════════════════════════════════════════════════════════
    // 🔧 UTILIDADES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Verifica si el usuario está completamente configurado
     */
    fun isUserFullyConfigured(): Boolean {
        val userData = getUserData()
        return hasValidToken() &&
                !getUserId().isNullOrEmpty() &&
                userData != null &&
                !userData.cedula.isNullOrBlank() &&
                !userData.fotoPerfilUrl.isNullOrBlank()
    }

    /**
     * Exporta todas las preferencias a un Map (útil para backup)
     */
    fun exportPreferences(): Map<String, Any?> {
        return mapOf(
            "token" to getToken(),
            "userId" to getUserId(),
            "padreId" to getPadreId(),
            "userData" to getUserData(),
            "fcmToken" to getFcmToken(),
            "fcmTokenEnviado" to isFcmTokenEnviado(),
            "ultimaNotificacionId" to getUltimaNotificacionId(),
            "notificationsEnabled" to areNotificationsEnabled(),
            "lastSyncTime" to getLastSyncTime()
        )
    }

    /**
     * Verifica si hay datos almacenados
     */
    fun hasStoredData(): Boolean {
        return sharedPreferences.all.isNotEmpty()
    }

    /**
     * Obtiene el tamaño aproximado de los datos almacenados (en KB)
     */
    fun getStorageSize(): Float {
        val allPrefs = sharedPreferences.all
        val totalChars = allPrefs.values
            .filterIsInstance<String>()
            .sumOf { it.length }
        return totalChars / 1024f
    }
}