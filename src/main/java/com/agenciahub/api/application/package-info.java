/**
 * Application layer: use-case orchestration, transaction boundaries, integrations, and scheduling.
 * <p>
 * Use cases: {@code application.usecases.{feature}/{action}/}. Integrações outbound:
 * {@code application.integrations.*}. Jobs: {@code application.scheduling}.
 * <p>
 * O pacote legado {@code com.agenciahub.api.service} foi eliminado (refatoração concluída).
 * <p>
 * Generic contracts: {@link com.agenciahub.api.application.UseCase} and
 * {@link com.agenciahub.api.application.VoidUseCase}.
 */
package com.agenciahub.api.application;
