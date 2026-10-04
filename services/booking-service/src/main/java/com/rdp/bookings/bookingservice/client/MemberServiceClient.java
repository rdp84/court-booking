package com.rdp.bookings.bookingservice.client;

import java.util.Optional;
import java.util.UUID;

public interface MemberServiceClient {
    Optional<MemberResponse> getMember(UUID memberId);
}
