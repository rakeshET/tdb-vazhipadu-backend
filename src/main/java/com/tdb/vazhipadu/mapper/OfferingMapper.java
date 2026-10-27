package com.tdb.vazhipadu.mapper;

import com.tdb.vazhipadu.dto.response.OfferingResponse;
import com.tdb.vazhipadu.entity.Offering;

public final class OfferingMapper {

    private OfferingMapper() {}

    public static OfferingResponse toResponse(Offering offering) {
        return new OfferingResponse(
                offering.getId(),
                offering.getTemple().getId(),
                offering.getName(),
                offering.getPrice(),
                offering.isActive(),
                offering.getDailyLimit());
    }
}
