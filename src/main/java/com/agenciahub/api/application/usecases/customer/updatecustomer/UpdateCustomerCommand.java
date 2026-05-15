package com.agenciahub.api.application.usecases.customer.updatecustomer;

import com.agenciahub.api.dto.customer.UpdateCustomerRequest;

import java.util.UUID;

public record UpdateCustomerCommand(UUID id, UpdateCustomerRequest request) {
}
