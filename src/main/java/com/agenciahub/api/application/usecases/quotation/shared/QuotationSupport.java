package com.agenciahub.api.application.usecases.quotation.shared;

import com.agenciahub.api.domain.QuotationStatus;
import com.agenciahub.api.domain.enums.AccountKind;
import com.agenciahub.api.application.persistence.entity.Quotation;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class QuotationSupport {

    private QuotationSupport() {}

    public static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String t = value.strip();
        return t.isEmpty() ? null : t;
    }

    public static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    public static Specification<Quotation> quotationSearchSpec(
            UUID agencyId, UUID customerId, QuotationStatus status, String rawSearch, UUID sellerId) {
        final String trimmed = rawSearch != null ? rawSearch.strip() : "";
        final boolean hasSearch = !trimmed.isEmpty();

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always scope to the caller's agency
            predicates.add(cb.equal(root.get("agency").get("id"), agencyId));

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
                var destPred = cb.like(cb.lower(root.get("destination")), pattern);
                var custJoin = root.join("customer");
                var namePred = cb.like(cb.lower(custJoin.get("name")), pattern);
                predicates.add(cb.or(titlePred, destPred, namePred));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static PlatformAccount resolveSellerInAgency(PlatformAccountRepository userRepository, UUID sellerId, UUID agencyId) {
        if (sellerId == null) {
            return null;
        }
        PlatformAccount u = userRepository
                .findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("vendedor não encontrado: " + sellerId));
        if (u.getAgency() == null || !u.getAgency().getId().equals(agencyId)) {
            throw new IllegalArgumentException("vendedor não pertence à agência deste cliente.");
        }
        if (u.getAccountKind() != AccountKind.SALES_AGENT && u.getAccountKind() != AccountKind.AGENCY_OWNER) {
            throw new IllegalArgumentException("usuário inválido como vendedor.");
        }
        return u;
    }
}
