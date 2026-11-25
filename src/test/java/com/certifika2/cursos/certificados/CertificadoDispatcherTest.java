package com.certifika2.cursos.certificados;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class CertificadoDispatcherTest {
    SolicitudCertificadoRepository repository = mock(SolicitudCertificadoRepository.class);
    CertificateAwsClient aws = mock(CertificateAwsClient.class);
    @Test void networkFailureLeavesRequestPendingForRetry() {
        var s = request(); when(repository.pendientes(any(), any())).thenReturn(List.of(s));
        when(aws.kinesis(any(), any())).thenThrow(new IllegalStateException("unavailable"));
        new CertificadoDispatcher(repository, aws, "stream").enviar();
        assertThat(s.getEnviadoEn()).isNull(); assertThat(s.getIntentos()).isEqualTo(1);
        assertThat(s.getProximoIntento()).isAfter(Instant.now());
    }
    @Test void onlyKinesisConfirmationMarksAccepted() throws Exception {
        var s = request(); when(repository.pendientes(any(), any())).thenReturn(List.of(s));
        when(aws.kinesis(eq("PutRecord"), any())).thenReturn(new ObjectMapper().readTree("{\"SequenceNumber\":\"123\"}"));
        new CertificadoDispatcher(repository, aws, "stream").enviar();
        assertThat(s.getEnviadoEn()).isNotNull(); assertThat(s.getUltimoError()).isNull();
    }
    private SolicitudCertificado request() {
        var s = new SolicitudCertificado(); s.setEventId("event"); s.setIdUsuario("student"); s.setPayload("{}"); return s;
    }
}
