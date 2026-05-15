package com.agenciahub.api.application.usecases.invitation.createinvitation;

import com.agenciahub.api.application.usecases.invitation.InvitationResponseMapper;
import com.agenciahub.api.application.usecases.invitation.InvitationLinkBuilder;
import com.agenciahub.api.domain.InvitationStatus;
import com.agenciahub.api.dto.invitation.InvitationResponse;
import com.agenciahub.api.entity.Invitation;
import com.agenciahub.api.entity.User;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.InvitationRepository;
import com.agenciahub.api.repository.UserRepository;
import com.agenciahub.api.application.integrations.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateInvitation implements CreateInvitationUseCase {

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final InvitationLinkBuilder invitationLinkBuilder;
    private final InvitationResponseMapper invitationResponseMapper;

    @Override
    @Transactional
    public InvitationResponse execute(CreateInvitationCommand command) {
        User inviter = command.inviter();
        User managedInviter = userRepository
                .findById(inviter.getId())
                .orElseThrow(() -> new ResourceNotFoundException("usuário não encontrado: " + inviter.getId()));

        String token = UUID.randomUUID().toString();
        String inviteUrl = invitationLinkBuilder.buildInviteUrl(token);

        Invitation invitation = Invitation.builder()
                .agency(managedInviter.getAgency())
                .invitedBy(managedInviter)
                .email(command.request().email().trim().toLowerCase())
                .token(token)
                .status(InvitationStatus.PENDING)
                .expiresAt(Instant.now().plus(72, ChronoUnit.HOURS))
                .build();

        invitation = invitationRepository.save(invitation);

        emailService.sendInvitation(
                command.request().email().trim().toLowerCase(),
                inviteUrl,
                managedInviter.getAgency().getName(),
                managedInviter.getName());

        return invitationResponseMapper.toResponse(invitation);
    }
}
