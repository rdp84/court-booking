package com.rdp.courts.courtservice.court;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = Replace.NONE)
class CourtRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15.2");

    @Autowired
    CourtRepository courtRepository;

    @Autowired
    TestEntityManager entityManager;

    @Test
    void shouldSaveAndRetrieveCourt() {
        final var court = new Court("Court 1", true);
        final var saved = courtRepository.save(court);

        entityManager.flush();
        entityManager.clear();

        final var retrieved = courtRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getId()).isNotNull();
        assertThat(retrieved.get().getCreatedAt()).isNotNull();
        assertThat(retrieved.get().getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(retrieved.get().getName()).isEqualTo("Court 1");
        assertThat(retrieved.get().getIsActive()).isTrue();
    }

    @Test
    void shouldSaveAndRetrieveInactiveCourt() {
        final var court = new Court("Court 1", false);
        final var saved = courtRepository.save(court);

        entityManager.flush();
        entityManager.clear();

        final var retrieved = courtRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getIsActive()).isFalse();
    }

    @Test
    void shouldRetrieveNoCourtsForNonExistentUUID() {
        final var uuid = UUID.randomUUID();
        final var retrieved = courtRepository.findById(uuid);
        assertThat(retrieved).isNotPresent();
    }

    @Test
    void shouldRetrieveAllCourts() {
        courtRepository.save(new Court("Court 1", true));
        courtRepository.save(new Court("Court 2", true));
        courtRepository.save(new Court("Court 3", true));
        courtRepository.save(new Court("Court 4", true));

        entityManager.flush();
        entityManager.clear();

        final var courts = courtRepository.findAll();
        assertThat(courts).hasSize(4);
    }

    @Test
    void shouldDeactivateCourt() {
        final var court = new Court("Court 2", true);
        var saved = courtRepository.save(court);

        saved.setIsActive(false);
        courtRepository.save(saved);

        entityManager.flush();
        entityManager.clear();

        final var retrieved = courtRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getIsActive()).isFalse();
    }

    @Test
    void shouldMaintainCreatedAtAfterUpdate() {
        final var court = new Court("Court 3", true);
        final var saved = courtRepository.save(court);
        entityManager.flush();
        entityManager.clear();

        var retrieved = courtRepository.findById(saved.getId()).orElseThrow();
        final var createdAt = retrieved.getCreatedAt();

        retrieved.setIsActive(false);
        courtRepository.save(retrieved);
        entityManager.flush();
        entityManager.clear();

        final var afterUpdate = courtRepository.findById(saved.getId());
        assertThat(afterUpdate).isPresent();
        assertThat(afterUpdate.get().getCreatedAt()).isEqualTo(createdAt);
    }
}
