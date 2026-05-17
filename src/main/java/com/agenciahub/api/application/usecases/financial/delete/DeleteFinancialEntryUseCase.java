package com.agenciahub.api.application.usecases.financial.delete;

import java.util.UUID;

public interface DeleteFinancialEntryUseCase {
    void execute(UUID id);
}
