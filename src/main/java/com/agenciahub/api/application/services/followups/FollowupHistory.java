package com.agenciahub.api.application.services.followups;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FollowupHistory {
    private final JdbcTemplate jdbc;
    public boolean existsForCustomer(UUID agency, UUID customer) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM commercial_followups WHERE agency_id=? AND customer_id=?)
                OR EXISTS(SELECT 1 FROM commercial_followup_events WHERE agency_id=? AND customer_id=?)
                """,Boolean.class,agency,customer,agency,customer));
    }
}
