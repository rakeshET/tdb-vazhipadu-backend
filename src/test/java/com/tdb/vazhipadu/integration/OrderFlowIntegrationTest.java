package com.tdb.vazhipadu.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OrderFlowIntegrationTest {

    private static final String TOMORROW =
            LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(1).toString();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private long templeId;

    @BeforeEach
    void createTemple() throws Exception {
        templeId = postForId("/api/temples", """
                {"name": "Sree Padmanabhaswamy"}""");
    }

    @Test
    void shouldPlaceOrderWithServerComputedTotalAndFetchItById() throws Exception {
        var pushpanjali = createOffering("Pushpanjali", "30", true);
        var archana = createOffering("Archana", "50", true);

        var orderId = postForId(
                "/api/temples/%d/orders".formatted(templeId),
                orderJson("[{\"offeringId\": %d, \"quantity\": 2}, {\"offeringId\": %d, \"quantity\": 1}]"
                        .formatted(pushpanjali, archana)));

        mockMvc.perform(get("/api/temples/%d/orders/%d".formatted(templeId, orderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(110.00))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.nakshatra").value("ROHINI"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].lineTotal").value(60.00));
    }

    @Test
    void shouldRejectOrderForInactiveOffering() throws Exception {
        var archana = createOffering("Archana", "50", false);

        mockMvc.perform(post("/api/temples/%d/orders".formatted(templeId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson("[{\"offeringId\": %d, \"quantity\": 1}]".formatted(archana))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("OFFERING_INACTIVE"));
    }

    @Test
    void shouldRejectOrderAfterOfferingIsDeactivated() throws Exception {
        var archana = createOffering("Archana", "50", true);
        mockMvc.perform(put("/api/temples/%d/offerings/%d".formatted(templeId, archana))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Archana\", \"price\": 50, \"active\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(post("/api/temples/%d/orders".formatted(templeId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson("[{\"offeringId\": %d, \"quantity\": 1}]".formatted(archana))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldReturn409WhenOfferingNameAlreadyExistsInTemple() throws Exception {
        createOffering("Archana", "50", true);

        mockMvc.perform(post("/api/temples/%d/offerings".formatted(templeId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"archana\", \"price\": 40}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_OFFERING"));
    }

    @Test
    void shouldReturn404WhenTempleUnknown() throws Exception {
        mockMvc.perform(get("/api/temples/999999/offerings"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    private long createOffering(String name, String price, boolean active) throws Exception {
        return postForId(
                "/api/temples/%d/offerings".formatted(templeId),
                "{\"name\": \"%s\", \"price\": %s, \"active\": %s}".formatted(name, price, active));
    }

    private static String orderJson(String items) {
        return """
                {"devoteeName": "Ravi", "phone": "9876543210", "nakshatra": "ROHINI",
                 "offeringDate": "%s", "items": %s}"""
                .formatted(TOMORROW, items);
    }

    private long postForId(String url, String json) throws Exception {
        var result = mockMvc.perform(
                        post(url).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();
        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asLong();
    }
}
