package com.rdp.members.memberservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;

@Provider("member-service")
@PactFolder("../pacts")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MemberServicePactVerificationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15.2");

    @Autowired
    JdbcTemplate jdbcTemplate;

    @LocalServerPort
    int port;

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(final PactVerificationContext context) {
        context.verifyInteraction();
    }

    @BeforeEach
    void setUp(final PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
        jdbcTemplate.execute("DELETE FROM members");
    }

    @State("a member exists with id f9c7a4d2-0000-0000-0000-000000000001")
    void memberExists() {
        jdbcTemplate.execute(
                """
                        INSERT INTO members (id, name, email, password_hash, account_balance, membership_start_date, membership_end_date, created_at)
                        VALUES ('f9c7a4d2-0000-0000-0000-000000000001', 'Alice Johnson', 'alice.johnson@example.com', 'test_hash', 20.00, '2000-01-01', '2001-01-01', NOW())
                        """);
    }

    @State("no member exists with id f9c7a4d2-0000-0000-0000-000000000002")
    void memberDoesNotExist() {

    }
}
