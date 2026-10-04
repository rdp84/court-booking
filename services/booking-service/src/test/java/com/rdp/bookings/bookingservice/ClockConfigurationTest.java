package com.rdp.bookings.bookingservice;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class ClockConfigurationTest {

    @Configuration
    @EnableConfigurationProperties(ClubProperties.class)
    @Import(ClockConfiguration.class)
    static class TestConfiguration {
    }

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void shouldUseClubTimeZone() {
        contextRunner.withPropertyValues("club.time-zone=Asia/Tokyo").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(Clock.class).getZone()).isEqualTo(ZoneId.of("Asia/Tokyo"));
        });
    }

    @Test
    void shouldFailToStartWithoutClubTimeZone() {
        contextRunner.run(context -> assertThat(context).hasFailed());
    }

    @Test
    void shouldFailToStartWithUnknownClubTimeZone() {
        contextRunner.withPropertyValues("club.time-zone=Not/AZone")
                .run(context -> assertThat(context).hasFailed());
    }
}
