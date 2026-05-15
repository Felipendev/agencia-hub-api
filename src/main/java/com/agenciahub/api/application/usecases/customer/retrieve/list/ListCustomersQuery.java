package com.agenciahub.api.application.usecases.customer.retrieve.list;

import com.agenciahub.api.domain.CustomerStatus;

public record ListCustomersQuery(String name, CustomerStatus status) {
}
