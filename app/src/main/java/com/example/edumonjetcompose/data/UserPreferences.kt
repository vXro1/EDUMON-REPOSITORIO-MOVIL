package com.example.edumonjetcompose.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.edumonjetcompose.ui.UserData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

// Extension para crear el DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {

    companion object {
        // Keys para DataStore
        private val TOKEN_KEY = stringPreferencesKey("user_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val PADRE_ID_KEY = stringPreferencesKey("padre_id") // 🔥 NUEVO
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
        private val USER_LASTNAME_KEY = stringPreferencesKey("user_lastname")
        private val USER_CEDULA_KEY = stringPreferencesKey("user_cedula")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
        private val USER_PHONE_KEY = stringPreferencesKey("user_phone")
        private val USER_ROL_KEY = stringPreferencesKey("user_rol")
        private val USER_PHOTO_KEY = stringPreferencesKey("user_photo")
        private val USER_STATUS_KEY = stringPreferencesKey("user_status")
        private val IS_LOGGED_IN_KEY = booleanPreferencesKey("is_logged_in")
    }

    // ==================== TOKEN ====================

    /**
     * Guardar token de autenticación
     */
    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[IS_LOGGED_IN_KEY] = true
        }
    }

    /**
     * Obtener token guardado
     */
    suspend fun getToken(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[TOKEN_KEY]
        }.firstOrNull()
    }

    /**
     * Flow para observar cambios en el token
     */
    val tokenFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[TOKEN_KEY]
    }

    // ==================== PADRE ID ==================== 🔥 NUEVO

    /**
     * Guardar ID del padre (para entregas de tareas)
     */
    suspend fun savePadreId(padreId: String) {
        context.dataStore.edit { preferences ->
            preferences[PADRE_ID_KEY] = padreId
        }
    }

    /**
     * Obtener ID del padre guardado
     */
    suspend fun getPadreId(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[PADRE_ID_KEY]
        }.firstOrNull()
    }

    /**
     * Flow para observar cambios en el padreId
     */
    val padreIdFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PADRE_ID_KEY]
    }

    /**
     * Limpiar solo el padreId
     */
    suspend fun clearPadreId() {
        context.dataStore.edit { preferences ->
            preferences.remove(PADRE_ID_KEY)
        }
    }

    // ==================== USER DATA ====================

    /**
     * Guardar toda la información del usuario
     */
    suspend fun saveUserData(userData: UserData) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userData.id
            preferences[PADRE_ID_KEY] = userData.id // 🔥 NUEVO - También guardar como padreId
            preferences[USER_NAME_KEY] = userData.nombre
            preferences[USER_LASTNAME_KEY] = userData.apellido
            userData.cedula?.let { preferences[USER_CEDULA_KEY] = it }
            preferences[USER_EMAIL_KEY] = userData.correo
            preferences[USER_PHONE_KEY] = userData.telefono
            preferences[USER_ROL_KEY] = userData.rol
            userData.fotoPerfilUrl?.let { preferences[USER_PHOTO_KEY] = it }
            preferences[USER_STATUS_KEY] = userData.estado
        }
    }

    /**
     * Obtener toda la información del usuario como UserData
     */
    suspend fun getUserData(): UserData? {
        val preferences = context.dataStore.data.firstOrNull() ?: return null

        val id = preferences[USER_ID_KEY] ?: return null
        val nombre = preferences[USER_NAME_KEY] ?: return null
        val apellido = preferences[USER_LASTNAME_KEY] ?: return null
        val correo = preferences[USER_EMAIL_KEY] ?: return null
        val telefono = preferences[USER_PHONE_KEY] ?: return null
        val rol = preferences[USER_ROL_KEY] ?: return null
        val estado = preferences[USER_STATUS_KEY] ?: "activo"

        return UserData(
            id = id,
            nombre = nombre,
            apellido = apellido,
            cedula = preferences[USER_CEDULA_KEY],
            correo = correo,
            telefono = telefono,
            rol = rol,
            fotoPerfilUrl = preferences[USER_PHOTO_KEY],
            estado = estado
        )
    }

    /**
     * Flow para observar cambios en los datos del usuario
     */
    val userDataFlow: Flow<UserData?> = context.dataStore.data.map { preferences ->
        val id = preferences[USER_ID_KEY] ?: return@map null
        val nombre = preferences[USER_NAME_KEY] ?: return@map null
        val apellido = preferences[USER_LASTNAME_KEY] ?: return@map null
        val correo = preferences[USER_EMAIL_KEY] ?: return@map null
        val telefono = preferences[USER_PHONE_KEY] ?: return@map null
        val rol = preferences[USER_ROL_KEY] ?: return@map null
        val estado = preferences[USER_STATUS_KEY] ?: "activo"

        UserData(
            id = id,
            nombre = nombre,
            apellido = apellido,
            cedula = preferences[USER_CEDULA_KEY],
            correo = correo,
            telefono = telefono,
            rol = rol,
            fotoPerfilUrl = preferences[USER_PHOTO_KEY],
            estado = estado
        )
    }

    // ==================== INDIVIDUAL FIELDS ====================

    /**
     * Guardar información básica del usuario (legacy)
     */
    suspend fun saveUserInfo(userId: String, userName: String, userRol: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
            preferences[PADRE_ID_KEY] = userId // 🔥 NUEVO - También como padreId
            preferences[USER_NAME_KEY] = userName
            preferences[USER_ROL_KEY] = userRol
        }
    }

    /**
     * Obtener ID del usuario
     */
    suspend fun getUserId(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[USER_ID_KEY]
        }.firstOrNull()
    }

    /**
     * Obtener nombre del usuario
     */
    suspend fun getUserName(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[USER_NAME_KEY]
        }.firstOrNull()
    }

    /**
     * Obtener apellido del usuario
     */
    suspend fun getUserLastName(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[USER_LASTNAME_KEY]
        }.firstOrNull()
    }

    /**
     * Obtener nombre completo del usuario
     */
    suspend fun getUserFullName(): String? {
        val preferences = context.dataStore.data.firstOrNull() ?: return null
        val nombre = preferences[USER_NAME_KEY] ?: return null
        val apellido = preferences[USER_LASTNAME_KEY] ?: return null
        return "$nombre $apellido"
    }

    /**
     * Obtener cédula del usuario
     */
    suspend fun getUserCedula(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[USER_CEDULA_KEY]
        }.firstOrNull()
    }

    /**
     * Obtener email del usuario
     */
    suspend fun getUserEmail(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[USER_EMAIL_KEY]
        }.firstOrNull()
    }

    /**
     * Obtener teléfono del usuario
     */
    suspend fun getUserPhone(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[USER_PHONE_KEY]
        }.firstOrNull()
    }

    /**
     * Obtener rol del usuario
     */
    suspend fun getUserRol(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[USER_ROL_KEY]
        }.firstOrNull()
    }

    /**
     * Obtener foto de perfil del usuario
     */
    suspend fun getUserPhoto(): String? {
        return context.dataStore.data.map { preferences ->
            preferences[USER_PHOTO_KEY]
        }.firstOrNull()
    }

    /**
     * Actualizar foto de perfil
     */
    suspend fun updateUserPhoto(photoUrl: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_PHOTO_KEY] = photoUrl
        }
    }

    /**
     * Actualizar cédula del usuario
     */
    suspend fun updateUserCedula(cedula: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_CEDULA_KEY] = cedula
        }
    }

    /**
     * Verificar si el usuario está logueado
     */
    suspend fun isLoggedIn(): Boolean {
        return context.dataStore.data.map { preferences ->
            preferences[IS_LOGGED_IN_KEY] ?: false
        }.firstOrNull() ?: false
    }

    /**
     * Flow para observar el estado de login
     */
    val isLoggedInFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_LOGGED_IN_KEY] ?: false
    }

    // ==================== LOGOUT ====================

    /**
     * Limpiar todos los datos (logout completo)
     */
    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    /**
     * Limpiar solo el token (mantiene datos del usuario)
     */
    suspend fun clearToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
            preferences.remove(PADRE_ID_KEY) // 🔥 NUEVO - También limpiar padreId
            preferences[IS_LOGGED_IN_KEY] = false
        }
    }

    // ==================== UTILIDADES ====================

    /**
     * Verificar si el usuario tiene cédula registrada
     */
    suspend fun hasCedula(): Boolean {
        return context.dataStore.data.map { preferences ->
            !preferences[USER_CEDULA_KEY].isNullOrBlank()
        }.firstOrNull() ?: false
    }

    /**
     * Verificar si el usuario tiene foto de perfil
     */
    suspend fun hasPhoto(): Boolean {
        return context.dataStore.data.map { preferences ->
            !preferences[USER_PHOTO_KEY].isNullOrBlank()
        }.firstOrNull() ?: false
    }

    /**
     * Verificar si el perfil está completo
     */
    suspend fun isProfileComplete(): Boolean {
        return hasCedula() && hasPhoto()
    }
}