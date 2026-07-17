package com.rdp.members.memberservice.member;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rdp.members.memberservice.transaction.AccountTransaction;

@RestController
@RequestMapping("/members")
class MemberController {
    private final MemberService memberService;

    MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/{id}")
    ResponseEntity<MemberResponse> getMember(@PathVariable UUID id) {
        return memberService.getMemberById(id).map(this::toMemberResponse).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/balance")
    ResponseEntity<BalanceResponse> getBalance(@PathVariable UUID id) {
        return memberService.getMemberById(id).map(member -> new BalanceResponse(member.getAccountBalance()))
                .map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/topup")
    MemberResponse topUp(@PathVariable UUID id, @RequestBody TopUpRequest request) {
        return toMemberResponse(memberService.topUp(id, request.amount()));
    }

    @GetMapping("/{id}/transactions")
    List<TransactionResponse> getTransactions(@PathVariable UUID id) {
        return memberService.getTransactionHistory(id).stream().map(this::toTransactionResponse).toList();
    }

    private MemberResponse toMemberResponse(Member member) {
        return new MemberResponse(member.getId(), member.getName(), member.getEmail(), member.getAccountBalance(),
                member.getMembershipStartDate(), member.getMembershipEndDate(), member.getCreatedAt());
    }

    private TransactionResponse toTransactionResponse(AccountTransaction transaction) {
        return new TransactionResponse(transaction.getId(), transaction.getAmount(), transaction.getTransactionType(),
                transaction.getReferenceId(), transaction.getCreatedAt());
    }
}
