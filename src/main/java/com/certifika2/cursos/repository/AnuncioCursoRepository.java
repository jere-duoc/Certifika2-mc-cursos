package com.certifika2.cursos.repository;

import com.certifika2.cursos.model.AnuncioCurso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnuncioCursoRepository extends JpaRepository<AnuncioCurso, Long> {
    List<AnuncioCurso> findByIdCursoOrderByFechaCreacionDesc(Long idCurso);
    List<AnuncioCurso> findByCodigoCursoOrderByFechaCreacionDesc(String codigoCurso);
}
