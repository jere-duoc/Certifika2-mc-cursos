package com.certifika2.cursos.certificados;
import com.certifika2.cursos.model.*;
import com.certifika2.cursos.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class CertificadoSolicitudServiceTest {
    CursoRepository cursos = mock(CursoRepository.class);
    InscripcionRepository inscripciones = mock(InscripcionRepository.class);
    SolicitudCertificadoRepository solicitudes = mock(SolicitudCertificadoRepository.class);
    ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    CertificadoSolicitudService service = new CertificadoSolicitudService(cursos, inscripciones, solicitudes, mapper);
    Curso curso = new Curso(); Inscripcion inscripcion = new Inscripcion();
    @BeforeEach void setup() {
        curso.setIdCurso(12L); curso.setCodigo("curso-real"); curso.setNombreCurso("Streaming de datos"); curso.setIdProfesor("teacher");
        inscripcion.setIdCurso(12L); inscripcion.setIdUsuario("student"); inscripcion.setEstaInscrito(true);
        when(inscripciones.bloquear(12L, "student")).thenReturn(Optional.of(inscripcion));
    }
    @Test void enrollmentAloneCannotProduceCompletionCertificate() {
        assertThatThrownBy(() -> service.solicitar(curso, "student", "PDF")).isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException)e).getStatusCode().value()).isEqualTo(409));
        assertThat(inscripcion.getFechaCompletado()).isNull(); verifyNoInteractions(solicitudes);
    }
    @Test void unrelatedTeacherCannotApproveCompletion() {
        assertThatThrownBy(() -> service.completar(curso, "student", "outsider", false)).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(inscripciones);
    }
    @Test void ownerCompletionRecordsActorAndTimestamp() {
        service.completar(curso, "student", "teacher", false);
        assertThat(inscripcion.getFechaCompletado()).isNotNull(); assertThat(inscripcion.getCompletadoPor()).isEqualTo("teacher");
        verify(inscripciones).save(inscripcion);
    }
    @Test void completedCoursePersistsRealMetadataAndStableEvent() throws Exception {
        inscripcion.setFechaCompletado(Instant.parse("2026-01-01T10:00:00Z"));
        var response = service.solicitar(curso, "student", "PDF");
        var captor = org.mockito.ArgumentCaptor.forClass(SolicitudCertificado.class);
        verify(solicitudes).save(captor.capture());
        var event = mapper.readTree(captor.getValue().getPayload());
        assertThat(event.path("idUsuario").asText()).isEqualTo("student");
        assertThat(event.path("nombreCurso").asText()).isEqualTo("Streaming de datos");
        assertThat(response.estadoEnvio()).isEqualTo("PENDIENTE");
        when(solicitudes.findById(response.eventId())).thenReturn(Optional.of(captor.getValue()));
        assertThat(service.solicitar(curso, "student", "PDF").eventId()).isEqualTo(response.eventId());
        verify(solicitudes, times(1)).save(any());
    }
    @Test void unsupportedFormatAndInactiveEnrollmentAreRejected() {
        assertThatThrownBy(() -> service.solicitar(curso, "student", "EXE")).isInstanceOf(ResponseStatusException.class);
        inscripcion.setEstaInscrito(false);
        assertThatThrownBy(() -> service.solicitar(curso, "student", "PDF")).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(solicitudes);
    }
}
