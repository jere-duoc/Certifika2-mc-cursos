package com.certifika2.cursos.service;
import com.certifika2.cursos.dto.AnuncioEvent;
import com.certifika2.cursos.model.EventoPendiente;
import com.certifika2.cursos.repository.EventoPendienteRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class OutboxTest {
    @Test void failedSendRemainsPendingThenRetriesSameEvent() {
        EventoPendienteRepository repo = mock(EventoPendienteRepository.class);
        SqsTemplate sqs = mock(SqsTemplate.class);
        AnuncioProducerService service = new AnuncioProducerService(sqs, repo, new ObjectMapper().findAndRegisterModules());
        ReflectionTestUtils.setField(service, "queue", "cola-anuncios-cursos");
        EventoPendiente pending = new EventoPendiente(); pending.setId("stable-event"); pending.setPayload("{}");
        when(repo.findTop20ByEnviadoFalseAndProximoIntentoLessThanEqualOrderByProximoIntentoAsc(any())).thenReturn(List.of(pending));
        doThrow(new IllegalStateException("SQS unavailable")).doReturn(null).when(sqs).send(any(java.util.function.Consumer.class));
        service.despacharPendientes();
        assertThat(pending.isEnviado()).isFalse(); assertThat(pending.getIntentos()).isEqualTo(1);
        service.despacharPendientes();
        assertThat(pending.isEnviado()).isTrue(); assertThat(pending.getId()).isEqualTo("stable-event");
    }
    @Test void enqueueSavesWireJsonWithoutSendingInline() {
        EventoPendienteRepository repo = mock(EventoPendienteRepository.class);
        SqsTemplate sqs = mock(SqsTemplate.class);
        AnuncioProducerService service = new AnuncioProducerService(sqs, repo, new ObjectMapper().findAndRegisterModules());
        service.publicarAnuncio(new AnuncioEvent(1L, "curso", "T", "C", "prof", "Profesor", LocalDateTime.now(), "id", "ANUNCIO", List.of("s")));
        verify(repo).save(argThat(e -> e.getPayload().contains("\"destinatarios\":[\"s\"]") && "id".equals(e.getId())));
        verifyNoInteractions(sqs);
    }
}
