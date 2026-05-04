package com.agenciahub.api.service;

import com.agenciahub.api.dto.opportunity.CreateOpportunityRequest;
import com.agenciahub.api.dto.opportunity.OpportunityResponse;
import com.agenciahub.api.dto.opportunity.UpdateOpportunityRequest;
import com.agenciahub.api.entity.Customer;
import com.agenciahub.api.entity.Opportunity;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.CustomerRepository;
import com.agenciahub.api.repository.OpportunityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OpportunityService {

    private final OpportunityRepository opportunityRepository;
    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public List<OpportunityResponse> list(UUID customerId) {
        List<Opportunity> rows = customerId == null
                ? opportunityRepository.findAllByOrderByExpectedTravelDateDesc()
                : opportunityRepository.findByCustomer_IdOrderByExpectedTravelDateDesc(customerId);
        return rows.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OpportunityResponse getById(UUID id) {
        return opportunityRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found: " + id));
    }

    @Transactional
    public OpportunityResponse create(CreateOpportunityRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + request.customerId()));
        String notes = request.notes() != null ? request.notes() : "";
        Opportunity entity = Opportunity.builder()
                .customer(customer)
                .title(request.title().strip())
                .destination(request.destination().strip())
                .estimatedAmount(request.estimatedAmount())
                .status(request.status())
                .expectedTravelDate(request.expectedTravelDate())
                .notes(notes)
                .build();
        Opportunity saved = opportunityRepository.save(entity);
        return toResponse(saved);
    }

    @Transactional
    public OpportunityResponse update(UUID id, UpdateOpportunityRequest request) {
        Opportunity entity = opportunityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found: " + id));
        if (request.title() != null) {
            entity.setTitle(request.title().strip());
        }
        if (request.destination() != null) {
            entity.setDestination(request.destination().strip());
        }
        if (request.estimatedAmount() != null) {
            entity.setEstimatedAmount(request.estimatedAmount());
        }
        if (request.status() != null) {
            entity.setStatus(request.status());
        }
        if (request.expectedTravelDate() != null) {
            entity.setExpectedTravelDate(request.expectedTravelDate());
        }
        if (request.notes() != null) {
            entity.setNotes(request.notes());
        }
        return toResponse(entity);
    }

    private OpportunityResponse toResponse(Opportunity o) {
        Customer c = o.getCustomer();
        return new OpportunityResponse(
                o.getId(),
                c.getId(),
                c.getName(),
                o.getTitle(),
                o.getDestination(),
                o.getEstimatedAmount(),
                o.getStatus(),
                o.getExpectedTravelDate(),
                o.getNotes()
        );
    }
}
