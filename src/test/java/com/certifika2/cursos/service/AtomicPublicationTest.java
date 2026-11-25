package com.certifika2.cursos.service;
import com.certifika2.cursos.model.Curso;
import com.certifika2.cursos.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest @Import({CursoService.class, AnuncioProducerService.class})
class AtomicPublicationTest {
    @Autowired CursoService service;
    @Autowired CursoRepository cursos;
    @Autowired AnuncioCursoRepository anuncios;
    @Autowired EventoPendienteRepository outbox;
    @MockBean ObjectMapper mapper;

    @Test @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void failureToRecordEventRollsBackAnnouncement() throws Exception {
        Curso c = new Curso(); c.setCodigo("atomic-test"); c.setNombreCurso("Test"); c.setIdProfesor("prof");
        cursos.save(c);
        when(mapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Cannot serialize") {});
        assertThatThrownBy(() -> service.publicarAnuncio("atomic-test", "prof", false, "Profesor", "Título", "Contenido"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(anuncios.count()).isZero();
        assertThat(outbox.count()).isZero();
    }
}
