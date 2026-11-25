package com.certifika2.cursos.service;

import com.certifika2.cursos.dto.AnuncioEvent;
import com.certifika2.cursos.model.EventoPendiente;
import com.certifika2.cursos.repository.EventoPendienteRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Slf4j @Service
public class AnuncioProducerService {
    private final SqsTemplate sqsTemplate;
    private final EventoPendienteRepository repository;
    private final ObjectMapper mapper;
    @Value("${app.sqs.queue-anuncios:cola-anuncios-cursos}") private String queue;

    public AnuncioProducerService(@Autowired(required = false) SqsTemplate sqsTemplate,
                                 EventoPendienteRepository repository, ObjectMapper mapper) {
        this.sqsTemplate = sqsTemplate;
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public void publicarAnuncio(AnuncioEvent evento) {
        try {
            EventoPendiente pendiente = new EventoPendiente();
            pendiente.setId(evento.eventoId());
            pendiente.setPayload(mapper.writeValueAsString(evento));
            repository.save(pendiente);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalStateException("No fue posible registrar la notificación", ex);
        }
    }

    // Content and outbox commit together. A failed send leaves the event available for retry.
    @Scheduled(fixedDelayString = "${app.sqs.dispatch-delay-ms:5000}")
    @Transactional
    public void despacharPendientes() {
        if (sqsTemplate == null) return;
        for (EventoPendiente evento : repository
                .findTop20ByEnviadoFalseAndProximoIntentoLessThanEqualOrderByProximoIntentoAsc(Instant.now())) {
            evento.setIntentos(evento.getIntentos() + 1);
            try {
                sqsTemplate.send(to -> to.queue(queue).payload(evento.getPayload()));
                evento.setEnviado(true);
            } catch (RuntimeException ex) {
                evento.setProximoIntento(Instant.now().plusSeconds(Math.min(300, 5L << Math.min(6, evento.getIntentos()))));
                log.warn("SQS no disponible; evento {} permanece pendiente (intento {})", evento.getId(), evento.getIntentos());
            }
        }
    }
}
