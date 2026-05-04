package com.agenciahub.api.dto.seller;

import com.agenciahub.api.dto.quotation.QuotationResponse;
import com.agenciahub.api.dto.user.UserResponse;

import java.math.BigDecimal;
import java.util.List;

public record SellerDashboardResponse(
        UserResponse seller,
        long totalQuotations,
        long openQuotations,
        long approvedQuotations,
        BigDecimal totalCommissionEarned,
        BigDecimal pendingCommission,
        List<QuotationResponse> recentQuotations
) {
}
