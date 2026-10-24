package com.plantorder.release;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ReleaseController {

    private final ReleaseRepository releases;

    public ReleaseController(ReleaseRepository releases) {
        this.releases = releases;
    }

    @GetMapping("/releases/{orderId}")
    public ReleaseEntity release(@PathVariable String orderId) {
        return releases.findById(orderId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "release not found"));
    }
}
