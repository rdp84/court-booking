package com.rdp.members.memberservice.member;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rdp.members.memberservice.transaction.AccountTransactionService;

@Service
class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountTransactionService accountTransactionService;

    MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder,
            AccountTransactionService accountTransactionService) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.accountTransactionService = accountTransactionService;
    }

    @Transactional
    Member registerMember(String name, String email, String rawPassword, MembershipTerm membershipTerm) {
        if (memberRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }

        final var startDate = LocalDate.now();
        final var member = new Member(name, email, passwordEncoder.encode(rawPassword), BigDecimal.ZERO, startDate,
                membershipTerm.calculateEndDate(startDate));
        return memberRepository.save(member);
    }

    Optional<Member> getMemberById(UUID id) {
        return memberRepository.findById(id);
    }

    @Transactional
    Member topUp(UUID memberId, BigDecimal amount) {
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
}
