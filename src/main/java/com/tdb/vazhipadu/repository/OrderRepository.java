package com.tdb.vazhipadu.repository;

import com.tdb.vazhipadu.entity.OfferingOrder;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

public interface OrderRepository extends JpaRepository<OfferingOrder, Long>, JpaSpecificationExecutor<OfferingOrder> {

    @EntityGraph(attributePaths = "items")
    Optional<OfferingOrder> findWithItemsByIdAndTempleId(Long id, Long templeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "10000"))
    Optional<OfferingOrder> findForUpdateByIdAndTempleId(Long id, Long templeId);
}
