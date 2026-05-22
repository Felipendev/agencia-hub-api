package com.agenciahub.api.application.usecases.salesagent.dashboard.build;

import java.math.BigDecimal;

/** Commission earned in a specific calendar month (format: "YYYY-MM"). */
public record MonthlyCommissionDTO(String yearMonth, BigDecimal amount) {
}
