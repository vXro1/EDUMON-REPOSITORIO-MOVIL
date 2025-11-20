package com.example.edumonjetcompose.network

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.example.edumonjetcompose.models.FcmTokenResponse
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.edumonjetcompose.network.ApiRoutes
import com.example.edumonjetcompose.ui.TAG
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
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

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
    suspend fun actualizarFcmToken(
        authToken: String,
        fcmToken: String
    ): Response<FcmTokenResponse> {
        return try {
            val body = mapOf("fcmToken" to fcmToken)
            val response = api.actualizarFcmToken(
                token = "Bearer $authToken",
                body = body
            )

            if (response.isSuccessful) {
                Log.d(TAG, "✅ Token FCM actualizado en servidor")
            } else {
                Log.e(TAG, "❌ Error al actualizar token FCM: ${response.code()}")
            }

            response
        } catch (e: Exception) {
            Log.e(TAG, "❌ Excepción al actualizar token FCM: ${e.message}", e)
            throw e
        }
    }

    // En ApiService.kt - REEMPLAZA el método changePassword existente

    suspend fun changePassword(
        token: String,
        contraseñaActual: String,
        contraseñaNueva: String
    ): Response<JsonObject> {
        val body = JsonObject().apply {
            addProperty("contraseñaActual", contraseñaActual)
            addProperty("contraseñaNueva", contraseñaNueva)
        }

        Log.d("ApiService", "🔐 Cambiando contraseña...")
        Log.d("ApiService", "Body: $body")

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
    ): Response<JsonObject> =
        api.updateUser("Bearer $token", id, body)

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

    suspend fun updateFotoPerfilPredeterminada(
        token: String,
        fotoPredeterminadaUrl: String
    ): Response<JsonObject> {
        val body = fotoPredeterminadaUrl.toRequestBody("text/plain".toMediaTypeOrNull())
        return api.updateFotoPerfilPredeterminada("Bearer $token", body)
    }

    suspend fun updateFotoPerfilConArchivo(
        token: String,
        fotoFile: File
    ): Response<JsonObject> {
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

    suspend fun deleteCurso(token: String, cursoId: String): Response<JsonObject> =
        api.archivarCurso("Bearer $token", cursoId)

    // Agregar estos métodos en ApiService.kt en la sección de CURSOS

// ==================== PARTICIPANTES ====================

    suspend fun agregarParticipante(
        token: String,
        cursoId: String,
        nombre: String,
        apellido: String,
        cedula: String,
        telefono: String?,
        contraseña: String?
    ): Response<JsonObject> {
        val body = JsonObject().apply {
            addProperty("nombre", nombre)
            addProperty("apellido", apellido)
            addProperty("cedula", cedula)
            telefono?.let { addProperty("telefono", it) }
            contraseña?.let { addProperty("contraseña", it) }
        }

        Log.d("ApiService", "📤 Agregando participante al curso: $cursoId")
        Log.d("ApiService", "Body: $body")

        return api.agregarParticipante("Bearer $token", cursoId, body)
    }

    suspend fun removerParticipante(
        token: String,
        cursoId: String,
        usuarioId: String
    ): Response<JsonObject> {
        Log.d("ApiService", "🗑️ Removiendo participante $usuarioId del curso $cursoId")
        return api.removerParticipante("Bearer $token", cursoId, usuarioId)
    }
    // ==================== MÓDULOS ====================
    suspend fun createModulo(
        token: String,
        cursoId: String,
        titulo: String,
        descripcion: String?,
        orden: Int?
    ): Response<JsonObject> {
        val body = JsonObject().apply {
            addProperty("cursoId", cursoId)
            addProperty("titulo", titulo)  // ✅ Cambiado de "Titulo" a "titulo"
            descripcion?.let { addProperty("descripcion", it) }
            orden?.let { addProperty("orden", it) }
        }
        return api.createModulo("Bearer $token", body)
    }

    suspend fun getModulosByCurso(token: String, cursoId: String): Response<JsonObject> =
        api.getModulosByCurso("Bearer $token", cursoId)

    suspend fun updateModulo(
        token: String,
        moduloId: String,
        titulo: String?,
        descripcion: String?,
        orden: Int?
    ): Response<JsonObject> {
        val body = JsonObject().apply {
            titulo?.let { addProperty("titulo", it) }  // ✅ Consistente con createModulo            descripcion?.let { addProperty("descripcion", it) }
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
        moduloId: String,
        docenteId: String,
        titulo: String,
        descripcion: String?,
        fechaEntrega: String,
        tipoEntrega: String = "archivo",
        asignacionTipo: String = "todos",
        participantesSeleccionados: List<String>? = null,
        etiquetas: List<String>? = null,
        criterios: String? = null,
        archivos: List<MultipartBody.Part>?,
        enlaces: List<Map<String, String>>? = null
    ): Response<JsonObject> {
        val parts = mutableListOf<MultipartBody.Part>()

        // Campos obligatorios
        parts.add(MultipartBody.Part.createFormData("titulo", titulo))
        parts.add(MultipartBody.Part.createFormData("cursoId", cursoId))
        parts.add(MultipartBody.Part.createFormData("moduloId", moduloId))
        parts.add(MultipartBody.Part.createFormData("docenteId", docenteId))
        parts.add(MultipartBody.Part.createFormData("fechaEntrega", fechaEntrega))
        parts.add(MultipartBody.Part.createFormData("tipoEntrega", tipoEntrega))
        parts.add(MultipartBody.Part.createFormData("asignacionTipo", asignacionTipo))

        // Campos opcionales
        descripcion?.let {
            parts.add(MultipartBody.Part.createFormData("descripcion", it))
        }

        criterios?.let {
            parts.add(MultipartBody.Part.createFormData("criterios", it))
        }

        // Participantes seleccionados (si aplica)
        if (asignacionTipo == "seleccionados" && !participantesSeleccionados.isNullOrEmpty()) {
            val participantesJson = JsonArray().apply {
                participantesSeleccionados.forEach { add(it) }
            }
            parts.add(
                MultipartBody.Part.createFormData(
                    "participantesSeleccionados",
                    participantesJson.toString()
                )
            )
        }

        // Etiquetas
        if (!etiquetas.isNullOrEmpty()) {
            val etiquetasJson = JsonArray().apply {
                etiquetas.forEach { add(it) }
            }
            parts.add(
                MultipartBody.Part.createFormData("etiquetas", etiquetasJson.toString())
            )
        }

        // Enlaces
        if (!enlaces.isNullOrEmpty()) {
            val enlacesJson = JsonArray().apply {
                enlaces.forEach { enlace ->
                    add(JsonObject().apply {
                        enlace["url"]?.let { addProperty("url", it) }
                        enlace["nombre"]?.let { addProperty("nombre", it) }
                        enlace["descripcion"]?.let { addProperty("descripcion", it) }
                    })
                }
            }
            parts.add(
                MultipartBody.Part.createFormData("enlaces", enlacesJson.toString())
            )
        }

        // Archivos adjuntos
        archivos?.forEach { parts.add(it) }

        Log.d("ApiService", "📤 Creando tarea con ${parts.size} partes")
        Log.d("ApiService", "   - Título: $titulo")
        Log.d("ApiService", "   - Curso: $cursoId")
        Log.d("ApiService", "   - Módulo: $moduloId")
        Log.d("ApiService", "   - Tipo entrega: $tipoEntrega")
        Log.d("ApiService", "   - Asignación: $asignacionTipo")
        Log.d("ApiService", "   - Archivos: ${archivos?.size ?: 0}")

        return api.createTareaMultipart("Bearer $token", parts)
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

        Log.d("ApiService", "📝 Creando entrega (JSON)")
        Log.d("ApiService", "Body: $body")

        return api.crearEntrega("Bearer $token", body)
    }

    suspend fun crearEntregaConArchivos(
        token: String,
        tareaId: String,
        padreId: String,
        textoRespuesta: String?,
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

            // Texto de respuesta opcional
            textoRespuesta?.takeIf { it.isNotBlank() }?.let {
                parts.add(MultipartBody.Part.createFormData("textoRespuesta", it))
            }

            // Procesar archivos
            Log.d("ApiService", "📤 Procesando ${archivos.size} archivo(s)...")

            archivos.forEachIndexed { index, uri ->
                try {
                    val file = uriToFile(context, uri)
                    if (file != null && file.exists()) {
                        val mimeType = getMimeType(context, uri)
                        val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())

                        val part = MultipartBody.Part.createFormData(
                            "archivos",
                            file.name,
                            requestFile
                        )
                        parts.add(part)

                        Log.d("ApiService", "✅ Archivo ${index + 1}: ${file.name} (${formatFileSize(file.length())}, $mimeType)")
                    } else {
                        Log.w("ApiService", "⚠️ Archivo ${index + 1} no pudo ser procesado")
                    }
                } catch (e: Exception) {
                    Log.e("ApiService", "❌ Error procesando archivo ${index + 1}: ${e.message}")
                }
            }

            Log.d("ApiService", "📦 Total de partes a enviar: ${parts.size}")
            Log.d("ApiService", "   - tareaId: $tareaId")
            Log.d("ApiService", "   - padreId: $padreId")
            Log.d("ApiService", "   - estado: $estado")
            Log.d("ApiService", "   - archivos: ${archivos.size}")
            Log.d("ApiService", "   - texto: ${if (textoRespuesta.isNullOrBlank()) "no" else "sí"}")

            api.crearEntregaMultipart("Bearer $token", parts)

        } catch (e: Exception) {
            Log.e("ApiService", "❌ Error en crearEntregaConArchivos", e)
            throw e
        }
    }

    suspend fun actualizarEntregaConArchivos(
        token: String,
        entregaId: String,
        textoRespuesta: String?,
        archivos: List<Uri>,
        context: Context
    ): Response<JsonObject> = withContext(Dispatchers.IO) {
        try {
            val parts = mutableListOf<MultipartBody.Part>()

            // Texto de respuesta
            textoRespuesta?.takeIf { it.isNotBlank() }?.let {
                parts.add(MultipartBody.Part.createFormData("textoRespuesta", it))
            }

            // Procesar archivos
            archivos.forEach { uri ->
                val file = uriToFile(context, uri)
                if (file != null && file.exists()) {
                    val mimeType = getMimeType(context, uri)
                    val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())

                    val part = MultipartBody.Part.createFormData(
                        "archivos",
                        file.name,
                        requestFile
                    )
                    parts.add(part)
                    Log.d("ApiService", "📎 Agregando archivo: ${file.name}")
                }
            }

            Log.d("ApiService", "✏️ Actualizando entrega $entregaId con ${parts.size} partes")

            api.updateEntregaMultipart("Bearer $token", entregaId, parts)

        } catch (e: Exception) {
            Log.e("ApiService", "❌ Error en actualizarEntregaConArchivos", e)
            throw e
        }
    }

    suspend fun enviarEntrega(token: String, entregaId: String): Response<JsonObject> {
        Log.d("ApiService", "📨 Enviando entrega: $entregaId")
        return api.enviarEntrega("Bearer $token", entregaId)
    }

    suspend fun eliminarEntrega(token: String, entregaId: String): Response<JsonObject> {
        Log.d("ApiService", "🗑️ Eliminando entrega: $entregaId")
        return api.deleteEntrega("Bearer $token", entregaId)
    }

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

    suspend fun deleteEntrega(token: String, entregaId: String): Response<JsonObject> =
        api.deleteEntrega("Bearer $token", entregaId)

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

    suspend fun getEntregasByPadreAndTarea(
        token: String,
        tareaId: String
    ): Response<JsonObject> =
        api.getEntregasByPadreAndTarea("Bearer $token", tareaId)

    suspend fun getEntregaById(token: String, entregaId: String): Response<JsonObject> =
        api.getEntregaById("Bearer $token", entregaId)

