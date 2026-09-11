package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.usecases.notifications.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/agency/checkin-notifications")
@PreAuthorize("hasRole('AGENCY_OWNER')")
@RequiredArgsConstructor
public class CheckinNotificationController {
    private final CheckinNotificationService service;

    @GetMapping("/preferences") public CheckinPreferences preferences() { return service.preferences(); }
    @PutMapping("/preferences") public CheckinPreferences save(@Valid @RequestBody CheckinPreferences prefs) { return service.save(prefs); }
    @GetMapping public List<CheckinNotificationService.Notification> inbox() { return service.inbox(); }
    @PutMapping("/{id}/read") public void read(@PathVariable UUID id) { service.read(id); }
    @PutMapping("/read-all") public void readAll() { service.readAll(); }
    @DeleteMapping public void dismissAll() { service.dismissAll(); }
}
