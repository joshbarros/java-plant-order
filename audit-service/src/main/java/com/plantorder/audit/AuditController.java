package com.plantorder.audit;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditController {

    private final AuditRepository entries;

    public AuditController(AuditRepository entries) {
        this.entries = entries;
    }

    @GetMapping("/audits/{orderId}")
    public List<AuditEntry> audits(@PathVariable String orderId) {
        return entries.findByOrderIdOrderByRecordedAtAsc(orderId);
    }
}
