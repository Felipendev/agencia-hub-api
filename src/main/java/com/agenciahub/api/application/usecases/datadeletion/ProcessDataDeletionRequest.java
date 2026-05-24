package com.agenciahub.api.application.usecases.datadeletion;

import com.agenciahub.api.application.integrations.email.EmailService;
import com.agenciahub.api.application.persistence.entity.DataDeletionRequest;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.application.persistence.entity.SolicitacaoSubmission;
import com.agenciahub.api.application.persistence.repository.CrmCustomerRepository;
import com.agenciahub.api.application.persistence.repository.DataDeletionRequestRepository;
import com.agenciahub.api.application.persistence.repository.SolicitacaoSubmissionRepository;
import com.agenciahub.api.domain.DataDeletionStatus;
import com.agenciahub.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcessDataDeletionRequest {

    private final DataDeletionRequestRepository repository;
    private final SolicitacaoSubmissionRepository submissionRepository;
    private final CrmCustomerRepository customerRepository;
    private final EmailService emailService;

    @Transactional
    public void execute(UUID requestId, ProcessDataDeletionRequestDTO dto, PlatformAccount processor) {
        DataDeletionRequest request = repository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("solicitação de exclusão não encontrada"));

        if (request.getStatus() != DataDeletionStatus.PENDING) {
            throw new IllegalStateException("solicitação já foi processada");
        }

        boolean excluir = "EXCLUIR".equals(dto.acao());

        if (excluir) {
            deleteSubmitterData(request.getEmail());
        }

        request.setStatus(excluir ? DataDeletionStatus.PROCESSED : DataDeletionStatus.REJECTED);
        request.setProcessedAt(Instant.now());
        request.setProcessedBy(processor);
        repository.save(request);

        emailService.sendDataDeletionProcessed(request.getEmail(), excluir, dto.justificativa());
    }

    private void deleteSubmitterData(String email) {
        List<SolicitacaoSubmission> submissions = submissionRepository.findByEmailIgnoreCase(email);
        for (SolicitacaoSubmission s : submissions) {
            s.setEmail("");
            s.setNome("EXCLUÍDO");
            s.setTelefone("");
            s.setConsentimentoLgpd(false);
            submissionRepository.save(s);
        }
        customerRepository.findAll().stream()
                .filter(c -> email.equalsIgnoreCase(c.getEmail()))
                .forEach(c -> {
                    c.setEmail("");
                    c.setName("EXCLUÍDO");
                    c.setPhone("");
                    customerRepository.save(c);
                });
    }
}
