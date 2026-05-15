package com.agenciahub.api.application.usecases.salesagent.dashboard.build;

import com.agenciahub.api.application.usecases.quotation.shared.QuotationSummaryResponseDTO;
import com.agenciahub.api.application.usecases.user.shared.UserSummaryResponseDTO;

import java.math.BigDecimal;
import java.util.List;

public record SalesAgentDashboardResponseDTO(
        UserSummaryResponseDTO seller,
        long totalQuotations,
        long openQuotations,
        long approvedQuotations,
        BigDecimal totalCommissionEarned,
        BigDecimal pendingCommission,
        List<QuotationSummaryResponseDTO> recentQuotations
) {
}
