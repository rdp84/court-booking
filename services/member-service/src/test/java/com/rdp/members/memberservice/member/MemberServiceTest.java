package com.rdp.members.memberservice.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.rdp.members.memberservice.transaction.AccountTransaction;
import com.rdp.members.memberservice.transaction.AccountTransactionService;
import com.rdp.members.memberservice.transaction.TransactionType;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    MemberRepository memberRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    AccountTransactionService accountTransactionService;

    @InjectMocks
    MemberService memberService;

    @ParameterizedTest
    @EnumSource(MembershipTerm.class)
    void shouldRegisterNewMember(final MembershipTerm membershipTerm) {
        given(memberRepository.existsByEmail("jane.doe@example.com")).willReturn(false);
        given(passwordEncoder.encode("plaintext-password")).willReturn("hashed-password");
        given(memberRepository.save(any(Member.class))).willAnswer(invocation -> invocation.getArgument(0));

        final var result = memberService.registerMember("Jane Doe", "jane.doe@example.com", "plaintext-password",
                membershipTerm);

        final var today = LocalDate.now();
        assertThat(result.getName()).isEqualTo("Jane Doe");
        assertThat(result.getEmail()).isEqualTo("jane.doe@example.com");
        assertThat(result.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(result.getAccountBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getMembershipStartDate()).isEqualTo(today);
        assertThat(result.getMembershipEndDate()).isEqualTo(membershipTerm.calculateEndDate(today));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "", " " })
    void shouldThrowIllegalArgumentExceptionForBlankName(final String name) {
        assertThatThrownBy(() -> memberService.registerMember(name, "jane.doe@example.com", "plaintext-password",
                MembershipTerm.ANNUAL)).isInstanceOf(IllegalArgumentException.class);

        verify(memberRepository, never()).save(any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "", " " })
    void shouldThrowIllegalArgumentExceptionForBlankEmail(final String email) {
        assertThatThrownBy(() -> memberService.registerMember("Jane Doe", email, "plaintext-password",
                MembershipTerm.ANNUAL)).isInstanceOf(IllegalArgumentException.class);

        verify(memberRepository, never()).save(any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "", " " })
    void shouldThrowIllegalArgumentExceptionForBlankPassword(final String password) {
        assertThatThrownBy(() -> memberService.registerMember("Jane Doe", "jane.doe@example.com", password,
                MembershipTerm.ANNUAL)).isInstanceOf(IllegalArgumentException.class);

        verify(memberRepository, never()).save(any());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionForNullMembershipTerm() {
        assertThatThrownBy(() -> memberService.registerMember("Jane Doe", "jane.doe@example.com",
                "plaintext-password", null)).isInstanceOf(IllegalArgumentException.class);

        verify(memberRepository, never()).save(any());
    }

    @Test
    void shouldThrowDuplicateEmailExceptionWhenEmailTaken() {
        given(memberRepository.existsByEmail("jane.doe@example.com")).willReturn(true);

        assertThatThrownBy(() -> memberService.registerMember("Jane Doe", "jane.doe@example.com",
                "plaintext-password", MembershipTerm.ANNUAL)).isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("jane.doe@example.com");

        verify(memberRepository, never()).save(any());
    }

    @Test
    void shouldReturnMemberWhenMemberExists() {
        final var uuid = UUID.randomUUID();
        final var member = new Member(uuid, "Jane Doe", "jane.doe@example.com", "hashed-password", BigDecimal.ZERO,
                LocalDate.of(2000, 1, 1), LocalDate.of(2000, 6, 1));

        given(memberRepository.findById(uuid)).willReturn(Optional.of(member));

        final var result = memberService.getMemberById(uuid);
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(uuid);
        assertThat(result.get().getName()).isEqualTo("Jane Doe");
    }

    @Test
    void shouldReturnEmptyOptionalWhenMemberNotFound() {
        final var uuid = UUID.randomUUID();
        given(memberRepository.findById(uuid)).willReturn(Optional.empty());

        final var result = memberService.getMemberById(uuid);
        assertThat(result).isNotPresent();
    }

    @Test
    void shouldTopUpMemberBalance() {
        final var uuid = UUID.randomUUID();
        final var member = new Member(uuid, "Jane Doe", "jane.doe@example.com", "hashed-password",
                new BigDecimal("10.00"), LocalDate.of(2000, 1, 1), LocalDate.of(2000, 6, 1));

        given(memberRepository.findByIdForUpdate(uuid)).willReturn(Optional.of(member));
        given(memberRepository.save(any(Member.class))).willAnswer(invocation -> invocation.getArgument(0));

        final var result = memberService.topUp(uuid, new BigDecimal("25.00"));

        assertThat(result.getAccountBalance()).isEqualByComparingTo(new BigDecimal("35.00"));
        verify(accountTransactionService).recordTopUp(result, new BigDecimal("25.00"));
    }

    @Test
    void shouldThrowMemberNotFoundExceptionWhenTopUpForNonExistentMember() {
        final var uuid = UUID.randomUUID();
        given(memberRepository.findByIdForUpdate(uuid)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.topUp(uuid, new BigDecimal("25.00")))
                .isInstanceOf(MemberNotFoundException.class).hasMessageContaining(uuid.toString());

        verify(memberRepository, never()).save(any());
        verify(accountTransactionService, never()).recordTopUp(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = { "0.00", "-5.00" })
    void shouldThrowIllegalArgumentExceptionForNonPositiveAmount(final String amount) {
        final var uuid = UUID.randomUUID();

        assertThatThrownBy(() -> memberService.topUp(uuid, new BigDecimal(amount)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(memberRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void shouldReturnTransactionHistoryForMemberWithTransactions() {
        final var uuid = UUID.randomUUID();
        final var member = new Member(uuid, "Jane Doe", "jane.doe@example.com", "hashed-password", BigDecimal.ZERO,
                LocalDate.of(2000, 1, 1), LocalDate.of(2000, 6, 1));
        final List<AccountTransaction> expected = List.of(mock(AccountTransaction.class));

        given(memberRepository.findById(uuid)).willReturn(Optional.of(member));
        given(accountTransactionService.getTransactionHistory(member)).willReturn(expected);

        final var result = memberService.getTransactionHistory(uuid);
        assertThat(result).isSameAs(expected);
    }

    @Test
    void shouldReturnEmptyTransactionHistoryForMemberWithNoTransactions() {
        final var uuid = UUID.randomUUID();
        final var member = new Member(uuid, "Jane Doe", "jane.doe@example.com", "hashed-password", BigDecimal.ZERO,
                LocalDate.of(2000, 1, 1), LocalDate.of(2000, 6, 1));
        final List<AccountTransaction> expected = List.of();

        given(memberRepository.findById(uuid)).willReturn(Optional.of(member));
        given(accountTransactionService.getTransactionHistory(member)).willReturn(expected);

        final var result = memberService.getTransactionHistory(uuid);
        assertThat(result).isSameAs(expected);
    }

    @Test
    void shouldThrowMemberNotFoundExceptionWhenGettingTransactionHistoryForNonExistentMember() {
        final var uuid = UUID.randomUUID();
        given(memberRepository.findById(uuid)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getTransactionHistory(uuid))
                .isInstanceOf(MemberNotFoundException.class).hasMessageContaining(uuid.toString());

        verify(accountTransactionService, never()).getTransactionHistory(any());
    }

    @Test
    void shouldDebitBalanceAndRecordPaymentWhenChargingForBooking() {
        final var member = newMember(new BigDecimal("20.00"));
        final var bookingId = UUID.randomUUID();
        final var payment = new AccountTransaction(member, new BigDecimal("-12.00"), TransactionType.BOOKING_PAYMENT,
                bookingId);

        given(memberRepository.findByIdForUpdate(member.getId())).willReturn(Optional.of(member));
        given(accountTransactionService.findByReference(member, TransactionType.BOOKING_PAYMENT, bookingId))
                .willReturn(Optional.empty());
        given(accountTransactionService.recordBookingPayment(member, new BigDecimal("12.00"), bookingId))
                .willReturn(payment);

        final var result = memberService.chargeForBooking(member.getId(), bookingId, new BigDecimal("12.00"));

        assertThat(result).isSameAs(payment);
        assertThat(member.getAccountBalance()).isEqualByComparingTo(new BigDecimal("8.00"));
        verify(memberRepository).save(member);
    }

    @Test
    void shouldChargeWhenBalanceExactlyCoversAmount() {
        final var member = newMember(new BigDecimal("12.00"));
        final var bookingId = UUID.randomUUID();

        given(memberRepository.findByIdForUpdate(member.getId())).willReturn(Optional.of(member));
        given(accountTransactionService.findByReference(member, TransactionType.BOOKING_PAYMENT, bookingId))
                .willReturn(Optional.empty());

        memberService.chargeForBooking(member.getId(), bookingId, new BigDecimal("12.00"));

        assertThat(member.getAccountBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldReturnExistingPaymentWithoutDebitingWhenChargeIsReplayed() {
        final var member = newMember(new BigDecimal("8.00"));
        final var bookingId = UUID.randomUUID();
        final var payment = new AccountTransaction(member, new BigDecimal("-12.00"), TransactionType.BOOKING_PAYMENT,
                bookingId);

        given(memberRepository.findByIdForUpdate(member.getId())).willReturn(Optional.of(member));
        given(accountTransactionService.findByReference(member, TransactionType.BOOKING_PAYMENT, bookingId))
                .willReturn(Optional.of(payment));

        final var result = memberService.chargeForBooking(member.getId(), bookingId, new BigDecimal("12.00"));

        assertThat(result).isSameAs(payment);
        assertThat(member.getAccountBalance()).isEqualByComparingTo(new BigDecimal("8.00"));
        verify(memberRepository, never()).save(any());
        verify(accountTransactionService, never()).recordBookingPayment(any(), any(), any());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenReplayedChargeHasDifferentAmount() {
        final var member = newMember(new BigDecimal("8.00"));
        final var bookingId = UUID.randomUUID();
        final var payment = new AccountTransaction(member, new BigDecimal("-12.00"), TransactionType.BOOKING_PAYMENT,
                bookingId);

        given(memberRepository.findByIdForUpdate(member.getId())).willReturn(Optional.of(member));
        given(accountTransactionService.findByReference(member, TransactionType.BOOKING_PAYMENT, bookingId))
                .willReturn(Optional.of(payment));

        assertThatThrownBy(() -> memberService.chargeForBooking(member.getId(), bookingId, new BigDecimal("15.00")))
                .isInstanceOf(IllegalArgumentException.class);

        verify(memberRepository, never()).save(any());
    }

    @Test
    void shouldThrowInsufficientBalanceExceptionWhenBalanceTooLow() {
        final var member = newMember(new BigDecimal("11.99"));
        final var bookingId = UUID.randomUUID();

        given(memberRepository.findByIdForUpdate(member.getId())).willReturn(Optional.of(member));
        given(accountTransactionService.findByReference(member, TransactionType.BOOKING_PAYMENT, bookingId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.chargeForBooking(member.getId(), bookingId, new BigDecimal("12.00")))
                .isInstanceOf(InsufficientBalanceException.class);

        assertThat(member.getAccountBalance()).isEqualByComparingTo(new BigDecimal("11.99"));
        verify(memberRepository, never()).save(any());
        verify(accountTransactionService, never()).recordBookingPayment(any(), any(), any());
    }

    @Test
    void shouldThrowMemberNotFoundExceptionWhenChargingNonExistentMember() {
        final var uuid = UUID.randomUUID();
        given(memberRepository.findByIdForUpdate(uuid)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.chargeForBooking(uuid, UUID.randomUUID(), new BigDecimal("12.00")))
                .isInstanceOf(MemberNotFoundException.class);

        verifyNoInteractions(accountTransactionService);
    }

    @ParameterizedTest
    @ValueSource(strings = { "0.00", "-5.00" })
    void shouldThrowIllegalArgumentExceptionForNonPositiveCharge(final String amount) {
        assertThatThrownBy(
                () -> memberService.chargeForBooking(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal(amount)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(memberRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void shouldCreditChargedAmountWhenRefundingBooking() {
        final var member = newMember(new BigDecimal("8.00"));
        final var bookingId = UUID.randomUUID();
        final var payment = new AccountTransaction(member, new BigDecimal("-12.00"), TransactionType.BOOKING_PAYMENT,
                bookingId);
        final var refund = new AccountTransaction(member, new BigDecimal("12.00"), TransactionType.REFUND, bookingId);

        given(memberRepository.findByIdForUpdate(member.getId())).willReturn(Optional.of(member));
        given(accountTransactionService.findByReference(member, TransactionType.BOOKING_PAYMENT, bookingId))
                .willReturn(Optional.of(payment));
        given(accountTransactionService.findByReference(member, TransactionType.REFUND, bookingId))
                .willReturn(Optional.empty());
        given(accountTransactionService.recordRefund(member, new BigDecimal("12.00"), bookingId)).willReturn(refund);

        final var result = memberService.refundBooking(member.getId(), bookingId);

        assertThat(result).containsSame(refund);
        assertThat(member.getAccountBalance()).isEqualByComparingTo(new BigDecimal("20.00"));
        verify(memberRepository).save(member);
    }

    @Test
    void shouldReturnExistingRefundWithoutCreditingWhenRefundIsReplayed() {
        final var member = newMember(new BigDecimal("20.00"));
        final var bookingId = UUID.randomUUID();
        final var payment = new AccountTransaction(member, new BigDecimal("-12.00"), TransactionType.BOOKING_PAYMENT,
                bookingId);
        final var refund = new AccountTransaction(member, new BigDecimal("12.00"), TransactionType.REFUND, bookingId);

        given(memberRepository.findByIdForUpdate(member.getId())).willReturn(Optional.of(member));
        given(accountTransactionService.findByReference(member, TransactionType.BOOKING_PAYMENT, bookingId))
                .willReturn(Optional.of(payment));
        given(accountTransactionService.findByReference(member, TransactionType.REFUND, bookingId))
                .willReturn(Optional.of(refund));

        final var result = memberService.refundBooking(member.getId(), bookingId);

        assertThat(result).containsSame(refund);
        assertThat(member.getAccountBalance()).isEqualByComparingTo(new BigDecimal("20.00"));
        verify(memberRepository, never()).save(any());
        verify(accountTransactionService, never()).recordRefund(any(), any(), any());
    }

    @Test
    void shouldReturnEmptyWhenRefundingBookingThatWasNeverCharged() {
        final var member = newMember(new BigDecimal("20.00"));
        final var bookingId = UUID.randomUUID();

        given(memberRepository.findByIdForUpdate(member.getId())).willReturn(Optional.of(member));
        given(accountTransactionService.findByReference(member, TransactionType.BOOKING_PAYMENT, bookingId))
                .willReturn(Optional.empty());

        final var result = memberService.refundBooking(member.getId(), bookingId);

        assertThat(result).isEmpty();
        assertThat(member.getAccountBalance()).isEqualByComparingTo(new BigDecimal("20.00"));
        verify(memberRepository, never()).save(any());
        verify(accountTransactionService, never()).recordRefund(any(), any(), any());
    }

    @Test
    void shouldThrowMemberNotFoundExceptionWhenRefundingNonExistentMember() {
        final var uuid = UUID.randomUUID();
        given(memberRepository.findByIdForUpdate(uuid)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.refundBooking(uuid, UUID.randomUUID()))
                .isInstanceOf(MemberNotFoundException.class);

        verifyNoInteractions(accountTransactionService);
    }

    private Member newMember(final BigDecimal balance) {
        return new Member(UUID.randomUUID(), "Jane Doe", "jane.doe@example.com", "hashed-password", balance,
                LocalDate.of(2000, 1, 1), LocalDate.of(2000, 6, 1));
    }
}
