package com.rdp.members.memberservice.member;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

record MemberResponse(UUID id, String name, String email, BigDecimal accountBalance, LocalDate membershipStartDate,
        LocalDate membershipEndDate, LocalDateTime createdAt) {
}
