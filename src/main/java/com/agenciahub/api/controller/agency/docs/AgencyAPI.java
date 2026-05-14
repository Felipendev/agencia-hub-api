package com.agenciahub.api.controller.agency.docs;

import com.agenciahub.api.api.docs.StandardErrorApiResponses;
import com.agenciahub.api.dto.agency.AgencyResponse;
import com.agenciahub.api.dto.agency.UpdateAgencyRequest;
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
        description = "Dados e atualização da agência do tenant atual. Acesso restrito a usuários com papel **OWNER**.")
@StandardErrorApiResponses
public interface AgencyAPI {

    @GetMapping
    @Operation(summary = "Obtém dados da agência", description = "Resolve pela agência do contexto (tenant).")
    AgencyResponse getAgency();

    @PatchMapping
    @Operation(summary = "Atualiza campos da agência", description = "Alterações parciais; validações de telefone, CNPJ e logo no serviço.")
    AgencyResponse updateAgency(@Valid @RequestBody UpdateAgencyRequest request);
}
