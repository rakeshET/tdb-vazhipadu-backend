package com.tdb.vazhipadu.repository;

import com.tdb.vazhipadu.entity.Offering;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OfferingRepository extends JpaRepository<Offering, Long> {

    List<Offering> findByTempleIdOrderByNameAsc(Long templeId);

    Optional<Offering> findByIdAndTempleId(Long id, Long templeId);

    boolean existsByTempleIdAndNameIgnoreCase(Long templeId, String name);

    boolean existsByTempleIdAndNameIgnoreCaseAndIdNot(Long templeId, String name, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Offering o where o.id in :ids order by o.id")
    List<Offering> lockAllByIdInOrderById(@Param("ids") Collection<Long> ids);
}
