package com.agenciahub.api.controller.user.docs;

import com.agenciahub.api.dto.user.CreateUserRequest;
import com.agenciahub.api.dto.user.UpdateUserRequest;
import com.agenciahub.api.dto.user.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.util.UUID;

@RequestMapping("/users")
@Tag(
        name = "Usuários",
        description = "Gestão de usuários da agência. Acesso restrito a papel **OWNER** (ver filtro de segurança).")
public interface UserAPI {

    @GetMapping
    @Operation(summary = "Lista usuários", description = "Ordenação alfabética por nome.")
    List<UserResponse> list();

    @GetMapping("/sellers")
    @Operation(summary = "Lista vendedores ativos", description = "Usado em combos de atribuição de cotação.")
    List<UserResponse> sellers();

    @GetMapping("/{id}")
    @Operation(summary = "Obtém usuário por id")
    UserResponse get(@Parameter(description = "id do usuário") @PathVariable UUID id);

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria usuário", description = "Owner ou vendedor conforme corpo da requisição.")
    UserResponse create(@Valid @RequestBody CreateUserRequest request);

    @PatchMapping("/{id}")
    @Operation(summary = "Atualiza usuário", description = "Nome, senha, ativo, comissão percentual ou fixa.")
    UserResponse patch(
            @Parameter(description = "id do usuário") @PathVariable UUID id,
            @RequestBody UpdateUserRequest request);
}
