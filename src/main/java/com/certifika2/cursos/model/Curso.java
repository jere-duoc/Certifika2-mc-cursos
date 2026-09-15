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

    @Column(unique = true, length = 120)
    private String codigo;

    @Column(nullable = false)
    private String nombreCurso;

    private String nivelCurso;

    @Column(nullable = false)
    private String idProfesor; // `sub` estable del usuario en Cognito

    @Column(length = 1000)
    private String descripcion;
}

