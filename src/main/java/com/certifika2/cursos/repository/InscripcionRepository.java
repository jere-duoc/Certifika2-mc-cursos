package com.certifika2.cursos.repository;

import com.certifika2.cursos.model.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {
    List<Inscripcion> findByIdCurso(Long idCurso);
    List<Inscripcion> findByIdUsuario(String idUsuario);
    Optional<Inscripcion> findByIdCursoAndIdUsuario(Long idCurso, String idUsuario);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select i from Inscripcion i where i.idCurso = :curso and i.idUsuario = :usuario")
    Optional<Inscripcion> bloquear(@org.springframework.data.repository.query.Param("curso") Long curso,
                                  @org.springframework.data.repository.query.Param("usuario") String usuario);
}

