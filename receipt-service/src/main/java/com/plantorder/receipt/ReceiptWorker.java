package com.plantorder.receipt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plantorder.common.Channels;
import com.plantorder.common.PlantRules;
import com.plantorder.common.ReceiptCommand;
import com.plantorder.common.ReceiptEvent;
import jakarta.persistence.EntityManager;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class ReceiptWorker {

    private final ReceiptRepository receipts;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;
    private final TransactionTemplate tx;

    public ReceiptWorker(ReceiptRepository receipts, OutboxRepository outbox, ObjectMapper mapper, EntityManager em,
            PlatformTransactionManager manager) {
        this.receipts = receipts;
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
        this.tx = new TransactionTemplate(manager);
    }

    @RabbitListener(queues = Channels.RECEIVE)
    public void onReceive(ReceiptCommand command) {
        Boolean dead = tx.execute(status -> {
            if (!InboxClaims.claim(em, command.orderId() + ":receive")) {
                return false;
            }
            if (PlantRules.closedDock(command.dock())) {
                publish(command.orderId(), 0, "FAILED");
                return true;
            }
            ReceiptEntity row = new ReceiptEntity();
            row.setOrderId(command.orderId());
            row.setQuantity(command.quantity());
            row.setDock(command.dock());
            row.setStatus("RECEIVED");
            receipts.save(row);
            publish(command.orderId(), command.quantity(), "RECEIVED");
            return false;
        });
        if (Boolean.TRUE.equals(dead)) {
            throw new AmqpRejectAndDontRequeueException("dock closed");
        }
    }

    private void publish(String orderId, int quantity, String status) {
        OutboxWriter.kafka(outbox, mapper, Channels.RECEIPTS, orderId, new ReceiptEvent(orderId, quantity, status));
    }
}
