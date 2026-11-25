package com.certifika2.cursos.controller;
import com.certifika2.cursos.model.EventoPendiente;
import com.certifika2.cursos.repository.EventoPendienteRepository;
import com.certifika2.cursos.service.QueueMetricsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class DeliveryAdminControllerTest {
    @Test void pendingRetryAndDistinctRecipientCountComeFromStoredEvent() {
        var repository = mock(EventoPendienteRepository.class);
        var pending = new EventoPendiente(); pending.setId("event-id"); pending.setIntentos(3);
        pending.setPayload("{\"titulo\":\"Aviso\",\"codigoCurso\":\"curso\",\"destinatarios\":[\"a\",\"a\",\"b\",null,\" \"]}");
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(pending)));
        when(repository.countByEnviado(false)).thenReturn(1L);
        var controller = new DeliveryAdminController(repository, new ObjectMapper(), new QueueMetricsService(null, new ObjectMapper()));
        var report = controller.list();
        assertThat(report.pending()).isEqualTo(1);
        assertThat(report.events().get(0).expectedRecipients()).isEqualTo(2);
        assertThat(report.events().get(0).acceptedBySqs()).isFalse();
        assertThat(report.events().get(0).attempts()).isEqualTo(3);
        assertThat(report.sqs().main().ready()).isNull();
    }
}
