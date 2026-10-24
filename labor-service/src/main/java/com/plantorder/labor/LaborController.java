package com.plantorder.labor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class LaborController {

    private final LaborRepository entries;

    public LaborController(LaborRepository entries) {
        this.entries = entries;
    }

    @GetMapping("/labor/{orderId}")
    public LaborEntity labor(@PathVariable String orderId) {
        return entries.findById(orderId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "labor not found"));
    }
}
