package com.plantorder.station;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class StationController {

    private final StationRepository stations;

    public StationController(StationRepository stations) {
        this.stations = stations;
    }

    @GetMapping("/stations/{orderId}")
    public StationEntity station(@PathVariable String orderId) {
        return stations.findById(orderId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "station not found"));
    }
}
