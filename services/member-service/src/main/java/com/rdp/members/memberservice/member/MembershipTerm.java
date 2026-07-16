package com.rdp.members.memberservice.member;

import java.time.LocalDate;
import java.time.Period;

enum MembershipTerm {
    QUARTERLY(Period.ofMonths(3)),
    HALF_YEARLY(Period.ofMonths(6)),
    ANNUAL(Period.ofYears(1));

    private final Period duration;

    MembershipTerm(Period duration) {
        this.duration = duration;
    }

    LocalDate calculateEndDate(LocalDate startDate) {
        return startDate.plus(duration);
    }
}
