package com.chaitanya.tradeflow.order.outbox;

import ch.qos.logback.core.util.FixedDelay;
import com.chaitanya.tradeflow.order.domain.OutboxEvent;
import com.chaitanya.tradeflow.order.repo.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Component
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final JsonMapper jsonMapper;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository, KafkaTemplate<String, Object> kafkaTemplate,
                           JsonMapper jsonMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.jsonMapper = jsonMapper;
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPending() {
        List<OutboxEvent> pending = outboxEventRepository.findTop100BySentAtIsNullOrderByIdAsc();
        for (OutboxEvent event : pending) {
            try {
                JsonNode json = jsonMapper.readTree(event.getPayload());
                kafkaTemplate.send(event.getTopic(), event.getMessageKey(), json).join();
                event.markSent();
                log.info("Outbox {} sent to {}", event.getId(), event.getTopic());
            } catch (Exception ex) {
                log.warn("Outbox {} not sent, will retry: {}", event.getId(), ex.getMessage());
                break;                            // Kafka is probably down, so stop and try again next round
            }

        }
    }
}
