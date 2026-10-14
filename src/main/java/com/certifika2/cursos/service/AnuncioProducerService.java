package com.certifika2.cursos.service;

import com.certifika2.cursos.dto.AnuncioEvent;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AnuncioProducerService {

    private final SqsTemplate sqsTemplate;

    @Value("${app.sqs.queue-anuncios:certifika2-anuncios-queue}")
    private String queueAnuncios;

    public AnuncioProducerService(@Autowired(required = false) SqsTemplate sqsTemplate) {
        this.sqsTemplate = sqsTemplate;
    }

    public void publicarAnuncio(AnuncioEvent evento) {
        if (sqsTemplate == null) {
            log.warn("SqsTemplate no está disponible. No se despachó el mensaje a SQS para el curso {}", evento.codigoCurso());
            return;
        }
        log.info("Publicando anuncio en SQS [{}] para curso '{}' (ID: {}): {}",
                queueAnuncios, evento.codigoCurso(), evento.idCurso(), evento.titulo());
        try {
            sqsTemplate.send(to -> to
                    .queue(queueAnuncios)
                    .payload(evento));
            log.info("Anuncio publicado exitosamente en Amazon SQS para curso {}", evento.codigoCurso());
        } catch (Exception ex) {
            log.error("Error al despachar mensaje a Amazon SQS [{}]: {}", queueAnuncios, ex.getMessage(), ex);
            throw new RuntimeException("Error al notificar por SQS: " + ex.getMessage(), ex);
        }
    }
}
