package com.certifika2.cursos.certificados;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

/** Small SigV4 transport for Kinesis JSON and S3 objects using the existing AWS SDK.
 * Uses the standard credentials chain; never stores keys or returns AWS error bodies.
 */
@Component
public class CertificateAwsClient {
    private final ObjectMapper mapper;
    private final Region region;
    private final String kinesisEndpoint;
    private final String s3Endpoint;
    private final software.amazon.awssdk.auth.credentials.AwsCredentialsProvider credentials;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();

    public CertificateAwsClient(ObjectMapper mapper,
            @Value("${AWS_REGION:us-east-1}") String region,
            @Value("${app.certificados.kinesis-endpoint:}") String kinesisEndpoint,
            @Value("${app.certificados.s3-endpoint:}") String s3Endpoint) {
        this(mapper, region, kinesisEndpoint, s3Endpoint, DefaultCredentialsProvider.create());
    }

    CertificateAwsClient(ObjectMapper mapper, String region, String kinesisEndpoint, String s3Endpoint,
            software.amazon.awssdk.auth.credentials.AwsCredentialsProvider credentials) {
        this.credentials = credentials;
        this.mapper = mapper;
        this.region = Region.of(region);
        this.kinesisEndpoint = kinesisEndpoint.isBlank() ? "https://kinesis." + region + ".amazonaws.com" : kinesisEndpoint;
        this.s3Endpoint = s3Endpoint.isBlank() ? "https://s3." + region + ".amazonaws.com" : s3Endpoint;
    }

    public JsonNode kinesis(String operation, Map<String, ?> payload) {
        try {
            byte[] data = mapper.writeValueAsBytes(payload);
            return mapper.readTree(send("kinesis", URI.create(kinesisEndpoint), SdkHttpMethod.POST,
                    data, Map.of("Content-Type", "application/x-amz-json-1.1",
                            "X-Amz-Target", "Kinesis_20131202." + operation)));
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Respuesta Kinesis invalida", ex);
        }
    }

    public void putObject(String bucket, String key, byte[] data, String type) {
        send("s3", objectUri(bucket, key), SdkHttpMethod.PUT, data,
                Map.of("Content-Type", type, "x-amz-server-side-encryption", "AES256"));
    }

    public byte[] getObject(String bucket, String key) {
        return send("s3", objectUri(bucket, key), SdkHttpMethod.GET, new byte[0], Map.of());
    }

    private URI objectUri(String bucket, String key) {
        if (!bucket.matches("[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]")
                || !key.matches("[a-zA-Z0-9/_\\-.]+") || key.contains("..")) {
            throw new IllegalArgumentException("Ubicacion del certificado invalida");
        }
        return URI.create(s3Endpoint.replaceAll("/$", "") + "/" + bucket + "/" + key);
    }

    private byte[] send(String service, URI uri, SdkHttpMethod method, byte[] body, Map<String, String> headers) {
        try {
            var builder = SdkHttpFullRequest.builder().uri(uri).method(method)
                    .contentStreamProvider(() -> new ByteArrayInputStream(body));
            headers.forEach(builder::putHeader);
            builder.putHeader("x-amz-content-sha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body)));
            var signed = Aws4Signer.create().sign(builder.build(), Aws4SignerParams.builder()
                    .awsCredentials(credentials.resolveCredentials()).signingName(service).signingRegion(region).build());
            var request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10));
            signed.headers().forEach((name, values) -> {
                if (!name.equalsIgnoreCase("host") && !name.equalsIgnoreCase("content-length"))
                    values.forEach(value -> request.header(name, value));
            });
            request.method(method.name(), body.length == 0
                    ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofByteArray(body));
            var response = http.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new IllegalStateException(service + " HTTP " + response.statusCode());
            return response.body();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Operacion AWS interrumpida", ex);
        } catch (java.io.IOException | java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Servicio AWS no disponible", ex);
        }
    }

    @jakarta.annotation.PreDestroy
    public void close() {
        if (credentials instanceof AutoCloseable closeable) {
            try { closeable.close(); }
            catch (Exception ex) { throw new IllegalStateException("No fue posible cerrar el proveedor AWS", ex); }
        }
    }
}

