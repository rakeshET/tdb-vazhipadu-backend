package com.tdb.vazhipadu.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tdb.vazhipadu.dto.response.DailyCollectionReport;
import com.tdb.vazhipadu.dto.response.OfferingCollection;
import com.tdb.vazhipadu.exception.ApiException;
import com.tdb.vazhipadu.service.ReportService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 1);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @Test
    void shouldReturnReportWithExactDecimalAmounts() throws Exception {
        when(reportService.dailyCollection(1L, DATE))
                .thenReturn(new DailyCollectionReport(
                        1L,
                        DATE,
                        ZoneId.of("Asia/Kolkata"),
                        new BigDecimal("99.99"),
                        List.of(new OfferingCollection(4L, "Archana", 3L, new BigDecimal("99.99")))));

        mockMvc.perform(get("/api/temples/1/reports/daily-collection").param("date", "2026-11-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-11-01"))
                .andExpect(jsonPath("$.timeZone").value("Asia/Kolkata"))
                .andExpect(jsonPath("$.totalAmount").value(99.99))
                .andExpect(jsonPath("$.offerings[0].quantity").value(3));
    }

    @Test
    void shouldReturn400WhenDateMissing() throws Exception {
        mockMvc.perform(get("/api/temples/1/reports/daily-collection"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        verifyNoInteractions(reportService);
    }

    @Test
    void shouldReturn400WhenDateUnparseable() throws Exception {
        mockMvc.perform(get("/api/temples/1/reports/daily-collection").param("date", "1-11-2026"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404WhenTempleUnknown() throws Exception {
        when(reportService.dailyCollection(99L, DATE)).thenThrow(ApiException.notFound("Temple", 99L));

        mockMvc.perform(get("/api/temples/99/reports/daily-collection").param("date", "2026-11-01"))
                .andExpect(status().isNotFound());
    }
}
