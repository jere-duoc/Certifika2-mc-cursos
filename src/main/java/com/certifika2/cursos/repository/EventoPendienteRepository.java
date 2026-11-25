package com.certifika2.cursos.repository;

import com.certifika2.cursos.model.EventoPendiente;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import java.time.Instant;
import java.util.List;

public interface EventoPendienteRepository extends JpaRepository<EventoPendiente, String> {
    long countByEnviado(boolean enviado);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<EventoPendiente> findTop20ByEnviadoFalseAndProximoIntentoLessThanEqualOrderByProximoIntentoAsc(Instant ahora);
}
