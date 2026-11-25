package com.certifika2.cursos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Read-only SQS attributes. Never polls, receives, deletes or redrives messages. */
@Service
public class QueueMetricsService {
    private final SqsAsyncClient client;
    private final ObjectMapper mapper;

    public QueueMetricsService(@Autowired(required = false) SqsAsyncClient client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    public Snapshot inspect(String queue) {
        if (client == null) return new Snapshot(Depth.unavailable(), Depth.unavailable());
        try {
            String url = queue.startsWith("https://") ? queue : client.getQueueUrl(b -> b.queueName(queue)).get(3, TimeUnit.SECONDS).queueUrl();
            Map<QueueAttributeName, String> values = attributes(url, true);
            Depth main = depth(values);
            String policy = values.get(QueueAttributeName.REDRIVE_POLICY);
            if (policy == null || policy.isBlank()) return new Snapshot(main, new Depth("NOT_CONFIGURED", null, null, null));
            try {
                String arn = mapper.readTree(policy).path("deadLetterTargetArn").asText();
                String[] parts = arn.split(":", 6);
                if (parts.length != 6) return new Snapshot(main, Depth.unavailable());
                String dlqUrl = client.getQueueUrl(b -> b.queueName(parts[5]).queueOwnerAWSAccountId(parts[4])).get(3, TimeUnit.SECONDS).queueUrl();
                return new Snapshot(main, depth(attributes(dlqUrl, false)));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return new Snapshot(main, Depth.unavailable());
            } catch (Exception ex) {
                return new Snapshot(main, Depth.unavailable());
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new Snapshot(Depth.unavailable(), Depth.unavailable());
        } catch (Exception ex) {
            return new Snapshot(Depth.unavailable(), Depth.unavailable());
        }
    }

    private Map<QueueAttributeName, String> attributes(String url, boolean redrive) throws Exception {
        var names = new java.util.ArrayList<>(java.util.List.of(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES,
                QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE,
                QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED));
        if (redrive) names.add(QueueAttributeName.REDRIVE_POLICY);
        return client.getQueueAttributes(b -> b.queueUrl(url).attributeNames(names)).get(3, TimeUnit.SECONDS).attributes();
    }

    private static Depth depth(Map<QueueAttributeName, String> values) {
        return new Depth("AVAILABLE", number(values, QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES),
                number(values, QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE),
                number(values, QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED));
    }
    private static Long number(Map<QueueAttributeName, String> values, QueueAttributeName name) {
        String value = values.get(name);
        return value == null ? null : Long.valueOf(value);
    }
    public record Depth(String state, Long ready, Long inFlight, Long delayed) {
        static Depth unavailable() { return new Depth("UNAVAILABLE", null, null, null); }
    }
    public record Snapshot(Depth main, Depth dlq) {}
}
