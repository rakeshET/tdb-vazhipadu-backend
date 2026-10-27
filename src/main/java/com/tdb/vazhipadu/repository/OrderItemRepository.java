package com.tdb.vazhipadu.repository;

import com.tdb.vazhipadu.entity.OrderItem;
import com.tdb.vazhipadu.entity.OrderStatus;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("""
            select coalesce(sum(i.quantity), 0) from OrderItem i
            where i.offering.id = :offeringId
              and i.order.offeringDate = :offeringDate
              and i.order.status = :status
            """)
    long sumQuantity(@Param("offeringId") Long offeringId,
                     @Param("offeringDate") LocalDate offeringDate,
                     @Param("status") OrderStatus status);
}
