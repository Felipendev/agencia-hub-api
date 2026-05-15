package com.agenciahub.api.application.usecases.salesagent.dashboard.build;

import com.agenciahub.api.application.usecases.quotation.retrieve.list.ListQuotationsQuery;
import com.agenciahub.api.application.usecases.quotation.retrieve.list.ListQuotationsUseCase;
import com.agenciahub.api.application.usecases.platformaccount.shared.PlatformAccountResponseMapper;
import com.agenciahub.api.domain.QuotationStatus;
import com.agenciahub.api.application.usecases.quotation.shared.QuotationSummaryResponseDTO;
import com.agenciahub.api.application.usecases.salesagent.dashboard.build.SalesAgentDashboardResponseDTO;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BuildSalesAgentDashboard implements BuildSalesAgentDashboardUseCase {

    private final ListQuotationsUseCase listQuotationsUseCase;
    private final PlatformAccountResponseMapper userResponseMapper;

    @Override
    public SalesAgentDashboardResponseDTO execute(PlatformAccount salesAgentEntity) {
        List<QuotationSummaryResponseDTO> all =
                listQuotationsUseCase.execute(new ListQuotationsQuery(null, null, null, salesAgentEntity));

        List<QuotationSummaryResponseDTO> recent = all.stream()
                .sorted((a, b) -> b.updatedAt().compareTo(a.updatedAt()))
                .limit(10)
                .toList();

        long open = all.stream().filter(q -> isOpen(q.status())).count();

        long approved = all.stream().filter(q -> q.status() == QuotationStatus.ACCEPTED).count();

        BigDecimal earned = calculateCommission(
                salesAgentEntity, all.stream().filter(q -> q.status() == QuotationStatus.ACCEPTED).toList());

        BigDecimal pending =
                calculateCommission(salesAgentEntity, all.stream().filter(q -> isOpen(q.status())).toList());

        return new SalesAgentDashboardResponseDTO(
                userResponseMapper.toResponse(salesAgentEntity),
                all.size(),
                open,
                approved,
                earned,
                pending,
                recent);
    }

    private boolean isOpen(QuotationStatus status) {
        return status == QuotationStatus.DRAFT
                || status == QuotationStatus.SENT
                || status == QuotationStatus.AWAITING_CLIENT;
    }

    private BigDecimal calculateCommission(PlatformAccount salesAgent, List<QuotationSummaryResponseDTO> quotations) {
        if (quotations.isEmpty()) {
            return BigDecimal.ZERO;
        }

        if (salesAgent.getCommissionPct() != null) {
            BigDecimal total = quotations.stream()
                    .map(QuotationSummaryResponseDTO::totalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return total.multiply(salesAgent.getCommissionPct()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        if (salesAgent.getCommissionFixed() != null) {
            return salesAgent.getCommissionFixed().multiply(BigDecimal.valueOf(quotations.size()));
        }

        return BigDecimal.ZERO;
    }
}
