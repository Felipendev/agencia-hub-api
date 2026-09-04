package com.agenciahub.api.application.persistence.repository.spec;

import com.agenciahub.api.domain.FinancialEntryCategory;
import com.agenciahub.api.domain.FinancialEntryStatus;
import com.agenciahub.api.domain.FinancialEntryType;
import com.agenciahub.api.application.persistence.entity.FinancialEntry;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class FinancialEntrySpecifications {

    private FinancialEntrySpecifications() {
    }

    public static Specification<FinancialEntry> withFilters(
            UUID agencyId,
            LocalDate from,
            LocalDate to,
            FinancialEntryType type,
            FinancialEntryCategory category,
            FinancialEntryStatus status,
            UUID customerId,
            String bankAccount) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("agency").get("id"), agencyId));

            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("entryDate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("entryDate"), to));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (customerId != null) {
                predicates.add(cb.equal(root.get("customer").get("id"), customerId));
            }
            if (bankAccount != null && !bankAccount.isBlank()) {
                predicates.add(cb.equal(root.get("bankAccount"), bankAccount.strip()));
            }

            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
