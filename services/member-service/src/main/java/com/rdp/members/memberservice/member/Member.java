package com.rdp.members.memberservice.member;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String name;
    private String email;
    private String passwordHash;
    private BigDecimal accountBalance;
    private LocalDate membershipStartDate;
    private LocalDate membershipEndDate;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public Member(final String name, final String email, final String passwordHash,
            final BigDecimal accountBalance, final LocalDate membershipStartDate,
            final LocalDate membershipEndDate) {
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.accountBalance = accountBalance;
        this.membershipStartDate = membershipStartDate;
        this.membershipEndDate = membershipEndDate;
    }

    Member(final UUID id, final String name, final String email, final String passwordHash,
            final BigDecimal accountBalance, final LocalDate membershipStartDate,
            final LocalDate membershipEndDate) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.accountBalance = accountBalance;
        this.membershipStartDate = membershipStartDate;
        this.membershipEndDate = membershipEndDate;
    }

    // Required by JPA
    Member() {
    }

    UUID getId() {
        return id;
    }

    String getName() {
        return name;
    }

    String getEmail() {
        return email;
    }

    String getPasswordHash() {
        return passwordHash;
    }

    BigDecimal getAccountBalance() {
        return accountBalance;
    }

    LocalDate getMembershipStartDate() {
        return membershipStartDate;
    }

    LocalDate getMembershipEndDate() {
        return membershipEndDate;
    }

    LocalDateTime getCreatedAt() {
        return createdAt;
    }

    void creditBalance(BigDecimal amount) {
        this.accountBalance = this.accountBalance.add(amount);
    }
}
