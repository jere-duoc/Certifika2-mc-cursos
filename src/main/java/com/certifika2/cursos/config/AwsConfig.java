package com.certifika2.cursos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kinesis.KinesisClientBuilder;

import java.net.URI;

@Configuration
public class AwsConfig {

    @Bean(destroyMethod = "close")
    public KinesisClient kinesisClient(AwsProperties props) {
        KinesisClientBuilder builder = KinesisClient.builder()
                .region(Region.of(props.region()));
        if (props.usaEndpointLocal()) {
            builder.endpointOverride(URI.create(props.endpoint()))
                   .credentialsProvider(StaticCredentialsProvider.create(
                           AwsBasicCredentials.create("test", "test")));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        return builder.build();
    }
}
