package com.plantorder.labor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plantorder.common.Channels;
import com.plantorder.common.LaborCommand;
import com.plantorder.common.LaborEvent;
import com.plantorder.common.PlantRules;
import jakarta.persistence.EntityManager;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LaborWorker {

    private final LaborRepository entries;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;

    public LaborWorker(LaborRepository entries, OutboxRepository outbox, ObjectMapper mapper, EntityManager em) {
        this.entries = entries;
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
    }

    @RabbitListener(queues = Channels.ACCRUE)
    @Transactional
    public void onAccrue(LaborCommand command) {
        if (!InboxClaims.claim(em, command.orderId() + ":labor")) {
            return;
        }
        int amount;
        try {
            amount = PlantRules.laborCents(command.rateCents(), command.quantity());
        } catch (ArithmeticException ex) {
            publish(command.orderId(), 0, "REJECTED");
            return;
        }
        LaborEntity row = new LaborEntity();
        row.setOrderId(command.orderId());
        row.setAmountCents(amount);
        row.setStatus("POSTED");
        entries.save(row);
        publish(command.orderId(), amount, "POSTED");
    }

    private void publish(String orderId, int amountCents, String status) {
        OutboxWriter.kafka(outbox, mapper, Channels.LABOR, orderId, new LaborEvent(orderId, amountCents, status));
    }
}
