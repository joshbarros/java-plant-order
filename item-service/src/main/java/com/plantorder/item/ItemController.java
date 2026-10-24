package com.plantorder.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plantorder.common.Channels;
import com.plantorder.common.ItemOpened;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ItemController {

    private final ItemRepository items;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;

    public ItemController(ItemRepository items, OutboxRepository outbox, ObjectMapper mapper) {
        this.items = items;
        this.outbox = outbox;
        this.mapper = mapper;
    }

    @PostMapping("/items")
    @Transactional
    public ResponseEntity<ItemEntity> create(@RequestBody CreateItem request) {
        if (request == null || badSku(request.sku()) || badKind(request.kind())
                || request.standardCents() == null || request.standardCents() < 0
                || request.laborCents() == null || request.laborCents() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "item fields are required");
        }
        if (items.existsById(request.sku().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "item exists");
        }
        ItemEntity row = new ItemEntity();
        row.setSku(request.sku().trim());
        row.setKind(request.kind().trim());
        row.setStandardCents(request.standardCents());
        row.setLaborCents(request.laborCents());
        items.save(row);
        OutboxWriter.kafka(outbox, mapper, Channels.ITEMS, row.getSku(),
                new ItemOpened(row.getSku(), row.getKind(), row.getStandardCents(), row.getLaborCents()));
        return ResponseEntity.status(HttpStatus.CREATED).body(row);
    }

    @GetMapping("/items/{sku}")
    public ItemEntity item(@PathVariable String sku) {
        return items.findById(sku).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "item not found"));
    }

    private static boolean badSku(String sku) {
        return sku == null || sku.isBlank() || sku.contains(":") || sku.contains("|");
    }

    private static boolean badKind(String kind) {
        return kind == null || (!"FINISHED".equals(kind.trim()) && !"COMPONENT".equals(kind.trim()));
    }

    public record CreateItem(String sku, String kind, Integer standardCents, Integer laborCents) {
    }
}
