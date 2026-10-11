package com.rdp.members.memberservice.member;

import java.math.BigDecimal;
import java.util.UUID;

class InsufficientBalanceException extends RuntimeException {
    InsufficientBalanceException(final UUID memberId, final BigDecimal balance, final BigDecimal amount) {
        super("Member " + memberId + " has balance " + balance + ", which is less than " + amount);
    }
}
