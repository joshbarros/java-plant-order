package com.plantorder.explosion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "explosions")
public class ExplosionEntity {
    @Id
    @Column(name = "order_id")
    private String orderId;
    @Column(columnDefinition = "text")
    private String lines;
    @Column(name = "material_cents")
    private int materialCents;
    private String status;

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getLines() { return lines; }
    public void setLines(String lines) { this.lines = lines; }
    public int getMaterialCents() { return materialCents; }
    public void setMaterialCents(int materialCents) { this.materialCents = materialCents; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
