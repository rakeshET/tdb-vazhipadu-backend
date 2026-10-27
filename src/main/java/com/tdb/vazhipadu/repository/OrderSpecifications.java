package com.tdb.vazhipadu.repository;

import com.tdb.vazhipadu.dto.request.OrderSearchCriteria;
import com.tdb.vazhipadu.entity.OfferingOrder;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import org.springframework.data.jpa.domain.Specification;

public final class OrderSpecifications {

    private OrderSpecifications() {
    }

    public static Specification<OfferingOrder> matching(Long templeId, OrderSearchCriteria criteria) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(cb.equal(root.get("temple").get("id"), templeId));
            if (criteria.offeringDate() != null) {
                predicates.add(cb.equal(root.get("offeringDate"), criteria.offeringDate()));
            }
            if (criteria.status() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.status()));
            }
            if (criteria.phone() != null) {
                predicates.add(cb.equal(root.get("phone"), criteria.phone()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
