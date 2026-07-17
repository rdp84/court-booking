package com.rdp.courts.courtservice.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(CourtPricingController.class)
class CourtPricingControllerTest {

    @Autowired
    MockMvcTester mockMvc;

    @MockitoBean
    CourtPricingService courtPricingService;

    @Test
    void shouldReturnFeeWhenPricingRuleFound() {
        final var day = LocalDate.of(2000, 1, 3);
        final var time = LocalTime.of(17, 15);
        final var fee = new BigDecimal("6.00");
        given(courtPricingService.calculateFee(day, time)).willReturn(Optional.of(fee));

        assertThat(mockMvc.get().uri("/courts/pricing?date=2000-01-03&time=17:15")).hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        {
                            "fee": 6.00
                        }
                        """);
    }

    @Test
    void shouldReturnA404ForNonExistentCourtPricing() {
        final var day = LocalDate.of(2000, 1, 4);
        final var time = LocalTime.of(3, 30);
        given(courtPricingService.calculateFee(day, time)).willReturn(Optional.empty());

        assertThat(mockMvc.get().uri("/courts/pricing?date=2000-01-04&time=03:30")).hasStatus(404);
    }

    @Test
    void shouldReturnA4XXClientErrorForInvalidDate() {
        assertThat(mockMvc.get().uri("/courts/pricing?date=2000xx04&time=12:00")).hasStatus4xxClientError();
    }

    @Test
    void shouldReturnA4XXClientErrorForInvalidTime() {
        assertThat(mockMvc.get().uri("/courts/pricing?date=2000-01-04&time=03..30")).hasStatus4xxClientError();
    }
}
