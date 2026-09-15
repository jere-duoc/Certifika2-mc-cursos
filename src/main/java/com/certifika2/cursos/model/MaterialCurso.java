package com.certifika2.cursos.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "materiales_curso")
@Data
@NoArgsConstructor
public class MaterialCurso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idMaterial;

    @Column(nullable = false)
    private Long idCurso;

    @Column(nullable = false, length = 160)
    private String titulo;

    @Column(nullable = false, length = 40)
    private String tipo;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
