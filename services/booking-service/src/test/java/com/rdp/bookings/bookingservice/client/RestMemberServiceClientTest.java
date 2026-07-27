package com.rdp.bookings.bookingservice.client;

import static org.assertj.core.api.Assertions.assertThat;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

import com.github.tomakehurst.wiremock.WireMockServer;

@SpringBootTest
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
        FlywayAutoConfiguration.class })
@EnableWireMock({ @ConfigureWireMock(name = "member-service", port = 8082) })
@TestPropertySource(properties = "services.member-service.url=http://localhost:8082")
class RestMemberServiceClientTest {

    @InjectWireMock("member-service")
    WireMockServer wireMockServer;

    @Autowired
    private RestMemberServiceClient client;

    @Test
    void shouldReturnMemberWhenFound() {
        final var memberId = UUID.randomUUID();

        wireMockServer.stubFor(get(urlEqualTo("/members/" + memberId))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("""
                        {
                            "id": "%s",
                            "accountBalance": 20.00,
                            "membershipStartDate": "2000-01-01",
                            "membershipEndDate": "2001-01-01"
                        }
                        """.formatted(memberId))));

        final var result = client.getMember(memberId);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(memberId);
        assertThat(result.get().accountBalance()).isEqualByComparingTo(new BigDecimal("20.00"));
    }

    @Test
    void shouldReturnEmptyWhenMemberNotFound() {
        final var memberId = UUID.randomUUID();

        wireMockServer.stubFor(get(urlEqualTo("/members/" + memberId)).willReturn(aResponse().withStatus(404)));

        final var result = client.getMember(memberId);

        assertThat(result).isNotPresent();
    }
}
