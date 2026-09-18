package com.agenciahub.api.application.controller;

import com.agenciahub.api.security.TenantContext;
import com.agenciahub.api.application.persistence.repository.QuotationRepository;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/quotations/{id}/flight-history")
@PreAuthorize("hasAnyRole('AGENCY_OWNER','SALES_AGENT')")
public class QuotationFlightHistoryController {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final QuotationRepository quotations;
    public QuotationFlightHistoryController(JdbcTemplate jdbc, ObjectMapper mapper, QuotationRepository quotations) {
        this.jdbc = jdbc; this.mapper = mapper; this.quotations = quotations;
    }
    @GetMapping
    public List<JsonNode> history(@PathVariable UUID id) {
        UUID agency = TenantContext.requireAgencyId();
        quotations.findByIdAndAgency_Id(id, agency).orElseThrow(() -> new ResourceNotFoundException("Cotação não encontrada."));
        return jdbc.query("SELECT plan::text FROM quotation_flight_history WHERE quotation_id=? AND agency_id=? ORDER BY saved_at DESC LIMIT 50",
                (rs, row) -> {
                    try { return mapper.readTree(rs.getString(1)); }
                    catch (java.io.IOException e) { throw new IllegalStateException("Histórico indisponível."); }
                }, id, agency);
    }
}
