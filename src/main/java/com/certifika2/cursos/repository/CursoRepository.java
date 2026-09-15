package com.certifika2.cursos.repository;

import com.certifika2.cursos.model.Curso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CursoRepository extends JpaRepository<Curso, Long> {
    List<Curso> findByIdProfesor(String idProfesor);
    Optional<Curso> findByCodigo(String codigo);
    Optional<Curso> findByNombreCurso(String nombreCurso);
}

