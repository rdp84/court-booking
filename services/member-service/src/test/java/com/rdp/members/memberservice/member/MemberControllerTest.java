package com.rdp.members.memberservice.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.rdp.members.memberservice.transaction.AccountTransaction;
import com.rdp.members.memberservice.transaction.TransactionType;

@WebMvcTest(MemberController.class)
class MemberControllerTest {

    @Autowired
    MockMvcTester mockMvc;

    @MockitoBean
    MemberService memberService;

    @Test
    void shouldRegisterMemberAndReturn201() {
        final var member = newMember();
        given(memberService.registerMember(member.getName(), member.getEmail(), "plaintext-password",
                MembershipTerm.ANNUAL)).willReturn(member);

        assertThat(mockMvc.post().uri("/members").contentType(MediaType.APPLICATION_JSON).content("""
                    {
                        "name": "Ada Lovelace",
                        "email": "ada@example.com",
                        "password": "plaintext-password",
                        "term": "ANNUAL"
                    }
                """)).hasStatus(201).bodyJson().isLenientlyEqualTo("""
                    {
                        "name": "Ada Lovelace",
                        "email": "ada@example.com"
                    }
                """);
    }

    @Test
    void shouldReturnBadRequestWhenRegisterMemberFieldsAreInvalid() {
        given(memberService.registerMember(null, "ada@example.com", "plaintext-password", MembershipTerm.ANNUAL))
                .willThrow(new IllegalArgumentException("Name must not be blank"));

        assertThat(mockMvc.post().uri("/members").contentType(MediaType.APPLICATION_JSON).content("""
                    {
                        "email": "ada@example.com",
                        "password": "plaintext-password",
                        "term": "ANNUAL"
                    }
                """)).hasStatus(400).bodyText().isEmpty();
    }

    @Test
    void shouldReturnBadRequestWhenMembershipTermIsInvalid() {
        assertThat(mockMvc.post().uri("/members").contentType(MediaType.APPLICATION_JSON).content("""
                    {
                        "name": "Ada Lovelace",
                        "email": "ada@example.com",
                        "password": "plaintext-password",
                        "term": "WEEKLY"
                    }
                """)).hasStatus(400);
    }

    @Test
    void shouldReturnConflictWhenRegisterMemberEmailIsTaken() {
        given(memberService.registerMember("Ada Lovelace", "ada@example.com", "plaintext-password",
                MembershipTerm.ANNUAL)).willThrow(new DuplicateEmailException("ada@example.com"));

        assertThat(mockMvc.post().uri("/members").contentType(MediaType.APPLICATION_JSON).content("""
                    {
                        "name": "Ada Lovelace",
                        "email": "ada@example.com",
                        "password": "plaintext-password",
                        "term": "ANNUAL"
                    }
                """)).hasStatus(409).bodyText().isEmpty();
    }

    @Test
    void shouldReturnMemberWhenFound() {
        final var member = newMember();
        given(memberService.getMemberById(member.getId())).willReturn(Optional.of(member));

        assertThat(mockMvc.get().uri("/members/{id}", member.getId())).hasStatusOk().bodyJson()
                .isLenientlyEqualTo("""
                        {
                            "id": "%s",
                            "name": "Ada Lovelace",
                            "email": "ada@example.com",
                            "accountBalance": 35.00,
                            "membershipStartDate": "2000-01-01",
                            "membershipEndDate": "2000-12-31"
                        }
                        """.formatted(member.getId()));
    }

    @Test
    void shouldReturnNotFoundWhenMemberDoesNotExist() {
        final var id = UUID.randomUUID();
        given(memberService.getMemberById(id)).willReturn(Optional.empty());

        assertThat(mockMvc.get().uri("/members/{id}", id)).hasStatus(404).bodyText().isEmpty();
    }

    @Test
    void shouldReturnBalanceWhenMemberFound() {
        final var member = newMember();
        given(memberService.getMemberById(member.getId())).willReturn(Optional.of(member));

        assertThat(mockMvc.get().uri("/members/{id}/balance", member.getId())).hasStatusOk().bodyJson()
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

        assertThat(mockMvc.get().uri("/members/{id}/balance", id)).hasStatus(404).bodyText().isEmpty();
    }

    @Test
    void shouldReturnUpdatedMemberWhenTopUpSucceeds() {
        final var member = newMember();
        given(memberService.topUp(member.getId(), new BigDecimal("10.00"))).willReturn(member);

        assertThat(mockMvc.post().uri("/members/{id}/topup", member.getId()).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "amount": 10.00
                        }
                        """)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                        {
                            "id": "%s",
                            "name": "Ada Lovelace",
                            "email": "ada@example.com",
                            "accountBalance": 35.00,
                            "membershipStartDate": "2000-01-01",
                            "membershipEndDate": "2000-12-31"
                        }
                        """.formatted(member.getId()));
    }

    @Test
    void shouldReturnBadRequestWhenTopUpAmountIsNotPositive() {
        final var id = UUID.randomUUID();
        given(memberService.topUp(id, new BigDecimal("-5.00")))
                .willThrow(new IllegalArgumentException("Top-up amount must be positive: -5.00"));

        assertThat(mockMvc.post().uri("/members/{id}/topup", id).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "amount": -5.00
                        }
                        """)).hasStatus(400).bodyText().isEmpty();
    }

    @Test
    void shouldReturnNotFoundForTopUpWhenMemberDoesNotExist() {
        final var id = UUID.randomUUID();
        given(memberService.topUp(id, new BigDecimal("10.00"))).willThrow(new MemberNotFoundException(id));

        assertThat(mockMvc.post().uri("/members/{id}/topup", id).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "amount": 10.00
                        }
                        """)).hasStatus(404).bodyText().isEmpty();
    }

    @Test
    void shouldReturnTransactionHistoryWhenMemberFound() {
        final var member = newMember();
        final var transaction = new AccountTransaction(member, new BigDecimal("10.00"), TransactionType.TOP_UP);
        given(memberService.getTransactionHistory(member.getId())).willReturn(List.of(transaction));

        assertThat(mockMvc.get().uri("/members/{id}/transactions", member.getId())).hasStatusOk().bodyJson()
                .isLenientlyEqualTo("""
                        [
                            {
                                "amount": 10.00,
                                "transactionType": "TOP_UP"
                            }
                        ]
                        """);
    }

    @Test
    void shouldReturnEmptyListWhenMemberHasNoTransactions() {
        final var id = UUID.randomUUID();
        given(memberService.getTransactionHistory(id)).willReturn(List.of());

        assertThat(mockMvc.get().uri("/members/{id}/transactions", id)).hasStatusOk().bodyJson()
                .isLenientlyEqualTo("[]");
    }

    @Test
    void shouldReturnNotFoundForTransactionsWhenMemberDoesNotExist() {
        final var id = UUID.randomUUID();
        given(memberService.getTransactionHistory(id)).willThrow(new MemberNotFoundException(id));

        assertThat(mockMvc.get().uri("/members/{id}/transactions", id)).hasStatus(404).bodyText().isEmpty();
    }

    private Member newMember() {
        final var id = UUID.randomUUID();
        return new Member(id, "Ada Lovelace", "ada@example.com", "hashed-password",
                new BigDecimal("35.00"), LocalDate.of(2000, 1, 1), LocalDate.of(2000, 12, 31));
    }
}
