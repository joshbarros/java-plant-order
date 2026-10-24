package com.plantorder.quality;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plantorder.common.Channels;
import com.plantorder.common.InspectCommand;
import com.plantorder.common.PlantRules;
import com.plantorder.common.QualityEvent;
import com.plantorder.common.QualityVoidCommand;
import jakarta.persistence.EntityManager;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class QualityWorker {

    private final QualityRepository inspections;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;

    public QualityWorker(QualityRepository inspections, OutboxRepository outbox, ObjectMapper mapper, EntityManager em) {
        this.inspections = inspections;
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
    }

    @RabbitListener(queues = Channels.INSPECT)
    @Transactional
    public void onInspect(InspectCommand command) {
        if (!InboxClaims.claim(em, command.orderId() + ":inspect")) {
            return;
        }
        if (PlantRules.scrapLot(command.lot())) {
            save(command, "SCRAP");
            publish(command.orderId(), "SCRAP");
            return;
        }
        if (PlantRules.holdLot(command.lot())) {
            publish(command.orderId(), "HOLD");
            return;
        }
        save(command, "ACCEPTED");
        publish(command.orderId(), "ACCEPTED");
    }

    @RabbitListener(queues = Channels.QUALITY_VOID)
    @Transactional
    public void onVoid(QualityVoidCommand command) {
        if (!InboxClaims.claim(em, command.orderId() + ":quality-void")) {
            return;
        }
        int updated = em.createNativeQuery("UPDATE inspections SET status = 'VOID' WHERE order_id = :id AND status = 'ACCEPTED'")
                .setParameter("id", command.orderId())
                .executeUpdate();
        if (updated != 1) {
            throw new IllegalStateException("quality void missed " + command.orderId());
        }
        publish(command.orderId(), "VOID");
    }

    private void save(InspectCommand command, String status) {
        QualityEntity row = new QualityEntity();
        row.setOrderId(command.orderId());
        row.setLot(command.lot());
        row.setStatus(status);
        inspections.save(row);
    }

    private void publish(String orderId, String status) {
        OutboxWriter.kafka(outbox, mapper, Channels.QUALITY, orderId, new QualityEvent(orderId, status));
    }
}
