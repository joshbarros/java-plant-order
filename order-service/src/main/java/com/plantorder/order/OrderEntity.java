package com.plantorder.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id
    private String id;
    @Column(name = "request_id", unique = true)
    private String requestId;
    @Column(name = "item_sku")
    private String itemSku;
    private int quantity;
    @Column(name = "station_token")
    private String stationToken;
    private String lot;
    private String dock;
    @Column(name = "labor_rate_cents")
    private int laborRateCents;
    @Column(name = "material_cents")
    private int materialCents;
    @Column(name = "labor_cents")
    private int laborCents;
    @Column(name = "cost_cents")
    private int costCents;
    @Column(name = "lines_text", columnDefinition = "text")
    private String linesText;
    private String status;
    @Column(name = "material_status")
    private String materialStatus;
    @Column(name = "labor_status")
    private String laborStatus;
    @Column(name = "cost_status")
    private String costStatus;
    private String reason;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getItemSku() { return itemSku; }
    public void setItemSku(String itemSku) { this.itemSku = itemSku; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getStationToken() { return stationToken; }
    public void setStationToken(String stationToken) { this.stationToken = stationToken; }
    public String getLot() { return lot; }
    public void setLot(String lot) { this.lot = lot; }
    public String getDock() { return dock; }
    public void setDock(String dock) { this.dock = dock; }
    public int getLaborRateCents() { return laborRateCents; }
    public void setLaborRateCents(int laborRateCents) { this.laborRateCents = laborRateCents; }
    public int getMaterialCents() { return materialCents; }
    public void setMaterialCents(int materialCents) { this.materialCents = materialCents; }
    public int getLaborCents() { return laborCents; }
    public void setLaborCents(int laborCents) { this.laborCents = laborCents; }
    public int getCostCents() { return costCents; }
    public void setCostCents(int costCents) { this.costCents = costCents; }
    public String getLinesText() { return linesText; }
    public void setLinesText(String linesText) { this.linesText = linesText; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMaterialStatus() { return materialStatus; }
    public void setMaterialStatus(String materialStatus) { this.materialStatus = materialStatus; }
    public String getLaborStatus() { return laborStatus; }
    public void setLaborStatus(String laborStatus) { this.laborStatus = laborStatus; }
    public String getCostStatus() { return costStatus; }
    public void setCostStatus(String costStatus) { this.costStatus = costStatus; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
