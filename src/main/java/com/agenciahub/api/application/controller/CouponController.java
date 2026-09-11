package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.persistence.entity.Coupon;
import com.agenciahub.api.application.persistence.repository.AgencyRepository;
import com.agenciahub.api.application.persistence.repository.CouponRepository;
import com.agenciahub.api.security.TenantContext;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Cupons de desconto do dono da agência (TODO-019/TODO-035). CRUD é restrito à agência; upsert usa UPPER-case no código. */
@RestController
@RequestMapping("/agency/coupons")
@PreAuthorize("hasRole('AGENCY_OWNER')")
@RequiredArgsConstructor
public class CouponController {
    private final CouponRepository coupons;
    private final AgencyRepository agencies;

    public record CouponDTO(
            UUID id, @NotBlank @Size(max = 40) String code, Instant expiresAt, boolean active, Instant createdAt,
            BigDecimal discountPercent, BigDecimal maxDiscountAmount, Integer maxUses, int usedCount) {
        static CouponDTO from(Coupon c) {
            return new CouponDTO(c.getId(), c.getCode(), c.getExpiresAt(), c.isActive(), c.getCreatedAt(),
                    c.getDiscountPercent(), c.getMaxDiscountAmount(), c.getMaxUses(), c.getUsedCount());
        }
    }

    public record UpsertCouponRequest(
            @NotBlank @Size(max = 40) String code, Instant expiresAt, Boolean active,
            @DecimalMin("0.01") @DecimalMax("100") BigDecimal discountPercent,
            @DecimalMin("0.01") BigDecimal maxDiscountAmount,
            @Min(1) Integer maxUses) {}

    @GetMapping
    public List<CouponDTO> list() {
        return coupons.findByAgency_IdOrderByCreatedAtDesc(TenantContext.requireAgencyId())
                .stream().map(CouponDTO::from).toList();
    }

    @PostMapping
    public CouponDTO create(@org.springframework.web.bind.annotation.RequestBody @jakarta.validation.Valid UpsertCouponRequest request) {
        UUID agencyId = TenantContext.requireAgencyId();
        if (coupons.existsByAgency_IdAndCodeIgnoreCase(agencyId, request.code()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um cupom com este código.");
        Coupon saved = coupons.save(Coupon.builder()
                .agency(agencies.getReferenceById(agencyId))
                .code(request.code())
                .expiresAt(request.expiresAt())
                .active(request.active() == null || request.active())
                .discountPercent(request.discountPercent())
                .maxDiscountAmount(request.maxDiscountAmount())
                .maxUses(request.maxUses())
                .build());
        return CouponDTO.from(saved);
    }

    @PatchMapping("/{id}")
    public CouponDTO update(@PathVariable UUID id, @org.springframework.web.bind.annotation.RequestBody @jakarta.validation.Valid UpsertCouponRequest request) {
        UUID agencyId = TenantContext.requireAgencyId();
        Coupon coupon = coupons.findByIdAndAgency_Id(id, agencyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "cupom não encontrado"));
        if (!coupon.getCode().equalsIgnoreCase(request.code())
                && coupons.existsByAgency_IdAndCodeIgnoreCase(agencyId, request.code()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um cupom com este código.");
        if (request.maxUses() != null && request.maxUses() < coupon.getUsedCount())
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O cupom já foi usado " + coupon.getUsedCount() + "x — o limite não pode ser menor que isso.");
        coupon.setCode(request.code());
        coupon.setExpiresAt(request.expiresAt());
        if (request.active() != null) coupon.setActive(request.active());
        coupon.setDiscountPercent(request.discountPercent());
        coupon.setMaxDiscountAmount(request.maxDiscountAmount());
        coupon.setMaxUses(request.maxUses());
        return CouponDTO.from(coupons.save(coupon));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        UUID agencyId = TenantContext.requireAgencyId();
        Coupon coupon = coupons.findByIdAndAgency_Id(id, agencyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "cupom não encontrado"));
        coupons.delete(coupon);
    }
}
