package com.certifika2.cursos.service;

import com.certifika2.cursos.model.Curso;
import com.certifika2.cursos.model.Inscripcion;
import com.certifika2.cursos.repository.CursoRepository;
import com.certifika2.cursos.repository.InscripcionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CursoService {

    private final CursoRepository cursoRepository;
    private final InscripcionRepository inscripcionRepository;

    public Curso crearCurso(Curso curso, Long idProfesor) {
        curso.setIdProfesor(idProfesor);
        return cursoRepository.save(curso);
    }

    public List<Curso> listarCursos() {
        return cursoRepository.findAll();
    }

    public Inscripcion inscribirEstudiante(Long idCurso, Long idUsuario) {
        Optional<Inscripcion> inscripcionExistente = inscripcionRepository.findByIdCursoAndIdUsuario(idCurso, idUsuario);
        if (inscripcionExistente.isPresent()) {
            throw new RuntimeException("El estudiante ya está inscrito en este curso");
        }

        Inscripcion nuevaInscripcion = new Inscripcion();
        nuevaInscripcion.setIdCurso(idCurso);
        nuevaInscripcion.setIdUsuario(idUsuario);
        nuevaInscripcion.setFechaInscripcion(new Date());
        nuevaInscripcion.setEstaInscrito(true);

        return inscripcionRepository.save(nuevaInscripcion);
    }
}

