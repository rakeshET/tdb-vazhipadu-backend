package com.tdb.vazhipadu.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tdb.vazhipadu.dto.request.OfferingRequest;
import com.tdb.vazhipadu.entity.Offering;
import com.tdb.vazhipadu.entity.Temple;
import com.tdb.vazhipadu.exception.ApiException;
import com.tdb.vazhipadu.exception.ErrorCode;
import com.tdb.vazhipadu.repository.OfferingRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OfferingServiceTest {

    private static final Long TEMPLE_ID = 1L;

    @Mock
    private OfferingRepository offeringRepository;
    @Mock
    private TempleService templeService;
    @InjectMocks
    private OfferingService offeringService;

    private Temple temple;

    @BeforeEach
    void setUp() {
        temple = new Temple("Sabarimala");
        ReflectionTestUtils.setField(temple, "id", TEMPLE_ID);
    }

    @Test
    void shouldCreateActiveOfferingByDefaultWithTrimmedName() {
        when(templeService.findTempleOrThrow(TEMPLE_ID)).thenReturn(temple);
        when(offeringRepository.existsByTempleIdAndNameIgnoreCase(TEMPLE_ID, "Archana")).thenReturn(false);
        when(offeringRepository.save(any(Offering.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = offeringService.createOffering(TEMPLE_ID,
                new OfferingRequest("  Archana ", new BigDecimal("50.00"), null));

        assertThat(response.name()).isEqualTo("Archana");
        assertThat(response.active()).isTrue();
        assertThat(response.templeId()).isEqualTo(TEMPLE_ID);
    }

    @Test
    void shouldRejectDuplicateOfferingNameIgnoringCase() {
        when(templeService.findTempleOrThrow(TEMPLE_ID)).thenReturn(temple);
        when(offeringRepository.existsByTempleIdAndNameIgnoreCase(TEMPLE_ID, "archana")).thenReturn(true);

        assertThatThrownBy(() -> offeringService.createOffering(TEMPLE_ID,
                new OfferingRequest("archana", BigDecimal.TEN, true)))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.code()).isEqualTo(ErrorCode.DUPLICATE_OFFERING));
        verify(offeringRepository, never()).save(any());
    }

    @Test
    void shouldDeactivateOfferingOnUpdate() {
        var offering = new Offering(temple, "Archana", new BigDecimal("50.00"), true);
        ReflectionTestUtils.setField(offering, "id", 7L);
        when(offeringRepository.findByIdAndTempleId(7L, TEMPLE_ID)).thenReturn(Optional.of(offering));

        var response = offeringService.updateOffering(TEMPLE_ID, 7L,
                new OfferingRequest("Archana", new BigDecimal("60.00"), false));

        assertThat(response.active()).isFalse();
        assertThat(response.price()).isEqualByComparingTo("60.00");
    }

    @Test
    void shouldThrowNotFoundWhenOfferingBelongsToAnotherTemple() {
        when(offeringRepository.findByIdAndTempleId(7L, TEMPLE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> offeringService.getOffering(TEMPLE_ID, 7L))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.code()).isEqualTo(ErrorCode.NOT_FOUND));
    }
}
