package com.tdb.vazhipadu.service;

import com.tdb.vazhipadu.dto.request.CreateTempleRequest;
import com.tdb.vazhipadu.dto.response.TempleResponse;
import com.tdb.vazhipadu.entity.Temple;
import com.tdb.vazhipadu.exception.ApiException;
import com.tdb.vazhipadu.mapper.TempleMapper;
import com.tdb.vazhipadu.repository.TempleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TempleService {

    private final TempleRepository templeRepository;

    public TempleService(TempleRepository templeRepository) {
        this.templeRepository = templeRepository;
    }

    @Transactional
    public TempleResponse createTemple(CreateTempleRequest request) {
        var temple = templeRepository.save(new Temple(request.name().strip()));
        return TempleMapper.toResponse(temple);
    }

    @Transactional(readOnly = true)
    public TempleResponse getTemple(Long templeId) {
        return TempleMapper.toResponse(findTempleOrThrow(templeId));
    }

    @Transactional(readOnly = true)
    public Temple findTempleOrThrow(Long templeId) {
        return templeRepository.findById(templeId)
                .orElseThrow(() -> ApiException.notFound("Temple", templeId));
    }
}
