package com.plantorder.bom;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plantorder.common.BomDefined;
import com.plantorder.common.Channels;
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
public class BomController {

    private final BomLineRepository lines;
    private final ItemDirectoryRepository items;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;

    public BomController(BomLineRepository lines, ItemDirectoryRepository items, OutboxRepository outbox,
            ObjectMapper mapper) {
        this.lines = lines;
        this.items = items;
        this.outbox = outbox;
        this.mapper = mapper;
    }

    @PostMapping("/boms")
    @Transactional
    public ResponseEntity<BomLineEntity> create(@RequestBody CreateBom request) {
        if (request == null || bad(request.parentSku()) || bad(request.componentSku())
                || request.qtyPer() == null || request.qtyPer() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bom fields are required");
        }
        ItemDirectory parent = items.findById(request.parentSku().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "parent not found"));
        ItemDirectory component = items.findById(request.componentSku().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "component not found"));
        if (!"FINISHED".equals(parent.getKind()) || !"COMPONENT".equals(component.getKind())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bom kinds are wrong");
        }
        String id = parent.getSku() + ":" + component.getSku();
        if (lines.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "bom line exists");
        }
        BomLineEntity row = new BomLineEntity();
        row.setId(id);
        row.setParentSku(parent.getSku());
        row.setComponentSku(component.getSku());
        row.setQtyPer(request.qtyPer());
        row.setStandardCents(component.getStandardCents());
        lines.save(row);
        OutboxWriter.kafka(outbox, mapper, Channels.BOMS, row.getId(),
                new BomDefined(row.getParentSku(), row.getComponentSku(), row.getQtyPer(), row.getStandardCents()));
        return ResponseEntity.status(HttpStatus.CREATED).body(row);
    }

    @GetMapping("/boms/{parentSku}")
    public List<BomLineEntity> bom(@PathVariable String parentSku) {
        return lines.findByParentSkuOrderByComponentSkuAsc(parentSku);
    }

    @GetMapping("/directory/{sku}")
    public ItemDirectory directory(@PathVariable String sku) {
        return items.findById(sku).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "item not found"));
    }

    private static boolean bad(String sku) {
        return sku == null || sku.isBlank() || sku.contains(":") || sku.contains("|");
    }

    public record CreateBom(String parentSku, String componentSku, Integer qtyPer) {
    }
}
