package com.rdp.members.memberservice.transaction;

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

import com.rdp.members.memberservice.member.Member;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = Replace.NONE)
class AccountTransactionRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15.2");

    @Autowired
    AccountTransactionRepository accountTransactionRepository;

    @Autowired
    TestEntityManager entityManager;

    @Test
    void shouldSaveAndRetrieveAccountTransaction() {
        final var member = newMember();
        entityManager.persist(member);
        entityManager.flush();
        entityManager.clear();

        final var accountTransaction = new AccountTransaction(member, new BigDecimal("25.00"), TransactionType.TOP_UP);
        final var saved = accountTransactionRepository.save(accountTransaction);

        entityManager.flush();
        entityManager.clear();

        final var retrieved = accountTransactionRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getMember()).isNotNull();
        assertThat(retrieved.get().getAmount()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(retrieved.get().getTransactionType()).isEqualTo(TransactionType.TOP_UP);
        assertThat(retrieved.get().getReferenceId()).isNull();
        assertThat(retrieved.get().getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    void shouldRetrieveNoAccountTransactionForNonExistentUUID() {
        final var retrieved = accountTransactionRepository.findById(UUID.randomUUID());
        assertThat(retrieved).isNotPresent();
    }

    @Test
    void shouldThrowConstraintViolationWhenAmountIsZero() {
        final var member = newMember();
        entityManager.persist(member);
        entityManager.flush();
        entityManager.clear();

        accountTransactionRepository.save(new AccountTransaction(member, BigDecimal.ZERO, TransactionType.TOP_UP));
        assertThatThrownBy(() -> entityManager.flush()).isInstanceOf(ConstraintViolationException.class)
                .extracting(e -> ((ConstraintViolationException) e).getConstraintName())
                .isEqualTo("chk_amount_non_zero");
    }

    private Member newMember() {
        return new Member("Jane Doe", "jane.doe@example.com", "hashed-password", BigDecimal.ZERO,
                LocalDate.of(2000, 1, 1), LocalDate.of(2000, 6, 1));
    }
}
