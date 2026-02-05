package ru.analytics.messaging.kafka.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ru.analytics.application.event.ClientRegisteredEvent;
import ru.analytics.messaging.kafka.producer.KafkaProducer;

@Component
@RequiredArgsConstructor
public class ClientRegisteredEventListener {

    private final KafkaProducer producer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ClientRegisteredEvent event) {
        producer.sendClientCreated(event);
    }

}
