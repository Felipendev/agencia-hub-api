package com.agenciahub.api.controller.invitation.docs;

import com.agenciahub.api.api.docs.StandardErrorApiResponses;
import com.agenciahub.api.dto.invitation.CreateInvitationRequest;
import com.agenciahub.api.dto.invitation.InvitationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.util.UUID;

@Tag(
        name = "Convites",
        description = """
                Gestão de convites de vendedores para a agência (criação, listagem e revogação).

                Acesso restrito a usuários com papel **OWNER**. O convite envia e-mail com link público `/convite/{token}`.""")
@RequestMapping("/invitations")
@StandardErrorApiResponses
public interface InvitationAPI {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Cria convite",
            description = "Gera token, prazo de validade (72h), persiste e dispara e-mail com o link de aceite.")
    InvitationResponse create(@Valid @RequestBody CreateInvitationRequest request);

    @GetMapping
    @Operation(summary = "Lista convites da agência do tenant", description = "Ordenação: mais recentes primeiro.")
    List<InvitationResponse> list();

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Revoga convite pendente",
            description = "Somente convites em status **PENDING** podem ser revogados; passa a **REVOKED**.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "convite revogado"),
            @ApiResponse(responseCode = "404", description = "convite inexistente ou de outra agência")
    })
    void revoke(@Parameter(description = "id do convite") @PathVariable UUID id);
}
