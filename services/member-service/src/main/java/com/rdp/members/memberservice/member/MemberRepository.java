package com.rdp.members.memberservice.member;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

interface MemberRepository extends JpaRepository<Member, UUID> {
    boolean existsByEmail(String email);

    // SELECT ... FOR UPDATE, so concurrent balance changes for the same member queue up instead of overwriting
    // each other's read-modify-write
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id = :id")
    Optional<Member> findByIdForUpdate(UUID id);
}
