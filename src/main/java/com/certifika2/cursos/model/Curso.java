package com.certifika2.cursos.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cursos")
@Data
@NoArgsConstructor
public class Curso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCurso;

    @Column(nullable = false)
    private String nombreCurso;

    private String nivelCurso;

    @Column(nullable = false)
    private Long idProfesor; // Refiere al ID del usuario creador

    @Column(length = 1000)
    private String descripcion;
}

