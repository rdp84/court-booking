package com.rdp.members.memberservice.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rdp.members.memberservice.member.Member;

@ExtendWith(MockitoExtension.class)
class AccountTransactionServiceTest {

    @Mock
    AccountTransactionRepository accountTransactionRepository;

    @InjectMocks
    AccountTransactionService accountTransactionService;

    @Test
    void shouldRecordTopUpTransaction() {
        final var member = new Member("Jane Doe", "jane.doe@example.com", "hashed-password", BigDecimal.ZERO,
                LocalDate.of(2000, 1, 1), LocalDate.of(2000, 6, 1));

        accountTransactionService.recordTopUp(member, new BigDecimal("25.00"));

        final var captor = ArgumentCaptor.forClass(AccountTransaction.class);
        verify(accountTransactionRepository).save(captor.capture());

        final var saved = captor.getValue();
        assertThat(saved.getMember()).isEqualTo(member);
        assertThat(saved.getAmount()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(saved.getTransactionType()).isEqualTo(TransactionType.TOP_UP);
    }

    @Test
    void shouldReturnTransactionHistoryForMember() {
        final var member = new Member("Jane Doe", "jane.doe@example.com", "hashed-password", BigDecimal.ZERO,
                LocalDate.of(2000, 1, 1), LocalDate.of(2000, 6, 1));
        final var expected = List.of(new AccountTransaction(member, new BigDecimal("10.00"), TransactionType.TOP_UP));

        given(accountTransactionRepository.findByMemberOrderByCreatedAtDesc(member)).willReturn(expected);

        final var result = accountTransactionService.getTransactionHistory(member);
        assertThat(result).isEqualTo(expected);
    }
}
