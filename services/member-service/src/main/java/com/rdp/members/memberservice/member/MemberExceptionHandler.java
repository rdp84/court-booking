package com.rdp.members.memberservice.member;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class MemberExceptionHandler {

    @ExceptionHandler(MemberNotFoundException.class)
    ResponseEntity<Void> handleMemberNotFound() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Void> handleIllegalArgument() {
        return ResponseEntity.badRequest().build();
    }
}
