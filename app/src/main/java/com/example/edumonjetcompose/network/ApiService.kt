package com.example.edumonjetcompose.network

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.io.File
import java.util.concurrent.TimeUnit
import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream

private const val BASE_URL = "https://backend-edumon.onrender.com/api/"

private val loggingInterceptor = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY
}
private val okHttpClient = OkHttpClient.Builder()
    .addInterceptor(loggingInterceptor)
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .build()
private val retrofit: Retrofit by lazy {
    Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}
interface ApiRoutes {

    // ==================== AUTH ====================
    @POST("auth/login")
    suspend fun login(@Body body: JsonObject): Response<JsonObject>

    @POST("auth/register")
    suspend fun registerUser(@Body body: JsonObject): Response<JsonObject>

    @GET("auth/profile")
    suspend fun getProfile(@Header("Authorization") auth: String): Response<JsonObject>

    @POST("auth/change-password")
    suspend fun changePassword(
        @Header("Authorization") auth: String,
        @Body body: JsonObject
    ): Response<JsonObject>

    @POST("auth/logout")
    suspend fun logout(@Header("Authorization") auth: String): Response<JsonObject>

    // ==================== USUARIOS ====================
    // Crear usuario
    @POST("users")
    suspend fun createUser(
        @Header("Authorization") auth: String,
        @Body body: JsonObject
    ): Response<JsonObject>

    // Obtener lista de usuarios
    @GET("users")
    suspend fun getUsers(
        @Header("Authorization") auth: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): Response<JsonObject>

    // Perfil del usuario autenticado
    @GET("users/me/profile")
    suspend fun getUserProfile(
        @Header("Authorization") auth: String
    ): Response<JsonObject>

    // Obtener fotos predeterminadas
    @GET("users/fotos-predeterminadas")
    suspend fun getFotosPredeterminadas(
        @Header("Authorization") auth: String
    ): Response<JsonObject>

    // Obtener usuario por ID
    @GET("users/{id}")
    suspend fun getUserById(
        @Header("Authorization") auth: String,
        @Path("id") id: String
    ): Response<JsonObject>

    // Actualizar usuario por ID
    @PUT("users/{id}")
    suspend fun updateUser(
        @Header("Authorization") auth: String,
        @Path("id") id: String,
        @Body body: JsonObject
    ): Response<JsonObject>

    // Actualizar foto de perfil (cuando se elige una foto predeterminada)
    @Multipart
    @PUT("users/me/foto-perfil")
    suspend fun updateFotoPerfilPredeterminada(
        @Header("Authorization") token: String,
        @Part("fotoPredeterminadaUrl") fotoPredeterminadaUrl: RequestBody
    ): Response<JsonObject>

    // Actualizar foto de perfil (cuando se sube un archivo desde el dispositivo)
    @Multipart
    @PUT("users/me/foto-perfil")
    suspend fun updateFotoPerfilConArchivo(
        @Header("Authorization") token: String,
        @Part foto: MultipartBody.Part
    ): Response<JsonObject>

    // Eliminar usuario por ID
    @DELETE("users/{id}")
    suspend fun deleteUser(
        @Header("Authorization") auth: String,
        @Path("id") id: String
    ): Response<JsonObject>

    // ==================== CURSOS ====================
    @Multipart
    @POST("cursos")
    suspend fun createCurso(
        @Header("Authorization") auth: String,
        @Part fotoPortada: MultipartBody.Part?,
        @Part archivoCSV: MultipartBody.Part?,
        @PartMap data: Map<String, @JvmSuppressWildcards RequestBody>
    ): Response<JsonObject>

    @GET("cursos")
    suspend fun getCursos(
        @Header("Authorization") auth: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): Response<JsonObject>

    @GET("cursos/mis-cursos")
    suspend fun getMisCursos(
        @Header("Authorization") auth: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): Response<JsonObject>

    @GET("cursos/{id}")
    suspend fun getCursoById(
        @Header("Authorization") auth: String,
        @Path("id") id: String
    ): Response<JsonObject>

    @GET("cursos/{id}/participantes")
    suspend fun getParticipantesCurso(
        @Header("Authorization") auth: String,
        @Path("id") id: String
    ): Response<JsonObject>

    @Multipart
    @PUT("cursos/{id}")
    suspend fun updateCurso(
        @Header("Authorization") auth: String,
        @Path("id") id: String,
        @Part fotoPortada: MultipartBody.Part?,
        @PartMap data: Map<String, @JvmSuppressWildcards RequestBody>
    ): Response<JsonObject>

    @DELETE("cursos/{id}")
    suspend fun archivarCurso(
        @Header("Authorization") auth: String,
        @Path("id") id: String
    ): Response<JsonObject>

    @POST("cursos/{id}/participantes")
    suspend fun agregarParticipante(
        @Header("Authorization") auth: String,
        @Path("id") id: String,
        @Body body: JsonObject
    ): Response<JsonObject>

    @DELETE("cursos/{id}/participantes/{usuarioId}")
    suspend fun removerParticipante(
        @Header("Authorization") auth: String,
        @Path("id") id: String,
        @Path("usuarioId") usuarioId: String
    ): Response<JsonObject>

