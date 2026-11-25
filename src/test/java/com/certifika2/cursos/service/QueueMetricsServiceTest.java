package com.certifika2.cursos.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class QueueMetricsServiceTest {
    @Test void missingAwsClientIsUnknownRatherThanAnEmptyQueue() {
        var result = new QueueMetricsService(null, new ObjectMapper()).inspect("queue");
        assertThat(result.main().state()).isEqualTo("UNAVAILABLE");
        assertThat(result.main().ready()).isNull();
        assertThat(result.dlq().ready()).isNull();
    }
}
