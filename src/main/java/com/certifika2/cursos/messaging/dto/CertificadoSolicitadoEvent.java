package com.certifika2.cursos.messaging.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Contrato del evento publicado en el stream "certificados-solicitados".
 * <p>
 * Lo produce ms-cursos cuando un estudiante termina un curso y lo consume
 * ms-usuarios, que genera el certificado y lo vincula al usuario.
 * <ul>
 *   <li>{@code eventId}: identificador único; el consumidor lo usa para descartar duplicados.</li>
 *   <li>{@code version}: versión del contrato, para evolucionarlo sin romper consumidores.</li>
 *   <li>{@code idUsuario}: {@code sub} de Cognito, el mismo identificador que usan ambos servicios.</li>
 *   <li>Los datos del curso viajan en el evento porque ms-usuarios no tiene acceso a la base de cursos.</li>
 * </ul>
 * Partition key en Kinesis: {@code idUsuario} (orden garantizado por usuario).
 */
public record CertificadoSolicitadoEvent(
        String eventId,
        String eventType,
        String version,
        Instant occurredAt,
        String idUsuario,
        String emailUsuario,
        String nombreUsuario,
        Long idCurso,
        String codigoCurso,
        String nombreCurso,
        String nivelCurso,
        String idProfesor,
        String solicitadoPor,
        Instant fechaCompletado,
        FormatoCertificado formato
) {
    public static final String TYPE = "CERTIFICADO_SOLICITADO";
    public static final String VERSION = "1.0";
}
