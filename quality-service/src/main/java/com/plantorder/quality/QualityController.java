package com.plantorder.quality;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class QualityController {

    private final QualityRepository inspections;

    public QualityController(QualityRepository inspections) {
        this.inspections = inspections;
    }

    @GetMapping("/inspections/{orderId}")
    public QualityEntity inspection(@PathVariable String orderId) {
        return inspections.findById(orderId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "inspection not found"));
    }
}
