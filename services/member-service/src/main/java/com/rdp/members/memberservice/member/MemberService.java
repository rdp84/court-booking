package com.rdp.members.memberservice.member;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rdp.members.memberservice.transaction.AccountTransaction;
import com.rdp.members.memberservice.transaction.AccountTransactionService;

@Service
class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountTransactionService accountTransactionService;

    MemberService(final MemberRepository memberRepository, final PasswordEncoder passwordEncoder,
            final AccountTransactionService accountTransactionService) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.accountTransactionService = accountTransactionService;
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

        final var startDate = LocalDate.now();
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

        final var member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));
        member.creditBalance(amount);
        final var saved = memberRepository.save(member);
        accountTransactionService.recordTopUp(saved, amount);
        return saved;
    }

    List<AccountTransaction> getTransactionHistory(final UUID memberId) {
        final var member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));
        return accountTransactionService.getTransactionHistory(member);
    }
}
