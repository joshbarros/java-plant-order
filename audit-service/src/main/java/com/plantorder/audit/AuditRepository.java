package com.plantorder.audit;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRepository extends JpaRepository<AuditEntry, String> {
    List<AuditEntry> findByOrderIdOrderByRecordedAtAsc(String orderId);
}
