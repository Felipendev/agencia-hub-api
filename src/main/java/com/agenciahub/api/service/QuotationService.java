package com.agenciahub.api.service;

import com.agenciahub.api.domain.QuotationStatus;
import com.agenciahub.api.domain.UserRole;
import com.agenciahub.api.dto.quotation.CreateQuotationRequest;
import com.agenciahub.api.dto.quotation.QuotationResponse;
import com.agenciahub.api.dto.quotation.UpdateQuotationRequest;
import com.agenciahub.api.entity.Customer;
import com.agenciahub.api.entity.Opportunity;
import com.agenciahub.api.entity.Quotation;
import com.agenciahub.api.entity.User;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.CustomerRepository;
import com.agenciahub.api.repository.OpportunityRepository;
import com.agenciahub.api.repository.QuotationRepository;
import com.agenciahub.api.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuotationService {

    private final QuotationRepository quotationRepository;
    private final CustomerRepository customerRepository;
    private final OpportunityRepository opportunityRepository;
    private final UserRepository userRepository;

    /**
     * Search quotations.
     * If {@code callerRole} is SELLER, results are automatically scoped to {@code callerId}.
     */
    @Transactional(readOnly = true)
    public List<QuotationResponse> search(UUID customerId, QuotationStatus status, String search,
                                          UUID callerId, UserRole callerRole) {
        // Sellers can only see their own quotations
        UUID effectiveSellerId = (callerRole == UserRole.SELLER) ? callerId : null;

        Specification<Quotation> spec = quotationSearchSpec(customerId, status, search, effectiveSellerId);
        List<Quotation> rows = quotationRepository.findAll(
                spec, Sort.by(Sort.Direction.DESC, "createdAt"));
        return rows.stream().map(this::toResponse).toList();
    }

    private Specification<Quotation> quotationSearchSpec(
            UUID customerId, QuotationStatus status, String rawSearch, UUID sellerId) {
        final String trimmed = rawSearch != null ? rawSearch.strip() : "";
        final boolean hasSearch = !trimmed.isEmpty();

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (customerId != null) {
                predicates.add(cb.equal(root.join("customer").get("id"), customerId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (sellerId != null) {
                predicates.add(cb.equal(root.get("seller").get("id"), sellerId));
            }
            if (hasSearch) {
                String pattern = "%" + escapeLike(trimmed) + "%";
                var titlePred = cb.like(cb.lower(root.get("title")), pattern);
                var destPred  = cb.like(cb.lower(root.get("destination")), pattern);
                var custJoin  = root.join("customer");
                var namePred  = cb.like(cb.lower(custJoin.get("name")), pattern);
                predicates.add(cb.or(titlePred, destPred, namePred));
            }

            return predicates.isEmpty() ? cb.conjunction()
                    : cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    @Transactional(readOnly = true)
    public QuotationResponse getById(UUID id) {
        return quotationRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation not found: " + id));
    }

    @Transactional
    public QuotationResponse create(CreateQuotationRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + request.customerId()));

        Opportunity opportunity = resolveOpportunity(request.opportunityId(), customer.getId());
        User seller = resolveSeller(request.sellerId());

        String description   = request.description() != null ? request.description() : "";
        String currency      = (request.currency() != null && !request.currency().isBlank())
                ? request.currency().strip().toUpperCase() : "BRL";
        QuotationStatus init = request.status() != null ? request.status() : QuotationStatus.DRAFT;
        List<String> tags    = request.tags() == null ? new ArrayList<>() : new ArrayList<>(request.tags());
        boolean priority     = Boolean.TRUE.equals(request.priority());
        String notes         = request.internalNotes() != null ? request.internalNotes().strip() : "";

        Quotation entity = Quotation.builder()
                .customer(customer)
                .opportunity(opportunity)
                .seller(seller)
                .title(request.title().strip())
                .destination(request.destination().strip())
                .description(description)
                .totalAmount(request.totalAmount())
                .currency(currency)
                .status(init)
                .validUntil(request.validUntil())
                .travelStartDate(request.travelStartDate())
                .travelEndDate(request.travelEndDate())
                .detailsJson(request.details())
                .tags(tags)
                .priority(priority)
                .assignee(blankToNull(request.assignee()))
                .internalNotes(notes)
                .build();

        return toResponse(quotationRepository.save(entity));
    }

    @Transactional
    public QuotationResponse update(UUID id, UpdateQuotationRequest request) {
        Quotation entity = quotationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation not found: " + id));

        if (request.opportunityId() != null) {
            entity.setOpportunity(resolveOpportunity(request.opportunityId(), entity.getCustomer().getId()));
        }
        if (request.sellerId() != null) {
            entity.setSeller(resolveSeller(request.sellerId()));
        }
        if (request.title()         != null) entity.setTitle(request.title().strip());
        if (request.destination()   != null) entity.setDestination(request.destination().strip());
        if (request.description()   != null) entity.setDescription(request.description());
        if (request.totalAmount()   != null) entity.setTotalAmount(request.totalAmount());
        if (request.currency()      != null) entity.setCurrency(request.currency().strip().toUpperCase());
        if (request.status()        != null) entity.setStatus(request.status());
        if (request.validUntil()    != null) entity.setValidUntil(request.validUntil());
        if (request.travelStartDate() != null) entity.setTravelStartDate(request.travelStartDate());
        if (request.travelEndDate() != null) entity.setTravelEndDate(request.travelEndDate());
        if (request.details()       != null) entity.setDetailsJson(request.details());
        if (request.tags()          != null) entity.setTags(new ArrayList<>(request.tags()));
        if (request.priority()      != null) entity.setPriority(request.priority());
        if (request.assignee()      != null) entity.setAssignee(blankToNull(request.assignee()));
        if (request.internalNotes() != null) entity.setInternalNotes(request.internalNotes().strip());

        return toResponse(entity);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static String blankToNull(String value) {
        if (value == null) return null;
        String t = value.strip();
        return t.isEmpty() ? null : t;
    }

    private Opportunity resolveOpportunity(UUID opportunityId, UUID customerId) {
        if (opportunityId == null) return null;
        Opportunity op = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found: " + opportunityId));
        if (!op.getCustomer().getId().equals(customerId)) {
            throw new IllegalArgumentException("Opportunity does not belong to the given customer");
        }
        return op;
    }

    private User resolveSeller(UUID sellerId) {
        if (sellerId == null) return null;
        return userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found: " + sellerId));
    }

    private QuotationResponse toResponse(Quotation q) {
        Customer c  = q.getCustomer();
        Opportunity o = q.getOpportunity();
        User s      = q.getSeller();
        return new QuotationResponse(
                q.getId(),
                c.getId(),
                c.getName(),
                o != null ? o.getId()    : null,
                o != null ? o.getTitle() : null,
                s != null ? s.getId()    : null,
                s != null ? s.getName()  : null,
                q.getTitle(),
                q.getDestination(),
                q.getDescription(),
                q.getTotalAmount(),
                q.getCurrency(),
                q.getStatus(),
                q.getValidUntil(),
                q.getTravelStartDate(),
                q.getTravelEndDate(),
                q.getDetailsJson(),
                q.getTags() != null ? List.copyOf(q.getTags()) : List.of(),
                Boolean.TRUE.equals(q.getPriority()),
                q.getAssignee(),
                q.getInternalNotes() != null ? q.getInternalNotes() : "",
                q.getCreatedAt(),
                q.getUpdatedAt()
        );
    }
}
