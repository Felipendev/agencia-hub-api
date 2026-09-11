package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.persistence.entity.Coupon;
import com.agenciahub.api.application.persistence.repository.CouponRedemptionRepository;
import com.agenciahub.api.application.persistence.repository.CouponRepository;
import com.agenciahub.api.application.persistence.repository.SolicitacaoConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Validação pública de cupom no formulário de orçamento — sem autenticação, ver SecurityConfig (/public/**).
 * Responde só aceito/recusado (TODO-035): validade, limite de uso e histórico de resgate são detalhe
 * interno da agência, nunca expostos ao cliente.
 */
@RestController
@RequiredArgsConstructor
public class PublicCouponController {
    private final CouponRepository coupons;
    private final CouponRedemptionRepository redemptions;
    private final SolicitacaoConfigRepository solicitacaoConfigs;

    public record CouponValidationResponse(boolean valid) {
        static final CouponValidationResponse INVALID = new CouponValidationResponse(false);
        static final CouponValidationResponse VALID = new CouponValidationResponse(true);
    }

    @GetMapping("/public/coupons/validate")
    public CouponValidationResponse validate(
            @RequestParam String slug, @RequestParam String code, @RequestParam String email) {
        if (email == null || email.isBlank()) return CouponValidationResponse.INVALID;
        return solicitacaoConfigs.findFirstBySlug(slug)
                .flatMap(config -> coupons.findByAgency_IdAndCodeIgnoreCase(config.getAgency().getId(), code))
                .filter(Coupon::isValidNow)
                .filter(c -> !redemptions.existsByCoupon_IdAndCustomerEmailIgnoreCase(c.getId(), email))
                .map(c -> CouponValidationResponse.VALID)
                .orElse(CouponValidationResponse.INVALID);
    }
}
