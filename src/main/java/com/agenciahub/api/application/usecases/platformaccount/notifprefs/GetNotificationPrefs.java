package com.agenciahub.api.application.usecases.platformaccount.notifprefs;

import com.agenciahub.api.security.SecurityContextUsers;
import org.springframework.stereotype.Service;

@Service
public class GetNotificationPrefs {

    public NotificationPrefsDTO execute() {
        var user = SecurityContextUsers.requireUser();
        return new NotificationPrefsDTO(Boolean.TRUE.equals(user.getNotifEmailSubmissao()));
    }
}
