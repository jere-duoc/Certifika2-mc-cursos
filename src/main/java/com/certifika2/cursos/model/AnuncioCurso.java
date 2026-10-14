package com.certifika2.cursos.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "anuncios_curso")
@Data
@NoArgsConstructor
public class AnuncioCurso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAnuncio;

    @Column(nullable = false)
    private Long idCurso;

    @Column(nullable = false, length = 120)
    private String codigoCurso;

    @Column(nullable = false, length = 180)
    private String titulo;

    @Column(nullable = false, length = 3000)
    private String contenido;

    @Column(nullable = false)
    private String idProfesor;

    private String nombreProfesor;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
