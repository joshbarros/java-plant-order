package com.plantorder.station;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "stations")
public class StationEntity {
    @Id
    @Column(name = "order_id")
    private String orderId;
    @Column(name = "station_token")
    private String stationToken;
    private String status;

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getStationToken() { return stationToken; }
    public void setStationToken(String stationToken) { this.stationToken = stationToken; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
