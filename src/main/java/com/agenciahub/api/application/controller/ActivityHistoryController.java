package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.services.history.ActivityHistoryService;
import com.agenciahub.api.application.services.history.ActivityHistoryService.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/activity-history")
@PreAuthorize("hasAnyRole('AGENCY_OWNER','SALES_AGENT')")
@RequiredArgsConstructor
public class ActivityHistoryController {
    private final ActivityHistoryService service;
    @GetMapping
    public ResponseEntity<Page> list(@RequestParam Product product, @RequestParam(required=false) UUID customerId,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam Set<Source> modules, @RequestParam(defaultValue="0") int page) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.list(new Filters(product,customerId,from,to,modules),page));
    }
    @GetMapping("/export.csv")
    @PreAuthorize("hasRole('AGENCY_OWNER')")
    public ResponseEntity<byte[]> export(@RequestParam Product product, @RequestParam(required=false) UUID customerId,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to, @RequestParam Set<Source> modules) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType("text/csv; charset=utf-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"historico.csv\"")
                .header("X-Content-Type-Options","nosniff")
                .body(service.export(new Filters(product,customerId,from,to,modules)));
    }
}
