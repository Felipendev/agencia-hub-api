package com.agenciahub.api.application.usecases.datadeletion;

import com.agenciahub.api.application.integrations.email.EmailService;
import com.agenciahub.api.application.persistence.entity.CrmCustomer;
import com.agenciahub.api.application.persistence.entity.DataDeletionRequest;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.application.persistence.entity.SolicitacaoSubmission;
import com.agenciahub.api.application.persistence.repository.CrmCustomerRepository;
import com.agenciahub.api.application.persistence.repository.DataDeletionRequestRepository;
import com.agenciahub.api.application.persistence.repository.SolicitacaoSubmissionRepository;
import com.agenciahub.api.domain.DataDeletionStatus;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Processa uma solicitação de exclusão LGPD (art. 18, VI). A solicitação em si é global
 * (o titular só informa e-mail/telefone — pode ter dado em várias agências), mas a ação de um
 * {@code AGENCY_OWNER} só pode apagar/anonimizar os dados da <b>própria</b> agência: nunca os de
 * outra. Um dono cujo agência não tenha nenhum registro para o e-mail recebe 404, como se a
 * solicitação não existisse — evita confirmar a existência de dados de terceiros.
 *
 * <p>Limitação conhecida: {@code status} é um campo único e global na solicitação. Se o mesmo
 * e-mail existir em mais de uma agência, a primeira agência a processar marca a solicitação como
 * concluída/rejeitada e as demais não conseguem mais agir sobre ela (recebem "já processada").
 * Resolver isso exigiria modelar uma linha de processamento por agência; fora do escopo desta
 * correção, que elimina o vazamento/exclusão cruzada entre agências.</p>
 */
@Service
@RequiredArgsConstructor
public class ProcessDataDeletionRequest {

    private final DataDeletionRequestRepository repository;
    private final SolicitacaoSubmissionRepository submissionRepository;
    private final CrmCustomerRepository customerRepository;
    private final EmailService emailService;

    @Transactional
    public void execute(UUID requestId, ProcessDataDeletionRequestDTO dto, PlatformAccount processor) {
        UUID agencyId = TenantContext.requireAgencyId();
        DataDeletionRequest request = repository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("solicitação de exclusão não encontrada"));

        if (request.getStatus() != DataDeletionStatus.PENDING) {
            throw new IllegalStateException("solicitação já foi processada");
        }

        List<SolicitacaoSubmission> ownSubmissions =
                submissionRepository.findByEmailIgnoreCaseAndAgency_Id(request.getEmail(), agencyId);
        List<CrmCustomer> ownCustomers = customerRepository
                .findFirstByEmailIgnoreCaseAndAgency_Id(request.getEmail(), agencyId)
                .map(List::of)
                .orElseGet(List::of);

        if (ownSubmissions.isEmpty() && ownCustomers.isEmpty()) {
            // Este e-mail não tem dado nesta agência: não é um recurso desta agência.
            throw new ResourceNotFoundException("solicitação de exclusão não encontrada");
        }

        boolean excluir = "EXCLUIR".equals(dto.acao());

        if (excluir) {
            for (SolicitacaoSubmission s : ownSubmissions) {
                s.setEmail("");
                s.setNome("EXCLUÍDO");
                s.setTelefone("");
                s.setConsentimentoLgpd(false);
                submissionRepository.save(s);
            }
            for (CrmCustomer c : ownCustomers) {
                c.setEmail(null);
                c.setName("EXCLUÍDO");
                c.setPhone(null);
                customerRepository.save(c);
            }
        }

        request.setStatus(excluir ? DataDeletionStatus.PROCESSED : DataDeletionStatus.REJECTED);
        request.setProcessedAt(Instant.now());
        request.setProcessedBy(processor);
        repository.save(request);

        emailService.sendDataDeletionProcessed(request.getEmail(), excluir, dto.justificativa());
    }
}
