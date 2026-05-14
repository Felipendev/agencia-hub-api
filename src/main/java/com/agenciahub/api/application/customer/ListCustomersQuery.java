package com.agenciahub.api.application.customer;

import com.agenciahub.api.domain.CustomerStatus;

public record ListCustomersQuery(String name, CustomerStatus status) {
}
