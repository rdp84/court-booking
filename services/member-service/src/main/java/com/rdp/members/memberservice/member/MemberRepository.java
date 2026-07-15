package com.rdp.members.memberservice.member;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface MemberRepository extends JpaRepository<Member, UUID> {
}
