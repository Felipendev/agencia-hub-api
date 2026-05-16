package com.agenciahub.api.application.usecases.customer.update;

import com.agenciahub.api.application.usecases.customer.update.UpdateCustomerRequestDTO;

import java.util.UUID;

public record UpdateCustomerCommand(UUID id, UpdateCustomerRequestDTO request) {
}
