package com.certifika2.cursos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "eventos_curso_pendientes", indexes = @Index(name = "idx_evento_pendiente", columnList = "enviado,proximoIntento"))
@Getter @Setter
public class EventoPendiente {
    @Id private String id;
    @Column(nullable = false, columnDefinition = "LONGTEXT") private String payload;
    @Column(nullable = false) private boolean enviado;
    @Column(nullable = false) private Instant proximoIntento = Instant.now();
    private int intentos;
}
