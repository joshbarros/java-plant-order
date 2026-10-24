package com.plantorder.explosion;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ExplosionController {

    private final ExplosionRepository explosions;

    public ExplosionController(ExplosionRepository explosions) {
        this.explosions = explosions;
    }

    @GetMapping("/explosions/{orderId}")
    public ExplosionEntity explosion(@PathVariable String orderId) {
        return explosions.findById(orderId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "explosion not found"));
    }
}
