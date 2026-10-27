package com.tdb.vazhipadu.repository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tdb.vazhipadu.entity.Offering;
import com.tdb.vazhipadu.entity.Temple;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class OfferingRepositoryConstraintTest {

    @Autowired
    private OfferingRepository offeringRepository;
    @Autowired
    private TempleRepository templeRepository;

    @Test
    void shouldRejectCaseVariantDuplicateNameInSameTemple() {
        var temple = templeRepository.save(new Temple("Sabarimala"));
        offeringRepository.saveAndFlush(new Offering(temple, "Archana", new BigDecimal("50.00"), true));

        assertThatThrownBy(() -> offeringRepository.saveAndFlush(
                new Offering(temple, "archana", new BigDecimal("40.00"), true)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("UQ_OFFERING_TEMPLE_NAME");
    }

    @Test
    void shouldAllowSameNameInDifferentTemples() {
        var sabarimala = templeRepository.save(new Temple("Sabarimala"));
        var guruvayur = templeRepository.save(new Temple("Guruvayur"));
        offeringRepository.saveAndFlush(new Offering(sabarimala, "Archana", new BigDecimal("50.00"), true));

        offeringRepository.saveAndFlush(new Offering(guruvayur, "Archana", new BigDecimal("50.00"), true));
    }
}
