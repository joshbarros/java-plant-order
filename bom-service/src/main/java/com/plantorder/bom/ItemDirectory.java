package com.plantorder.bom;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "item_directory")
public class ItemDirectory {
    @Id
    private String sku;
    private String kind;
    @Column(name = "standard_cents")
    private int standardCents;

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public int getStandardCents() { return standardCents; }
    public void setStandardCents(int standardCents) { this.standardCents = standardCents; }
}
