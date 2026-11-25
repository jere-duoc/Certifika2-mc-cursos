package com.certifika2.cursos.certificados;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity @Table(name = "solicitudes_certificado") @Getter @Setter
public class SolicitudCertificado {
    @Id @Column(length = 36) private String eventId;
    private Long idCurso;
    @Column(length = 128, nullable = false) private String idUsuario;
    private String formato;
    @Lob @Column(nullable = false, columnDefinition = "TEXT") private String payload;
    private Instant fechaSolicitud;
    private Instant enviadoEn;
    private int intentos;
    private Instant proximoIntento;
    private String ultimoError;
}
