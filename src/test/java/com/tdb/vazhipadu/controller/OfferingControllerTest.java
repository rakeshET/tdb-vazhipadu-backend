package com.tdb.vazhipadu.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tdb.vazhipadu.dto.request.OfferingRequest;
import com.tdb.vazhipadu.dto.response.OfferingResponse;
import com.tdb.vazhipadu.service.OfferingService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OfferingController.class)
class OfferingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OfferingService offeringService;

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 100_001})
    void shouldReturn400WhenDailyLimitOutOfRange(int dailyLimit) throws Exception {
        mockMvc.perform(post("/api/temples/1/offerings").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Udayasthamana Pooja\", \"price\": 5000, \"dailyLimit\": %d}"
                                .formatted(dailyLimit)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("dailyLimit"));
        verifyNoInteractions(offeringService);
    }

    @Test
    void shouldReturn400WhenPriceNotPositive() throws Exception {
        mockMvc.perform(post("/api/temples/1/offerings").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Archana\", \"price\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("price"));
    }

    @Test
    void shouldCreateOfferingWithDailyLimit() throws Exception {
        var request = new OfferingRequest("Udayasthamana Pooja", new BigDecimal("5000"), null, 50);
        when(offeringService.createOffering(eq(1L), eq(request)))
                .thenReturn(new OfferingResponse(7L, 1L, "Udayasthamana Pooja", new BigDecimal("5000.00"), true, 50));

        mockMvc.perform(post("/api/temples/1/offerings").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Udayasthamana Pooja\", \"price\": 5000, \"dailyLimit\": 50}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dailyLimit").value(50));
    }
}
