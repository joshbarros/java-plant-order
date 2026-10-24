package com.plantorder.cost;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plantorder.common.Channels;
import com.plantorder.common.CostCommand;
import com.plantorder.common.CostEvent;
import com.plantorder.common.PlantRules;
import jakarta.persistence.EntityManager;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CostWorker {

    private final CostRepository costs;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;

    public CostWorker(CostRepository costs, OutboxRepository outbox, ObjectMapper mapper, EntityManager em) {
        this.costs = costs;
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
    }

    @RabbitListener(queues = Channels.COST)
    @Transactional
    public void onRoll(CostCommand command) {
        if (!InboxClaims.claim(em, command.orderId() + ":cost")) {
            return;
        }
        int amount = PlantRules.rollup(command.materialCents(), command.laborCents());
        CostEntity row = new CostEntity();
        row.setOrderId(command.orderId());
        row.setMaterialCents(command.materialCents());
        row.setLaborCents(command.laborCents());
        row.setAmountCents(amount);
        row.setStatus("POSTED");
        costs.save(row);
        OutboxWriter.kafka(outbox, mapper, Channels.COSTS, command.orderId(),
                new CostEvent(command.orderId(), amount, "POSTED"));
    }
}
