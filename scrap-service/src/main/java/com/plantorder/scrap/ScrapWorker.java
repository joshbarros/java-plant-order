package com.plantorder.scrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plantorder.common.Channels;
import com.plantorder.common.ScrapCommand;
import com.plantorder.common.ScrapEvent;
import jakarta.persistence.EntityManager;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ScrapWorker {

    private final ScrapRepository scraps;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;

    public ScrapWorker(ScrapRepository scraps, OutboxRepository outbox, ObjectMapper mapper, EntityManager em) {
        this.scraps = scraps;
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
    }

    @RabbitListener(queues = Channels.SCRAP)
    @Transactional
    public void onScrap(ScrapCommand command) {
        if (!InboxClaims.claim(em, command.orderId() + ":scrap")) {
            return;
        }
        ScrapEntity row = new ScrapEntity();
        row.setOrderId(command.orderId());
        row.setQuantity(command.quantity());
        row.setStatus("RECORDED");
        scraps.save(row);
        OutboxWriter.kafka(outbox, mapper, Channels.SCRAPS, command.orderId(),
                new ScrapEvent(command.orderId(), command.quantity(), "RECORDED"));
    }
}
