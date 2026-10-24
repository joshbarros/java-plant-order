package com.plantorder.order;

import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plantorder.common.Channels;
import com.plantorder.common.ConsumeCommand;
import com.plantorder.common.CostCommand;
import com.plantorder.common.ExplosionEvent;
import com.plantorder.common.FloorCommand;
import com.plantorder.common.FloorVoidCommand;
import com.plantorder.common.InspectCommand;
import com.plantorder.common.LaborCommand;
import com.plantorder.common.LaborEvent;
import com.plantorder.common.MaterialEvent;
import com.plantorder.common.OrderFact;
import com.plantorder.common.PlantRules;
import com.plantorder.common.QualityEvent;
import com.plantorder.common.QualityVoidCommand;
import com.plantorder.common.ReceiptCommand;
import com.plantorder.common.ReceiptEvent;
import com.plantorder.common.ReleaseEvent;
import com.plantorder.common.ReleaseMaterialCommand;
import com.plantorder.common.ReserveCommand;
import com.plantorder.common.ScrapCommand;
import com.plantorder.common.StationCommand;
import com.plantorder.common.StationEvent;
import com.plantorder.common.CostEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderSaga {

    private final OrderLineRepository lines;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;

    public OrderSaga(OrderLineRepository lines, OutboxRepository outbox, ObjectMapper mapper, EntityManager em) {
        this.lines = lines;
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
    }

    @KafkaListener(topics = Channels.EXPLOSIONS, groupId = "order-explosions")
    @Transactional
    public void onExplosion(ExplosionEvent event) {
        if (!"EXPLODED".equals(event.status()) && !"REJECTED".equals(event.status()) && !"EMPTY".equals(event.status())) {
            return;
        }
        OrderEntity row = lock(event.orderId());
        if (row == null || !"DRAFT".equals(row.getStatus())) {
            return;
        }
        if ("REJECTED".equals(event.status())) {
            cancel(row, "PRICE");
            return;
        }
        if ("EMPTY".equals(event.status())) {
            cancel(row, "EMPTY");
            return;
        }
        row.setMaterialCents(event.materialCents());
        row.setLinesText(event.lines());
        List<PlantRules.PartQty> parts = PlantRules.parse(event.lines());
        int position = 0;
        for (PlantRules.PartQty part : parts) {
            OrderLineEntity line = new OrderLineEntity();
            line.setId(row.getId() + ":" + part.sku());
            line.setOrderId(row.getId());
            line.setSku(part.sku());
            line.setQuantity(part.quantity());
            line.setStandardCents(part.standardCents());
            line.setPosition(position);
            line.setState(position == 0 ? "RESERVING" : "PENDING");
            lines.save(line);
            position++;
        }
        row.setStatus("EXPLODED");
        fact(row);
        if (!parts.isEmpty()) {
            PlantRules.PartQty first = parts.get(0);
            reserve(row.getId(), first.sku(), first.quantity());
        }
    }

    @KafkaListener(topics = Channels.MATERIALS, groupId = "order-materials")
    @Transactional
    public void onMaterial(MaterialEvent event) {
        if ("CONSUMED".equals(event.status())) {
            OrderEntity row = lock(event.orderId());
            if (row != null && "NONE".equals(row.getMaterialStatus())) {
                row.setMaterialStatus("CONSUMED");
            }
            return;
        }
        if (!"RESERVED".equals(event.status()) && !"SHORT".equals(event.status())) {
            return;
        }
        OrderEntity row = lock(event.orderId());
        if (row == null || !"EXPLODED".equals(row.getStatus())) {
            return;
        }
        OrderLineEntity line = lines.findById(event.orderId() + ":" + event.sku()).orElse(null);
        if (line == null || !"RESERVING".equals(line.getState())) {
            return;
        }
        if ("SHORT".equals(event.status())) {
            cancel(row, "SHORT");
            return;
        }
        line.setState("RESERVED");
        List<OrderLineEntity> pending = lines.findByOrderIdAndState(row.getId(), "PENDING");
        pending.sort(Comparator.comparingInt(OrderLineEntity::getPosition));
        if (!pending.isEmpty()) {
            OrderLineEntity next = pending.get(0);
            next.setState("RESERVING");
            reserve(row.getId(), next.getSku(), next.getQuantity());
            return;
        }
        row.setStatus("RESERVED");
        fact(row);
        OutboxWriter.rabbit(outbox, mapper, Channels.FLOOR, row.getId(), new FloorCommand(row.getId()));
    }

    @KafkaListener(topics = Channels.RELEASES, groupId = "order-releases")
    @Transactional
    public void onRelease(ReleaseEvent event) {
        if (!"RELEASED".equals(event.status())) {
            return;
        }
        OrderEntity row = lock(event.orderId());
        if (row == null || !"RESERVED".equals(row.getStatus())) {
            return;
        }
        row.setStatus("RELEASED");
        fact(row);
        OutboxWriter.rabbit(outbox, mapper, Channels.STATION, row.getId(),
                new StationCommand(row.getId(), row.getStationToken()));
    }

    @KafkaListener(topics = Channels.STATIONS, groupId = "order-stations")
    @Transactional
    public void onStation(StationEvent event) {
        if (!"PASSED".equals(event.status()) && !"FAILED".equals(event.status())) {
            return;
        }
        OrderEntity row = lock(event.orderId());
        if (row == null || !"RELEASED".equals(row.getStatus())) {
            return;
        }
        if (!"PASSED".equals(event.status())) {
            cancel(row, "STATION");
            return;
        }
        row.setStatus("STATIONED");
        fact(row);
        OutboxWriter.rabbit(outbox, mapper, Channels.INSPECT, row.getId(),
                new InspectCommand(row.getId(), row.getLot()));
    }

    @KafkaListener(topics = Channels.QUALITY, groupId = "order-quality")
    @Transactional
    public void onQuality(QualityEvent event) {
        if (!"ACCEPTED".equals(event.status()) && !"HOLD".equals(event.status()) && !"SCRAP".equals(event.status())) {
            return;
        }
        OrderEntity row = lock(event.orderId());
        if (row == null || !"STATIONED".equals(row.getStatus())) {
            return;
        }
        if ("HOLD".equals(event.status())) {
            cancel(row, "HOLD");
            return;
        }
        if ("SCRAP".equals(event.status())) {
            row.setStatus("SCRAPPED");
            row.setReason("SCRAP");
            fact(row);
            OutboxWriter.rabbit(outbox, mapper, Channels.SCRAP, row.getId(),
                    new ScrapCommand(row.getId(), row.getQuantity()));
            OutboxWriter.rabbit(outbox, mapper, Channels.ACCRUE, row.getId(),
                    new LaborCommand(row.getId(), row.getLaborRateCents(), row.getQuantity()));
            return;
        }
        row.setStatus("ACCEPTED");
        fact(row);
        OutboxWriter.rabbit(outbox, mapper, Channels.RECEIVE, row.getId(),
                new ReceiptCommand(row.getId(), row.getQuantity(), row.getDock()));
    }

    @KafkaListener(topics = Channels.RECEIPTS, groupId = "order-receipts")
    @Transactional
    public void onReceipt(ReceiptEvent event) {
        if (!"RECEIVED".equals(event.status()) && !"FAILED".equals(event.status())) {
            return;
        }
        OrderEntity row = lock(event.orderId());
        if (row == null || !"ACCEPTED".equals(row.getStatus())) {
            return;
        }
        if (!"RECEIVED".equals(event.status())) {
            cancel(row, "RECEIPT");
            return;
        }
        row.setStatus("RECEIVED");
        fact(row);
        OutboxWriter.rabbit(outbox, mapper, Channels.ACCRUE, row.getId(),
                new LaborCommand(row.getId(), row.getLaborRateCents(), row.getQuantity()));
    }

    @KafkaListener(topics = Channels.LABOR, groupId = "order-labor")
    @Transactional
    public void onLabor(LaborEvent event) {
        if (!"POSTED".equals(event.status())) {
            return;
        }
        OrderEntity row = lock(event.orderId());
        if (row == null || !"NONE".equals(row.getLaborStatus())) {
            return;
        }
        if ("SCRAPPED".equals(row.getStatus())) {
            row.setLaborCents(event.amountCents());
            row.setLaborStatus("POSTED");
            roll(row);
            return;
        }
        if (!"RECEIVED".equals(row.getStatus())) {
            return;
        }
        row.setLaborCents(event.amountCents());
        row.setLaborStatus("POSTED");
        row.setStatus("LABOR");
        fact(row);
        roll(row);
    }

    @KafkaListener(topics = Channels.COSTS, groupId = "order-costs")
    @Transactional
    public void onCost(CostEvent event) {
        if (!"POSTED".equals(event.status())) {
            return;
        }
        OrderEntity row = lock(event.orderId());
        if (row == null || !"NONE".equals(row.getCostStatus())) {
            return;
        }
        if ("LABOR".equals(row.getStatus())) {
            row.setCostCents(event.amountCents());
            row.setCostStatus("POSTED");
            row.setStatus("COSTED");
            fact(row);
            return;
        }
        if ("SCRAPPED".equals(row.getStatus())) {
            row.setCostCents(event.amountCents());
            row.setCostStatus("POSTED");
        }
    }

    private void roll(OrderEntity row) {
        OutboxWriter.rabbit(outbox, mapper, Channels.COST, row.getId(),
                new CostCommand(row.getId(), row.getMaterialCents(), row.getLaborCents()));
        OutboxWriter.rabbit(outbox, mapper, Channels.CONSUME, row.getId(),
                new ConsumeCommand(row.getId(), reservedCount(row.getId())));
    }

    private int reservedCount(String orderId) {
        int count = 0;
        for (OrderLineEntity line : lines.findByOrderIdOrderByPositionAsc(orderId)) {
            if ("RESERVED".equals(line.getState())) {
                count++;
            }
        }
        return count;
    }

    private void reserve(String orderId, String sku, int quantity) {
        OutboxWriter.rabbit(outbox, mapper, Channels.RESERVE, orderId, new ReserveCommand(orderId, sku, quantity));
    }

    private OrderEntity lock(String id) {
        return em.find(OrderEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
    }

    private void fact(OrderEntity row) {
        OutboxWriter.kafka(outbox, mapper, Channels.ORDERS, row.getId(),
                new OrderFact(row.getId(), row.getStatus(), row.getReason()));
    }

    private void cancel(OrderEntity row, String reason) {
        String status = row.getStatus();
        row.setStatus("CANCELLED");
        row.setReason(reason);
        fact(row);
        if (PlantRules.releaseMaterial(status)) {
            for (OrderLineEntity line : lines.findByOrderIdAndState(row.getId(), "RESERVED")) {
                OutboxWriter.rabbit(outbox, mapper, Channels.MATERIAL_RELEASE, row.getId(),
                        new ReleaseMaterialCommand(row.getId(), line.getSku(), line.getQuantity()));
            }
        }
        if (PlantRules.voidRelease(status)) {
            OutboxWriter.rabbit(outbox, mapper, Channels.FLOOR_VOID, row.getId(), new FloorVoidCommand(row.getId()));
        }
        if (PlantRules.voidQuality(status)) {
            OutboxWriter.rabbit(outbox, mapper, Channels.QUALITY_VOID, row.getId(), new QualityVoidCommand(row.getId()));
        }
    }
}
