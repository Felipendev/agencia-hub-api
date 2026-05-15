package com.agenciahub.api.application.usecases.customer.listcustomers;

import com.agenciahub.api.domain.CustomerStatus;

public record ListCustomersQuery(String name, CustomerStatus status) {
}
