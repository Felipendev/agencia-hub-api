package com.agenciahub.api.application.usecases.salesagent.dashboard.build;

import com.agenciahub.api.application.persistence.entity.SolicitacaoSubmission;
import com.agenciahub.api.application.persistence.repository.SolicitacaoSubmissionRepository;
import com.agenciahub.api.application.usecases.quotation.retrieve.list.ListQuotationsQuery;
import com.agenciahub.api.application.usecases.quotation.retrieve.list.ListQuotationsUseCase;
import com.agenciahub.api.application.usecases.platformaccount.shared.PlatformAccountResponseMapper;
import com.agenciahub.api.domain.QuotationStatus;
import com.agenciahub.api.application.usecases.quotation.shared.QuotationSummaryResponseDTO;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BuildSalesAgentDashboard implements BuildSalesAgentDashboardUseCase {

    private static final DateTimeFormatter YEAR_MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM").withZone(ZoneOffset.UTC);
    private static final int RECENT_SUBMISSIONS_DAYS = 30;
    private static final int MONTHLY_HISTORY_MONTHS = 12;

    private final ListQuotationsUseCase listQuotationsUseCase;
    private final PlatformAccountResponseMapper userResponseMapper;
    private final SolicitacaoSubmissionRepository submissionRepository;

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

        List<SubmissionSummaryDTO> recentSubmissions = buildRecentSubmissions(salesAgentEntity.getId());

        List<MonthlyCommissionDTO> monthlyCommissions = buildMonthlyCommissions(
                salesAgentEntity,
                all.stream().filter(q -> q.status() == QuotationStatus.ACCEPTED).toList());

        return new SalesAgentDashboardResponseDTO(
                userResponseMapper.toResponse(salesAgentEntity),
                all.size(),
                open,
                approved,
                earned,
                pending,
                recent,
                recentSubmissions,
                monthlyCommissions);
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

    private List<SubmissionSummaryDTO> buildRecentSubmissions(UUID sellerId) {
        Instant since = Instant.now().minus(RECENT_SUBMISSIONS_DAYS, ChronoUnit.DAYS);
        return submissionRepository
                .findByReferralSeller_IdAndCreatedAtAfterOrderByCreatedAtDesc(sellerId, since)
                .stream()
                .map(s -> new SubmissionSummaryDTO(s.getId(), s.getNome(), s.getEmail(), s.getTelefone(), s.getCreatedAt()))
                .toList();
    }

    private List<MonthlyCommissionDTO> buildMonthlyCommissions(
            PlatformAccount salesAgent, List<QuotationSummaryResponseDTO> accepted) {

        Instant cutoff = Instant.now().minus((long) MONTHLY_HISTORY_MONTHS * 30, ChronoUnit.DAYS);
        Map<String, List<QuotationSummaryResponseDTO>> byMonth = accepted.stream()
                .filter(q -> q.updatedAt() != null && q.updatedAt().isAfter(cutoff))
                .collect(Collectors.groupingBy(
                        q -> YEAR_MONTH_FMT.format(q.updatedAt()),
                        TreeMap::new,
                        Collectors.toList()));

        return byMonth.entrySet().stream()
                .map(e -> new MonthlyCommissionDTO(e.getKey(), calculateCommission(salesAgent, e.getValue())))
                .sorted(Comparator.comparing(MonthlyCommissionDTO::yearMonth))
                .toList();
    }
}
