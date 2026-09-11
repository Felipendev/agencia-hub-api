package com.agenciahub.api.application.usecases.coupon;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Registra o resgate de um cupom por um cliente (1x por e-mail, ver V44) e incrementa o contador
 * de uso do cupom — tudo numa única instrução atômica, segura contra corrida entre requisições
 * concorrentes (ex.: duplo clique/duplo submit do formulário público).
 */
@Service
@RequiredArgsConstructor
public class CouponRedemptionService {
    private final JdbcTemplate jdbc;

    /** @return true se este foi o primeiro resgate desse cupom por esse e-mail (contador incrementado). */
    @Transactional
    public boolean redeem(UUID couponId, UUID agencyId, String customerEmail, UUID submissionId) {
        int rows = jdbc.update("""
                WITH ins AS (
                    INSERT INTO coupon_redemptions (id, coupon_id, agency_id, customer_email, submission_id, redeemed_at)
                    VALUES (gen_random_uuid(), ?, ?, LOWER(TRIM(?)), ?, NOW())
                    ON CONFLICT (coupon_id, customer_email) DO NOTHING
                    RETURNING 1
                )
                UPDATE coupons SET used_count = used_count + 1
                WHERE id = ? AND EXISTS (SELECT 1 FROM ins)
                """, couponId, agencyId, customerEmail, submissionId, couponId);
        return rows > 0;
    }
}
