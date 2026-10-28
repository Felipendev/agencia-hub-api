package com.agenciahub.api.application.controller;
import com.agenciahub.api.application.services.diagnostics.DiagnosticService;
import com.agenciahub.api.application.services.diagnostics.DiagnosticDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequiredArgsConstructor @RequestMapping("/public/miles-diagnostics/{token}")
public class PublicMilesDiagnosticController {
    private final DiagnosticService service;
    @GetMapping public PublicConfig config(@PathVariable UUID token){return service.publicConfig(token);}
    @PostMapping @ResponseStatus(HttpStatus.ACCEPTED)
    public void submit(@PathVariable UUID token,@RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody Submission input){service.submit(token,key,input);}
}
