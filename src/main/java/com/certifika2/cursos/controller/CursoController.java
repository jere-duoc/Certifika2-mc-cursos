package com.certifika2.cursos.controller;

import com.certifika2.cursos.model.Curso;
import com.certifika2.cursos.model.Inscripcion;
import com.certifika2.cursos.model.MaterialCurso;
import com.certifika2.cursos.service.CursoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/cursos")
@RequiredArgsConstructor
public class CursoController {

    private final CursoService cursoService;

    @GetMapping
    public ResponseEntity<List<Curso>> listarCursos() {
        return ResponseEntity.ok(cursoService.listarCursos());
    }

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<Curso> obtenerCursoPorCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(cursoService.obtenerCursoPorCodigo(codigo));
    }

    @GetMapping("/mis-cursos")
    public ResponseEntity<List<Curso>> listarMisCursos(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(cursoService.listarCursosInscritos(jwt.getSubject()));
    }

    @GetMapping("/profesor/mis-cursos")
    @PreAuthorize("hasAnyRole('PROFESOR', 'ADMIN')")
    public ResponseEntity<List<Curso>> listarCursosDelProfesor(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(cursoService.listarCursosDelProfesor(jwt.getSubject()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PROFESOR', 'ADMIN')")
    public ResponseEntity<Curso> crearCurso(@RequestBody Curso curso, @AuthenticationPrincipal Jwt jwt) {
        Curso nuevoCurso = cursoService.crearCurso(curso, jwt.getSubject());
        return ResponseEntity.ok(nuevoCurso);
    }

    @PostMapping("/{idCurso}/inscribir")
    @PreAuthorize("hasAnyRole('ESTUDIANTE', 'PROFESOR', 'ADMIN')")
    public ResponseEntity<Inscripcion> inscribirCurso(@PathVariable Long idCurso, @AuthenticationPrincipal Jwt jwt) {
        Inscripcion inscripcion = cursoService.inscribirEstudiante(idCurso, jwt.getSubject());
        return ResponseEntity.ok(inscripcion);
    }

    @PostMapping("/codigo/{codigo}/inscribir")
    @PreAuthorize("hasAnyRole('ESTUDIANTE', 'PROFESOR', 'ADMIN')")
    public ResponseEntity<Inscripcion> inscribirCursoPorCodigo(
            @PathVariable String codigo,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(cursoService.inscribirEstudiantePorCodigo(codigo, jwt.getSubject()));
    }

    @PostMapping("/{idCurso}/upload")
    @PreAuthorize("hasAnyRole('PROFESOR', 'ADMIN')")
    public ResponseEntity<String> subirMaterial(@PathVariable Long idCurso, @RequestParam("file") MultipartFile file) {
        // Lógica para guardar el archivo (S3 o Local)
        // Por ahora simulamos que se subió exitosamente
        return ResponseEntity.ok("Archivo " + file.getOriginalFilename() + " subido exitosamente al curso " + idCurso);
    }

    @GetMapping("/codigo/{codigo}/materiales")
    public ResponseEntity<List<MaterialCurso>> listarMateriales(@PathVariable String codigo) {
        return ResponseEntity.ok(cursoService.listarMateriales(codigo));
    }

    @PostMapping("/codigo/{codigo}/materiales")
    @PreAuthorize("hasAnyRole('PROFESOR', 'ADMIN')")
    public ResponseEntity<MaterialCurso> agregarMaterial(
            @PathVariable String codigo,
            @RequestBody MaterialRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        List<String> grupos = jwt.getClaimAsStringList("cognito:groups");
        boolean esAdmin = grupos != null && grupos.contains("ADMIN");
        return ResponseEntity.ok(cursoService.agregarMaterial(
                codigo, jwt.getSubject(), esAdmin,
                request.titulo(), request.tipo(), request.url()));
    }

    public record MaterialRequest(String titulo, String tipo, String url) {}
}

