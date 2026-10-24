package com.plantorder.material;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plantorder.common.Channels;
import com.plantorder.common.ConsumeCommand;
import com.plantorder.common.MaterialEvent;
import com.plantorder.common.ReleaseMaterialCommand;
import com.plantorder.common.ReserveCommand;
import jakarta.persistence.EntityManager;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MaterialWorker {

    private final StockRepository stocks;
    private final ReservationRepository reservations;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;

    public MaterialWorker(StockRepository stocks, ReservationRepository reservations, OutboxRepository outbox,
            ObjectMapper mapper, EntityManager em) {
        this.stocks = stocks;
        this.reservations = reservations;
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
    }

    @RabbitListener(queues = Channels.RESERVE, concurrency = "4")
    @Transactional
    public void onReserve(ReserveCommand command) {
        if (!InboxClaims.claim(em, command.orderId() + ":" + command.sku() + ":reserve")) {
            return;
        }
        if (command.quantity() <= 0 || stocks.reserve(command.sku(), command.quantity()) != 1) {
            publish(command.orderId(), command.sku(), "SHORT");
            return;
        }
        ReservationEntity row = new ReservationEntity();
        row.setId(command.orderId() + ":" + command.sku());
        row.setOrderId(command.orderId());
        row.setSku(command.sku());
        row.setQuantity(command.quantity());
        row.setState("HELD");
        reservations.save(row);
        publish(command.orderId(), command.sku(), "RESERVED");
    }

    @RabbitListener(queues = Channels.MATERIAL_RELEASE, concurrency = "4")
    @Transactional
    public void onRelease(ReleaseMaterialCommand command) {
        if (!InboxClaims.claim(em, command.orderId() + ":" + command.sku() + ":material-release")) {
            return;
        }
        int deleted = em.createNativeQuery("DELETE FROM reservations WHERE id = :id AND state = 'HELD'")
                .setParameter("id", command.orderId() + ":" + command.sku())
                .executeUpdate();
        if (deleted != 1 || stocks.release(command.sku(), command.quantity()) != 1) {
            throw new IllegalStateException("material release missed " + command.orderId());
        }
        publish(command.orderId(), command.sku(), "RELEASED");
    }

    @RabbitListener(queues = Channels.CONSUME, concurrency = "4")
    @Transactional
    public void onConsume(ConsumeCommand command) {
        if (!InboxClaims.claim(em, command.orderId() + ":consume")) {
            return;
        }
        int updated = em.createNativeQuery("UPDATE reservations SET state = 'CONSUMED' WHERE order_id = :id AND state = 'HELD'")
                .setParameter("id", command.orderId())
                .executeUpdate();
        if (updated != command.lineCount()) {
            throw new IllegalStateException("material consume missed " + command.orderId());
        }
        publish(command.orderId(), "", "CONSUMED");
    }

    private void publish(String orderId, String sku, String status) {
        OutboxWriter.kafka(outbox, mapper, Channels.MATERIALS, orderId, new MaterialEvent(orderId, sku, status));
    }
}
