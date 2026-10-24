package com.plantorder.receipt;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "receipts")
public class ReceiptEntity {
    @Id
    @Column(name = "order_id")
    private String orderId;
    private int quantity;
    private String dock;
    private String status;

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getDock() { return dock; }
    public void setDock(String dock) { this.dock = dock; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
