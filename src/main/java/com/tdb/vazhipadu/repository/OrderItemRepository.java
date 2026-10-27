package com.tdb.vazhipadu.repository;

import com.tdb.vazhipadu.dto.response.OfferingCollection;
import com.tdb.vazhipadu.entity.OrderItem;
import com.tdb.vazhipadu.entity.OrderStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query(
            """
            select coalesce(sum(i.quantity), 0) from OrderItem i
            where i.offering.id = :offeringId
              and i.order.offeringDate = :offeringDate
              and i.order.status = :status
            """)
    long sumQuantity(
            @Param("offeringId") Long offeringId,
            @Param("offeringDate") LocalDate offeringDate,
            @Param("status") OrderStatus status);

    @Query(
            """
            select new com.tdb.vazhipadu.dto.response.OfferingCollection(f.id, f.name, sum(i.quantity), sum(i.lineTotal))
            from OrderItem i join i.order o join i.offering f
            where o.temple.id = :templeId
              and o.status = :status
              and o.createdAt >= :from
              and o.createdAt < :to
            group by f.id, f.name
            order by f.name
            """)
    List<OfferingCollection> collectByOffering(
            @Param("templeId") Long templeId,
            @Param("status") OrderStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
