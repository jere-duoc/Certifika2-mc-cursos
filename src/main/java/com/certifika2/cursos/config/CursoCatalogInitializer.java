package com.certifika2.cursos.config;

import com.certifika2.cursos.model.Curso;
import com.certifika2.cursos.repository.CursoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CursoCatalogInitializer implements CommandLineRunner {

    private final CursoRepository cursoRepository;

    @Override
    public void run(String... args) {
        catalogo().forEach(definicion -> {
            Curso curso = cursoRepository.findByCodigo(definicion.codigo())
                    .orElseGet(() -> cursoRepository.findByNombreCurso(definicion.nombre())
                            .orElseGet(Curso::new));
            curso.setCodigo(definicion.codigo());
            curso.setNombreCurso(definicion.nombre());
            curso.setNivelCurso(definicion.nivel());
            curso.setDescripcion(definicion.descripcion());
            if (curso.getIdProfesor() == null || curso.getIdProfesor().isBlank()) {
                curso.setIdProfesor("catalogo-certifika2");
            }
            cursoRepository.save(curso);
        });
    }

    private List<DefinicionCurso> catalogo() {
        return List.of(
                new DefinicionCurso("seguridad-informatica-fundamentos", "Seguridad Informática: Fundamentos", "Básico", "Conceptos esenciales de seguridad, cifrado y control de acceso."),
                new DefinicionCurso("ethical-hacking", "Ethical Hacking y Pruebas de Penetración", "Avanzado", "Metodologías ofensivas para auditar sistemas de forma autorizada."),
                new DefinicionCurso("iot-basico", "IoT: Introducción a Dispositivos Conectados", "Básico", "Sensores, actuadores y conexión segura con servicios en la nube."),
                new DefinicionCurso("edge-computing-iot", "Edge Computing aplicado a IoT", "Intermedio", "Procesamiento cercano a la fuente para soluciones IoT."),
                new DefinicionCurso("docker-kubernetes", "Docker y Kubernetes: Fundamentos", "Intermedio", "Contenedores y orquestación para aplicaciones cloud native."),
                new DefinicionCurso("cloud-native", "Cloud Native: Arquitectura y Microservicios", "Avanzado", "Microservicios, observabilidad, resiliencia y escalado."),
                new DefinicionCurso("data-analisis-python", "Análisis de Datos con Python", "Básico", "Limpieza, transformación y visualización de datos con Python."),
                new DefinicionCurso("machine-learning-fundamentos", "Machine Learning: Fundamentos", "Intermedio", "Modelos supervisados, no supervisados y evaluación."),
                new DefinicionCurso("fullstack-moderno", "Desarrollo Full-Stack Moderno", "Intermedio", "Frontend, backend, persistencia y despliegue de una aplicación completa."),
                new DefinicionCurso("redes-fundamentos", "Redes de Computadoras: Fundamentos", "Básico", "Modelo OSI, TCP/IP, direccionamiento y administración de redes.")
        );
    }

    private record DefinicionCurso(String codigo, String nombre, String nivel, String descripcion) {}
}
