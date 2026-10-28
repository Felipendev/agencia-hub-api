package com.agenciahub.api.application.services.diagnostics;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.UUID;
@Service @RequiredArgsConstructor
public class DiagnosticHistory {
    private final JdbcTemplate jdbc;
    public boolean customer(UUID agency,UUID customer){return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM miles_diagnostics WHERE agency_id=? AND customer_id=?)",Boolean.class,agency,customer));}
    public boolean email(UUID agency,String email){return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM miles_diagnostics WHERE agency_id=? AND (lower(original_data->>'email')=lower(?) OR lower(data->>'email')=lower(?))) OR EXISTS(SELECT 1 FROM miles_diagnostic_events WHERE agency_id=? AND lower(snapshot#>>'{data,email}')=lower(?))",Boolean.class,agency,email,email,agency,email));}
}
