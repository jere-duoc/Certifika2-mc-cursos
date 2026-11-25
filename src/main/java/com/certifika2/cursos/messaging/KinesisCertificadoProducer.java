package com.certifika2.cursos.messaging;

import com.certifika2.cursos.config.KinesisProperties;
import com.certifika2.cursos.exception.PublicacionEventoException;
import com.certifika2.cursos.messaging.dto.CertificadoSolicitadoEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;

/**
 * Infraestructura de mensajería: serializa el evento a JSON y lo escribe en Kinesis.
 * La lógica de negocio (quién puede pedir un certificado) vive en CertificadoCursoService.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KinesisCertificadoProducer {

    private final KinesisClient kinesisClient;
    private final KinesisProperties kinesisProperties;
    private final ObjectMapper objectMapper;

    public PutRecordResponse publicar(CertificadoSolicitadoEvent evento) {
        try {
            String json = objectMapper.writeValueAsString(evento);
            PutRecordRequest request = PutRecordRequest.builder()
                    .streamName(kinesisProperties.streamSolicitudes())
                    // Mismo usuario -> mismo shard -> sus solicitudes se procesan en orden.
                    .partitionKey(evento.idUsuario())
                    .data(SdkBytes.fromUtf8String(json))
                    .build();

            // El SDK ya reintenta errores transitorios (throttling, 5xx) con backoff.
            PutRecordResponse response = kinesisClient.putRecord(request);
            log.info("Evento {} publicado en Kinesis stream={} shard={} seq={}",
                    evento.eventId(), kinesisProperties.streamSolicitudes(),
                    response.shardId(), response.sequenceNumber());
            return response;
        } catch (JsonProcessingException e) {
            throw new PublicacionEventoException("No se pudo serializar el evento " + evento.eventId(), e);
        } catch (SdkException e) {
            log.error("Fallo al publicar evento {} en Kinesis: {}", evento.eventId(), e.getMessage());
            throw new PublicacionEventoException("Kinesis no disponible, intente nuevamente", e);
        }
    }
}
