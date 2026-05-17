package com.agenciahub.api.application.controller;

import com.agenciahub.api.application.usecases.platformaccount.notifprefs.GetNotificationPrefs;
import com.agenciahub.api.application.usecases.platformaccount.notifprefs.NotificationPrefsDTO;
import com.agenciahub.api.application.usecases.platformaccount.notifprefs.UpdateNotificationPrefs;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profile/notification-prefs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('AGENCY_OWNER','SALES_AGENT')")
public class NotificationPrefsController {

    private final GetNotificationPrefs getNotificationPrefs;
    private final UpdateNotificationPrefs updateNotificationPrefs;

    @GetMapping
    public NotificationPrefsDTO get() {
        return getNotificationPrefs.execute();
    }

    @PutMapping
    public NotificationPrefsDTO update(@RequestBody NotificationPrefsDTO body) {
        return updateNotificationPrefs.execute(body);
    }
}
