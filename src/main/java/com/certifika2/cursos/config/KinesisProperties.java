package com.certifika2.cursos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "certifika2.kinesis")
public record KinesisProperties(String streamSolicitudes) {
}
