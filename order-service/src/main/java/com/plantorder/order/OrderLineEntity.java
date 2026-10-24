package com.plantorder.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_lines")
public class OrderLineEntity {
    @Id
    private String id;
    @Column(name = "order_id")
    private String orderId;
    private String sku;
    private int quantity;
    @Column(name = "standard_cents")
    private int standardCents;
    @Column(name = "line_position")
    private int position;
    private String state;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getStandardCents() { return standardCents; }
    public void setStandardCents(int standardCents) { this.standardCents = standardCents; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
}