    @Multipart
    @POST("cursos/{id}/usuarios-masivo")
    suspend fun registrarUsuariosMasivo(
        @Header("Authorization") auth: String,
        @Path("id") id: String,
        @Part archivoCSV: MultipartBody.Part
    ): Response<JsonObject>

    // ==================== MÓDULOS ====================
    @POST("modulos")
    suspend fun createModulo(
        @Header("Authorization") auth: String,
        @Body body: JsonObject
    ): Response<JsonObject>

    @GET("modulos")
    suspend fun getModulos(
        @Header("Authorization") auth: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): Response<JsonObject>

    @GET("modulos/curso/{cursoId}")
    suspend fun getModulosByCurso(
        @Header("Authorization") auth: String,
        @Path("cursoId") cursoId: String
    ): Response<JsonObject>

    @GET("modulos/{id}")
    suspend fun getModuloById(
        @Header("Authorization") auth: String,
        @Path("id") id: String
    ): Response<JsonObject>

    @PUT("modulos/{id}")
    suspend fun updateModulo(
        @Header("Authorization") auth: String,
        @Path("id") id: String,
        @Body body: JsonObject
    ): Response<JsonObject>

    @DELETE("modulos/{id}")
    suspend fun deleteModulo(
        @Header("Authorization") auth: String,
        @Path("id") id: String
    ): Response<JsonObject>

    @PATCH("modulos/{id}/restore")
    suspend fun restoreModulo(
        @Header("Authorization") auth: String,
        @Path("id") id: String
    ): Response<JsonObject>

    // ==================== TAREAS ====================
    @Multipart
    @POST("tareas")
    suspend fun createTarea(
        @Header("Authorization") auth: String,
        @Part archivos: List<MultipartBody.Part>?,
        @PartMap data: Map<String, @JvmSuppressWildcards RequestBody>
    ): Response<JsonObject>

    @GET("tareas")
    suspend fun getTareas(
        @Header("Authorization") auth: String,
        @Query("cursoId") cursoId: String?,
        @Query("moduloId") moduloId: String?,
        @Query("estado") estado: String?,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): Response<JsonObject>

    @GET("tareas/{id}")
    suspend fun getTareaById(
        @Header("Authorization") auth: String,
        @Path("id") tareaId: String
    ): Response<JsonObject>

    @Multipart
    @PUT("tareas/{id}")
    suspend fun updateTarea(
        @Header("Authorization") auth: String,
        @Path("id") tareaId: String,
        @Part archivos: List<MultipartBody.Part>?,
        @PartMap data: Map<String, @JvmSuppressWildcards RequestBody>
    ): Response<JsonObject>

    @PATCH("tareas/{id}/close")
    suspend fun closeTarea(
        @Header("Authorization") auth: String,
        @Path("id") tareaId: String
    ): Response<JsonObject>

    @DELETE("tareas/{id}")
    suspend fun deleteTarea(
        @Header("Authorization") auth: String,
        @Path("id") tareaId: String
    ): Response<JsonObject>

    // ==================== ENTREGAS ====================
    // ✅ CORRECTO:
    @POST("entregas")
    suspend fun crearEntrega(
        @Header("Authorization") auth: String,
        @Body body: JsonObject
    ): Response<JsonObject>

    @GET("entregas")
    suspend fun getAllEntregas(
        @Header("Authorization") auth: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10,
        @Query("estado") estado: String?
    ): Response<JsonObject>

    @GET("entregas/tarea/{tareaId}")
    suspend fun getEntregasByTarea(
        @Header("Authorization") auth: String,
        @Path("tareaId") tareaId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("estado") estado: String?
    ): Response<JsonObject>

    @GET("entregas/padre/{padreId}")
    suspend fun getEntregasByPadre(
        @Header("Authorization") auth: String,
        @Path("padreId") padreId: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10,
        @Query("estado") estado: String?
    ): Response<JsonObject>

    @GET("entregas/mis-entregas/{tareaId}")
    suspend fun getEntregasByPadreAndTarea(
        @Header("Authorization") auth: String,
        @Path("tareaId") tareaId: String
    ): Response<JsonObject>

    @GET("entregas/{id}")
    suspend fun getEntregaById(
        @Header("Authorization") auth: String,
        @Path("id") entregaId: String
    ): Response<JsonObject>

    @PUT("entregas/{id}")
    suspend fun updateEntrega(
        @Header("Authorization") auth: String,
        @Path("id") entregaId: String,
        @Body body: JsonObject
    ): Response<JsonObject>

    @PATCH("entregas/{id}/enviar")
    suspend fun enviarEntrega(
        @Header("Authorization") auth: String,
        @Path("id") entregaId: String
    ): Response<JsonObject>

    @PATCH("entregas/{id}/calificar")
    suspend fun calificarEntrega(
        @Header("Authorization") auth: String,
        @Path("id") entregaId: String,
        @Body body: JsonObject
    ): Response<JsonObject>

    @DELETE("entregas/{id}")
    suspend fun deleteEntrega(
        @Header("Authorization") auth: String,
        @Path("id") entregaId: String
    ): Response<JsonObject>

