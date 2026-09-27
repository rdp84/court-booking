package com.rdp.bookings.bookingservice.client;

import java.util.Optional;
import java.util.UUID;

interface MemberServiceClient {
    Optional<MemberResponse> getMember(UUID memberId);
}
