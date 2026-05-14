package com.agenciahub.api.application.customer;

import com.agenciahub.api.dto.customer.UpdateCustomerRequest;

import java.util.UUID;

public record UpdateCustomerCommand(UUID id, UpdateCustomerRequest request) {
}
