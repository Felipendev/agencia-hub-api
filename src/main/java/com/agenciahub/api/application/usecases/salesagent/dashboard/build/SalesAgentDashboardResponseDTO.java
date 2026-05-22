package com.agenciahub.api.application.usecases.salesagent.dashboard.build;

import com.agenciahub.api.application.usecases.quotation.shared.QuotationSummaryResponseDTO;
import com.agenciahub.api.application.usecases.platformaccount.shared.PlatformAccountSummaryResponseDTO;

import java.math.BigDecimal;
import java.util.List;

public record SalesAgentDashboardResponseDTO(
        PlatformAccountSummaryResponseDTO salesAgent,
        long totalQuotations,
        long openQuotations,
        long approvedQuotations,
        BigDecimal totalCommissionEarned,
        BigDecimal pendingCommission,
        List<QuotationSummaryResponseDTO> recentQuotations,
        List<SubmissionSummaryDTO> recentSubmissions,
        List<MonthlyCommissionDTO> monthlyCommissions
) {
}
