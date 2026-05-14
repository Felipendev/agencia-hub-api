package com.agenciahub.api.application;

/**
 * Generic contract for a single application operation with explicit input and output.
 *
 * @param <I> input type (command, query, or application-level DTO)
 * @param <O> output type (result or application-level DTO)
 */
@FunctionalInterface
public interface UseCase<I, O> {

    O execute(I input);
}
