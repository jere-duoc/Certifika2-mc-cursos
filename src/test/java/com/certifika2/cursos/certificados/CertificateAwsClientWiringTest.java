package com.certifika2.cursos.certificados;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.assertThat;
class CertificateAwsClientWiringTest {
    @Test void springCanCreateClientWithoutResolvingAwsCredentials() {
        new ApplicationContextRunner()
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(CertificateAwsClient.class)
            .run(context -> assertThat(context).hasSingleBean(CertificateAwsClient.class));
    }
}
