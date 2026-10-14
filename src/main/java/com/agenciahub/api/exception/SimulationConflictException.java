package com.agenciahub.api.exception;

public class SimulationConflictException extends RuntimeException {
    public SimulationConflictException() {
        super("Esta simulação foi atualizada por outra pessoa. Recarregue o cenário antes de salvar suas alterações.");
    }
}
