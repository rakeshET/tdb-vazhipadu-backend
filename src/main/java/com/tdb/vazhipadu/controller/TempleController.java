package com.tdb.vazhipadu.controller;

import com.tdb.vazhipadu.dto.request.CreateTempleRequest;
import com.tdb.vazhipadu.dto.response.TempleResponse;
import com.tdb.vazhipadu.service.TempleService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/temples")
public class TempleController {

    private final TempleService templeService;

    public TempleController(TempleService templeService) {
        this.templeService = templeService;
    }

    @PostMapping
    public ResponseEntity<TempleResponse> createTemple(@Valid @RequestBody CreateTempleRequest request) {
        var created = templeService.createTemple(request);
        return ResponseEntity.created(URI.create("/api/temples/" + created.id())).body(created);
    }

    @GetMapping("/{templeId}")
    public TempleResponse getTemple(@PathVariable Long templeId) {
        return templeService.getTemple(templeId);
    }
}
