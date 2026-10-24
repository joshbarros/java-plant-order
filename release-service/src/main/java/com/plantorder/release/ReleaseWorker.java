package com.plantorder.release;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plantorder.common.Channels;
import com.plantorder.common.FloorCommand;
import com.plantorder.common.FloorVoidCommand;
import com.plantorder.common.ReleaseEvent;
import jakarta.persistence.EntityManager;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ReleaseWorker {

    private final ReleaseRepository releases;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;

    public ReleaseWorker(ReleaseRepository releases, OutboxRepository outbox, ObjectMapper mapper, EntityManager em) {
        this.releases = releases;
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
    }

    @RabbitListener(queues = Channels.FLOOR)
    @Transactional
    public void onIssue(FloorCommand command) {
        if (!InboxClaims.claim(em, command.orderId() + ":floor")) {
            return;
        }
        ReleaseEntity row = new ReleaseEntity();
        row.setOrderId(command.orderId());
        row.setStatus("RELEASED");
        releases.save(row);
        publish(command.orderId(), "RELEASED");
    }

    @RabbitListener(queues = Channels.FLOOR_VOID)
    @Transactional
    public void onVoid(FloorVoidCommand command) {
        if (!InboxClaims.claim(em, command.orderId() + ":floor-void")) {
            return;
        }
        int updated = em.createNativeQuery("UPDATE floor_releases SET status = 'VOID' WHERE order_id = :id AND status = 'RELEASED'")
                .setParameter("id", command.orderId())
                .executeUpdate();
        if (updated != 1) {
            throw new IllegalStateException("release void missed " + command.orderId());
        }
        publish(command.orderId(), "VOID");
    }

    private void publish(String orderId, String status) {
        OutboxWriter.kafka(outbox, mapper, Channels.RELEASES, orderId, new ReleaseEvent(orderId, status));
    }
}
