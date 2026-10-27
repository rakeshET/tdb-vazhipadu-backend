package com.tdb.vazhipadu.controller;

import com.tdb.vazhipadu.dto.request.OfferingRequest;
import com.tdb.vazhipadu.dto.response.OfferingResponse;
import com.tdb.vazhipadu.service.OfferingService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/temples/{templeId}/offerings")
public class OfferingController {

    private final OfferingService offeringService;

    public OfferingController(OfferingService offeringService) {
        this.offeringService = offeringService;
    }

    @PostMapping
    public ResponseEntity<OfferingResponse> createOffering(
            @PathVariable Long templeId, @Valid @RequestBody OfferingRequest request) {
        var created = offeringService.createOffering(templeId, request);
        var location = URI.create("/api/temples/%d/offerings/%d".formatted(templeId, created.id()));
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public List<OfferingResponse> listOfferings(@PathVariable Long templeId) {
        return offeringService.listOfferings(templeId);
    }

    @GetMapping("/{offeringId}")
    public OfferingResponse getOffering(@PathVariable Long templeId, @PathVariable Long offeringId) {
        return offeringService.getOffering(templeId, offeringId);
    }

    @PutMapping("/{offeringId}")
    public OfferingResponse updateOffering(
            @PathVariable Long templeId, @PathVariable Long offeringId, @Valid @RequestBody OfferingRequest request) {
        return offeringService.updateOffering(templeId, offeringId, request);
    }
}
