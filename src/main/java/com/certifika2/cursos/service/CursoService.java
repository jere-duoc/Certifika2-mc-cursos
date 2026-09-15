package com.certifika2.cursos.service;

import com.certifika2.cursos.model.Curso;
import com.certifika2.cursos.model.Inscripcion;
import com.certifika2.cursos.model.MaterialCurso;
import com.certifika2.cursos.repository.CursoRepository;
import com.certifika2.cursos.repository.InscripcionRepository;
import com.certifika2.cursos.repository.MaterialCursoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CursoService {

    private final CursoRepository cursoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final MaterialCursoRepository materialCursoRepository;

    public Curso crearCurso(Curso curso, String idProfesor) {
        String codigo = curso.getCodigo() == null ? "" : curso.getCodigo().trim().toLowerCase();
        String nombre = curso.getNombreCurso() == null ? "" : curso.getNombreCurso().trim();
        if (!codigo.matches("[a-z0-9]+(?:-[a-z0-9]+)*") || nombre.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código o nombre de curso inválido");
        }
        if (cursoRepository.findByCodigo(codigo).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un curso con ese código");
        }
        curso.setCodigo(codigo);
        curso.setNombreCurso(nombre);
        curso.setIdProfesor(idProfesor);
        return cursoRepository.save(curso);
    }

    public List<Curso> listarCursosDelProfesor(String idProfesor) {
        return cursoRepository.findByIdProfesor(idProfesor);
    }

    public List<Curso> listarCursos() {
        return cursoRepository.findAll();
    }

    public Curso obtenerCursoPorCodigo(String codigo) {
        return cursoRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado"));
    }

    public Inscripcion inscribirEstudiante(Long idCurso, String idUsuario) {
        if (!cursoRepository.existsById(idCurso)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado");
        }
        Optional<Inscripcion> inscripcionExistente = inscripcionRepository.findByIdCursoAndIdUsuario(idCurso, idUsuario);
        if (inscripcionExistente.isPresent()) {
            return inscripcionExistente.get();
        }

        Inscripcion nuevaInscripcion = new Inscripcion();
        nuevaInscripcion.setIdCurso(idCurso);
        nuevaInscripcion.setIdUsuario(idUsuario);
        nuevaInscripcion.setFechaInscripcion(new Date());
        nuevaInscripcion.setEstaInscrito(true);

        return inscripcionRepository.save(nuevaInscripcion);
    }

    public Inscripcion inscribirEstudiantePorCodigo(String codigo, String idUsuario) {
        Curso curso = cursoRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado"));
        return inscribirEstudiante(curso.getIdCurso(), idUsuario);
    }

    public List<Curso> listarCursosInscritos(String idUsuario) {
        List<Long> ids = inscripcionRepository.findByIdUsuario(idUsuario).stream()
                .filter(inscripcion -> Boolean.TRUE.equals(inscripcion.getEstaInscrito()))
                .map(Inscripcion::getIdCurso)
                .toList();
        return cursoRepository.findAllById(ids);
    }

    public List<MaterialCurso> listarMateriales(String codigo) {
        Curso curso = obtenerCursoPorCodigo(codigo);
        return materialCursoRepository.findByIdCursoOrderByFechaCreacionAsc(curso.getIdCurso());
    }

    public MaterialCurso agregarMaterial(
            String codigo,
            String idUsuario,
            boolean esAdmin,
            String titulo,
            String tipo,
            String url) {
        Curso curso = obtenerCursoPorCodigo(codigo);
        if (!esAdmin && !Objects.equals(curso.getIdProfesor(), idUsuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el profesor dueño puede agregar material");
        }
        String tituloLimpio = titulo == null ? "" : titulo.trim();
        String tipoLimpio = tipo == null ? "" : tipo.trim();
        String urlLimpia = url == null ? "" : url.trim();
        if (tituloLimpio.isBlank() || tipoLimpio.isBlank()
                || !(urlLimpia.startsWith("https://") || urlLimpia.startsWith("http://"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Título, tipo o URL de material inválidos");
        }
        MaterialCurso material = new MaterialCurso();
        material.setIdCurso(curso.getIdCurso());
        material.setTitulo(tituloLimpio);
        material.setTipo(tipoLimpio);
        material.setUrl(urlLimpia);
        return materialCursoRepository.save(material);
    }
}

