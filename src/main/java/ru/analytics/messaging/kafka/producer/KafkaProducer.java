package ru.analytics.messaging.kafka.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.analytics.application.event.ClientRegisteredEvent;
import ru.analytics.application.event.TransferCompletedEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendTransferCompleted(TransferCompletedEvent event) {
        log.info("Sending TransferCompletedEvent to Kafka: {}", event);
        kafkaTemplate.send(
                "analytics-events",
                event.externalReference(),
                event
        );
    }

    public void sendClientCreated(ClientRegisteredEvent event) {
        log.info("Sending ClientRegisteredEvent to Kafka: {}", event);
        kafkaTemplate.send(
                "analytics-events",
                event.clientId().toString(),
                event
        );
    }

}

