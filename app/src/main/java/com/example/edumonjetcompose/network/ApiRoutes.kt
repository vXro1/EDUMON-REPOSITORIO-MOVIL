package com.example.edumonjetcompose.network

import com.example.edumonjetcompose.models.FcmTokenResponse
import com.google.gson.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

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
    /**
     * Actualiza el token FCM del usuario autenticado
     */
    @PUT("usuarios/me/fcm-token")
    suspend fun actualizarFcmToken(
        @Header("Authorization") token: String,
        @Body body: Map<String, String>
    ): Response<FcmTokenResponse>


// ==================== USUARIOS ====================
    @POST("users")
    suspend fun createUser(
        @Header("Authorization") auth: String,
        @Body body: JsonObject
    ): Response<JsonObject>


    @GET("users")
    suspend fun getUsers(
        @Header("Authorization") auth: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10
    ): Response<JsonObject>

    @GET("users/me/profile")
    suspend fun getUserProfile(
        @Header("Authorization") auth: String
    ): Response<JsonObject>

    @GET("users/fotos-predeterminadas")
    suspend fun getFotosPredeterminadas(
        @Header("Authorization") auth: String
    ): Response<JsonObject>

    @GET("users/{id}")
    suspend fun getUserById(
        @Header("Authorization") auth: String,
        @Path("id") id: String
    ): Response<JsonObject>

    @PUT("users/{id}")
    suspend fun updateUser(
        @Header("Authorization") auth: String,
        @Path("id") id: String,
        @Body body: JsonObject
    ): Response<JsonObject>

    @Multipart
    @PUT("users/me/foto-perfil")
    suspend fun updateFotoPerfilPredeterminada(
        @Header("Authorization") token: String,
        @Part("fotoPredeterminadaUrl") fotoPredeterminadaUrl: RequestBody
    ): Response<JsonObject>

    @Multipart
    @PUT("users/me/foto-perfil")
    suspend fun updateFotoPerfilConArchivo(
        @Header("Authorization") token: String,
        @Part foto: MultipartBody.Part
    ): Response<JsonObject>

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
        @Query("limit") limit: Int = 10,
        @Query("estado") estado: String? = null,
        @Query("docenteId") docenteId: String? = null
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
        @Path("id") id: String,
        @Query("etiqueta") etiqueta: String? = null,
        @Query("search") search: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
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
    suspend fun createTareaMultipart(
        @Header("Authorization") token: String,
        @Part parts: List<MultipartBody.Part>
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
    @POST("entregas")
    suspend fun crearEntrega(
        @Header("Authorization") auth: String,
        @Body body: JsonObject
    ): Response<JsonObject>

    @Multipart
    @POST("entregas")
    suspend fun crearEntregaMultipart(
        @Header("Authorization") token: String,
        @Part parts: List<MultipartBody.Part>
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

    @Multipart
    @PUT("entregas/{id}")
    suspend fun updateEntregaMultipart(
        @Header("Authorization") token: String,
        @Path("id") entregaId: String,
        @Part parts: List<MultipartBody.Part>
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

    @DELETE("entregas/{id}/archivos/{archivoId}")
    suspend fun eliminarArchivoEntrega(
        @Header("Authorization") auth: String,
        @Path("id") entregaId: String,
        @Path("archivoId") archivoId: String
    ): Response<JsonObject>

    // ==================== NOTIFICACIONES ====================

    @GET("notificaciones")
    suspend fun getMisNotificaciones(
        @Header("Authorization") token: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("leido") leido: Boolean? // ✅ Retrofit maneja null automáticamente
    ): Response<JsonObject>

    @GET("notificaciones/no-leidas")
    suspend fun getConteoNoLeidas(
        @Header("Authorization") token: String
    ): Response<JsonObject>

    @GET("notificaciones/{id}")
    suspend fun getNotificacionById(
        @Header("Authorization") token: String,
        @Path("id") notificacionId: String
    ): Response<JsonObject>

    @PUT("notificaciones/{id}/leer")
    suspend fun marcarComoLeida(
        @Header("Authorization") token: String,
        @Path("id") notificacionId: String
    ): Response<JsonObject>

    @PUT("notificaciones/marcar-leidas")
    suspend fun marcarVariasLeidas(
        @Header("Authorization") token: String,
        @Body body: JsonObject
    ): Response<JsonObject>

    @PUT("notificaciones/marcar-todas-leidas")
    suspend fun marcarTodasLeidas(
        @Header("Authorization") token: String
    ): Response<JsonObject>

    @DELETE("notificaciones/{id}")
    suspend fun deleteNotificacion(
        @Header("Authorization") token: String,
        @Path("id") notificacionId: String
    ): Response<JsonObject>

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
    @GET("eventos/curso/{cursoId}")
    suspend fun getEventosByCurso(
        @Header("Authorization") token: String,
        @Path("cursoId") cursoId: String
    ): Response<JsonObject>

    @DELETE("eventos/{eventoId}")
    suspend fun deleteEvento(
        @Header("Authorization") token: String,
        @Path("eventoId") eventoId: String
    ): Response<JsonObject>

    @POST("eventos")
    suspend fun createEvento(
        @Header("Authorization") token: String,
        @Body body: JsonObject
    ): Response<JsonObject>
    @POST("api/notificaciones/registrar-fcm-token")
    suspend fun registrarFcmToken(
        @Header("Authorization") token: String,
        @Body body: JsonObject
    ): Response<JsonObject>

    @POST("api/notificaciones/eliminar-fcm-token")
    suspend fun eliminarFcmToken(
        @Header("Authorization") token: String,
        @Body body: JsonObject
    ): Response<JsonObject>



    // ==================== FOROS ====================
    @Multipart
    @POST("foros")
    suspend fun createForo(
        @Header("Authorization") authorization: String,
        @Part archivos: List<MultipartBody.Part>?,
        @PartMap data: Map<String, @JvmSuppressWildcards RequestBody>
    ): Response<JsonObject>
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