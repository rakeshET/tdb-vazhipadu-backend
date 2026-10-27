package com.tdb.vazhipadu.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DailyLimitIntegrationTest {

    @Autowired
    private TestRestTemplate rest;
    @Autowired
    private Clock clock;

    private long templeId;
    private String tomorrow;

    @BeforeEach
    void createTemple() {
        tomorrow = LocalDate.now(clock).plusDays(1).toString();
        templeId = post("/api/temples", "{\"name\": \"Guruvayur\"}").getBody().get("id").asLong();
    }

    @Test
    void shouldRejectOrderForTwoAndAcceptOrderForOneWhenFourOfFiveBooked() {
        var pooja = createOffering("Udayasthamana Pooja", 5);
        for (int i = 0; i < 4; i++) {
            assertThat(placeOrder(pooja, 1, tomorrow).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        var tooMany = placeOrder(pooja, 2, tomorrow);
        var lastSlot = placeOrder(pooja, 1, tomorrow);

        assertThat(tooMany.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(tooMany.getBody().get("code").asText()).isEqualTo("DAILY_LIMIT_EXCEEDED");
        assertThat(lastSlot.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(placeOrder(pooja, 1, tomorrow).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void shouldAcceptExactlyFiveOfTwentySimultaneousOrders() throws Exception {
        var pooja = createOffering("Udayasthamana Pooja", 5);
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(20);
        try {
            var futures = new ArrayList<Future<HttpStatusCode>>();
            for (int i = 0; i < 20; i++) {
                Callable<HttpStatusCode> order = () -> {
                    start.await();
                    return placeOrder(pooja, 1, tomorrow).getStatusCode();
                };
                futures.add(executor.submit(order));
            }
            start.countDown();

            var statuses = new ArrayList<HttpStatusCode>();
            for (var future : futures) {
                statuses.add(future.get());
            }

            assertThat(statuses).filteredOn(HttpStatus.CREATED::equals).hasSize(5);
            assertThat(statuses).filteredOn(HttpStatus.CONFLICT::equals).hasSize(15);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldCountLimitPerOfferingDate() {
        var pooja = createOffering("Udayasthamana Pooja", 1);
        var dayAfter = LocalDate.now(clock).plusDays(2).toString();

        assertThat(placeOrder(pooja, 1, tomorrow).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(placeOrder(pooja, 1, dayAfter).getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void shouldNotLimitOfferingsWithoutDailyLimit() {
        var archana = createOffering("Archana", null);

        assertThat(placeOrder(archana, 100, tomorrow).getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private long createOffering(String name, Integer dailyLimit) {
        var body = "{\"name\": \"%s\", \"price\": 500, \"dailyLimit\": %s}".formatted(name, dailyLimit);
        var response = post("/api/temples/%d/offerings".formatted(templeId), body);
        assertThat(response.getBody().get("dailyLimit").isNull()).isEqualTo(dailyLimit == null);
        return response.getBody().get("id").asLong();
    }

    private ResponseEntity<JsonNode> placeOrder(long offeringId, int quantity, String date) {
        return post("/api/temples/%d/orders".formatted(templeId), """
                {"devoteeName": "Devotee", "phone": "9876543210", "nakshatra": "ROHINI", "offeringDate": "%s",
                 "items": [{"offeringId": %d, "quantity": %d}]}""".formatted(date, offeringId, quantity));
    }

    private ResponseEntity<JsonNode> post(String url, String json) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.postForEntity(url, new HttpEntity<>(json, headers), JsonNode.class);
    }
}
