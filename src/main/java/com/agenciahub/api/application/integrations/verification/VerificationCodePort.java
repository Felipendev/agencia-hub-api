package com.agenciahub.api.application.integrations.verification;

import com.agenciahub.api.domain.VerificationCodeType;
import com.agenciahub.api.entity.User;

/**
 * Geração, persistência e validação de códigos de verificação (e-mail / reset de senha).
 * Implementação: {@link DefaultVerificationCodeService}.
 */
public interface VerificationCodePort {

    boolean generateAndSend(String email, VerificationCodeType type, User user, String userName);

    boolean verify(String email, String code, VerificationCodeType type);

    void invalidatePrevious(String email, VerificationCodeType type);
}
