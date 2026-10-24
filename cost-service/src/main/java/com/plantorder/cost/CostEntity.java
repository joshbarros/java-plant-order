package com.plantorder.cost;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "costs")
public class CostEntity {
    @Id
    @Column(name = "order_id")
    private String orderId;
    @Column(name = "material_cents")
    private int materialCents;
    @Column(name = "labor_cents")
    private int laborCents;
    @Column(name = "amount_cents")
    private int amountCents;
    private String status;

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public int getMaterialCents() { return materialCents; }
    public void setMaterialCents(int materialCents) { this.materialCents = materialCents; }
    public int getLaborCents() { return laborCents; }
    public void setLaborCents(int laborCents) { this.laborCents = laborCents; }
    public int getAmountCents() { return amountCents; }
    public void setAmountCents(int amountCents) { this.amountCents = amountCents; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
