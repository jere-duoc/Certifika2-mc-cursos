package com.certifika2.cursos.certificados;

import com.certifika2.cursos.model.Curso;
import com.certifika2.cursos.model.Inscripcion;
import com.certifika2.cursos.repository.CursoRepository;
import com.certifika2.cursos.repository.InscripcionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service @RequiredArgsConstructor
public class CertificadoSolicitudService {
    private final CursoRepository cursos;
    private final InscripcionRepository inscripciones;
    private final SolicitudCertificadoRepository solicitudes;
    private final ObjectMapper mapper;

    public Curso curso(String codigo) {
        return cursos.findByCodigo(codigo).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Curso no encontrado"));
    }
    public Curso curso(Long id) {
        return cursos.findById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Curso no encontrado"));
    }
    public void autorizar(Curso curso, String actor, boolean admin) {
        if (!admin && !Objects.equals(curso.getIdProfesor(), actor))
            throw error(HttpStatus.FORBIDDEN, "Solo el profesor del curso o un administrador");
    }

    @Transactional
    public Inscripcion completar(Curso curso, String alumno, String actor, boolean admin) {
        autorizar(curso, actor, admin);
        var inscripcion = inscrito(curso, alumno);
        if (inscripcion.getFechaCompletado() == null) {
            inscripcion.setFechaCompletado(Instant.now()); inscripcion.setCompletadoPor(actor);
        }
        return inscripciones.save(inscripcion);
    }

    @Transactional
    public Resultado solicitar(Curso curso, String alumno, String formato) {
        if (!Set.of("PDF", "PNG").contains(formato)) throw error(HttpStatus.BAD_REQUEST, "Formato debe ser PDF o PNG");
        var inscripcion = inscrito(curso, alumno);
        if (inscripcion.getFechaCompletado() == null)
            throw error(HttpStatus.CONFLICT, "El profesor debe registrar la finalizacion del curso antes de emitir el certificado");
        String eventId = UUID.nameUUIDFromBytes((alumno + ":" + curso.getIdCurso() + ":" + formato)
                .getBytes(StandardCharsets.UTF_8)).toString();
        var existente = solicitudes.findById(eventId);
        if (existente.isPresent()) return resultado(existente.get(), curso);
        var solicitud = new SolicitudCertificado();
        solicitud.setEventId(eventId);
        solicitud.setIdCurso(curso.getIdCurso());
        solicitud.setIdUsuario(alumno);
        solicitud.setFormato(formato);
        solicitud.setFechaSolicitud(Instant.now());
        solicitud.setProximoIntento(solicitud.getFechaSolicitud());
        try {
            solicitud.setPayload(mapper.writeValueAsString(new Evento(1, eventId, alumno, curso.getIdCurso(),
                    curso.getCodigo(), "DEMO".equals(inscripcion.getCompletadoPor()) ? "DEMO · " + curso.getNombreCurso() : curso.getNombreCurso(),
                    "DEMO".equals(inscripcion.getCompletadoPor()) ? "Demostración · Nota de ejemplo: " + inscripcion.getNotaDemo() : curso.getNivelCurso(), formato,
                    inscripcion.getFechaCompletado(), solicitud.getFechaSolicitud())));
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) { throw new IllegalStateException(ex); }
        solicitudes.save(solicitud);
        return resultado(solicitud, curso);
    }
    private Inscripcion inscrito(Curso curso, String alumno) {
        return inscripciones.bloquear(curso.getIdCurso(), alumno).filter(i -> Boolean.TRUE.equals(i.getEstaInscrito()))
                .orElseThrow(() -> error(HttpStatus.CONFLICT, "El estudiante debe estar inscrito"));
    }
    private Resultado resultado(SolicitudCertificado s, Curso c) {
        return new Resultado(s.getEventId(), c.getIdCurso(), c.getCodigo(), s.getFormato(), "EN_PROCESO",
                s.getEnviadoEn() == null ? "PENDIENTE" : "ACEPTADO_KINESIS",
                "Solicitud registrada. Consulte /certificados para verificar su emision");
    }
    private ResponseStatusException error(HttpStatus status, String message) { return new ResponseStatusException(status, message); }
    public record Evento(int version, String eventId, String idUsuario, Long idCurso, String codigoCurso,
                         String nombreCurso, String nivelCurso, String formato, Instant fechaCompletado, Instant fechaSolicitud) {}
    public record Resultado(String eventId, Long idCurso, String codigoCurso, String formato, String estado,
                            String estadoEnvio, String mensaje) {}
}
