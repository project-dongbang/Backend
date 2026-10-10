package com.dongbang.organization.application.port;

import java.time.Instant;

public interface FutureEventRegistrationCleanupPort {
    void cancelFutureRegistrations(Long membershipId, Long organizationId, Instant leftAt);
}
