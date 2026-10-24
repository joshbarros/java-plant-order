package com.plantorder.receipt;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ReceiptController {

    private final ReceiptRepository receipts;

    public ReceiptController(ReceiptRepository receipts) {
        this.receipts = receipts;
    }

    @GetMapping("/receipts/{orderId}")
    public ReceiptEntity receipt(@PathVariable String orderId) {
        return receipts.findById(orderId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "receipt not found"));
    }
}
