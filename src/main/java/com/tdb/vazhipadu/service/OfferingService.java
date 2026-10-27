package com.tdb.vazhipadu.service;

import com.tdb.vazhipadu.dto.request.OfferingRequest;
import com.tdb.vazhipadu.dto.response.OfferingResponse;
import com.tdb.vazhipadu.entity.Offering;
import com.tdb.vazhipadu.exception.ApiException;
import com.tdb.vazhipadu.exception.ErrorCode;
import com.tdb.vazhipadu.mapper.OfferingMapper;
import com.tdb.vazhipadu.repository.OfferingRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfferingService {

    private final OfferingRepository offeringRepository;
    private final TempleService templeService;

    public OfferingService(OfferingRepository offeringRepository, TempleService templeService) {
        this.offeringRepository = offeringRepository;
        this.templeService = templeService;
    }

    @Transactional
    public OfferingResponse createOffering(Long templeId, OfferingRequest request) {
        var temple = templeService.findTempleOrThrow(templeId);
        var name = request.name().strip();
        if (offeringRepository.existsByTempleIdAndNameIgnoreCase(templeId, name)) {
            throw duplicateOffering(name);
        }
        var offering = new Offering(temple, name, normalise(request.price()), request.activeOr(true));
        return OfferingMapper.toResponse(offeringRepository.save(offering));
    }

    @Transactional
    public OfferingResponse updateOffering(Long templeId, Long offeringId, OfferingRequest request) {
        var offering = findOfferingOrThrow(templeId, offeringId);
        var name = request.name().strip();
        if (offeringRepository.existsByTempleIdAndNameIgnoreCaseAndIdNot(templeId, name, offeringId)) {
            throw duplicateOffering(name);
        }
        offering.update(name, normalise(request.price()), request.activeOr(offering.isActive()));
        return OfferingMapper.toResponse(offering);
    }

    @Transactional(readOnly = true)
    public OfferingResponse getOffering(Long templeId, Long offeringId) {
        return OfferingMapper.toResponse(findOfferingOrThrow(templeId, offeringId));
    }

    @Transactional(readOnly = true)
    public List<OfferingResponse> listOfferings(Long templeId) {
        templeService.findTempleOrThrow(templeId);
        return offeringRepository.findByTempleIdOrderByNameAsc(templeId).stream()
                .map(OfferingMapper::toResponse)
                .toList();
    }

    private Offering findOfferingOrThrow(Long templeId, Long offeringId) {
        return offeringRepository.findByIdAndTempleId(offeringId, templeId)
                .orElseThrow(() -> ApiException.notFound("Offering", offeringId));
    }

    private static BigDecimal normalise(BigDecimal price) {
        return price.setScale(2, RoundingMode.UNNECESSARY);
    }

    private static ApiException duplicateOffering(String name) {
        return new ApiException(ErrorCode.DUPLICATE_OFFERING,
                "Offering '%s' already exists for this temple".formatted(name));
    }
}
