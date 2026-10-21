package com.agenciahub.api.application.services.referrals;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.UUID;

/** Participantes e contatos continuam sujeitos à revisão de privacidade após excluir a indicação. */
@Service
@RequiredArgsConstructor
public class ReferralHistory {
    private final JdbcTemplate jdbc;

    public boolean existsForCustomer(UUID agencyId, UUID customerId) {
        String id=customerId.toString();
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM referrals WHERE agency_id=? AND (referrer_customer_id=? OR referred_customer_id=?))
                OR EXISTS(SELECT 1 FROM referral_events WHERE agency_id=? AND
                    (detail->>'participantReferrerId'=? OR detail->>'participantReferredId'=? OR detail->>'customerId'=?
                     OR detail->>'referrerId'=? OR detail->>'referredCustomerId'=?))
                """, Boolean.class,agencyId,customerId,customerId,agencyId,id,id,id,id,id));
    }

    public boolean existsForEmail(UUID agencyId, String email) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM referrals WHERE agency_id=? AND lower(referred_email)=lower(?))
                OR EXISTS(SELECT 1 FROM referral_events WHERE agency_id=? AND
                    (lower(detail->>'participantEmail')=lower(?) OR lower(detail->>'previousEmail')=lower(?)))
                """,Boolean.class,agencyId,email,agencyId,email,email));
    }
}
