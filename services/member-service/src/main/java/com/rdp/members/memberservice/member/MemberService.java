package com.rdp.members.memberservice.member;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rdp.members.memberservice.transaction.AccountTransaction;
import com.rdp.members.memberservice.transaction.AccountTransactionService;
import com.rdp.members.memberservice.transaction.TransactionType;

@Service
class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountTransactionService accountTransactionService;
    private final Clock clock;

    MemberService(final MemberRepository memberRepository, final PasswordEncoder passwordEncoder,
            final AccountTransactionService accountTransactionService, final Clock clock) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.accountTransactionService = accountTransactionService;
        this.clock = clock;
    }

    @Transactional
    Member registerMember(final String name, final String email, final String rawPassword,
            final MembershipTerm membershipTerm) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name must not be blank");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email must not be blank");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password must not be blank");
        }
        if (membershipTerm == null) {
            throw new IllegalArgumentException("Membership term must not be null");
        }

        if (memberRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }

        final var startDate = LocalDate.now(clock);
        final var member = new Member(name, email, passwordEncoder.encode(rawPassword), BigDecimal.ZERO, startDate,
                membershipTerm.calculateEndDate(startDate));
        return memberRepository.save(member);
    }

    Optional<Member> getMemberById(final UUID id) {
        return memberRepository.findById(id);
    }

    @Transactional
    Member topUp(final UUID memberId, final BigDecimal amount) {
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Top-up amount must be positive: " + amount);
        }

        final var member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));
        member.creditBalance(amount);
        final var saved = memberRepository.save(member);
        accountTransactionService.recordTopUp(saved, amount);
        return saved;
    }

    // Idempotent on bookingId: the Booking Service may retry a charge whose response it never saw
    @Transactional
    AccountTransaction chargeForBooking(final UUID memberId, final UUID bookingId, final BigDecimal amount) {
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Charge amount must be positive: " + amount);
        }

        final var member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));
        final var existing = accountTransactionService.findByReference(member, TransactionType.BOOKING_PAYMENT,
                bookingId);
        if (existing.isPresent()) {
            if (existing.get().getAmount().negate().compareTo(amount) != 0) {
                throw new IllegalArgumentException("Booking " + bookingId + " was already charged "
                        + existing.get().getAmount().negate() + ", not " + amount);
            }
            return existing.get();
        }

        member.debitBalance(amount);
        memberRepository.save(member);
        return accountTransactionService.recordBookingPayment(member, amount, bookingId);
    }

    // Refunds whatever was charged for bookingId, at most once. Empty if nothing was charged, e.g. the Booking
    // Service is undoing a charge that never landed, or the booking predates charging.
    @Transactional
    Optional<AccountTransaction> refundBooking(final UUID memberId, final UUID bookingId) {
        final var member = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));
        final var payment = accountTransactionService.findByReference(member, TransactionType.BOOKING_PAYMENT,
                bookingId);
        if (payment.isEmpty()) {
            return Optional.empty();
        }
        final var existingRefund = accountTransactionService.findByReference(member, TransactionType.REFUND,
                bookingId);
        if (existingRefund.isPresent()) {
            return existingRefund;
        }

        final var amount = payment.get().getAmount().negate();
        member.creditBalance(amount);
        memberRepository.save(member);
        return Optional.of(accountTransactionService.recordRefund(member, amount, bookingId));
    }

    List<AccountTransaction> getTransactionHistory(final UUID memberId) {
        final var member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));
        return accountTransactionService.getTransactionHistory(member);
    }
}
