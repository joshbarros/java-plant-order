package com.plantorder.bom;

import com.plantorder.common.Channels;
import com.plantorder.common.ItemOpened;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ItemProjection {

    private final ItemDirectoryRepository items;

    public ItemProjection(ItemDirectoryRepository items) {
        this.items = items;
    }

    @KafkaListener(topics = Channels.ITEMS, groupId = "bom-items")
    @Transactional
    public void onItem(ItemOpened event) {
        if (items.existsById(event.sku())) {
            return;
        }
        ItemDirectory row = new ItemDirectory();
        row.setSku(event.sku());
        row.setKind(event.kind());
        row.setStandardCents(event.standardCents());
        items.save(row);
    }
}