    // ✅ CORRECTO: Sin el prefijo "api/"
    @Multipart
    @POST("entregas")
    suspend fun crearEntregaMultipart(
        @Header("Authorization") token: String,
        @Part parts: List<MultipartBody.Part>
    ): Response<JsonObject>

    @Multipart
    @PUT("entregas/{id}")
    suspend fun updateEntregaMultipart(
        @Header("Authorization") token: String,
        @Path("id") entregaId: String,
        @Part parts: List<MultipartBody.Part>
    ): Response<JsonObject>

//==================== NOTIFICACIONES ====================
    @POST("notificaciones")
    suspend fun createNotificacion(@Header("Authorization") auth: String, @Body body: JsonObject): Response<JsonObject>

    @GET("notificaciones")
    suspend fun getMisNotificaciones(
        @Header("Authorization") auth: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10,
        @Query("leida") leida: Boolean?
    ): Response<JsonObject>

    @GET("notificaciones/conteo-no-leidas")
    suspend fun getConteoNoLeidas(@Header("Authorization") auth: String): Response<JsonObject>

    @GET("notificaciones/{id}")
    suspend fun getNotificacionById(@Header("Authorization") auth: String, @Path("id") id: String): Response<JsonObject>

    @PATCH("notificaciones/{id}/leer")
    suspend fun marcarComoLeida(@Header("Authorization") auth: String, @Path("id") id: String): Response<JsonObject>

    @PATCH("notificaciones/leer-multiples")
    suspend fun marcarVariasLeidas(@Header("Authorization") auth: String, @Body body: JsonObject): Response<JsonObject>

    @PATCH("notificaciones/leer-todas")
    suspend fun marcarTodasLeidas(@Header("Authorization") auth: String): Response<JsonObject>

    @DELETE("notificaciones/{id}")
    suspend fun deleteNotificacion(@Header("Authorization") auth: String, @Path("id") id: String): Response<JsonObject>

    @DELETE("notificaciones/limpiar/antiguas")
    suspend fun eliminarLeidasAntiguas(@Header("Authorization") auth: String): Response<JsonObject>
    // ==================== CALENDARIO ====================

    @GET("calendario/{cursoId}")
    suspend fun getCalendarioCurso(
        @Header("Authorization") auth: String,
        @Path("cursoId") cursoId: String,
        @Query("mes") mes: Int?,
        @Query("anio") anio: Int?
    ): Response<JsonObject>

    @GET("calendario/{cursoId}/dia")
    suspend fun getEventosDia(
        @Header("Authorization") auth: String,
        @Path("cursoId") cursoId: String,
        @Query("fecha") fecha: String
    ): Response<JsonObject>

    @GET("calendario/{cursoId}/proximos")
    suspend fun getProximosEventos(
        @Header("Authorization") auth: String,
        @Path("cursoId") cursoId: String,
        @Query("limite") limite: Int?
    ): Response<JsonObject>

    // ==================== EVENTOS ====================
    @POST("eventos")
    suspend fun createEvento(
        @Header("Authorization") auth: String,
        @Body body: JsonObject
    ): Response<JsonObject>

    @GET("eventos")
    suspend fun getEventos(
        @Header("Authorization") auth: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): Response<JsonObject>

    @GET("eventos/hoy")
    suspend fun getEventosHoy(
        @Header("Authorization") auth: String
    ): Response<JsonObject>

    @GET("eventos/{id}")
    suspend fun getEventoById(
        @Header("Authorization") auth: String,
        @Path("id") id: String
    ): Response<JsonObject>
    // En la interface ApiRoutes, agrega:

    // ==================== FOROS ====================
    @GET("foros/curso/{cursoId}")
    suspend fun getForosPorCurso(
        @Header("Authorization") auth: String,
        @Path("cursoId") cursoId: String
    ): Response<JsonObject>

    @GET("foros/{id}")
    suspend fun getForoById(
        @Header("Authorization") auth: String,
        @Path("id") foroId: String
    ): Response<JsonObject>

    // ==================== MENSAJES DE FORO ====================
    @GET("mensajes-foro/foro/{foroId}")
    suspend fun getMensajesPorForo(
        @Header("Authorization") auth: String,
        @Path("foroId") foroId: String
    ): Response<JsonObject>

    @Multipart
    @POST("mensajes-foro")
    suspend fun crearMensajeForo(
        @Header("Authorization") token: String,
        @Part parts: List<MultipartBody.Part>
    ): Response<JsonObject>

    @POST("mensajes-foro/{id}/like")
    suspend fun toggleLikeMensaje(
        @Header("Authorization") auth: String,
        @Path("id") mensajeId: String
    ): Response<JsonObject>

    @PUT("mensajes-foro/{id}")
    suspend fun actualizarMensajeForo(
        @Header("Authorization") auth: String,
        @Path("id") mensajeId: String,
        @Body body: JsonObject
    ): Response<JsonObject>

    @DELETE("mensajes-foro/{id}")
    suspend fun eliminarMensajeForo(
        @Header("Authorization") auth: String,
        @Path("id") mensajeId: String
    ): Response<JsonObject>
}

// ---- ApiService: funciones de conveniencia utilizadas por la app ----
object ApiService {
    private val api: ApiRoutes by lazy { retrofit.create(ApiRoutes::class.java) }

