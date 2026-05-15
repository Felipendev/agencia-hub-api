package com.agenciahub.api.application.controllers.docs;

import com.agenciahub.api.domain.QuotationStatus;
import com.agenciahub.api.dto.quotation.CreateQuotationRequest;
import com.agenciahub.api.dto.quotation.QuotationResponse;
import com.agenciahub.api.dto.quotation.UpdateQuotationRequest;
import com.agenciahub.api.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.util.UUID;

@Tag(
        name = "Cotações",
        description = """
                Endpoints para listar, consultar, criar, atualizar e excluir permanentemente cotações.

                Regras de visibilidade e atribuição de vendedor ficam na camada de aplicação (casos de uso), não no controller.""")
@RequestMapping("/quotations")
@StandardErrorApiResponses
public interface QuotationAPI {

    @GetMapping
    @Operation(
            summary = "Lista cotações com filtros opcionais",
            description = """
                    - **OWNER**: vê todas as cotações da agência (filtros aplicam-se normalmente).
                    - **SELLER**: resultados restritos às cotações em que ele é o vendedor (`seller`), independentemente dos filtros de texto.""")
    List<QuotationResponse> list(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) QuotationStatus status,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal User caller);

    @GetMapping("/{id}")
    @Operation(summary = "Obtém uma cotação pelo id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "cotação encontrada"),
            @ApiResponse(responseCode = "404", description = "cotação inexistente")
    })
    QuotationResponse get(@Parameter(description = "id da cotação") @PathVariable UUID id);

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Cria uma cotação",
            description = """
                    Se o chamador for **SELLER** e o corpo **não** enviar `sellerId`, o sistema atribui automaticamente o vendedor ao usuário autenticado.

                    Demais validações de agência, cliente e submissão pública permanecem no caso de uso / serviço de aplicação.""")
    QuotationResponse create(
            @Valid @RequestBody CreateQuotationRequest request,
            @AuthenticationPrincipal User caller);

    @PatchMapping("/{id}")
    @Operation(summary = "Atualiza parcialmente uma cotação")
    QuotationResponse patch(
            @Parameter(description = "id da cotação") @PathVariable UUID id,
            @RequestBody UpdateQuotationRequest request);

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Exclui cotação permanentemente",
            description = "Remove o registro da base de dados. Não há lixeira nem restauração.")
    void delete(@Parameter(description = "id da cotação") @PathVariable UUID id);
}
