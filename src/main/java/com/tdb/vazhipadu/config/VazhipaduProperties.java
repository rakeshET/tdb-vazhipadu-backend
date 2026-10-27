package com.tdb.vazhipadu.config;

import jakarta.validation.constraints.NotNull;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "vazhipadu")
public record VazhipaduProperties(@NotNull ZoneId zone) {}
