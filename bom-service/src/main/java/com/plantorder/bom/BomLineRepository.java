package com.plantorder.bom;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BomLineRepository extends JpaRepository<BomLineEntity, String> {
    List<BomLineEntity> findByParentSkuOrderByComponentSkuAsc(String parentSku);
}
