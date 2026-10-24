package com.plantorder.station;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plantorder.common.Channels;
import com.plantorder.common.PlantRules;
import com.plantorder.common.StationCommand;
import com.plantorder.common.StationEvent;
import jakarta.persistence.EntityManager;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class StationWorker {

    private final StationRepository stations;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;
    private final TransactionTemplate tx;

    public StationWorker(StationRepository stations, OutboxRepository outbox, ObjectMapper mapper, EntityManager em,
            PlatformTransactionManager manager) {
        this.stations = stations;
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
        this.tx = new TransactionTemplate(manager);
    }

    @RabbitListener(queues = Channels.STATION)
    public void onPass(StationCommand command) {
        Boolean dead = tx.execute(status -> {
            if (!InboxClaims.claim(em, command.orderId() + ":station")) {
                return false;
            }
            if (PlantRules.brokenStation(command.stationToken())) {
                publish(command.orderId(), "FAILED");
                return true;
            }
            StationEntity row = new StationEntity();
            row.setOrderId(command.orderId());
            row.setStationToken(command.stationToken());
            row.setStatus("PASSED");
            stations.save(row);
            publish(command.orderId(), "PASSED");
            return false;
        });
        if (Boolean.TRUE.equals(dead)) {
            throw new AmqpRejectAndDontRequeueException("station broken");
        }
    }

    private void publish(String orderId, String status) {
        OutboxWriter.kafka(outbox, mapper, Channels.STATIONS, orderId, new StationEvent(orderId, status));
    }
}