    // ==================== AUTH ====================
    suspend fun login(telefono: String, contraseña: String): Response<JsonObject> {
        val body = JsonObject().apply {
            addProperty("telefono", telefono)
            addProperty("contraseña", contraseña)
        }
        return api.login(body)
    }

    suspend fun registerUser(
        nombre: String,
        apellido: String,
        correo: String,
        telefono: String,
        contraseña: String,
        rol: String = "padre"
    ): Response<JsonObject> {
        val body = JsonObject().apply {
            addProperty("nombre", nombre)
            addProperty("apellido", apellido)
            addProperty("correo", correo)
            addProperty("telefono", telefono)
            addProperty("contraseña", contraseña)
            addProperty("rol", rol)
        }
        return api.registerUser(body)
    }

    suspend fun getProfile(token: String): Response<JsonObject> =
        api.getProfile("Bearer $token")

    suspend fun changePassword(token: String, contraseñaActual: String, nuevaContraseña: String): Response<JsonObject> {
        val body = JsonObject().apply {
            addProperty("contraseñaActual", contraseñaActual)
            addProperty("nuevaContraseña", nuevaContraseña)
        }
        return api.changePassword("Bearer $token", body)
    }

    suspend fun logout(token: String): Response<JsonObject> =
        api.logout("Bearer $token")

    // ==================== USUARIOS ====================
    suspend fun createUser(
        token: String,
        nombre: String,
        apellido: String,
        correo: String,
        telefono: String,
        contraseña: String,
        rol: String
    ): Response<JsonObject> {
        val body = JsonObject().apply {
            addProperty("nombre", nombre)
            addProperty("apellido", apellido)
            addProperty("correo", correo)
            addProperty("telefono", telefono)
            addProperty("contraseña", contraseña)
            addProperty("rol", rol)
        }
        return api.createUser("Bearer $token", body)
    }

    suspend fun getUsers(token: String, page: Int = 1, limit: Int = 10): Response<JsonObject> =
        api.getUsers("Bearer $token", page, limit)

    suspend fun getUserProfile(token: String): Response<JsonObject> =
        api.getUserProfile("Bearer $token")

    suspend fun getFotosPredeterminadas(token: String): Response<JsonObject> =
        api.getFotosPredeterminadas("Bearer $token")

    suspend fun getUserById(token: String, id: String): Response<JsonObject> =
        api.getUserById("Bearer $token", id)

    suspend fun updateUser(
        token: String,
        id: String,
        body: JsonObject
    ): Response<JsonObject> {
        return api.updateUser("Bearer $token", id, body)
    }

    // En ApiService object, actualiza la función:
    suspend fun updateFotoPerfilPredeterminada(token: String, fotoPredeterminadaUrl: String): Response<JsonObject> {
        val body = fotoPredeterminadaUrl.toRequestBody("text/plain".toMediaTypeOrNull())
        return api.updateFotoPerfilPredeterminada("Bearer $token", body)
    }

    suspend fun updateFotoPerfilConArchivo(token: String, fotoFile: File): Response<JsonObject> {
        val requestBody = fotoFile.asRequestBody("image/*".toMediaTypeOrNull())
        val fotoPart = MultipartBody.Part.createFormData("foto", fotoFile.name, requestBody)
        return api.updateFotoPerfilConArchivo("Bearer $token", fotoPart)
    }

    suspend fun deleteUser(token: String, id: String): Response<JsonObject> =
        api.deleteUser("Bearer $token", id)

