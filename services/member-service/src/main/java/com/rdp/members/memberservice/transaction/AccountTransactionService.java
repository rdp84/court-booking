package com.rdp.members.memberservice.transaction;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.rdp.members.memberservice.member.Member;

@Service
public class AccountTransactionService {
    private final AccountTransactionRepository accountTransactionRepository;

    AccountTransactionService(AccountTransactionRepository accountTransactionRepository) {
        this.accountTransactionRepository = accountTransactionRepository;
    }

    public void recordTopUp(Member member, BigDecimal amount) {
        accountTransactionRepository.save(new AccountTransaction(member, amount, TransactionType.TOP_UP));
    }
}
