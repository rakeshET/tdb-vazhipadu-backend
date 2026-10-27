package com.tdb.vazhipadu.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTempleRequest(@NotBlank @Size(max = 150) String name) {
}
