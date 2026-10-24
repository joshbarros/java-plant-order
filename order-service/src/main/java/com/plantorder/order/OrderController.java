package com.plantorder.order;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plantorder.common.Channels;
import com.plantorder.common.ExplodeCommand;
import com.plantorder.common.PlantRules;
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
public class OrderController {

    private final OrderRepository orders;
    private final ItemDirectoryRepository items;
    private final BomDirectoryRepository boms;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;

    public OrderController(OrderRepository orders, ItemDirectoryRepository items, BomDirectoryRepository boms,
            OutboxRepository outbox, ObjectMapper mapper) {
        this.orders = orders;
        this.items = items;
        this.boms = boms;
        this.outbox = outbox;
        this.mapper = mapper;
    }

    @PostMapping("/orders")
    @Transactional
    public ResponseEntity<OrderEntity> create(@RequestBody CreateOrder request) {
        if (request == null || blank(request.requestId()) || blank(request.itemSku()) || request.quantity() == null
                || request.quantity() <= 0 || blank(request.stationToken()) || blank(request.lot())
                || blank(request.dock())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "order fields are required");
        }
        OrderEntity existing = orders.findByRequestId(request.requestId().trim()).orElse(null);
        if (existing != null) {
            return ResponseEntity.ok(existing);
        }
        ItemDirectory item = items.findById(request.itemSku().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "item not found"));
        if (!"FINISHED".equals(item.getKind())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "item is not a finished good");
        }
        List<PlantRules.PartQty> parts = new ArrayList<>();
        for (BomDirectory line : boms.findByParentSkuOrderByComponentSkuAsc(item.getSku())) {
            parts.add(new PlantRules.PartQty(line.getComponentSku(), line.getQtyPer(), line.getStandardCents()));
        }
        parts.sort(Comparator.comparing(PlantRules.PartQty::sku));
        OrderEntity row = new OrderEntity();
        row.setId(UUID.randomUUID().toString());
        row.setRequestId(request.requestId().trim());
        row.setItemSku(item.getSku());
        row.setQuantity(request.quantity());
        row.setStationToken(request.stationToken().trim());
        row.setLot(request.lot().trim());
        row.setDock(request.dock().trim());
        row.setLaborRateCents(item.getLaborCents());
        row.setMaterialCents(0);
        row.setLaborCents(0);
        row.setCostCents(0);
        row.setLinesText("");
        row.setStatus("DRAFT");
        row.setMaterialStatus("NONE");
        row.setLaborStatus("NONE");
        row.setCostStatus("NONE");
        row.setReason("");
        orders.save(row);
        OutboxWriter.rabbit(outbox, mapper, Channels.EXPLODE, row.getId(),
                new ExplodeCommand(row.getId(), PlantRules.format(parts), row.getQuantity()));
        return ResponseEntity.status(HttpStatus.CREATED).body(row);
    }

    @GetMapping("/orders/{id}")
    public OrderEntity order(@PathVariable String id) {
        return orders.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found"));
    }

    @GetMapping("/directory/{sku}")
    public ItemDirectory directory(@PathVariable String sku) {
        return items.findById(sku).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "item not found"));
    }

    @GetMapping("/boms/{parentSku}")
    public List<BomDirectory> boms(@PathVariable String parentSku) {
        return boms.findByParentSkuOrderByComponentSkuAsc(parentSku);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    public record CreateOrder(String requestId, String itemSku, Integer quantity, String stationToken, String lot,
            String dock) {
    }
}
