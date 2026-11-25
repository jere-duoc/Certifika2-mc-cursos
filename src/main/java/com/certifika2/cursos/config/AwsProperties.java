package com.certifika2.cursos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Conexión a AWS. Si {@code endpoint} tiene valor se apunta a LocalStack
 * con credenciales de prueba; si está vacío se usa AWS real con la cadena
 * de credenciales por defecto (variables de entorno, perfil o rol de la instancia).
 */
@ConfigurationProperties(prefix = "certifika2.aws")
public record AwsProperties(String region, String endpoint) {

    public boolean usaEndpointLocal() {
        return endpoint != null && !endpoint.isBlank();
    }
}
