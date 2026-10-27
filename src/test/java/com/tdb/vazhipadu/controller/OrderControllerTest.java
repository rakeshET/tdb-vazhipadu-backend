package com.tdb.vazhipadu.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tdb.vazhipadu.exception.ApiException;
import com.tdb.vazhipadu.exception.ErrorCode;
import com.tdb.vazhipadu.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    void shouldReturn400WithFieldErrorsWhenBodyInvalid() throws Exception {
        var body = """
                {"devoteeName": " ", "phone": "12345", "nakshatra": "ROHINI",
                 "offeringDate": "2026-11-01", "items": []}
                """;

        mockMvc.perform(post("/api/temples/1/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.path").value("/api/temples/1/orders"))
                .andExpect(jsonPath("$.errors[*].field").value(
                        containsInAnyOrder("devoteeName", "items", "phone")));
        verifyNoInteractions(orderService);
    }

    @Test
    void shouldReturn400WhenItemQuantityIsZero() throws Exception {
        var body = """
                {"devoteeName": "Ravi", "phone": "9876543210", "nakshatra": "ROHINI",
                 "offeringDate": "2026-11-01", "items": [{"offeringId": 1, "quantity": 0}]}
                """;

        mockMvc.perform(post("/api/temples/1/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("items[0].quantity"));
    }

    @Test
    void shouldReturn400NamingFieldWhenNakshatraUnknown() throws Exception {
        var body = """
                {"devoteeName": "Ravi", "phone": "9876543210", "nakshatra": "PLUTO",
                 "offeringDate": "2026-11-01", "items": [{"offeringId": 1, "quantity": 1}]}
                """;

        mockMvc.perform(post("/api/temples/1/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.errors[0].field").value("nakshatra"));
    }

    @Test
    void shouldMapBusinessRuleViolationTo422() throws Exception {
        when(orderService.placeOrder(eq(1L), any()))
                .thenThrow(new ApiException(ErrorCode.OFFERING_INACTIVE, "Offering 'Archana' is not currently active"));
        var body = """
                {"devoteeName": "Ravi", "phone": "9876543210", "nakshatra": "rohini",
                 "offeringDate": "2026-11-01", "items": [{"offeringId": 1, "quantity": 1}]}
                """;

        mockMvc.perform(post("/api/temples/1/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("OFFERING_INACTIVE"))
                .andExpect(jsonPath("$.message").value("Offering 'Archana' is not currently active"));
    }

    @Test
    void shouldReturn404InSameFormatWhenOrderNotFound() throws Exception {
        when(orderService.getOrder(1L, 42L)).thenThrow(ApiException.notFound("Order", 42L));

        mockMvc.perform(get("/api/temples/1/orders/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Order 42 not found"));
    }

    @Test
    void shouldReturn400WhenBodyMissing() throws Exception {
        mockMvc.perform(post("/api/temples/1/orders").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void shouldReturn409WhenDatabaseConstraintIsViolated() throws Exception {
        when(orderService.getOrder(1L, 1L)).thenThrow(new DataIntegrityViolationException("duplicate key"));

        mockMvc.perform(get("/api/temples/1/orders/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DATA_CONFLICT"));
    }

    @Test
    void shouldKeepFrameworkStatusInsteadOf500ForErrorResponseExceptions() throws Exception {
        when(orderService.getOrder(1L, 1L))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Try again later"));

        mockMvc.perform(get("/api/temples/1/orders/1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("REQUEST_REJECTED"))
                .andExpect(jsonPath("$.message").value("Try again later"));
    }

    @Test
    void shouldReturn400WhenPathIdIsNotANumber() throws Exception {
        mockMvc.perform(get("/api/temples/1/orders/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }
}
