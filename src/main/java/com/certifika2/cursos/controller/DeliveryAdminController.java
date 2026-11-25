package com.certifika2.cursos.controller;

import com.certifika2.cursos.repository.EventoPendienteRepository;
import com.certifika2.cursos.service.QueueMetricsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;

@RestController
@RequestMapping("/api/cursos/admin/entregas")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class DeliveryAdminController {
    private final EventoPendienteRepository repository;
    private final ObjectMapper mapper;
    private final QueueMetricsService metrics;
    @Value("${app.sqs.queue-anuncios:certifika2-anuncios-queue}") private String queue;

    @GetMapping
    public Report list() {
        List<Event> events = repository.findAll(PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "proximoIntento")))
                .stream().map(event -> {
                    JsonNode payload;
                    try { payload = mapper.readTree(event.getPayload()); }
                    catch (Exception ex) { payload = mapper.createObjectNode(); }
                    var recipients = payload.path("destinatarios");
                    var unique = new HashSet<String>();
                    if (recipients.isArray()) recipients.forEach(value -> { if (value.isTextual() && !value.asText().isBlank()) unique.add(value.asText()); });
                    return new Event(event.getId(), payload.path("codigoCurso").asText(""), payload.path("titulo").asText("Evento sin detalle disponible"),
                            payload.path("tipoEvento").asText("ANUNCIO"), event.isEnviado(), event.getIntentos(),
                            event.isEnviado() ? null : event.getProximoIntento(), payload.path("fechaCreacion").asText(""),
                            recipients.isArray() ? unique.size() : null);
                }).toList();
        return new Report(Instant.now(), queue, repository.countByEnviado(false), repository.countByEnviado(true), events, metrics.inspect(queue));
    }

    public record Event(String id, String courseCode, String title, String type, boolean acceptedBySqs,
                        int attempts, Instant nextAttemptAt, String createdAt, Integer expectedRecipients) {}
    public record Report(Instant observedAt, String queue, long pending, long accepted,
                         List<Event> events, QueueMetricsService.Snapshot sqs) {}
}
