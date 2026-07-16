package com.rdp.members.memberservice.transaction;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface AccountTransactionRepository extends JpaRepository<AccountTransaction, UUID> {
}
