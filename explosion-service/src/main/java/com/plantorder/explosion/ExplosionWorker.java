package com.plantorder.explosion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plantorder.common.Channels;
import com.plantorder.common.ExplodeCommand;
import com.plantorder.common.ExplosionEvent;
import com.plantorder.common.PlantRules;
import jakarta.persistence.EntityManager;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ExplosionWorker {

    private final ExplosionRepository explosions;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;

    public ExplosionWorker(ExplosionRepository explosions, OutboxRepository outbox, ObjectMapper mapper,
            EntityManager em) {
        this.explosions = explosions;
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
    }

    @RabbitListener(queues = Channels.EXPLODE)
    @Transactional
    public void onExplode(ExplodeCommand command) {
        if (!InboxClaims.claim(em, command.orderId() + ":explode")) {
            return;
        }
        if (command.lines() == null || command.lines().isBlank() || command.quantity() <= 0) {
            publish(command.orderId(), "", 0, "EMPTY");
            return;
        }
        try {
            String exploded = PlantRules.explode(command.lines(), command.quantity());
            int material = PlantRules.materialCents(exploded);
            ExplosionEntity row = new ExplosionEntity();
            row.setOrderId(command.orderId());
            row.setLines(exploded);
            row.setMaterialCents(material);
            row.setStatus("EXPLODED");
            explosions.save(row);
            publish(command.orderId(), exploded, material, "EXPLODED");
        } catch (ArithmeticException ex) {
            publish(command.orderId(), "", 0, "REJECTED");
        }
    }

    private void publish(String orderId, String lines, int materialCents, String status) {
        OutboxWriter.kafka(outbox, mapper, Channels.EXPLOSIONS, orderId,
                new ExplosionEvent(orderId, lines, materialCents, status));
    }
}
