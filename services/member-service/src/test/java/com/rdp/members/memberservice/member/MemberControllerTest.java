package com.rdp.members.memberservice.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(MemberController.class)
class MemberControllerTest {

    @Autowired
    MockMvcTester mockMvc;

    @MockitoBean
    MemberService memberService;

    @Test
    void shouldReturnMemberWhenFound() {
        final var id = UUID.randomUUID();
        final var member = new Member(id, "Ada Lovelace", "ada@example.com", "hashed-password",
                new BigDecimal("35.00"), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        given(memberService.getMemberById(id)).willReturn(Optional.of(member));

        assertThat(mockMvc.get().uri("/members/{id}", id)).hasStatusOk().bodyJson()
                .isLenientlyEqualTo("""
                        {
                            "id": "%s",
                            "name": "Ada Lovelace",
                            "email": "ada@example.com",
                            "accountBalance": 35.00,
                            "membershipStartDate": "2026-01-01",
                            "membershipEndDate": "2026-12-31"
                        }
                        """.formatted(id));
    }

    @Test
    void shouldReturnNotFoundWhenMemberDoesNotExist() {
        final var id = UUID.randomUUID();
        given(memberService.getMemberById(id)).willReturn(Optional.empty());

        assertThat(mockMvc.get().uri("/members/{id}", id)).hasStatus4xxClientError().hasStatus(404);
    }

    @Test
    void shouldReturnBalanceWhenMemberFound() {
        final var id = UUID.randomUUID();
        final var member = new Member(id, "Ada Lovelace", "ada@example.com", "hashed-password",
                new BigDecimal("35.00"), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        given(memberService.getMemberById(id)).willReturn(Optional.of(member));

        assertThat(mockMvc.get().uri("/members/{id}/balance", id)).hasStatusOk().bodyJson()
                .isLenientlyEqualTo("""
                        {
                            "accountBalance": 35.00
                        }
                        """);
    }

    @Test
    void shouldReturnNotFoundForBalanceWhenMemberDoesNotExist() {
        final var id = UUID.randomUUID();
        given(memberService.getMemberById(id)).willReturn(Optional.empty());

        assertThat(mockMvc.get().uri("/members/{id}/balance", id)).hasStatus4xxClientError().hasStatus(404);
    }
}
