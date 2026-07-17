package com.rdp.members.memberservice.transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.rdp.members.memberservice.member.Member;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "account_transactions")
public class AccountTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    // Only set once a transaction is linked to a booking
    // (BOOKING_PAYMENT/REFUND/OPPONENT_TRANSFER)
    private UUID referenceId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    AccountTransaction(Member member, BigDecimal amount, TransactionType transactionType) {
        this.member = member;
        this.amount = amount;
        this.transactionType = transactionType;
    }

    // Required by JPA
    AccountTransaction() {
    }

    UUID getId() {
        return id;
    }

    Member getMember() {
        return member;
    }

    BigDecimal getAmount() {
        return amount;
    }

    TransactionType getTransactionType() {
        return transactionType;
    }

    UUID getReferenceId() {
        return referenceId;
    }

    LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
