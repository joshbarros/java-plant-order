package com.plantorder.quality;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "inspections")
public class QualityEntity {
    @Id
    @Column(name = "order_id")
    private String orderId;
    private String lot;
    private String status;

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getLot() { return lot; }
    public void setLot(String lot) { this.lot = lot; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
