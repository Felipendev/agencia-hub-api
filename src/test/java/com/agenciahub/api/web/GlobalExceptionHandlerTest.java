package com.agenciahub.api.web;

import com.agenciahub.api.exception.MissingAgencyContextException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    @RestController
    static class ProbeController {
        @GetMapping("/probe/missing-agency")
        void missingAgency() {
            throw new MissingAgencyContextException("agência não definida no contexto da requisição");
        }
    }

    @Test
    void missingAgencyContext_returns403() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mvc.perform(get("/probe/missing-agency").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MISSING_AGENCY_CONTEXT"))
                .andExpect(jsonPath("$.message").value("agência não definida no contexto da requisição"));
    }
}
