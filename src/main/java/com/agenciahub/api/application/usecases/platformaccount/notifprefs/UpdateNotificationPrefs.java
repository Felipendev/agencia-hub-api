package com.agenciahub.api.application.usecases.platformaccount.notifprefs;

import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import com.agenciahub.api.security.SecurityContextUsers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateNotificationPrefs {

    private final PlatformAccountRepository userRepository;

    @Transactional
    public NotificationPrefsDTO execute(NotificationPrefsDTO patch) {
        var user = SecurityContextUsers.requireUser();
        user.setNotifEmailSubmissao(patch.notifEmailSubmissao());
        userRepository.save(user);
        return new NotificationPrefsDTO(user.getNotifEmailSubmissao());
    }
}
