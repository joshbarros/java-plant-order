package com.plantorder.cost;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class CostController {

    private final CostRepository costs;

    public CostController(CostRepository costs) {
        this.costs = costs;
    }

    @GetMapping("/costs/{orderId}")
    public CostEntity cost(@PathVariable String orderId) {
        return costs.findById(orderId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "cost not found"));
    }
}
