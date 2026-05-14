package com.agenciahub.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${openapi.server-url}")
    private String openApiServerUrl;

    @Bean
    public OpenAPI agenciaHubOpenApi() {
        final String schemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("AgenciaHub API")
                        .version("0.2.0")
                        .description("""
                                API REST para gestão de agências de viagens; autenticação Bearer JWT.

                                **Erros:** o corpo segue o schema **ApiError** (`#/components/schemas/ApiError`): campos `message` (texto em português) e `code` (identificador estável para o cliente).

                                Códigos emitidos pelo `GlobalExceptionHandler`:
                                - **401** `UNAUTHENTICATED` — sem usuário autenticado utilizável no contexto (ex.: `SecurityContextUsers.requireUserId` / `requireUser`).
                                - **403** `MISSING_AGENCY_CONTEXT` — tenant (agência) ausente no request.
                                - **404** `NOT_FOUND` — recurso inexistente; em login, credenciais inválidas também usam este código.
                                - **409** `DUPLICATE_CUSTOMER` — conflito de cliente (e-mail ou telefone); `DATA_INTEGRITY` — outra violação de integridade tratada.
                                - **400** `BAD_REQUEST` — regra de negócio / argumento ilegal ou estado ilegal genérico; `VALIDATION_ERROR` — Bean Validation no corpo da requisição.
                                - **500** `INTERNAL_ERROR` — falha não tratada.""")
                        .contact(new Contact().name("AgenciaHub").url("https://github.com")))
                .servers(List.of(
                        new Server()
                                .url(openApiServerUrl)
                                .description("API base (includes context path /api/v1)")))
                .components(new Components()
                        .addSchemas("ApiError", new ObjectSchema()
                                .name("ApiError")
                                .description("Envelope padrão de erro JSON da API (`GlobalExceptionHandler`).")
                                .addProperty("message", new StringSchema()
                                        .description("Mensagem legível, em geral em português e minúsculas."))
                                .addProperty("code", new StringSchema()
                                        .description("Código estável; ver descrição principal da API para o mapa HTTP.")
                                        .example("NOT_FOUND"))))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .schemaRequirement(schemeName, new SecurityScheme()
                        .name(schemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"));
    }
}
