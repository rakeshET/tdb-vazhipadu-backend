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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConcurrentCancellationIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private Clock clock;

    @Test
    void shouldCancelOnlyOnceWhenTenCancelRequestsArriveTogether() throws Exception {
        var templeId = post("/api/temples", "{\"name\": \"Vaikom\"}").get("id").asLong();
        var offeringId = post("/api/temples/%d/offerings".formatted(templeId), "{\"name\": \"Archana\", \"price\": 50}")
                .get("id")
                .asLong();
        var orderId = post(
                        "/api/temples/%d/orders".formatted(templeId),
                        """
                {"devoteeName": "Devotee", "phone": "9876543210", "nakshatra": "ROHINI", "offeringDate": "%s",
                 "items": [{"offeringId": %d, "quantity": 1}]}"""
                                .formatted(LocalDate.now(clock).plusDays(1), offeringId))
                .get("id")
                .asLong();
        var cancelUrl = "/api/temples/%d/orders/%d/cancel".formatted(templeId, orderId);

        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(10);
        try {
            var futures = new ArrayList<Future<HttpStatusCode>>();
            for (int i = 0; i < 10; i++) {
                Callable<HttpStatusCode> cancel = () -> {
                    start.await();
                    return rest.postForEntity(cancelUrl, null, JsonNode.class).getStatusCode();
                };
                futures.add(executor.submit(cancel));
            }
            start.countDown();

            var statuses = new ArrayList<HttpStatusCode>();
            for (var future : futures) {
                statuses.add(future.get());
            }

            assertThat(statuses).filteredOn(HttpStatus.OK::equals).hasSize(1);
            assertThat(statuses).filteredOn(HttpStatus.CONFLICT::equals).hasSize(9);
        } finally {
            executor.shutdownNow();
        }
    }

    private JsonNode post(String url, String json) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var response = rest.postForEntity(url, new HttpEntity<>(json, headers), JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }
}
