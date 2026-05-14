/**
 * Application layer: use-case orchestration, transaction boundaries, and coordination
 * between domain behavior and infrastructure ports.
 * <p>
 * Existing types under {@code com.agenciahub.api.service} remain in place; new use cases
 * can be introduced here incrementally without a big-bang move.
 * <p>
 * Generic contracts: {@link com.agenciahub.api.application.UseCase} and
 * {@link com.agenciahub.api.application.VoidUseCase}. Prefer dedicated interfaces per
 * operation (see backend architecture doc) when the use case grows or has many collaborators.
 */
package com.agenciahub.api.application;
