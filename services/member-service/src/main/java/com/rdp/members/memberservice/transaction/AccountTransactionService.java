package com.rdp.members.memberservice.transaction;

import java.math.BigDecimal;
import java.util.List;

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

    public List<AccountTransaction> getTransactionHistory(final Member member) {
        return accountTransactionRepository.findByMemberOrderByCreatedAtDesc(member);
    }
}
