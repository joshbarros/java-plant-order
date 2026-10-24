package com.plantorder.scrap;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ScrapController {

    private final ScrapRepository scraps;

    public ScrapController(ScrapRepository scraps) {
        this.scraps = scraps;
    }

    @GetMapping("/scraps/{orderId}")
    public ScrapEntity scrap(@PathVariable String orderId) {
        return scraps.findById(orderId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "scrap not found"));
    }
}
