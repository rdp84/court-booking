package com.rdp.members.memberservice.transaction;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rdp.members.memberservice.member.Member;

interface AccountTransactionRepository extends JpaRepository<AccountTransaction, UUID> {
    List<AccountTransaction> findByMemberOrderByCreatedAtDesc(Member member);
}
