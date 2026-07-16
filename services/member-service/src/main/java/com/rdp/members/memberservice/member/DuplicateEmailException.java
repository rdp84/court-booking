package com.rdp.members.memberservice.member;

class DuplicateEmailException extends RuntimeException {
    DuplicateEmailException(String email) {
        super("Email already registered: " + email);
    }
}
