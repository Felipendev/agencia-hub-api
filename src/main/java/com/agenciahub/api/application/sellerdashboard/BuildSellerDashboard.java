package com.agenciahub.api.application.sellerdashboard;

import com.agenciahub.api.application.user.UserResponseMapper;
import com.agenciahub.api.domain.QuotationStatus;
import com.agenciahub.api.domain.UserRole;
import com.agenciahub.api.dto.quotation.QuotationResponse;
import com.agenciahub.api.dto.seller.SellerDashboardResponse;
import com.agenciahub.api.entity.User;
import com.agenciahub.api.service.QuotationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BuildSellerDashboard implements BuildSellerDashboardUseCase {

    private final QuotationService quotationService;
    private final UserResponseMapper userResponseMapper;

    @Override
    public SellerDashboardResponse execute(User sellerEntity) {
        UUID sellerId = sellerEntity.getId();
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
                userResponseMapper.toResponse(sellerEntity),
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
        if (quotations.isEmpty()) {
            return BigDecimal.ZERO;
        }

        if (seller.getCommissionPct() != null) {
            BigDecimal total = quotations.stream()
                    .map(QuotationResponse::totalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return total.multiply(seller.getCommissionPct())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        if (seller.getCommissionFixed() != null) {
            return seller.getCommissionFixed()
                    .multiply(BigDecimal.valueOf(quotations.size()));
        }

        return BigDecimal.ZERO;
    }
}
