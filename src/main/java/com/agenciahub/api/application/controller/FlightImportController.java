package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.services.flights.FlightImportService;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.security.TenantContext;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/flight-imports")
@PreAuthorize("hasAnyRole('AGENCY_OWNER','SALES_AGENT')")
public class FlightImportController {
    private final FlightImportService service;
    public FlightImportController(FlightImportService service) { this.service = service; }
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FlightImportService.ImportResult extract(@RequestPart("file") MultipartFile file,
                                                    @AuthenticationPrincipal PlatformAccount caller) throws Exception {
        if (file.isEmpty() || file.getSize() > 4 * 1024 * 1024) throw new IllegalArgumentException("Envie um arquivo de até 4 MB.");
        return service.extract(TenantContext.requireAgencyId(), caller.getId(), file.getOriginalFilename(), file.getBytes());
    }
}
