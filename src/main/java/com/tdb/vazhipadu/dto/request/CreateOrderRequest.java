package com.tdb.vazhipadu.dto.request;

import com.tdb.vazhipadu.entity.Nakshatra;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record CreateOrderRequest(
        @NotBlank @Size(max = 100) String devoteeName,
        @NotBlank @Pattern(regexp = "[6-9]\\d{9}", message = "must be a 10-digit Indian mobile number")
        String phone,
        @NotNull Nakshatra nakshatra,
        @NotNull LocalDate offeringDate,
        @NotEmpty @Size(max = 20) List<@NotNull @Valid OrderItemRequest> items) {
}
