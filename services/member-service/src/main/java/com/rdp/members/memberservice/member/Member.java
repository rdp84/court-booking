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

    // Required by JPA
    Member() {
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public BigDecimal getAccountBalance() {
        return accountBalance;
    }

    public LocalDate getMembershipStartDate() {
        return membershipStartDate;
    }

    public LocalDate getMembershipEndDate() {
        return membershipEndDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
