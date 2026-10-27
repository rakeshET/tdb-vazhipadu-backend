package com.tdb.vazhipadu.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tdb.vazhipadu.entity.Nakshatra;
import com.tdb.vazhipadu.entity.OfferingOrder;
import com.tdb.vazhipadu.repository.OfferingRepository;
import com.tdb.vazhipadu.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

@SpringBootTest
@AutoConfigureMockMvc
class CancellationAndCollectionIntegrationTest {

    private static final String TODAY = "2026-11-01";
    private static final String YESTERDAY = "2026-10-31";

    @TestConfiguration
    static class QuarterPastMidnightIst {

        @Bean
        @Primary
        Clock fixedClock() {
            var zone = ZoneId.of("Asia/Kolkata");
            return Clock.fixed(OffsetDateTime.parse(TODAY + "T00:15:00+05:30").toInstant(), zone);
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private OfferingRepository offeringRepository;
    @Autowired
    private OrderRepository orderRepository;

    private long templeId;
    private long pooja;
    private long archana;

    @BeforeEach
    void seed() throws Exception {
        templeId = create("/api/temples", "{\"name\": \"Chottanikkara\"}").get("id").asLong();
        pooja = create(offeringsUrl(), "{\"name\": \"Udayasthamana Pooja\", \"price\": 5000.50, \"dailyLimit\": 1}")
                .get("id").asLong();
        archana = create(offeringsUrl(), "{\"name\": \"Archana\", \"price\": 33.33}").get("id").asLong();
    }

    @Test
    void shouldCountOrderPlacedAtQuarterPastMidnightIstOnThatDate() throws Exception {
        placeOrder(archana, 3, status().isCreated());

        var today = report(TODAY);
        var yesterday = report(YESTERDAY);

        assertThat(today.get("totalAmount").decimalValue()).isEqualByComparingTo("99.99");
        assertThat(today.get("offerings").get(0).get("quantity").asLong()).isEqualTo(3);
        assertThat(yesterday.get("totalAmount").decimalValue()).isEqualByComparingTo("0.00");
        assertThat(yesterday.get("offerings")).isEmpty();
    }

    @Test
    void shouldExcludeCancelledOrderFromReportAndFreeItsDailyLimit() throws Exception {
        var first = placeOrder(pooja, 1, status().isCreated()).get("id").asLong();
        placeOrder(pooja, 1, status().isConflict());

        mockMvc.perform(post("/api/temples/%d/orders/%d/cancel".formatted(templeId, first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").exists());

        assertThat(report(TODAY).get("offerings")).isEmpty();
        placeOrder(pooja, 1, status().isCreated());
        assertThat(report(TODAY).get("totalAmount").decimalValue()).isEqualByComparingTo("5000.50");
    }

    @Test
    void shouldReportTotalEqualToSumOfOfferingTotals() throws Exception {
        placeOrder(archana, 2, status().isCreated());
        placeOrder(archana, 1, status().isCreated());
        placeOrder(pooja, 1, status().isCreated());

        var report = report(TODAY);

        var sumOfOfferings = BigDecimal.ZERO;
        for (var offering : report.get("offerings")) {
            sumOfOfferings = sumOfOfferings.add(offering.get("amount").decimalValue());
        }
        assertThat(report.get("totalAmount").decimalValue()).isEqualByComparingTo(sumOfOfferings)
                .isEqualByComparingTo("5100.49");
    }

    @Test
    void shouldKeepClosedDayReportUnchangedWhenOrderFromEarlierDayCannotBeCancelled() throws Exception {
        var offering = offeringRepository.findById(archana).orElseThrow();
        var order = new OfferingOrder(offering.getTemple(), "Devotee", "9876543210", Nakshatra.ROHINI,
                LocalDate.parse(TODAY).plusDays(9), OffsetDateTime.parse(YESTERDAY + "T10:00:00+05:30").toInstant());
        order.addItem(offering, 1);
        var orderId = orderRepository.save(order).getId();
        var before = report(YESTERDAY).get("totalAmount").decimalValue();

        mockMvc.perform(post("/api/temples/%d/orders/%d/cancel".formatted(templeId, orderId)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_CANCELLABLE"));

        assertThat(before).isEqualByComparingTo("33.33");
        assertThat(report(YESTERDAY).get("totalAmount").decimalValue()).isEqualByComparingTo(before);
    }

    @Test
    void shouldReturn400ForDateOutsideSupportedRange() throws Exception {
        mockMvc.perform(get("/api/temples/%d/reports/daily-collection".formatted(templeId))
                        .param("date", "+999999999-12-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void shouldRejectCancellingTwice() throws Exception {
        var orderId = placeOrder(archana, 1, status().isCreated()).get("id").asLong();
        var cancelUrl = "/api/temples/%d/orders/%d/cancel".formatted(templeId, orderId);

        mockMvc.perform(post(cancelUrl)).andExpect(status().isOk());
        mockMvc.perform(post(cancelUrl))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_ALREADY_CANCELLED"));
    }

    private JsonNode placeOrder(long offeringId, int quantity, ResultMatcher expectedStatus) throws Exception {
        var body = """
                {"devoteeName": "Devotee", "phone": "9876543210", "nakshatra": "ROHINI", "offeringDate": "%s",
                 "items": [{"offeringId": %d, "quantity": %d}]}""".formatted(TODAY, offeringId, quantity);
        var result = mockMvc.perform(post("/api/temples/%d/orders".formatted(templeId))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(expectedStatus)
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode report(String date) throws Exception {
        var result = mockMvc.perform(get("/api/temples/%d/reports/daily-collection".formatted(templeId))
                        .param("date", date))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode create(String url, String json) throws Exception {
        var result = mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String offeringsUrl() {
        return "/api/temples/%d/offerings".formatted(templeId);
    }
}
