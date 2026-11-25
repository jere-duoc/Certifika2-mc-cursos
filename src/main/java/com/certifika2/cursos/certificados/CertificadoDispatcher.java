package com.certifika2.cursos.certificados;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.nio.charset.StandardCharsets;

@Service @Slf4j @ConditionalOnProperty(name = "app.certificados.enabled", havingValue = "true")
public class CertificadoDispatcher {
    private final SolicitudCertificadoRepository repository;
    private final CertificateAwsClient aws;
    private final String stream;
    public CertificadoDispatcher(SolicitudCertificadoRepository repository, CertificateAwsClient aws,
            @Value("${app.certificados.stream}") String stream) {
        this.repository = repository; this.aws = aws; this.stream = stream;
        if (stream.isBlank()) throw new IllegalArgumentException("Kinesis stream obligatorio");
    }
    @Scheduled(fixedDelayString = "${app.certificados.dispatch-delay-ms:5000}") @Transactional
    public void enviar() {
        for (var s : repository.pendientes(Instant.now(), PageRequest.of(0, 20))) {
            s.setIntentos(s.getIntentos() + 1);
            try {
                var result = aws.kinesis("PutRecord", Map.of("StreamName", stream, "PartitionKey", s.getIdUsuario(),
                        "Data", Base64.getEncoder().encodeToString(s.getPayload().getBytes(StandardCharsets.UTF_8))));
                if (!result.hasNonNull("SequenceNumber")) throw new IllegalStateException("Kinesis sin confirmacion");
                s.setEnviadoEn(Instant.now()); s.setUltimoError(null);
            } catch (RuntimeException ex) {
                s.setUltimoError(ex.getClass().getSimpleName());
                s.setProximoIntento(Instant.now().plusSeconds(Math.min(300, 5L << Math.min(s.getIntentos(), 6))));
                log.warn("Certificado {} pendiente de Kinesis, intento {}", s.getEventId(), s.getIntentos());
            }
        }
    }
}
