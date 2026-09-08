package com.certifika2.cursos.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Entity
@Table(name = "inscripciones")
@Data
@NoArgsConstructor
public class Inscripcion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idInscripcion;

    @Column(nullable = false)
    private Long idCurso;

    @Column(nullable = false)
    private Long idUsuario;

    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaInscripcion;

    private Boolean estaInscrito;
}

