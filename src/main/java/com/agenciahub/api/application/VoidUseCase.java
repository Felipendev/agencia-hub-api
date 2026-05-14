package com.agenciahub.api.application;

/**
 * Application operation with a single input and no return value (side effects only).
 *
 * @param <I> input type (command or application-level DTO)
 */
@FunctionalInterface
public interface VoidUseCase<I> {

    void execute(I input);
}
