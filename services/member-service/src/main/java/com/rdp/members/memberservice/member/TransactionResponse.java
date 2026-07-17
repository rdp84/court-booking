package com.rdp.members.memberservice.member;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.rdp.members.memberservice.transaction.TransactionType;

record TransactionResponse(UUID id, BigDecimal amount, TransactionType transactionType, UUID referenceId,
        LocalDateTime createdAt) {
}
