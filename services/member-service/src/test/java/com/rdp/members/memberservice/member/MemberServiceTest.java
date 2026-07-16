package com.rdp.members.memberservice.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    MemberRepository memberRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    MemberService memberService;

    @ParameterizedTest
    @EnumSource(MembershipTerm.class)
    void shouldRegisterNewMember(MembershipTerm membershipTerm) {
        given(memberRepository.existsByEmail("jane.doe@example.com")).willReturn(false);
        given(passwordEncoder.encode("plaintext-password")).willReturn("hashed-password");
        given(memberRepository.save(any(Member.class))).willAnswer(invocation -> invocation.getArgument(0));

        final var result = memberService.registerMember("Jane Doe", "jane.doe@example.com", "plaintext-password",
                membershipTerm);

        final var today = LocalDate.now();
        assertThat(result.getName()).isEqualTo("Jane Doe");
        assertThat(result.getEmail()).isEqualTo("jane.doe@example.com");
        assertThat(result.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(result.getAccountBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getMembershipStartDate()).isEqualTo(today);
        assertThat(result.getMembershipEndDate()).isEqualTo(membershipTerm.calculateEndDate(today));
    }

    @Test
    void shouldThrowDuplicateEmailExceptionWhenEmailTaken() {
        given(memberRepository.existsByEmail("jane.doe@example.com")).willReturn(true);

        assertThatThrownBy(() -> memberService.registerMember("Jane Doe", "jane.doe@example.com",
                "plaintext-password", MembershipTerm.ANNUAL)).isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("jane.doe@example.com");

        verify(memberRepository, never()).save(any());
    }

    @Test
    void shouldReturnMemberWhenMemberExists() {
        final var uuid = UUID.randomUUID();
        final var member = new Member(uuid, "Jane Doe", "jane.doe@example.com", "hashed-password", BigDecimal.ZERO,
                LocalDate.of(2000, 1, 1), LocalDate.of(2000, 6, 1));

        given(memberRepository.findById(uuid)).willReturn(Optional.of(member));

        final var result = memberService.getMemberById(uuid);
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(uuid);
        assertThat(result.get().getName()).isEqualTo("Jane Doe");
    }

    @Test
    void shouldReturnEmptyOptionalWhenMemberNotFound() {
        final var uuid = UUID.randomUUID();
        given(memberRepository.findById(uuid)).willReturn(Optional.empty());

        final var result = memberService.getMemberById(uuid);
        assertThat(result).isNotPresent();
    }
}
