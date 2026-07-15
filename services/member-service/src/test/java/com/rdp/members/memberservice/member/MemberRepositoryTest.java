package com.rdp.members.memberservice.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = Replace.NONE)
class MemberRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15.2");

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    TestEntityManager entityManager;

    @Test
    void shouldSaveAndRetrieveMember() {
        final var member = newMember("jane.doe@example.com", new BigDecimal("25.00"));
        final var saved = memberRepository.save(member);

        entityManager.flush();
        entityManager.clear();

        final var retrieved = memberRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getId()).isNotNull();
        assertThat(retrieved.get().getName()).isEqualTo(member.getName());
        assertThat(retrieved.get().getEmail()).isEqualTo(member.getEmail());
        assertThat(retrieved.get().getPasswordHash()).isEqualTo(member.getPasswordHash());
        assertThat(retrieved.get().getAccountBalance()).isEqualByComparingTo(member.getAccountBalance());
        assertThat(retrieved.get().getMembershipStartDate()).isEqualTo(member.getMembershipStartDate());
        assertThat(retrieved.get().getMembershipEndDate()).isEqualTo(member.getMembershipEndDate());
        assertThat(retrieved.get().getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void shouldRetrieveNoMemberForNonExistentUUID() {
        final var retrieved = memberRepository.findById(UUID.randomUUID());
        assertThat(retrieved).isNotPresent();
    }

    @Test
    void shouldRetrieveAllMembers() {
        memberRepository.save(newMember("member.a@example.com"));
        memberRepository.save(newMember("member.b@example.com"));
        memberRepository.save(newMember("member.c@example.com"));

        entityManager.flush();
        entityManager.clear();

        final var members = memberRepository.findAll();
        assertThat(members).extracting(Member::getEmail).contains("member.a@example.com", "member.b@example.com",
                "member.c@example.com");
    }

    @Test
    void shouldThrowConstraintViolationWhenDuplicateEmail() {
        memberRepository.save(newMember("duplicate@example.com"));
        entityManager.flush();
        entityManager.clear();

        memberRepository.save(newMember("duplicate@example.com"));
        assertThatThrownBy(() -> entityManager.flush()).isInstanceOf(ConstraintViolationException.class)
                .extracting(e -> ((ConstraintViolationException) e).getConstraintName()).isEqualTo("uq_email");
    }

    @Test
    void shouldThrowConstraintViolationWhenMembershipEndNotAfterStart() {
        final var member = new Member("Jane Doe", "jane.end-date@example.com", "hashed-password",
                new BigDecimal("0.00"), LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 1));
        memberRepository.save(member);
        assertThatThrownBy(() -> entityManager.flush()).isInstanceOf(ConstraintViolationException.class)
                .extracting(e -> ((ConstraintViolationException) e).getConstraintName())
                .isEqualTo("chk_membership_dates");
    }

    @Test
    void shouldThrowConstraintViolationWhenBalanceNegative() {
        final var member = newMember("jane.balance@example.com", new BigDecimal("-10.00"));
        memberRepository.save(member);
        assertThatThrownBy(() -> entityManager.flush()).isInstanceOf(ConstraintViolationException.class)
                .extracting(e -> ((ConstraintViolationException) e).getConstraintName())
                .isEqualTo("chk_balance_non_negative");
    }

    private Member newMember(String email) {
        return newMember(email, new BigDecimal("0.00"));
    }

    private Member newMember(String email, BigDecimal accountBalance) {
        return new Member("Jane Doe", email, "hashed-password", accountBalance, LocalDate.of(2000, 1, 1),
                LocalDate.of(2000, 6, 1));
    }
}
