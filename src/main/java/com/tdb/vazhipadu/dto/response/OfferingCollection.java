package com.tdb.vazhipadu.dto.response;

import java.math.BigDecimal;

public record OfferingCollection(Long offeringId, String offeringName, Long quantity, BigDecimal amount) {
}