// Agregar estas funciones a tu ApiService existente

    suspend fun getMisNotificaciones(
        token: String,
        page: Int = 1,
        limit: Int = 20,
        leida: Boolean? = null
    ): Response<JsonObject> {
        android.util.Log.d("ApiService", """
            📡 getMisNotificaciones
               Token: ${token.take(50)}...
               Page: $page
               Limit: $limit
               Leida: $leida
        """.trimIndent())

        return api.getMisNotificaciones(
            token = "Bearer $token",
            page = page,
            limit = limit,
            leido = leida
        )
    }

    suspend fun getConteoNoLeidas(token: String): Response<JsonObject> {
        android.util.Log.d("ApiService", "📡 getConteoNoLeidas")
        return api.getConteoNoLeidas("Bearer $token")
    }

    suspend fun getNotificacionById(
        token: String,
        notificacionId: String
    ): Response<JsonObject> {
        android.util.Log.d("ApiService", "📡 getNotificacionById: $notificacionId")
        return api.getNotificacionById("Bearer $token", notificacionId)
    }

    suspend fun marcarComoLeida(
        token: String,
        notificacionId: String
    ): Response<JsonObject> {
        android.util.Log.d("ApiService", "📡 marcarComoLeida: $notificacionId")
        return api.marcarComoLeida("Bearer $token", notificacionId)
    }

    suspend fun marcarVariasLeidas(
        token: String,
        notificacionIds: List<String>
    ): Response<JsonObject> {
        android.util.Log.d("ApiService", "📡 marcarVariasLeidas: ${notificacionIds.size} notificaciones")

        val body = JsonObject().apply {
            add("notificacionIds", com.google.gson.JsonArray().apply {
                notificacionIds.forEach { add(it) }
            })
        }

        return api.marcarVariasLeidas("Bearer $token", body)
    }

    suspend fun marcarTodasLeidas(token: String): Response<JsonObject> {
        android.util.Log.d("ApiService", "📡 marcarTodasLeidas")
        return api.marcarTodasLeidas("Bearer $token")
    }

    suspend fun deleteNotificacion(
        token: String,
        notificacionId: String
    ): Response<JsonObject> {
        android.util.Log.d("ApiService", "📡 deleteNotificacion: $notificacionId")
        return api.deleteNotificacion("Bearer $token", notificacionId)
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
    suspend fun getEventosByCurso(
        token: String,
        cursoId: String
    ): Response<JsonObject> {
        return api.getEventosByCurso("Bearer $token", cursoId)
    }

    suspend fun deleteEvento(
        token: String,
        eventoId: String
    ): Response<JsonObject> {
        return api.deleteEvento("Bearer $token", eventoId)
    }

    suspend fun createEvento(
        token: String,
        titulo: String,
        descripcion: String,
        fechaInicio: String,
        fechaFin: String,
        hora: String,
        ubicacion: String,
        categoria: String,
        cursosIds: List<String>,
        docenteId: String
    ): Response<JsonObject> {
        val body = JsonObject().apply {
            addProperty("titulo", titulo)
            addProperty("descripcion", descripcion)
            addProperty("fechaInicio", fechaInicio)
            addProperty("fechaFin", fechaFin)
            addProperty("hora", hora)
            addProperty("ubicacion", ubicacion)
            addProperty("categoria", categoria)
            addProperty("docenteId", docenteId)

            add("cursosIds", JsonArray().apply {
                cursosIds.forEach { add(it) }
            })
        }
        return api.createEvento("Bearer $token", body)
    }

    // ==================== FOROS ====================
    suspend fun getForosPorCurso(token: String, cursoId: String): Response<JsonObject> =
        api.getForosPorCurso("Bearer $token", cursoId)

    suspend fun getForoById(token: String, foroId: String): Response<JsonObject> =
        api.getForoById("Bearer $token", foroId)
    suspend fun createForo(
        token: String,
        cursoId: String,
        docenteId: String,
        titulo: String,
        descripcion: String,
        archivos: List<MultipartBody.Part>?
    ): Response<JsonObject> {
        val data = mutableMapOf(
            "cursoId" to cursoId.toRequestBody("text/plain".toMediaTypeOrNull()),
            "docenteId" to docenteId.toRequestBody("text/plain".toMediaTypeOrNull()),
            "titulo" to titulo.toRequestBody("text/plain".toMediaTypeOrNull()),
            "descripcion" to descripcion.toRequestBody("text/plain".toMediaTypeOrNull())
        )

        return api.createForo("Bearer $token", archivos, data)
    }
    // ==================== MENSAJES DE FORO ====================
    suspend fun getMensajesForo(token: String, foroId: String): Response<JsonObject> =
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
                    val mimeType = getMimeType(context, uri)
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

    suspend fun toggleLikeMensaje(token: String, mensajeId: String): Response<JsonObject> =
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

    suspend fun eliminarMensajeForo(token: String, mensajeId: String): Response<JsonObject> =
        api.eliminarMensajeForo("Bearer $token", mensajeId)

    // ==================== FUNCIONES AUXILIARES ====================

    /**
     * Convertir Uri a File temporal
     */
    private fun uriToFile(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null

            // Obtener nombre original del archivo
            val displayName = getFileName(context, uri)

            // Crear archivo temporal con el nombre original
            val extension = displayName.substringAfterLast('.', "")
            val nameWithoutExtension = displayName.substringBeforeLast('.', displayName)

            val tempFile = File.createTempFile(
                "${nameWithoutExtension}_",
                if (extension.isNotEmpty()) ".$extension" else "",
                context.cacheDir
            )

            // Copiar contenido
            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }

            inputStream.close()

            Log.d("ApiService", "✅ Archivo temporal: ${tempFile.name} (${formatFileSize(tempFile.length())})")
            tempFile

        } catch (e: Exception) {
            Log.e("ApiService", "❌ Error convirtiendo Uri a File: ${e.message}", e)
            null
        }
    }

    /**
     * Obtener nombre del archivo desde Uri
     */
    private fun getFileName(context: Context, uri: Uri): String {
        var result = "file_${System.currentTimeMillis()}"

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    result = cursor.getString(nameIndex)
                }
            }
        }

        return result
    }

    /**
     * Obtener MIME type del archivo
     */
    private fun getMimeType(context: Context, uri: Uri): String {
        return context.contentResolver.getType(uri) ?: run {
            // Fallback: obtener por extensión
            val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
                ?: "application/octet-stream"
        }
    }

    /**
     * Formatear tamaño de archivo
     */
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
}