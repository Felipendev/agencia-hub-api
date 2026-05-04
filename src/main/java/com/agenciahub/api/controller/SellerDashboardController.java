package com.agenciahub.api.controller;

import com.agenciahub.api.domain.QuotationStatus;
import com.agenciahub.api.domain.UserRole;
import com.agenciahub.api.dto.quotation.QuotationResponse;
import com.agenciahub.api.dto.seller.SellerDashboardResponse;
import com.agenciahub.api.entity.User;
import com.agenciahub.api.service.QuotationService;
import com.agenciahub.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/seller-dashboard")
@RequiredArgsConstructor
@Tag(name = "Seller Dashboard")
public class SellerDashboardController {

    private final QuotationService quotationService;
    private final UserService userService;

    /** Seller sees their own dashboard. */
    @GetMapping("/me")
    @Operation(summary = "Get dashboard for the authenticated seller")
    public SellerDashboardResponse myDashboard(@AuthenticationPrincipal User caller) {
        return buildDashboard(caller, caller.getId());
    }

    /** Owner can view any seller's dashboard. */
    @GetMapping("/{sellerId}")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Get dashboard for a specific seller (owner only)")
    public SellerDashboardResponse sellerDashboard(@PathVariable UUID sellerId,
                                                    @AuthenticationPrincipal User caller) {
        User seller = userService.getEntityById(sellerId);
        return buildDashboard(seller, sellerId);
    }

    private SellerDashboardResponse buildDashboard(User sellerEntity, UUID sellerId) {
        List<QuotationResponse> all = quotationService.search(
                null, null, null, sellerId, UserRole.SELLER);

        List<QuotationResponse> recent = all.stream()
                .sorted((a, b) -> b.updatedAt().compareTo(a.updatedAt()))
                .limit(10)
                .toList();

        long open = all.stream()
                .filter(q -> isOpen(q.status()))
                .count();

        long approved = all.stream()
                .filter(q -> q.status() == QuotationStatus.ACCEPTED)
                .count();

        BigDecimal earned = calculateCommission(sellerEntity,
                all.stream().filter(q -> q.status() == QuotationStatus.ACCEPTED).toList());

        BigDecimal pending = calculateCommission(sellerEntity,
                all.stream().filter(q -> isOpen(q.status())).toList());

        return new SellerDashboardResponse(
                userService.toResponse(sellerEntity),
                all.size(),
                open,
                approved,
                earned,
                pending,
                recent
        );
    }

    private boolean isOpen(QuotationStatus status) {
        return status == QuotationStatus.DRAFT
                || status == QuotationStatus.SENT
                || status == QuotationStatus.AWAITING_CLIENT;
    }

    private BigDecimal calculateCommission(User seller, List<QuotationResponse> quotations) {
        if (quotations.isEmpty()) return BigDecimal.ZERO;

        if (seller.getCommissionPct() != null) {
            BigDecimal total = quotations.stream()
                    .map(QuotationResponse::totalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return total.multiply(seller.getCommissionPct())
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        }

        if (seller.getCommissionFixed() != null) {
            return seller.getCommissionFixed()
                    .multiply(BigDecimal.valueOf(quotations.size()));
        }

        return BigDecimal.ZERO;
    }
}
