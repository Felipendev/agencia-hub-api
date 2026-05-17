package com.agenciahub.api.application.usecases.auth.verifyemailbylink;

import com.agenciahub.api.application.usecases.auth.verifyemail.VerifyEmailRequestDTO;
import com.agenciahub.api.application.usecases.auth.verifyemail.VerifyEmailResponseDTO;
import com.agenciahub.api.application.usecases.auth.verifyemail.VerifyEmailUseCase;
import com.agenciahub.api.security.VerificationLinkTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VerifyEmailByLink {

    private final VerificationLinkTokenService linkTokenService;
    private final VerifyEmailUseCase verifyEmailUseCase;

    public VerifyEmailResponseDTO execute(VerifyEmailByLinkRequestDTO request) {
        VerificationLinkTokenService.LinkPayload payload = linkTokenService.parse(request.token());
        return verifyEmailUseCase.execute(new VerifyEmailRequestDTO(payload.email(), payload.code()));
    }
}
