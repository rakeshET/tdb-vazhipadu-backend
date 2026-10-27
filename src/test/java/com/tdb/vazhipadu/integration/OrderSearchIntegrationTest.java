package com.tdb.vazhipadu.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OrderSearchIntegrationTest {

    private static final String RAVI = "9876543210";
    private static final String MEERA = "9123456789";

    @Autowired
    private Clock clock;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private long templeId;
    private String tomorrow;

    @BeforeEach
    void seedOrders() throws Exception {
        tomorrow = LocalDate.now(clock).plusDays(1).toString();
        templeId =
                create("/api/temples", "{\"name\": \"Ettumanoor\"}").get("id").asLong();
        var offeringId = create(
                        "/api/temples/%d/offerings".formatted(templeId), "{\"name\": \"Archana\", \"price\": 50}")
                .get("id")
                .asLong();
        for (int i = 0; i < 7; i++) {
            placeOrder(offeringId, i % 2 == 0 ? RAVI : MEERA);
        }
    }

    @Test
    void shouldReturnOnlyThatDevoteesOrdersWhenSearchingByPhone() throws Exception {
        var page = search("phone=" + RAVI + "&size=100");

        assertThat(page.get("totalElements").asInt()).isEqualTo(4);
        page.get("content")
                .forEach(order -> assertThat(order.get("phone").asText()).isEqualTo(RAVI));
    }

    @Test
    void shouldReturnNextResultsOnPageTwoWithoutRepeatsOrGaps() throws Exception {
        var all = ids(search("size=100"));
        var pageOne = search("page=0&size=3");
        var pageTwo = search("page=1&size=3");

        assertThat(ids(pageOne)).containsExactlyElementsOf(all.subList(0, 3));
        assertThat(ids(pageTwo)).containsExactlyElementsOf(all.subList(3, 6));
        assertThat(pageOne.get("totalPages").asInt()).isEqualTo(3);
        assertThat(pageTwo.get("hasNext").asBoolean()).isTrue();
    }

    @Test
    void shouldFilterByDateAndStatus() throws Exception {
        assertThat(search("date=" + tomorrow + "&status=CONFIRMED")
                        .get("totalElements")
                        .asInt())
                .isEqualTo(7);
        assertThat(search("status=CANCELLED").get("totalElements").asInt()).isZero();
        assertThat(search("date=2099-01-01").get("totalElements").asInt()).isZero();
    }

    private void placeOrder(long offeringId, String phone) throws Exception {
        create(
                "/api/temples/%d/orders".formatted(templeId),
                """
                {"devoteeName": "Devotee", "phone": "%s", "nakshatra": "ROHINI", "offeringDate": "%s",
                 "items": [{"offeringId": %d, "quantity": 1}]}"""
                        .formatted(phone, tomorrow, offeringId));
    }

    private JsonNode search(String query) throws Exception {
        var result = mockMvc.perform(get("/api/temples/%d/orders?%s".formatted(templeId, query)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode create(String url, String json) throws Exception {
        var result = mockMvc.perform(
                        post(url).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private static List<Long> ids(JsonNode page) {
        var ids = new ArrayList<Long>();
        page.get("content").forEach(order -> ids.add(order.get("id").asLong()));
        return ids;
    }
}
