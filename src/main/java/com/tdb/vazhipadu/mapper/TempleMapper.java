package com.tdb.vazhipadu.mapper;

import com.tdb.vazhipadu.dto.response.TempleResponse;
import com.tdb.vazhipadu.entity.Temple;

public final class TempleMapper {

    private TempleMapper() {}

    public static TempleResponse toResponse(Temple temple) {
        return new TempleResponse(temple.getId(), temple.getName());
    }
}
