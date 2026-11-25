package com.certifika2.cursos.certificados;

import com.certifika2.cursos.model.Curso;
import com.certifika2.cursos.model.Inscripcion;
import com.certifika2.cursos.repository.CursoRepository;
import com.certifika2.cursos.repository.InscripcionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/cursos/demo")
@RequiredArgsConstructor
public class DemoLearningController {
    private final CursoRepository cursos;
    private final InscripcionRepository inscripciones;
    private final CertificadoSolicitudService certificados;

    @GetMapping("/avance")
    public List<Avance> avance(@AuthenticationPrincipal Jwt jwt) {
        return inscripciones.findByIdUsuario(jwt.getSubject()).stream()
                .filter(i -> i.getNotaDemo() != null).map(this::vista).filter(Objects::nonNull).toList();
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Avance> admin() {
        return inscripciones.findByNotaDemoIsNotNull(PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "fechaDemo")))
                .stream().map(this::vista).filter(Objects::nonNull).toList();
    }

    @PostMapping("/completar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Transactional
    public List<Avance> completar(@AuthenticationPrincipal Jwt jwt) {
        List<Avance> result = new ArrayList<>();
        for (Inscripcion candidate : inscripciones.findByIdUsuario(jwt.getSubject())) {
            if (!Boolean.TRUE.equals(candidate.getEstaInscrito())) continue;
            Curso course = cursos.findById(candidate.getIdCurso()).orElse(null);
            // Legacy synthetic courses are excluded: only use existing catalogue enrolments.
            if (course == null || course.getCodigo() == null || course.getCodigo().startsWith("demo-")) continue;
            Inscripcion enrollment = inscripciones.bloquear(course.getIdCurso(), jwt.getSubject()).orElseThrow();
            if (enrollment.getNotaDemo() == null) {
                enrollment.setNotaDemo((55 + Math.floorMod(Objects.hash(jwt.getSubject(), course.getIdCurso()), 16)) / 10.0);
                enrollment.setFechaDemo(Instant.now());
            }
            if (enrollment.getFechaCompletado() == null) {
                enrollment.setFechaCompletado(Instant.now());
                enrollment.setCompletadoPor("DEMO");
            }
            inscripciones.saveAndFlush(enrollment);
            certificados.solicitar(course, jwt.getSubject(), "PDF");
            result.add(vista(enrollment));
        }
        if (result.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inscríbete en un curso del catálogo antes de generar la demostración.");
        return result;
    }

    private Avance vista(Inscripcion enrollment) {
        Curso course = cursos.findById(enrollment.getIdCurso()).orElse(null);
        if (course == null || course.getCodigo() == null || course.getCodigo().startsWith("demo-")) return null;
        String event = UUID.nameUUIDFromBytes((enrollment.getIdUsuario() + ":" + course.getIdCurso() + ":PDF")
                .getBytes(StandardCharsets.UTF_8)).toString();
        return new Avance(course.getCodigo(), course.getNombreCurso(), enrollment.getNotaDemo(), "COMPLETADO",
                event, enrollment.getIdUsuario(), enrollment.getFechaDemo());
    }
    public record Avance(String codigo, String nombreCurso, Double nota, String estado, String eventId,
                         String idUsuario, Instant fechaDemo) {}
}