package com.plantorder.material;

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
public class MaterialController {

    private final StockRepository stocks;

    public MaterialController(StockRepository stocks) {
        this.stocks = stocks;
    }

    @PostMapping("/stock")
    @Transactional
    public ResponseEntity<StockEntity> create(@RequestBody CreateStock request) {
        if (request == null || request.sku() == null || request.sku().isBlank()
                || request.onHand() == null || request.onHand() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "stock fields are required");
        }
        if (stocks.existsById(request.sku().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "stock exists");
        }
        StockEntity row = new StockEntity();
        row.setSku(request.sku().trim());
        row.setOnHand(request.onHand());
        stocks.save(row);
        return ResponseEntity.status(HttpStatus.CREATED).body(row);
    }

    @GetMapping("/stock/{sku}")
    public StockEntity stock(@PathVariable String sku) {
        return stocks.findById(sku).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "stock not found"));
    }

    public record CreateStock(String sku, Integer onHand) {
    }
}
