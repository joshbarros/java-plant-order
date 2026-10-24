package com.plantorder.bom;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "bom_lines")
public class BomLineEntity {
    @Id
    private String id;
    @Column(name = "parent_sku")
    private String parentSku;
    @Column(name = "component_sku")
    private String componentSku;
    @Column(name = "qty_per")
    private int qtyPer;
    @Column(name = "standard_cents")
    private int standardCents;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getParentSku() { return parentSku; }
    public void setParentSku(String parentSku) { this.parentSku = parentSku; }
    public String getComponentSku() { return componentSku; }
    public void setComponentSku(String componentSku) { this.componentSku = componentSku; }
    public int getQtyPer() { return qtyPer; }
    public void setQtyPer(int qtyPer) { this.qtyPer = qtyPer; }
    public int getStandardCents() { return standardCents; }
    public void setStandardCents(int standardCents) { this.standardCents = standardCents; }
}
