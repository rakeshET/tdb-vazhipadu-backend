package com.tdb.vazhipadu.repository;

import com.tdb.vazhipadu.entity.Offering;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfferingRepository extends JpaRepository<Offering, Long> {

    List<Offering> findByTempleIdOrderByNameAsc(Long templeId);

    Optional<Offering> findByIdAndTempleId(Long id, Long templeId);

    boolean existsByTempleIdAndNameIgnoreCase(Long templeId, String name);

    boolean existsByTempleIdAndNameIgnoreCaseAndIdNot(Long templeId, String name, Long id);
}
