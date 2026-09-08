package com.certifika2.cursos.controller;

import com.certifika2.cursos.model.Curso;
import com.certifika2.cursos.model.Inscripcion;
import com.certifika2.cursos.model.Usuario;
import com.certifika2.cursos.service.CursoService;
import com.certifika2.cursos.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    private final UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<List<Curso>> listarCursos() {
        return ResponseEntity.ok(cursoService.listarCursos());
    }

    @PostMapping
    public ResponseEntity<Curso> crearCurso(@RequestBody Curso curso, @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        Usuario usuario = usuarioService.obtenerUsuarioPorEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
                
        // Todo: Validar que el usuario tenga ROLE_PROFESOR

        Curso nuevoCurso = cursoService.crearCurso(curso, usuario.getIdUsuario());
        return ResponseEntity.ok(nuevoCurso);
    }

    @PostMapping("/{idCurso}/inscribir")
    public ResponseEntity<Inscripcion> inscribirCurso(@PathVariable Long idCurso, @AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        Usuario usuario = usuarioService.obtenerUsuarioPorEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Inscripcion inscripcion = cursoService.inscribirEstudiante(idCurso, usuario.getIdUsuario());
        return ResponseEntity.ok(inscripcion);
    }

    @PostMapping("/{idCurso}/upload")
    public ResponseEntity<String> subirMaterial(@PathVariable Long idCurso, @RequestParam("file") MultipartFile file) {
        // Lógica para guardar el archivo (S3 o Local)
        // Por ahora simulamos que se subió exitosamente
        return ResponseEntity.ok("Archivo " + file.getOriginalFilename() + " subido exitosamente al curso " + idCurso);
    }
}

