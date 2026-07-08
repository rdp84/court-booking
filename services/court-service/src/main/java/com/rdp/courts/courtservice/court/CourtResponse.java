package com.rdp.courts.courtservice.court;

import java.util.UUID;

record CourtResponse(UUID id, String name, boolean isActive) {
}
