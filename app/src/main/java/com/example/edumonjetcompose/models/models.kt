package com.example.edumonjetcompose.models

import android.net.Uri

/**
 * ==========================================================
 * 🧍‍♀️🧍‍♂️ MODELOS DE USUARIO Y PERFIL
 * ==========================================================
 */

/** Datos básicos del usuario logueado */
data class UserData(
    val id: String,
    val nombre: String,
    val apellido: String,
    val cedula: String?,
    val correo: String,
    val telefono: String,
    val rol: String,
    val fotoPerfilUrl: String?,
    val estado: String
)

/** Perfil del usuario (información ampliada) */
data class UserProfile(
    val id: String,
    val nombre: String,
    val apellido: String,
    val cedula: String,
    val correo: String,
    val telefono: String,
    val fotoPerfilUrl: String?
)

/** Información general del usuario (usado en foros, mensajes, etc.) */
data class UsuarioInfo(
    val id: String,
    val nombre: String,
    val apellido: String,
    val fotoPerfilUrl: String?,
    val rol: String
)

/** Opciones de avatar predeterminadas */
data class AvatarOption(
    val url: String,
    val name: String
)

/**
 * ==========================================================
 * 🎓 MODELOS DE CURSOS, MÓDULOS Y TAREAS
 * ==========================================================
 */

/** Curso general (para listados o dashboard) */
data class CursoItem(
    val id: String,
    val nombre: String,
    val descripcion: String?,
    val fotoPortadaUrl: String?,
    val estado: String,
    val totalParticipantes: Int,
    val docenteNombre: String
)

/** Información completa del curso */
data class Curso(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val fotoPortadaUrl: String?,
    val docenteNombre: String,
    val docenteApellido: String,
    val participantesCount: Int,
    val fechaCreacion: String,
    val estado: String
)

/** Detalle del curso (con cantidad de módulos) */
data class CursoDetalle(
    val nombre: String,
    val descripcion: String,
    val fotoPortadaUrl: String?,
    val docenteNombre: String,
    val docenteApellido: String,
    val docenteFotoUrl: String?, // NUEVO CAMPO
    val participantesCount: Int,
    val modulosCount: Int
)

/** Representa un módulo que contiene tareas */
data class ModuloConTareas(
    val id: String,
    val nombre: String,
    val tareas: List<TareaInfo>
)

/** Información básica de una tarea */
data class TareaInfo(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val fechaEntrega: String,
    val estado: String,
    val estaVencida: Boolean
)

/** Detalle completo de una tarea */
data class TareaDetalle(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val fechaCreacion: String,
    val fechaEntrega: String,
    val estado: String,
    val tipoEntrega: String,
    val criterios: String,
    val archivosAdjuntos: List<ArchivoAdjunto>
)

/** Archivo adjunto de una tarea */
data class ArchivoAdjunto(
    val tipo: String,
    val url: String,
    val nombre: String,
    val descripcion: String? = null
)

/** Entrega realizada por un estudiante */
data class Entrega(
    val id: String,
    val textoRespuesta: String,
    val fechaEntrega: String,
    val estado: String,
    val nota: String?,
    val comentario: String?,
    val archivos: List<String>
)

/** Archivo seleccionado localmente (para subir entregas) */
data class ArchivoSeleccionado(
    val uri: Uri,
    val nombre: String,
    val tipo: String,
    val tamano: Long
)

/**
 * ==========================================================
 * 💬 MODELOS DE FOROS Y MENSAJES
 * ==========================================================
 */

/** Información general del foro */
data class Foro(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val estado: String,
    val docenteId: DocenteInfo,
    val fechaCreacion: String,
    val totalMensajes: Int,
    val archivos: List<ArchivoForo>
)

/** Información del docente creador del foro */
data class DocenteInfo(
    val id: String,
    val nombre: String,
    val apellido: String,
    val fotoPerfilUrl: String?,
    val rol: String
)

/** Archivo compartido en el foro */
data class ArchivoForo(
    val url: String,
    val tipo: String,
    val nombre: String
)

/** Mensaje dentro de un foro, puede tener respuestas anidadas */
data class MensajeForo(
    val id: String,
    val contenido: String,
    val usuarioId: UsuarioInfo,
    val fechaCreacion: String,
    val likes: List<String>,
    val archivos: List<ArchivoForo>,
    val respuestas: List<MensajeForo>
)

/**
 * ==========================================================
 * 🗓️ MODELOS DE CALENDARIO Y EVENTOS
 * ==========================================================
 */

/** Evento o tarea visible en el calendario */
data class EventoCalendario(
    val id: String,
    val tipo: String,          // "tarea" o "evento"
    val titulo: String,
    val descripcion: String?,
    val fecha: String,
    val fechaInicio: String,
    val fechaFin: String,
    val estado: String,
    val modulo: String? = null,
    val moduloId: String? = null,
    val categoria: String? = null,
    val ubicacion: String? = null,
    val hora: String? = null,
    val color: String,
    val icono: String
)
data class CursoDetalleProfesor(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val fotoPortadaUrl: String?,
    val docenteNombre: String,
    val docenteApellido: String,
    val docenteFotoUrl: String?,
    val participantesCount: Int,
    val modulosCount: Int
)
data class Participante(
    val id: String,
    val nombre: String,
    val apellido: String,
    val correo: String,
    val telefono: String?,
    val cedula: String?,
    val rol: String,
    val etiqueta: String,
    val fotoPerfilUrl: String?
)

// ==================== MODELOS ====================

data class Modulo(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val cursoId: String
)
data class DetalleEntrega(
    val id: String,
    val estudianteNombre: String,
    val estudianteFoto: String?,
    val tareaTitulo: String,
    val textoRespuesta: String,
    val fechaEntrega: String?,
    val estado: String,
    val nota: Double?,
    val comentario: String?,
    val archivos: List<String>
)data class ForoInfo(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val fechaCreacion: String,
    val mensajesCount: Int
)

data class EventoInfo(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val fecha: String,
    val hora: String?,
    val categoria: String
)
