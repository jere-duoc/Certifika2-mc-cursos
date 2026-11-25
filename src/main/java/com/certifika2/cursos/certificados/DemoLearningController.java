package com.certifika2.cursos.certificados;

import com.certifika2.cursos.model.Curso;
import com.certifika2.cursos.model.Inscripcion;
import com.certifika2.cursos.repository.CursoRepository;
import com.certifika2.cursos.repository.InscripcionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/** Synthetic learning data, isolated from real courses and scoped to the caller. */
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
                .filter(i -> i.getFechaCompletado() != null)
                .map(i -> cursos.findById(i.getIdCurso()).orElse(null))
                .filter(c -> c != null && c.getCodigo() != null && c.getCodigo().startsWith(prefix(jwt.getSubject())))
                .map(c -> new Avance(c.getCodigo(), c.getNombreCurso(), nota(c.getCodigo()), "COMPLETADO", null)).toList();
    }

    @PostMapping("/completar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Transactional
    public List<Avance> completar(@AuthenticationPrincipal Jwt jwt) {
        List<Avance> result = new ArrayList<>();
        String subject = jwt.getSubject();
        for (int n = 1; n <= 2; n++) {
            String code = prefix(subject) + n;
            final int number = n;
            Curso course = cursos.findByCodigo(code).orElseGet(() -> {
                Curso c = new Curso();
                c.setCodigo(code);
                c.setNombreCurso("DEMO · " + (number == 1 ? "Fundamentos de AWS" : "Eventos con Kinesis"));
                c.setNivelCurso("Demostración");
                c.setIdProfesor(subject);
                c.setDescripcion("Curso sintético para exposición. Nota de ejemplo: " + nota(code) + ". No acredita una evaluación real.");
                return cursos.saveAndFlush(c);
            });
            Inscripcion enrollment = inscripciones.findByIdCursoAndIdUsuario(course.getIdCurso(), subject).orElseGet(Inscripcion::new);
            enrollment.setIdCurso(course.getIdCurso());
            enrollment.setIdUsuario(subject);
            if (enrollment.getFechaInscripcion() == null) enrollment.setFechaInscripcion(new Date());
            enrollment.setEstaInscrito(true);
            if (enrollment.getFechaCompletado() == null) enrollment.setFechaCompletado(Instant.now());
            enrollment.setCompletadoPor("DEMO");
            inscripciones.saveAndFlush(enrollment);
            var request = certificados.solicitar(course, subject, "PDF");
            result.add(new Avance(code, course.getNombreCurso(), nota(code), "COMPLETADO", request.eventId()));
        }
        return result;
    }

    private static String prefix(String subject) {
        return "demo-" + UUID.nameUUIDFromBytes(subject.getBytes(StandardCharsets.UTF_8)) + "-";
    }
    private static double nota(String code) { return code.endsWith("1") ? 6.5 : 6.8; }
    public record Avance(String codigo, String nombreCurso, double nota, String estado, String eventId) {}
}
