package com.certifika2.cursos.service;
import com.certifika2.cursos.dto.AnuncioEvent;
import com.certifika2.cursos.model.*;
import com.certifika2.cursos.repository.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import org.mockito.ArgumentCaptor;

class CursoNotificacionesTest {
    CursoRepository cursos = mock(CursoRepository.class);
    InscripcionRepository inscritos = mock(InscripcionRepository.class);
    MaterialCursoRepository materiales = mock(MaterialCursoRepository.class);
    AnuncioCursoRepository anuncios = mock(AnuncioCursoRepository.class);
    AnuncioProducerService producer = mock(AnuncioProducerService.class);
    CursoService service = new CursoService(cursos, inscritos, materiales, anuncios, producer);
    void setup() {
        Curso curso = new Curso(); curso.setIdCurso(1L); curso.setCodigo("curso"); curso.setIdProfesor("prof");
        when(cursos.findByCodigo("curso")).thenReturn(Optional.of(curso));
        Inscripcion active = new Inscripcion(); active.setIdUsuario("student"); active.setEstaInscrito(true);
        Inscripcion inactive = new Inscripcion(); inactive.setIdUsuario("inactive"); inactive.setEstaInscrito(false);
        when(inscritos.findByIdCurso(1L)).thenReturn(List.of(active, inactive, active));
        when(anuncios.save(any())).thenAnswer(i -> i.getArgument(0));
        when(materiales.save(any())).thenAnswer(i -> i.getArgument(0));
    }
    @Test void announcementsNotifyOnlyActiveDistinctEnrollees() {
        setup(); service.publicarAnuncio("curso", "prof", false, "Profesor", "Título", "Información");
        ArgumentCaptor<AnuncioEvent> captor = ArgumentCaptor.forClass(AnuncioEvent.class);
        verify(producer).publicarAnuncio(captor.capture());
        assertThat(captor.getValue().destinatarios()).containsExactly("student");
        assertThat(captor.getValue().eventoId()).isNotBlank();
        assertThat(captor.getValue().tipoEvento()).isEqualTo("ANUNCIO");
    }
    @Test void newMaterialProducesNotificationToo() {
        setup(); service.agregarMaterial("curso", "prof", false, "Guía", "PDF", "https://example.com/guia");
        ArgumentCaptor<AnuncioEvent> captor = ArgumentCaptor.forClass(AnuncioEvent.class);
        verify(producer).publicarAnuncio(captor.capture());
        assertThat(captor.getValue().tipoEvento()).isEqualTo("MATERIAL");
        assertThat(captor.getValue().destinatarios()).containsExactly("student");
    }
    @Test void unauthorizedTeacherCannotPublish() {
        setup(); assertThatThrownBy(() -> service.publicarAnuncio("curso", "other", false, "Otro", "T", "C"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        verifyNoInteractions(producer);
    }
}
