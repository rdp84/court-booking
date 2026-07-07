package com.rdp.courts.courtservice.timeslot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;
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

import com.rdp.courts.courtservice.court.Court;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = Replace.NONE)
class TimeSlotRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15.2");

    @Autowired
    TimeSlotRepository timeSlotRepository;

    @Autowired
    TestEntityManager entityManager;

    @Test
    void shouldSaveAndRetrieveTimeSlot() {
        final var court = new Court("Court 1", true);
        entityManager.persist(court);
        entityManager.flush();
        entityManager.clear();

        final var timeSlot = new TimeSlot(court, LocalTime.of(6, 45));
        timeSlotRepository.save(timeSlot);
        entityManager.flush();
        entityManager.clear();

        final var retrieved = timeSlotRepository.findById(timeSlot.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getCourt().getId()).isEqualTo(court.getId());
        assertThat(retrieved.get().getSlotStart()).isEqualTo(LocalTime.of(6, 45));
        assertThat(retrieved.get().getSlotEnd()).isEqualTo(LocalTime.of(7, 30));
    }

    @Test
    void shouldOnlyFindTimeSlotsForSpecifiedCourt() {
        final var court1 = new Court("Court 1", true);
        final var court3 = new Court("Court 3", true);
        entityManager.persist(court1);
        entityManager.persist(court3);
        entityManager.flush();
        entityManager.clear();

        timeSlotRepository.save(new TimeSlot(court1, LocalTime.of(6, 45)));
        timeSlotRepository.save(new TimeSlot(court1, LocalTime.of(17, 15)));
        timeSlotRepository.save(new TimeSlot(court1, LocalTime.of(21, 0)));
        timeSlotRepository.save(new TimeSlot(court3, LocalTime.of(7, 15)));

        entityManager.flush();
        entityManager.clear();

        final var slots = timeSlotRepository.findByCourt(court1);

        assertThat(slots).hasSize(3);
        assertThat(slots).extracting(TimeSlot::getSlotStart).containsExactlyInAnyOrder(LocalTime.of(6, 45),
                LocalTime.of(17, 15), LocalTime.of(21, 0));
    }

    @Test
    void shouldThrowConstraintViolationWhenDuplicateCourtAndSlotStart() {
        final var court = new Court("Court 2", true);
        entityManager.persist(court);
        entityManager.flush();
        entityManager.clear();

        timeSlotRepository.save(new TimeSlot(court, LocalTime.of(12, 0)));
        entityManager.flush();
        entityManager.clear();

        timeSlotRepository.save(new TimeSlot(court, LocalTime.of(12, 0)));
        assertThatThrownBy(() -> entityManager.flush()).isInstanceOf(ConstraintViolationException.class)
                .extracting(e -> ((ConstraintViolationException) e).getConstraintName()).isEqualTo("uq_court_slot");
    }

    @Test
    void shouldRetrieveNoTimeSlotsForNonExistentUUID() {
        final var uuid = UUID.randomUUID();
        final var retrieved = timeSlotRepository.findById(uuid);
        assertThat(retrieved).isNotPresent();
    }
}
