package com.rdp.members.memberservice.member;

record RegisterMemberRequest(String name, String email, String password, MembershipTerm term) {
}
