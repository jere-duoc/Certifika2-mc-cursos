package com.certifika2.cursos.repository;

import com.certifika2.cursos.model.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {
    List<Inscripcion> findByIdCurso(Long idCurso);
    List<Inscripcion> findByIdUsuario(Long idUsuario);
    Optional<Inscripcion> findByIdCursoAndIdUsuario(Long idCurso, Long idUsuario);
}

