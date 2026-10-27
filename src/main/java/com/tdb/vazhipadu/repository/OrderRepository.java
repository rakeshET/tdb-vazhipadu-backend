package com.tdb.vazhipadu.repository;

import com.tdb.vazhipadu.entity.OfferingOrder;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OrderRepository extends JpaRepository<OfferingOrder, Long>, JpaSpecificationExecutor<OfferingOrder> {

    @EntityGraph(attributePaths = "items")
    Optional<OfferingOrder> findWithItemsByIdAndTempleId(Long id, Long templeId);
}
