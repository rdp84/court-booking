package com.rdp.members.memberservice.member;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    private MemberResponse toMemberResponse(Member member) {
        return new MemberResponse(member.getId(), member.getName(), member.getEmail(), member.getAccountBalance(),
                member.getMembershipStartDate(), member.getMembershipEndDate(), member.getCreatedAt());
    }
}
