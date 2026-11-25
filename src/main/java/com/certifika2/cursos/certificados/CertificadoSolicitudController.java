package com.certifika2.cursos.certificados;

import com.certifika2.cursos.model.Inscripcion;
import com.certifika2.cursos.repository.InscripcionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.List;

@RestController @RequestMapping("/api/cursos") @RequiredArgsConstructor
public class CertificadoSolicitudController {
    private final CertificadoSolicitudService service;
    private final SolicitudCertificadoRepository solicitudes;
    private final InscripcionRepository inscripciones;

    @PostMapping("/codigo/{codigo}/certificado") @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('ESTUDIANTE', 'PROFESOR', 'ADMIN')")
    public CertificadoSolicitudService.Resultado solicitar(@PathVariable String codigo,
            @RequestParam(defaultValue = "PDF") String formato, @AuthenticationPrincipal Jwt jwt) {
        return service.solicitar(service.curso(codigo), jwt.getSubject(), formato);
    }
    @PostMapping("/{idCurso}/certificado") @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('ESTUDIANTE', 'PROFESOR', 'ADMIN')")
    public CertificadoSolicitudService.Resultado solicitarPorId(@PathVariable Long idCurso,
            @RequestParam(defaultValue = "PDF") String formato, @AuthenticationPrincipal Jwt jwt) {
        return service.solicitar(service.curso(idCurso), jwt.getSubject(), formato);
    }
    @PostMapping("/codigo/{codigo}/estudiantes/{sub}/certificado") @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('PROFESOR', 'ADMIN')")
    public CertificadoSolicitudService.Resultado emitir(@PathVariable String codigo, @PathVariable String sub,
            @RequestParam(defaultValue = "PDF") String formato, @AuthenticationPrincipal Jwt jwt) {
        var curso = service.curso(codigo);
        service.autorizar(curso, jwt.getSubject(), admin(jwt));
        return service.solicitar(curso, sub, formato);
    }
    @PostMapping("/codigo/{codigo}/estudiantes/{sub}/completar")
    @PreAuthorize("hasAnyRole('PROFESOR', 'ADMIN')")
    public Inscripcion completar(@PathVariable String codigo, @PathVariable String sub, @AuthenticationPrincipal Jwt jwt) {
        return service.completar(service.curso(codigo), sub, jwt.getSubject(), admin(jwt));
    }
    @GetMapping("/codigo/{codigo}/estudiantes") @PreAuthorize("hasAnyRole('PROFESOR', 'ADMIN')")
    public List<Inscripcion> estudiantes(@PathVariable String codigo, @AuthenticationPrincipal Jwt jwt) {
        var curso = service.curso(codigo); service.autorizar(curso, jwt.getSubject(), admin(jwt));
        return inscripciones.findByIdCurso(curso.getIdCurso());
    }
    @GetMapping("/admin/certificados") @PreAuthorize("hasRole('ADMIN')")
    public List<Entrega> entregas() {
        return solicitudes.findAll(PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "fechaSolicitud")))
                .stream().map(s -> new Entrega(s.getEventId(), s.getIdCurso(), s.getFechaSolicitud(),
                        s.getEnviadoEn(), s.getIntentos(), s.getProximoIntento(), s.getUltimoError())).toList();
    }
    private boolean admin(Jwt jwt) {
        var grupos = jwt.getClaimAsStringList("cognito:groups"); return grupos != null && grupos.contains("ADMIN");
    }
    public record Entrega(String eventId, Long idCurso, java.time.Instant fechaSolicitud, java.time.Instant aceptadoKinesis,
                          int intentos, java.time.Instant proximoIntento, String ultimoError) {}
}