    // ==================== CURSOS ====================
    suspend fun createCurso(
        token: String,
        nombre: String,
        descripcion: String,
        docenteId: String,
        fotoPortadaFile: File?,
        archivoCSVFile: File?
    ): Response<JsonObject> {
        val fotoPortada = fotoPortadaFile?.let {
            val requestBody = it.asRequestBody("image/*".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("fotoPortada", it.name, requestBody)
        }

        val archivoCSV = archivoCSVFile?.let {
            val requestBody = it.asRequestBody("text/csv".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("archivoCSV", it.name, requestBody)
        }

        val data = mapOf(
            "nombre" to nombre.toRequestBody("text/plain".toMediaTypeOrNull()),
            "descripcion" to descripcion.toRequestBody("text/plain".toMediaTypeOrNull()),
            "docenteId" to docenteId.toRequestBody("text/plain".toMediaTypeOrNull())
        )

        return api.createCurso("Bearer $token", fotoPortada, archivoCSV, data)
    }

    suspend fun getCursos(token: String, page: Int = 1, limit: Int = 10): Response<JsonObject> =
        api.getCursos("Bearer $token", page, limit)

    suspend fun getMisCursos(token: String, page: Int = 1, limit: Int = 10): Response<JsonObject> =
        api.getMisCursos("Bearer $token", page, limit)

    suspend fun getCursoById(token: String, cursoId: String): Response<JsonObject> =
        api.getCursoById("Bearer $token", cursoId)

    suspend fun getParticipantesCurso(token: String, cursoId: String): Response<JsonObject> =
        api.getParticipantesCurso("Bearer $token", cursoId)

    suspend fun updateCurso(
        token: String,
        cursoId: String,
        nombre: String?,
        descripcion: String?,
        fotoPortadaFile: File?
    ): Response<JsonObject> {
        val fotoPortada = fotoPortadaFile?.let {
            val requestBody = it.asRequestBody("image/*".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("fotoPortada", it.name, requestBody)
        }

        val data = mutableMapOf<String, RequestBody>()
        nombre?.let { data["nombre"] = it.toRequestBody("text/plain".toMediaTypeOrNull()) }
        descripcion?.let { data["descripcion"] = it.toRequestBody("text/plain".toMediaTypeOrNull()) }

        return api.updateCurso("Bearer $token", cursoId, fotoPortada, data)
    }

    suspend fun archivarCurso(token: String, cursoId: String): Response<JsonObject> =
        api.archivarCurso("Bearer $token", cursoId)

    suspend fun agregarParticipante(token: String, cursoId: String, usuarioId: String): Response<JsonObject> {
        val body = JsonObject().apply {
            addProperty("usuarioId", usuarioId)
        }
        return api.agregarParticipante("Bearer $token", cursoId, body)
    }

    suspend fun removerParticipante(token: String, cursoId: String, usuarioId: String): Response<JsonObject> =
        api.removerParticipante("Bearer $token", cursoId, usuarioId)

    suspend fun registrarUsuariosMasivo(token: String, cursoId: String, csvFile: File): Response<JsonObject> {
        val requestBody = csvFile.asRequestBody("text/csv".toMediaTypeOrNull())
        val csvPart = MultipartBody.Part.createFormData("archivoCSV", csvFile.name, requestBody)
        return api.registrarUsuariosMasivo("Bearer $token", cursoId, csvPart)
    }

    // ==================== MÓDULOS ====================
    suspend fun createModulo(
        token: String,
        cursoId: String,
        nombre: String,
        descripcion: String?,
        orden: Int?
    ): Response<JsonObject> {
        val body = JsonObject().apply {
            addProperty("cursoId", cursoId)
            addProperty("nombre", nombre)
            descripcion?.let { addProperty("descripcion", it) }
            orden?.let { addProperty("orden", it) }
        }
        return api.createModulo("Bearer $token", body)
    }

    suspend fun getModulos(token: String, page: Int = 1, limit: Int = 10): Response<JsonObject> =
        api.getModulos("Bearer $token", page, limit)

    suspend fun getModulosByCurso(token: String, cursoId: String): Response<JsonObject> =
        api.getModulosByCurso("Bearer $token", cursoId)

    suspend fun getModuloById(token: String, moduloId: String): Response<JsonObject> =
        api.getModuloById("Bearer $token", moduloId)

    suspend fun updateModulo(
        token: String,
        moduloId: String,
        nombre: String?,
        descripcion: String?,
        orden: Int?
    ): Response<JsonObject> {
        val body = JsonObject().apply {
            nombre?.let { addProperty("nombre", it) }
            descripcion?.let { addProperty("descripcion", it) }
            orden?.let { addProperty("orden", it) }
        }
        return api.updateModulo("Bearer $token", moduloId, body)
    }

    suspend fun deleteModulo(token: String, moduloId: String): Response<JsonObject> =
        api.deleteModulo("Bearer $token", moduloId)

    suspend fun restoreModulo(token: String, moduloId: String): Response<JsonObject> =
        api.restoreModulo("Bearer $token", moduloId)

    // ==================== TAREAS ====================
    suspend fun createTarea(
        token: String,
        cursoId: String,
        moduloId: String?,
        docenteId: String,
        titulo: String,
        descripcion: String?,
        fechaLimite: String,
        archivos: List<File>?
    ): Response<JsonObject> {
        val archivosParts = archivos?.map { file ->
            val requestBody = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("archivos", file.name, requestBody)
        }

        val data = mutableMapOf<String, RequestBody>().apply {
            put("cursoId", cursoId.toRequestBody("text/plain".toMediaTypeOrNull()))
            put("docenteId", docenteId.toRequestBody("text/plain".toMediaTypeOrNull()))
            put("titulo", titulo.toRequestBody("text/plain".toMediaTypeOrNull()))
            put("fechaLimite", fechaLimite.toRequestBody("text/plain".toMediaTypeOrNull()))
            moduloId?.let { put("moduloId", it.toRequestBody("text/plain".toMediaTypeOrNull())) }
            descripcion?.let { put("descripcion", it.toRequestBody("text/plain".toMediaTypeOrNull())) }
        }

        return api.createTarea("Bearer $token", archivosParts, data)
    }

    suspend fun getTareas(
        token: String,
        cursoId: String? = null,
        moduloId: String? = null,
        estado: String? = null,
        page: Int = 1,
        limit: Int = 10
    ): Response<JsonObject> =
        api.getTareas("Bearer $token", cursoId, moduloId, estado, page, limit)

    suspend fun getTareaById(token: String, tareaId: String): Response<JsonObject> =
        api.getTareaById("Bearer $token", tareaId)

    suspend fun updateTarea(
        token: String,
        tareaId: String,
        titulo: String?,
        descripcion: String?,
        fechaLimite: String?,
        archivos: List<File>?
    ): Response<JsonObject> {
        val archivosParts = archivos?.map { file ->
            val requestBody = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("archivos", file.name, requestBody)
        }

        val data = mutableMapOf<String, RequestBody>()
        titulo?.let { data["titulo"] = it.toRequestBody("text/plain".toMediaTypeOrNull()) }
        descripcion?.let { data["descripcion"] = it.toRequestBody("text/plain".toMediaTypeOrNull()) }
        fechaLimite?.let { data["fechaLimite"] = it.toRequestBody("text/plain".toMediaTypeOrNull()) }

        return api.updateTarea("Bearer $token", tareaId, archivosParts, data)
    }

    suspend fun closeTarea(token: String, tareaId: String): Response<JsonObject> =
        api.closeTarea("Bearer $token", tareaId)

    suspend fun deleteTarea(token: String, tareaId: String): Response<JsonObject> =
        api.deleteTarea("Bearer $token", tareaId)



// ==================== ENTREGAS ====================
    suspend fun crearEntrega(
        token: String,
        tareaId: String,
        padreId: String,
        textoRespuesta: String?,
        archivos: List<String>?,
        estado: String = "borrador"
    ): Response<JsonObject> {
        val body = JsonObject().apply {
            addProperty("tareaId", tareaId)
            addProperty("padreId", padreId)
            addProperty("estado", estado)

            if (!textoRespuesta.isNullOrEmpty()) {
                addProperty("textoRespuesta", textoRespuesta)
            }

            archivos?.takeIf { it.isNotEmpty() }?.let { list ->
                add("archivos", JsonArray().apply {
                    list.forEach { add(it) }
                })
            }
        }

        Log.d("ApiService", "📤 Creando entrega (JSON)")
        Log.d("ApiService", "Body: $body")

        return api.crearEntrega("Bearer $token", body)
    }

    suspend fun crearEntregaMultipart(
        token: String,
        tareaId: String,
        padreId: String,
        textoRespuesta: String?,
        enlace: String?,
        archivos: List<Uri>,
        estado: String = "borrador",
        context: Context
    ): Response<JsonObject> = withContext(Dispatchers.IO) {
        try {
            val parts = mutableListOf<MultipartBody.Part>()

            // Campos obligatorios
            parts.add(MultipartBody.Part.createFormData("tareaId", tareaId))
            parts.add(MultipartBody.Part.createFormData("padreId", padreId))
            parts.add(MultipartBody.Part.createFormData("estado", estado))

            // Campos opcionales
            textoRespuesta?.takeIf { it.isNotBlank() }?.let {
                parts.add(MultipartBody.Part.createFormData("textoRespuesta", it))
            }

            enlace?.takeIf { it.isNotBlank() }?.let {
                parts.add(MultipartBody.Part.createFormData("enlace", it))
            }

            // Archivos
            archivos.forEach { uri ->
                val file = uriToFile(context, uri)
                if (file != null && file.exists()) {
                    val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                    val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())

                    val part = MultipartBody.Part.createFormData(
                        "archivos",
                        file.name,
                        requestFile
                    )
                    parts.add(part)
                    Log.d("ApiService", "📎 Archivo: ${file.name} (${file.length()} bytes)")
                }
            }

            Log.d("ApiService", "📤 Enviando ${parts.size} partes (Multipart)")
            Log.d("ApiService", "   tareaId: $tareaId")
            Log.d("ApiService", "   padreId: $padreId")
            Log.d("ApiService", "   estado: $estado")
            Log.d("ApiService", "   archivos: ${archivos.size}")

            api.crearEntregaMultipart("Bearer $token", parts)

        } catch (e: Exception) {
            Log.e("ApiService", "❌ Error en crearEntregaMultipart", e)
            throw e
        }
    }

    suspend fun updateEntrega(
        token: String,
        entregaId: String,
        textoRespuesta: String?,
        archivos: List<String>?
    ): Response<JsonObject> {
        val body = JsonObject().apply {
            if (!textoRespuesta.isNullOrEmpty()) {
                addProperty("textoRespuesta", textoRespuesta)
            }
            archivos?.takeIf { it.isNotEmpty() }?.let { list ->
                add("archivos", JsonArray().apply {
                    list.forEach { add(it) }
                })
            }
        }

        Log.d("ApiService", "Actualizando entrega (JSON)")
        Log.d("ApiService", "Body: $body")

        return api.updateEntrega("Bearer $token", entregaId, body)
    }

    // Función para actualizar entrega CON archivos nuevos
    suspend fun updateEntregaMultipart(
        token: String,
        entregaId: String,
        textoRespuesta: String?,
        enlace: String?,
        archivos: List<Uri>,
        context: Context
    ): Response<JsonObject> = withContext(Dispatchers.IO) {
        try {
            val parts = mutableListOf<MultipartBody.Part>()

            textoRespuesta?.takeIf { it.isNotBlank() }?.let {
                parts.add(MultipartBody.Part.createFormData("textoRespuesta", it))
            }

            enlace?.takeIf { it.isNotBlank() }?.let {
                parts.add(MultipartBody.Part.createFormData("enlace", it))
            }

            archivos.forEach { uri ->
                val file = uriToFile(context, uri)
                if (file != null && file.exists()) {
                    val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                    val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())

                    val part = MultipartBody.Part.createFormData(
                        "archivos",
                        file.name,
                        requestFile
                    )
                    parts.add(part)
                }
            }

            Log.d("ApiService", " Actualizando entrega $entregaId con ${parts.size} partes")

            api.updateEntregaMultipart("Bearer $token", entregaId, parts)

        } catch (e: Exception) {
            Log.e("ApiService", " Error en updateEntregaMultipart", e)
            throw e
        }
    }

    suspend fun enviarEntrega(
        token: String,
        entregaId: String
    ): Response<JsonObject> =
        api.enviarEntrega("Bearer $token", entregaId)

    suspend fun calificarEntrega(
        token: String,
        entregaId: String,
        nota: Double,
        comentario: String?,
        docenteId: String
    ): Response<JsonObject> {
        val body = JsonObject().apply {
            addProperty("nota", nota)
            addProperty("docenteId", docenteId)
            comentario?.let { addProperty("comentario", it) }
        }
        return api.calificarEntrega("Bearer $token", entregaId, body)
    }
    suspend fun deleteEntrega(
        token: String,
        entregaId: String
    ): Response<JsonObject> =
        api.deleteEntrega("Bearer $token", entregaId)
    // Función auxiliar para crear archivo temporal desde Uri
    private fun uriToFile(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null

            // Obtener nombre original del archivo
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            val displayName = cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) it.getString(nameIndex) else null
                } else null
            } ?: "file_${System.currentTimeMillis()}"

            // Crear archivo temporal con el nombre original
            val extension = displayName.substringAfterLast('.', "")
            val tempFile = File.createTempFile(
                displayName.substringBeforeLast('.'),
                if (extension.isNotEmpty()) ".$extension" else "",
                context.cacheDir
            )

            // Copiar contenido
            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }

            inputStream.close()

            Log.d("ApiService", " Archivo temporal creado: ${tempFile.name} (${tempFile.length()} bytes)")
            tempFile

        } catch (e: Exception) {
            Log.e("ApiService", " Error convirtiendo Uri a File: ${e.message}", e)
            null
        }
    }



    suspend fun getEntregasByPadreAndTarea(
        token: String,
        tareaId: String
    ): Response<JsonObject> =
        api.getEntregasByPadreAndTarea("Bearer $token", tareaId)

    suspend fun getAllEntregas(
        token: String,
        page: Int = 1,
        limit: Int = 10,
        estado: String? = null
    ): Response<JsonObject> =
        api.getAllEntregas("Bearer $token", page, limit, estado)

    suspend fun getEntregasByTarea(
        token: String,
        tareaId: String,
        page: Int = 1,
        limit: Int = 20,
        estado: String? = null
    ): Response<JsonObject> =
        api.getEntregasByTarea("Bearer $token", tareaId, page, limit, estado)

    suspend fun getEntregasByPadre(
        token: String,
        padreId: String,
        page: Int = 1,
        limit: Int = 10,
        estado: String? = null
    ): Response<JsonObject> =
        api.getEntregasByPadre("Bearer $token", padreId, page, limit, estado)

    suspend fun getEntregaById(
        token: String,
        entregaId: String
    ): Response<JsonObject> =
        api.getEntregaById("Bearer $token", entregaId)

    // ==================== NOTIFICACIONES ====================
    suspend fun createNotificacion(
        token: String,
        usuarioId: String,
        titulo: String,
        mensaje: String,
        tipo: String
    ): Response<JsonObject> {
        val body = JsonObject().apply {
            addProperty("usuarioId", usuarioId)
            addProperty("titulo", titulo)
            addProperty("mensaje", mensaje)
            addProperty("tipo", tipo)
        }
        return api.createNotificacion("Bearer $token", body)
    }

    suspend fun getMisNotificaciones(
        token: String,
        page: Int = 1,
        limit: Int = 10,
        leida: Boolean? = null
    ): Response<JsonObject> =
        api.getMisNotificaciones("Bearer $token", page, limit, leida)

    suspend fun getConteoNoLeidas(token: String): Response<JsonObject> =
        api.getConteoNoLeidas("Bearer $token")

    suspend fun getNotificacionById(token: String, notificacionId: String): Response<JsonObject> =
        api.getNotificacionById("Bearer $token", notificacionId)

    suspend fun marcarComoLeida(token: String, notificacionId: String): Response<JsonObject> =
        api.marcarComoLeida("Bearer $token", notificacionId)

    suspend fun marcarVariasLeidas(token: String, notificacionIds: List<String>): Response<JsonObject> {
        val body = JsonObject().apply {
            add("notificacionIds", JsonArray().apply {
                notificacionIds.forEach { add(it) }
            })
        }
        return api.marcarVariasLeidas("Bearer $token", body)
    }

    suspend fun marcarTodasLeidas(token: String): Response<JsonObject> =
        api.marcarTodasLeidas("Bearer $token")

    suspend fun deleteNotificacion(token: String, notificacionId: String): Response<JsonObject> =
        api.deleteNotificacion("Bearer $token", notificacionId)

    suspend fun eliminarLeidasAntiguas(token: String): Response<JsonObject> =
        api.eliminarLeidasAntiguas("Bearer $token")
