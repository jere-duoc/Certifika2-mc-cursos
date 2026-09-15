package com.certifika2.cursos.repository;

import com.certifika2.cursos.model.MaterialCurso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterialCursoRepository extends JpaRepository<MaterialCurso, Long> {
    List<MaterialCurso> findByIdCursoOrderByFechaCreacionAsc(Long idCurso);
}
