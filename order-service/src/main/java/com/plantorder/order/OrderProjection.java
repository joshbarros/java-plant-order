package com.plantorder.order;

import com.plantorder.common.BomDefined;
import com.plantorder.common.Channels;
import com.plantorder.common.ItemOpened;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderProjection {

    private final ItemDirectoryRepository items;
    private final BomDirectoryRepository boms;

    public OrderProjection(ItemDirectoryRepository items, BomDirectoryRepository boms) {
        this.items = items;
        this.boms = boms;
    }

    @KafkaListener(topics = Channels.ITEMS, groupId = "order-items")
    @Transactional
    public void onItem(ItemOpened event) {
        if (items.existsById(event.sku())) {
            return;
        }
        ItemDirectory row = new ItemDirectory();
        row.setSku(event.sku());
        row.setKind(event.kind());
        row.setStandardCents(event.standardCents());
        row.setLaborCents(event.laborCents());
        items.save(row);
    }

    @KafkaListener(topics = Channels.BOMS, groupId = "order-boms")
    @Transactional
    public void onBom(BomDefined event) {
        String id = event.parentSku() + ":" + event.componentSku();
        if (boms.existsById(id)) {
            return;
        }
        BomDirectory row = new BomDirectory();
        row.setId(id);
        row.setParentSku(event.parentSku());
        row.setComponentSku(event.componentSku());
        row.setQtyPer(event.qtyPer());
        row.setStandardCents(event.standardCents());
        boms.save(row);
    }
}
