package com.rdp.members.memberservice.member;

import java.util.UUID;

class MemberNotFoundException extends RuntimeException {
    MemberNotFoundException(final UUID memberId) {
        super("Member not found: " + memberId);
    }
}
