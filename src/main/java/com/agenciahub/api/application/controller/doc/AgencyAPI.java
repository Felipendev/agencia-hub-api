package com.agenciahub.api.application.controller.doc;

import com.agenciahub.api.application.usecases.agency.shared.AgencySummaryResponseDTO;
import com.agenciahub.api.application.usecases.agency.update.UpdateAgencyRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/agency")
@Tag(
        name = "Agência",
        description = "Dados e atualização da agência do tenant atual. Acesso restrito a usuários com papel **AGENCY_OWNER**.")
@StandardErrorApiResponses
public interface AgencyAPI {

    @GetMapping
    @Operation(summary = "Obtém dados da agência", description = "Resolve pela agência do contexto (tenant).")
    AgencySummaryResponseDTO getAgency();

    @PatchMapping
    @Operation(summary = "Atualiza campos da agência", description = "Alterações parciais; validações de telefone, CNPJ e logo no serviço.")
    AgencySummaryResponseDTO updateAgency(@Valid @RequestBody UpdateAgencyRequestDTO request);
}
