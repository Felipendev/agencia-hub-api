package com.agenciahub.api.controller;

import com.agenciahub.api.dto.solicitacao.PublicSolicitacaoSubmitRequest;
import com.agenciahub.api.dto.solicitacao.PublicSolicitacaoSubmitResponse;
import com.agenciahub.api.service.SolicitacaoSubmissionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = PublicSolicitacaoSubmitController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
@Import(GlobalExceptionHandler.class)
class PublicSolicitacaoSubmitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SolicitacaoSubmissionService submissionService;

    @Test
    void submit_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(submissionService.submit(any(PublicSolicitacaoSubmitRequest.class)))
                .thenReturn(new PublicSolicitacaoSubmitResponse(true, id));

        mockMvc.perform(post("/public/solicitacao/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "slug": "demo",
                                  "nome": "Maria",
                                  "email": "m@test.com",
                                  "telefone": "11987654321",
                                  "detalhes": { "origem": "SP", "destinosTrechos": ["SP — RJ"] },
                                  "observacoes": ""
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void submit_withSellerPublicCode_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(submissionService.submit(any(PublicSolicitacaoSubmitRequest.class)))
                .thenReturn(new PublicSolicitacaoSubmitResponse(true, id));

        mockMvc.perform(post("/public/solicitacao/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "slug": "demo",
                                  "nome": "Maria",
                                  "email": "m@test.com",
                                  "telefone": "11987654321",
                                  "detalhes": { "origem": "SP", "destinosTrechos": ["SP — RJ"] },
                                  "observacoes": "",
                                  "sellerPublicCode": "abc123xyz456"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void submit_invalidBody_returns400() throws Exception {
        mockMvc.perform(post("/public/solicitacao/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
