package com.rdp.members.memberservice.transaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.rdp.members.memberservice.member.Member;

@Service
public class AccountTransactionService {
    private final AccountTransactionRepository accountTransactionRepository;

    AccountTransactionService(final AccountTransactionRepository accountTransactionRepository) {
        this.accountTransactionRepository = accountTransactionRepository;
    }

    public void recordTopUp(final Member member, final BigDecimal amount) {
        accountTransactionRepository.save(new AccountTransaction(member, amount, TransactionType.TOP_UP));
    }

    // Stored as a debit, so the amount is negated
    public AccountTransaction recordBookingPayment(final Member member, final BigDecimal amount,
            final UUID bookingId) {
        return accountTransactionRepository.save(
                new AccountTransaction(member, amount.negate(), TransactionType.BOOKING_PAYMENT, bookingId));
    }

    public AccountTransaction recordRefund(final Member member, final BigDecimal amount, final UUID bookingId) {
        return accountTransactionRepository
                .save(new AccountTransaction(member, amount, TransactionType.REFUND, bookingId));
    }

    public Optional<AccountTransaction> findByReference(final Member member, final TransactionType transactionType,
            final UUID referenceId) {
        return accountTransactionRepository.findByMemberAndTransactionTypeAndReferenceId(member, transactionType,
                referenceId);
    }

    public List<AccountTransaction> getTransactionHistory(final Member member) {
        return accountTransactionRepository.findByMemberOrderByCreatedAtDesc(member);
    }
}
