package com.tdb.vazhipadu.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequest(
        @NotNull Long offeringId,
        @NotNull @Min(1) @Max(100) Integer quantity) {
}
