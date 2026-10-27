package com.agenciahub.api.application.services.stages;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.UUID;

/** A auditoria continua pessoal mesmo quando a etapa ou a viagem já foi excluída. */
@Service
@RequiredArgsConstructor
public class TripStageHistory {
    private final JdbcTemplate jdbc;

    public boolean existsForCustomer(UUID agencyId, UUID customerId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM trip_stages s JOIN trips t ON t.id=s.trip_id AND t.agency_id=s.agency_id
                    WHERE s.agency_id=? AND t.customer_id=?)
                OR EXISTS(SELECT 1 FROM trip_stage_events e LEFT JOIN trips t ON t.id=e.trip_id AND t.agency_id=e.agency_id
                    WHERE e.agency_id=? AND (t.customer_id=? OR e.detail->>'customerId'=?))
                """, Boolean.class, agencyId, customerId, agencyId, customerId, customerId.toString()));
    }
}
