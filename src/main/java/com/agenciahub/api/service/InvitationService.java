package com.agenciahub.api.service;

import com.agenciahub.api.domain.InvitationStatus;
import com.agenciahub.api.entity.Invitation;
import com.agenciahub.api.entity.User;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.repository.InvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final EmailService emailService;

    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    /**
     * Creates a new invitation, generates a UUID token, sets 72h expiration,
     * saves the invitation, sends email, and returns the invitation with invite URL.
     */
    @Transactional
    public Invitation createInvitation(String email, User inviter) {
        String token = UUID.randomUUID().toString();
        String inviteUrl = baseUrl + "/convite/" + token;

        Invitation invitation = Invitation.builder()
                .agency(inviter.getAgency())
                .invitedBy(inviter)
                .email(email.trim().toLowerCase())
                .token(token)
                .status(InvitationStatus.PENDING)
                .expiresAt(Instant.now().plus(72, ChronoUnit.HOURS))
                .build();

        invitation = invitationRepository.save(invitation);

        emailService.sendInvitation(
                email.trim().toLowerCase(),
                inviteUrl,
                inviter.getAgency().getName(),
                inviter.getName()
        );

        return invitation;
    }

    /**
     * Validates a token: checks existence, expiration, and status (PENDING).
     * Returns the invitation or throws an appropriate error.
     */
    @Transactional(readOnly = true)
    public Invitation validateToken(String token) {
        Invitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Convite não encontrado"));

        if (invitation.getStatus() == InvitationStatus.ACCEPTED) {
            throw new IllegalStateException("Este convite já foi utilizado");
        }

        if (invitation.getStatus() == InvitationStatus.REVOKED) {
            throw new IllegalStateException("Este convite foi cancelado");
        }

        if (Instant.now().isAfter(invitation.getExpiresAt())) {
            throw new IllegalStateException("Este convite expirou");
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalStateException("Este convite não está mais disponível");
        }

        return invitation;
    }

    /**
     * Returns all invitations for the given agency, ordered by createdAt desc.
     */
    @Transactional(readOnly = true)
    public List<Invitation> listByAgency(UUID agencyId) {
        return invitationRepository.findByAgency_IdOrderByCreatedAtDesc(agencyId);
    }

    /**
     * Revokes a pending invitation by setting its status to REVOKED.
     */
    @Transactional
    public void revoke(UUID invitationId, UUID agencyId) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Convite não encontrado"));

        if (!invitation.getAgency().getId().equals(agencyId)) {
            throw new ResourceNotFoundException("Convite não encontrado");
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalArgumentException("Apenas convites pendentes podem ser revogados");
        }

        invitation.setStatus(InvitationStatus.REVOKED);
        invitationRepository.save(invitation);
    }

    /**
     * Builds the invite URL for a given token.
     */
    public String buildInviteUrl(String token) {
        return baseUrl + "/convite/" + token;
    }
}
