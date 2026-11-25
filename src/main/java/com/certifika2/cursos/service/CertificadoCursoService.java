package com.certifika2.cursos.service;

import com.certifika2.cursos.messaging.KinesisCertificadoProducer;
import com.certifika2.cursos.messaging.dto.CertificadoSolicitadoEvent;
import com.certifika2.cursos.messaging.dto.FormatoCertificado;
import com.certifika2.cursos.model.Curso;
import com.certifika2.cursos.model.Inscripcion;
import com.certifika2.cursos.repository.CursoRepository;
import com.certifika2.cursos.repository.InscripcionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * Reglas de negocio para solicitar certificados. Solo se certifica a usuarios
 * inscritos; al solicitarlo, la inscripción queda marcada como completada y
 * se publica el evento CERTIFICADO_SOLICITADO en Kinesis.
 */
@Service
@RequiredArgsConstructor
public class CertificadoCursoService {

    private final CursoRepository cursoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final KinesisCertificadoProducer producer;

    /** Datos del usuario autenticado que pide la operación. */
    public record Solicitante(String sub, String nombre, String email, boolean esAdmin) {
    }

    /** El propio estudiante solicita su certificado al terminar el curso. */
    @Transactional
    public CertificadoSolicitadoEvent solicitarPorEstudiante(Curso curso, Solicitante estudiante,
                                                             FormatoCertificado formato) {
        return completarYPublicar(curso, estudiante.sub(), estudiante.nombre(), estudiante.email(),
                estudiante.sub(), formato);
    }

    /** El profesor dueño del curso (o un ADMIN) emite el certificado de un estudiante inscrito. */
    @Transactional
    public CertificadoSolicitadoEvent emitirPorProfesor(Curso curso, String subEstudiante, Solicitante profesor,
                                                        FormatoCertificado formato) {
        if (!profesor.esAdmin() && !Objects.equals(curso.getIdProfesor(), profesor.sub())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el profesor del curso puede emitir certificados");
        }
        // Nombre y email nulos: ms-usuarios los completa desde su propia base.
        return completarYPublicar(curso, subEstudiante, null, null, profesor.sub(), formato);
    }

    public Curso buscarPorId(Long idCurso) {
        return cursoRepository.findById(idCurso)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado"));
    }

    public Curso buscarPorCodigo(String codigo) {
        return cursoRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado"));
    }

    private CertificadoSolicitadoEvent completarYPublicar(Curso curso, String subUsuario, String nombre,
                                                          String email, String solicitadoPor,
                                                          FormatoCertificado formato) {
        Inscripcion inscripcion = inscripcionRepository.findByIdCursoAndIdUsuario(curso.getIdCurso(), subUsuario)
                .filter(i -> Boolean.TRUE.equals(i.getEstaInscrito()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "El usuario no está inscrito en el curso " + curso.getCodigo()));

        if (!Boolean.TRUE.equals(inscripcion.getCompletado()) || inscripcion.getFechaCompletado() == null) {
            inscripcion.setCompletado(true);
            inscripcion.setFechaCompletado(new Date());
            inscripcionRepository.save(inscripcion);
        }

        CertificadoSolicitadoEvent evento = new CertificadoSolicitadoEvent(
                UUID.randomUUID().toString(),
                CertificadoSolicitadoEvent.TYPE,
                CertificadoSolicitadoEvent.VERSION,
                Instant.now(),
                subUsuario, email, nombre,
                curso.getIdCurso(), curso.getCodigo(), curso.getNombreCurso(), curso.getNivelCurso(),
                curso.getIdProfesor(), solicitadoPor,
                inscripcion.getFechaCompletado().toInstant(),
                formato == null ? FormatoCertificado.PDF : formato);

        // Si Kinesis falla se lanza excepción y la transacción hace rollback:
        // la inscripción no queda "completada" sin su evento.
        producer.publicar(evento);
        return evento;
    }
}
