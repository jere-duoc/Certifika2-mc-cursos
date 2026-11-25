package com.certifika2.cursos.certificados;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;

public interface SolicitudCertificadoRepository extends JpaRepository<SolicitudCertificado, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SolicitudCertificado s where s.enviadoEn is null and s.proximoIntento <= :now order by s.proximoIntento")
    List<SolicitudCertificado> pendientes(@Param("now") Instant now, Pageable page);
}
