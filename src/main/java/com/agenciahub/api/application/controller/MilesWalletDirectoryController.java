package com.agenciahub.api.application.controller;

import com.agenciahub.api.exception.ApiError;
import com.agenciahub.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Directory of mileage relationships; customer identity remains in the shared CRM. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/miles-wallets")
@PreAuthorize("hasAnyRole('AGENCY_OWNER','SALES_AGENT')")
public class MilesWalletDirectoryController {
    private final JdbcTemplate jdbc;

    @GetMapping
    public List<Entry> list() {
        return jdbc.query("""
            SELECT c.id,c.name,
                (p.customer_id IS NOT NULL AND NOT p.archived) AS profile_active,
                COALESCE(i.active_items,0) AS active_items,
                COALESCE(i.accounts,0) AS accounts,
                COALESCE(i.cards,0) AS cards,
                COALESCE(i.clubs,0) AS clubs,
                i.next_expiry
            FROM customers c
            LEFT JOIN customer_miles_profiles p ON p.customer_id=c.id AND p.agency_id=c.agency_id
            LEFT JOIN LATERAL (
                SELECT count(*) AS total_items,
                    count(*) FILTER (WHERE NOT x.archived) AS active_items,
                    count(*) FILTER (WHERE x.kind='ACCOUNT' AND NOT x.archived) AS accounts,
                    count(*) FILTER (WHERE x.kind='CARD' AND NOT x.archived) AS cards,
                    count(*) FILTER (WHERE x.kind='CLUB' AND NOT x.archived) AS clubs,
                    min((x.data->>'expiresOn')::date) FILTER (
                        WHERE x.kind='EXPIRY' AND NOT x.archived
                        AND (x.data->>'expiresOn')::date >= (now() AT TIME ZONE 'America/Fortaleza')::date
                        AND EXISTS (SELECT 1 FROM customer_miles_items a
                            WHERE a.id::text=x.data->>'accountId' AND a.customer_id=c.id
                            AND a.agency_id=c.agency_id AND a.kind='ACCOUNT' AND NOT a.archived)
                    ) AS next_expiry
                FROM customer_miles_items x WHERE x.customer_id=c.id AND x.agency_id=c.agency_id
            ) i ON true
            WHERE c.agency_id=? AND (p.customer_id IS NOT NULL OR i.total_items>0)
            ORDER BY lower(c.name),c.id
            """, (r,n)->new Entry(r.getObject("id",UUID.class),r.getString("name"),
                !r.getBoolean("profile_active")&&r.getLong("active_items")==0,
                r.getLong("accounts"),r.getLong("cards"),r.getLong("clubs"),r.getObject("next_expiry",LocalDate.class)),TenantContext.requireAgencyId());
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError forbidden() {
        return new ApiError("Seu perfil não tem acesso à área de milhas.","MILES_ACCESS_DENIED");
    }
    public record Entry(UUID customerId,String name,boolean archived,long accounts,long cards,long clubs,LocalDate nextExpiry) {}
}
