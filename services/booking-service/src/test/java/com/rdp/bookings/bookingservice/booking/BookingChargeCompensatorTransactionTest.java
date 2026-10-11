package com.rdp.bookings.bookingservice.booking;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.rdp.bookings.bookingservice.client.MemberServiceClient;

// Checks the listener is wired to the transaction outcome, which the unit test can't: the refund must follow a
// rollback and must not follow a commit
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import(BookingChargeCompensator.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class BookingChargeCompensatorTransactionTest {
    private static final UUID BOOKING_ID = UUID.randomUUID();
    private static final UUID MEMBER_ID = UUID.randomUUID();

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15.2");

    @MockitoBean
    MemberServiceClient memberServiceClient;

    @Autowired
    ApplicationEventPublisher eventPublisher;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Test
    void shouldRefundWhenTransactionRollsBack() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            eventPublisher.publishEvent(new BookingChargeAttempted(BOOKING_ID, MEMBER_ID));
            status.setRollbackOnly();
        });

        verify(memberServiceClient).refundBooking(MEMBER_ID, BOOKING_ID);
    }

    @Test
    void shouldNotRefundWhenTransactionCommits() {
        new TransactionTemplate(transactionManager).executeWithoutResult(
                status -> eventPublisher.publishEvent(new BookingChargeAttempted(BOOKING_ID, MEMBER_ID)));

        verify(memberServiceClient, never()).refundBooking(any(), any());
    }
}
