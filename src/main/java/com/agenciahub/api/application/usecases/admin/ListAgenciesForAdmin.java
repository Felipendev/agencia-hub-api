package com.agenciahub.api.application.usecases.admin;

import com.agenciahub.api.application.persistence.entity.Agency;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.application.persistence.repository.AgencyRepository;
import com.agenciahub.api.application.persistence.repository.CrmCustomerRepository;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import com.agenciahub.api.application.persistence.repository.QuotationRepository;
import com.agenciahub.api.domain.enums.AccountKind;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListAgenciesForAdmin {

    private final AgencyRepository agencyRepository;
    private final PlatformAccountRepository platformAccountRepository;
    private final CrmCustomerRepository customerRepository;
    private final QuotationRepository quotationRepository;

    @Transactional(readOnly = true)
    public List<AgencyAdminSummaryDTO> execute() {
        return agencyRepository.findAll().stream().map(agency -> {
            long ownerCount = platformAccountRepository.countByAgency_IdAndAccountKind(agency.getId(), AccountKind.AGENCY_OWNER);
            long sellerCount = platformAccountRepository.countByAgency_IdAndAccountKind(agency.getId(), AccountKind.SALES_AGENT);
            long customerCount = customerRepository.countByAgency_Id(agency.getId());
            long quotationCount = quotationRepository.countByAgency_Id(agency.getId());
            String ownerEmail = platformAccountRepository.findByAgency_Id(agency.getId()).stream()
                    .filter(u -> u.getAccountKind() == AccountKind.AGENCY_OWNER)
                    .map(PlatformAccount::getEmail)
                    .findFirst()
                    .orElse(null);
            return new AgencyAdminSummaryDTO(
                    agency.getId(),
                    agency.getName(),
                    agency.getStatus(),
                    agency.getSubscriptionStatus(),
                    agency.getTrialEndsAt(),
                    agency.getCreatedAt(),
                    agency.getDeletionScheduledAt(),
                    ownerCount,
                    sellerCount,
                    customerCount,
                    quotationCount,
                    ownerEmail
            );
        }).toList();
    }
}
