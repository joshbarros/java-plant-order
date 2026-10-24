package com.plantorder.order;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BomDirectoryRepository extends JpaRepository<BomDirectory, String> {
    List<BomDirectory> findByParentSkuOrderByComponentSkuAsc(String parentSku);
}
