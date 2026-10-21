package com.agenciahub.api.application.usecases.customer.delete;

import com.agenciahub.api.application.persistence.entity.CrmCustomer;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.application.persistence.repository.CrmCustomerRepository;
import com.agenciahub.api.application.persistence.repository.FinancialEntryRepository;
import com.agenciahub.api.application.persistence.repository.QuotationRepository;
import com.agenciahub.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteCustomer implements DeleteCustomerUseCase {

    private final CrmCustomerRepository customerRepository;
    private final QuotationRepository quotationRepository;
    private final FinancialEntryRepository financialEntryRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final com.agenciahub.api.application.services.referrals.ReferralHistory referralHistory;
    private final com.agenciahub.api.application.services.stages.TripStageHistory stageHistory;

    @Override
    @Transactional
    public void execute(UUID id) {
        UUID agencyId = TenantContext.requireAgencyId();
        CrmCustomer entity = customerRepository
                .findByIdAndAgency_Id(id, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("cliente não encontrado: " + id));
        UUID customerId = entity.getId();
        // O evento de exclusão também conserva dados pessoais, mesmo sem compromisso ativo.
        jdbc.queryForList("SELECT id FROM customers WHERE id=? AND agency_id=? FOR UPDATE", customerId, agencyId);
        if (referralHistory.existsForCustomer(agencyId,customerId)) {
            throw new com.agenciahub.api.exception.ReferralHistoryException();
        }
        if (stageHistory.existsForCustomer(agencyId, customerId)) {
            throw new com.agenciahub.api.exception.TripStageHistoryException();
        }
        Boolean agendaHistory = jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM appointments WHERE agency_id=? AND customer_id=?) OR EXISTS(SELECT 1 FROM appointment_events WHERE agency_id=? AND customer_id=?)",
                Boolean.class, agencyId, customerId, agencyId, customerId);
        if (Boolean.TRUE.equals(agendaHistory)) {
            throw new com.agenciahub.api.exception.AgendaHistoryException();
        }
        quotationRepository.deleteAll(quotationRepository.findByCustomer_IdOrderByCreatedAtDesc(customerId));
        financialEntryRepository.unlinkCustomer(customerId);
        customerRepository.delete(entity);
    }
}
