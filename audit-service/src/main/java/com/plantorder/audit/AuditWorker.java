package com.plantorder.audit;

import java.time.Instant;

import com.plantorder.common.Channels;
import com.plantorder.common.CostEvent;
import com.plantorder.common.ExplosionEvent;
import com.plantorder.common.LaborEvent;
import com.plantorder.common.MaterialEvent;
import com.plantorder.common.OrderFact;
import com.plantorder.common.QualityEvent;
import com.plantorder.common.ReceiptEvent;
import com.plantorder.common.ReleaseEvent;
import com.plantorder.common.ScrapEvent;
import com.plantorder.common.StationEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuditWorker {

    private final AuditRepository entries;

    public AuditWorker(AuditRepository entries) {
        this.entries = entries;
    }

    @KafkaListener(topics = Channels.ORDERS, groupId = "audit-orders")
    @Transactional
    public void onOrder(OrderFact event) {
        add("OrderFact", event.orderId(), event.status());
    }

    @KafkaListener(topics = Channels.EXPLOSIONS, groupId = "audit-explosions")
    @Transactional
    public void onExplosion(ExplosionEvent event) {
        add("ExplosionEvent", event.orderId(), event.status());
    }

    @KafkaListener(topics = Channels.MATERIALS, groupId = "audit-materials")
    @Transactional
    public void onMaterial(MaterialEvent event) {
        add("MaterialEvent", event.orderId(), event.status());
    }

    @KafkaListener(topics = Channels.RELEASES, groupId = "audit-releases")
    @Transactional
    public void onRelease(ReleaseEvent event) {
        add("ReleaseEvent", event.orderId(), event.status());
    }

    @KafkaListener(topics = Channels.STATIONS, groupId = "audit-stations")
    @Transactional
    public void onStation(StationEvent event) {
        add("StationEvent", event.orderId(), event.status());
    }

    @KafkaListener(topics = Channels.QUALITY, groupId = "audit-quality")
    @Transactional
    public void onQuality(QualityEvent event) {
        add("QualityEvent", event.orderId(), event.status());
    }

    @KafkaListener(topics = Channels.SCRAPS, groupId = "audit-scraps")
    @Transactional
    public void onScrap(ScrapEvent event) {
        add("ScrapEvent", event.orderId(), event.status());
    }

    @KafkaListener(topics = Channels.RECEIPTS, groupId = "audit-receipts")
    @Transactional
    public void onReceipt(ReceiptEvent event) {
        add("ReceiptEvent", event.orderId(), event.status());
    }

    @KafkaListener(topics = Channels.LABOR, groupId = "audit-labor")
    @Transactional
    public void onLabor(LaborEvent event) {
        add("LaborEvent", event.orderId(), event.status());
    }

    @KafkaListener(topics = Channels.COSTS, groupId = "audit-costs")
    @Transactional
    public void onCost(CostEvent event) {
        add("CostEvent", event.orderId(), event.status());
    }

    private void add(String kind, String orderId, String status) {
        String eventId = kind + ":" + orderId + ":" + status;
        if (entries.existsById(eventId)) {
            return;
        }
        AuditEntry row = new AuditEntry();
        row.setEventId(eventId);
        row.setOrderId(orderId);
        row.setKind(kind);
        row.setStatus(status);
        row.setRecordedAt(Instant.now());
        entries.save(row);
    }
}
