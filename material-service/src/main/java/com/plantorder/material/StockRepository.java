package com.plantorder.material;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockRepository extends JpaRepository<StockEntity, String> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE stocks SET on_hand = on_hand - :qty WHERE sku = :sku AND on_hand >= :qty", nativeQuery = true)
    int reserve(@Param("sku") String sku, @Param("qty") int qty);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE stocks SET on_hand = on_hand + :qty WHERE sku = :sku", nativeQuery = true)
    int release(@Param("sku") String sku, @Param("qty") int qty);
}