// ==================== USUARIOS (funciones faltantes) ====================

    suspend fun updateUserWithCedula(
        token: String,
        id: String,
        nombre: String,
        apellido: String,
        cedula: String,
        correo: String,
        telefono: String
    ): Response<JsonObject> {
        val body = JsonObject().apply {
            addProperty("nombre", nombre)
            addProperty("apellido", apellido)
            addProperty("cedula", cedula)
            addProperty("correo", correo)
            addProperty("telefono", telefono)
        }
        return api.updateUser("Bearer $token", id, body)
    }
    // ==================== CALENDARIO ====================
    suspend fun getCalendarioCurso(
        token: String,
        cursoId: String,
        mes: Int? = null,
        anio: Int? = null
    ): Response<JsonObject> =
        api.getCalendarioCurso("Bearer $token", cursoId, mes, anio)

    suspend fun getEventosDia(
        token: String,
        cursoId: String,
        fecha: String
    ): Response<JsonObject> =
        api.getEventosDia("Bearer $token", cursoId, fecha)

    suspend fun getProximosEventos(
        token: String,
        cursoId: String,
        limite: Int = 10
    ): Response<JsonObject> =
        api.getProximosEventos("Bearer $token", cursoId, limite)

    // ==================== EVENTOS ====================
    suspend fun createEvento(
        token: String,
        titulo: String,
        descripcion: String,
        fechaInicio: String,
        fechaFin: String,
        hora: String?,
        ubicacion: String?,
        categoria: String,
        cursosIds: List<String>,
        docenteId: String
    ): Response<JsonObject> {
        val body = JsonObject().apply {
            addProperty("titulo", titulo)
            addProperty("descripcion", descripcion)
            addProperty("fechaInicio", fechaInicio)
            addProperty("fechaFin", fechaFin)
            hora?.let { addProperty("hora", it) }
            ubicacion?.let { addProperty("ubicacion", it) }
            addProperty("categoria", categoria)
            addProperty("docenteId", docenteId)

            add("cursosIds", JsonArray().apply {
                cursosIds.forEach { add(it) }
            })
        }
        return api.createEvento("Bearer $token", body)
    }

    suspend fun getEventos(
        token: String,
        page: Int = 1,
        limit: Int = 10
    ): Response<JsonObject> =
        api.getEventos("Bearer $token", page, limit)

    suspend fun getEventosHoy(token: String): Response<JsonObject> =
        api.getEventosHoy("Bearer $token")

    suspend fun getEventoById(token: String, eventoId: String): Response<JsonObject> =
        api.getEventoById("Bearer $token", eventoId)
    // En ApiService object, agrega estas funciones:

    // ==================== FOROS ====================
    suspend fun getForosPorCurso(
        token: String,
        cursoId: String
    ): Response<JsonObject> =
        api.getForosPorCurso("Bearer $token", cursoId)

    suspend fun getForoById(
        token: String,
        foroId: String
    ): Response<JsonObject> =
        api.getForoById("Bearer $token", foroId)

    // ==================== MENSAJES DE FORO ====================
    suspend fun getMensajesForo(
        token: String,
        foroId: String
    ): Response<JsonObject> =
        api.getMensajesPorForo("Bearer $token", foroId)

    suspend fun crearMensajeForo(
        token: String,
        foroId: String,
        contenido: String,
        respuestaA: String?,
        archivos: List<Uri>,
        context: Context
    ): Response<JsonObject> = withContext(Dispatchers.IO) {
        try {
            val parts = mutableListOf<MultipartBody.Part>()

            // Campos obligatorios
            parts.add(MultipartBody.Part.createFormData("foroId", foroId))
            parts.add(MultipartBody.Part.createFormData("contenido", contenido))

            // Campo opcional: respuestaA
            respuestaA?.let {
                parts.add(MultipartBody.Part.createFormData("respuestaA", it))
            }

            // Archivos
            archivos.forEach { uri ->
                val file = uriToFile(context, uri)
                if (file != null && file.exists()) {
                    val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
                    val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())

                    val part = MultipartBody.Part.createFormData(
                        "archivos",
                        file.name,
                        requestFile
                    )
                    parts.add(part)
                    Log.d("ApiService", "📎 Archivo adjunto: ${file.name}")
                }
            }

            Log.d("ApiService", "📤 Enviando mensaje al foro: $foroId")
            api.crearMensajeForo("Bearer $token", parts)

        } catch (e: Exception) {
            Log.e("ApiService", "❌ Error al crear mensaje de foro", e)
            throw e
        }
    }

    suspend fun toggleLikeMensaje(
        token: String,
        mensajeId: String
    ): Response<JsonObject> =
        api.toggleLikeMensaje("Bearer $token", mensajeId)

    suspend fun actualizarMensajeForo(
        token: String,
        mensajeId: String,
        contenido: String
    ): Response<JsonObject> {
        val body = JsonObject().apply {
            addProperty("contenido", contenido)
        }
        return api.actualizarMensajeForo("Bearer $token", mensajeId, body)
    }

    suspend fun eliminarMensajeForo(
        token: String,
        mensajeId: String
    ): Response<JsonObject> =
        api.eliminarMensajeForo("Bearer $token", mensajeId)
}
