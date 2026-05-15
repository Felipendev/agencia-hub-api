package com.agenciahub.api.application.quotation;

import com.agenciahub.api.domain.QuotationStatus;
import com.agenciahub.api.domain.UserRole;
import com.agenciahub.api.entity.Quotation;
import com.agenciahub.api.entity.User;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class QuotationSupport {

    private QuotationSupport() {}

    static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String t = value.strip();
        return t.isEmpty() ? null : t;
    }

    static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    static Specification<Quotation> quotationSearchSpec(
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
                var destPred = cb.like(cb.lower(root.get("destination")), pattern);
                var custJoin = root.join("customer");
                var namePred = cb.like(cb.lower(custJoin.get("name")), pattern);
                predicates.add(cb.or(titlePred, destPred, namePred));
            }

            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    static User resolveSellerInAgency(UserRepository userRepository, UUID sellerId, UUID agencyId) {
        if (sellerId == null) {
            return null;
        }
        User u = userRepository
                .findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("vendedor não encontrado: " + sellerId));
        if (u.getAgency() == null || !u.getAgency().getId().equals(agencyId)) {
            throw new IllegalArgumentException("vendedor não pertence à agência deste cliente.");
        }
        if (u.getRole() != UserRole.SELLER && u.getRole() != UserRole.OWNER) {
            throw new IllegalArgumentException("usuário inválido como vendedor.");
        }
        return u;
    }
}
