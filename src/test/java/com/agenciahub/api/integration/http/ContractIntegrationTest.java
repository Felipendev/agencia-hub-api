package com.agenciahub.api.integration.http;

import com.agenciahub.api.application.persistence.entity.Agency;
import com.agenciahub.api.application.persistence.entity.CrmCustomer;
import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.application.persistence.repository.AgencyRepository;
import com.agenciahub.api.application.persistence.repository.CrmCustomerRepository;
import com.agenciahub.api.application.persistence.repository.PlatformAccountRepository;
import com.agenciahub.api.domain.AgencyStatus;
import com.agenciahub.api.domain.CustomerStatus;
import com.agenciahub.api.domain.SubscriptionStatus;
import com.agenciahub.api.domain.enums.AccountKind;
import com.agenciahub.api.support.AbstractIntegrationTest;
import com.agenciahub.api.support.IntegrationHttpSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** F02 — ciclo completo de contratos com banco embarcado e Flyway. */
class ContractIntegrationTest extends AbstractIntegrationTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired AgencyRepository agencies;
    @Autowired CrmCustomerRepository customers;
    @Autowired PlatformAccountRepository accounts;
    @Autowired PasswordEncoder passwords;

    private static final String BODY = """
            CONTRATO DE ASSESSORIA
            Contratante: {{cliente.nome}}, CPF/CNPJ {{cliente.cpfCnpj}}.
            Contratada: {{agencia.nome}}.
            Plano: {{contrato.plano}} — valor {{contrato.valor}}, {{contrato.formaPagamento}}.
            Vigência a partir de {{contrato.vigenciaInicio}}. Emitido em {{contrato.dataEmissao}}.""";

    private final String today = LocalDate.now(ZoneId.of("America/Fortaleza")).toString();

    // ------------------------------------------------------------------ apoio

    private MockHttpServletRequestBuilder as(String jwt, MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", "Bearer " + jwt);
    }

    private JsonNode json(String jwt, MockHttpServletRequestBuilder builder, int expectedStatus) throws Exception {
        String body = mockMvc.perform(as(jwt, builder)).andExpect(status().is(expectedStatus)).andReturn().getResponse().getContentAsString();
        return body.isEmpty() ? null : http.parse(body);
    }

    private static MockHttpServletRequestBuilder jsonBody(MockHttpServletRequestBuilder builder, String body) {
        return builder.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private String customer(String jwt, String name) throws Exception {
        String body = "{\"name\":\"%s %s\",\"status\":\"ACTIVE\",\"profileData\":{\"documentoCpfCnpj\":\"123.456.789-00\"}}".formatted(name, UUID.randomUUID());
        return json(jwt, jsonBody(post("/customers"), body), 201).path("id").asText();
    }

    private JsonNode template(String jwt, String body) throws Exception {
        return template(jwt, body, "TRAVEL");
    }

    private JsonNode template(String jwt, String body, String product) throws Exception {
        String payload = "{\"product\":\"%s\",\"name\":\"Assessoria %s\",\"description\":\"Modelo da agência\",\"body\":%s}"
                .formatted(product, UUID.randomUUID(), quote(body));
        return json(jwt, jsonBody(post("/contract-templates"), payload), 201);
    }

    private static String quote(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    private static String draft(String customerId, String templateId, String extra) {
        return draft("TRAVEL", customerId, templateId, extra);
    }

    private static String draft(String product, String customerId, String templateId, String extra) {
        return "{\"product\":\"%s\",\"customerId\":\"%s\",\"templateId\":\"%s\",\"title\":\"Assessoria anual\"%s}"
                .formatted(product, customerId, templateId, extra);
    }

    private static final String COMPLETE = ",\"planName\":\"Plano Ouro\",\"price\":1200.00,\"startDate\":\"2026-11-01\",\"paymentMode\":\"INSTALLMENTS\",\"installments\":3";

    private JsonNode createDraft(String jwt, String customerId, String templateId, String extra) throws Exception {
        return json(jwt, jsonBody(post("/contracts").header("Idempotency-Key", UUID.randomUUID()), draft(customerId, templateId, extra)), 201);
    }

    private String member(AccountKind kind) throws Exception {
        String owner = http.loginOwner(mockMvc);
        Agency agency = customers.findById(UUID.fromString(customer(owner, "Equipe"))).orElseThrow().getAgency();
        String email = "contract-" + kind.name().toLowerCase() + "-" + UUID.randomUUID() + "@test.local";
        accounts.save(PlatformAccount.builder().agency(agency).name("Pessoa da equipe").email(email)
                .passwordHash(passwords.encode("contract-test-password")).accountKind(kind)
                .active(true).emailVerified(true).termsAccepted(true).build());
        String login = mockMvc.perform(jsonBody(post("/auth/login"), "{\"email\":\"%s\",\"password\":\"contract-test-password\"}".formatted(email)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return http.parse(login).path("token").asText();
    }

    private long count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
    }

    // ------------------------------------------------------------------ modelos

    @Test
    void templateCrudKeepsImmutableVersionsAndArchivesWhenUsed() throws Exception {
        String jwt = http.loginOwner(mockMvc);
        json(jwt, get("/contract-templates/fields"), 200);
        json(jwt, jsonBody(post("/contract-templates"), "{\"product\":\"TRAVEL\",\"name\":\"Inválido\",\"body\":\"Olá {{cliente.apelido}}\"}"), 400);
        json(jwt, jsonBody(post("/contract-templates"), "{\"name\":\"Sem produto\",\"body\":\"Texto\"}"), 400);

        JsonNode created = template(jwt, BODY);
        String id = created.path("template").path("id").asText();
        assertThat(created.path("template").path("currentVersion").asInt()).isEqualTo(1);
        assertThat(created.path("usedFields").toString()).contains("cliente.nome", "contrato.valor");

        String rename = "{\"name\":\"Assessoria renomeada\",\"body\":%s,\"expectedVersion\":1}".formatted(quote(BODY));
        JsonNode renamed = json(jwt, jsonBody(patch("/contract-templates/" + id), rename), 200);
        assertThat(renamed.path("template").path("id").asText()).isEqualTo(id);
        assertThat(renamed.path("template").path("currentVersion").asInt()).isEqualTo(1);

        String changed = "{\"name\":\"Assessoria renomeada\",\"body\":%s,\"expectedVersion\":2}".formatted(quote(BODY + "\nCláusula nova."));
        JsonNode versioned = json(jwt, jsonBody(patch("/contract-templates/" + id), changed), 200);
        assertThat(versioned.path("template").path("currentVersion").asInt()).isEqualTo(2);
        assertThat(versioned.path("versions").size()).isEqualTo(2);
        assertThat(json(jwt, get("/contract-templates/" + id + "/versions/1"), 200).path("body").asText()).isEqualTo(BODY);

        // Versão desatualizada é recusada sem sobrescrever.
        json(jwt, jsonBody(patch("/contract-templates/" + id), changed), 409);

        // Modelo usado é arquivado, não excluído; some da lista padrão e pode ser reativado.
        createDraft(jwt, customer(jwt, "Pessoa"), id, "");
        JsonNode archived = json(jwt, delete("/contract-templates/" + id).param("expectedVersion", "3"), 200);
        assertThat(archived.path("outcome").asText()).isEqualTo("ARCHIVED");
        assertThat(json(jwt, get("/contract-templates"), 200).toString()).doesNotContain(id);
        assertThat(json(jwt, get("/contract-templates").param("includeArchived", "true"), 200).toString()).contains(id);
        json(jwt, jsonBody(post("/contracts").header("Idempotency-Key", UUID.randomUUID()), draft(customer(jwt, "Outra"), id, "")), 409);
        json(jwt, post("/contract-templates/" + id + "/restore").param("expectedVersion", "4"), 200);

        // Modelo sem uso é excluído de verdade e registrado na auditoria.
        String unused = template(jwt, "Texto simples").path("template").path("id").asText();
        assertThat(json(jwt, delete("/contract-templates/" + unused).param("expectedVersion", "1"), 200).path("outcome").asText()).isEqualTo("DELETED");
        json(jwt, get("/contract-templates/" + unused), 404);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM contract_audit WHERE resource_id = ?::uuid AND action = 'TEMPLATE_DELETED'", Long.class, unused)).isEqualTo(1);
    }

    // ------------------------------------------------------------------ ciclo do contrato

    @Test
    void lifecycleFreezesIssuedDocumentAndRequiresEvidenceWithoutFinancialSideEffects() throws Exception {
        String jwt = http.loginOwner(mockMvc);
        long sales = count("sales"), receivables = count("receivables"), entries = count("financial_entries");
        String person = customer(jwt, "Ana");
        String templateId = template(jwt, BODY).path("template").path("id").asText();

        UUID key = UUID.randomUUID();
        String payload = draft(person, templateId, "");
        JsonNode created = json(jwt, jsonBody(post("/contracts").header("Idempotency-Key", key), payload), 201);
        JsonNode again = json(jwt, jsonBody(post("/contracts").header("Idempotency-Key", key), payload), 201);
        String id = created.path("contract").path("id").asText();
        assertThat(again.path("contract").path("id").asText()).isEqualTo(id).isEqualTo(key.toString());
        assertThat(created.path("missingFields").toString()).contains("contrato.plano", "contrato.valor", "contrato.vigenciaInicio")
                .doesNotContain("cliente.nome", "cliente.cpfCnpj");

        // Campos faltantes bloqueiam a geração e são devolvidos para destaque.
        JsonNode incomplete = json(jwt, jsonBody(post("/contracts/" + id + "/documents"), "{\"expectedVersion\":1}"), 422);
        assertThat(incomplete.path("code").asText()).isEqualTo("CONTRACT_INCOMPLETE");
        assertThat(incomplete.path("missingFields").size()).isGreaterThanOrEqualTo(3);

        JsonNode filled = json(jwt, jsonBody(patch("/contracts/" + id), draft(person, templateId, COMPLETE + ",\"expectedVersion\":1")), 200);
        assertThat(filled.path("contract").path("id").asText()).isEqualTo(id);
        assertThat(filled.path("contract").path("rowVersion").asInt()).isEqualTo(2);
        assertThat(filled.path("missingFields").size()).isZero();
        assertThat(json(jwt, get("/contracts/" + id + "/preview"), 200).path("content").asText()).contains("R$ 1.200,00", "Parcelado em 3 parcelas");

        JsonNode document = json(jwt, jsonBody(post("/contracts/" + id + "/documents"), "{\"expectedVersion\":2}"), 201);
        assertThat(document.path("version").asInt()).isEqualTo(1);
        assertThat(json(jwt, jsonBody(post("/contracts/" + id + "/documents"), "{\"expectedVersion\":2}"), 201).path("version").asInt()).isEqualTo(1);

        // Alterar a pessoa depois não muda o documento emitido.
        String originalName = json(jwt, get("/contracts/" + id), 200).path("contract").path("customerName").asText();
        json(jwt, jsonBody(patch("/customers/" + person), "{\"name\":\"Nome alterado depois\"}"), 200);
        JsonNode issued = json(jwt, get("/contracts/" + id + "/documents/1"), 200);
        assertThat(issued.path("renderedContent").asText()).contains(originalName).doesNotContain("Nome alterado depois");
        assertThat(issued.path("dataSnapshot").path("fields").path("cliente.nome").asText()).isEqualTo(originalName);
        assertThat(issued.path("dataSnapshot").path("templateVersion").asInt()).isEqualTo(1);

        byte[] pdf = mockMvc.perform(as(jwt, get("/contracts/" + id + "/documents/1/pdf")))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();
        try (var loaded = Loader.loadPDF(pdf)) {
            assertThat(new PDFTextStripper().getText(loaded)).contains(originalName).contains("Documento v1");
        }

        // Envio exige data válida; enviado não é editável nem excluível.
        json(jwt, jsonBody(post("/contracts/" + id + "/send"), "{\"expectedVersion\":2,\"sentOn\":\"2999-01-01\"}"), 400);
        JsonNode sent = json(jwt, jsonBody(post("/contracts/" + id + "/send"), "{\"expectedVersion\":2,\"sentOn\":\"%s\"}".formatted(today)), 200);
        assertThat(sent.path("contract").path("status").asText()).isEqualTo("SENT");
        json(jwt, jsonBody(patch("/contracts/" + id), draft(person, templateId, COMPLETE + ",\"expectedVersion\":3")), 409);
        json(jwt, delete("/contracts/" + id).param("expectedVersion", "3"), 409);

        // Assinatura exige evidência anexada.
        mockMvc.perform(as(jwt, multipart("/contracts/" + id + "/sign").file(new MockMultipartFile("file", "vazio.pdf", "application/pdf", new byte[0]))
                .param("expectedVersion", "3").param("signedOn", today))).andExpect(status().isBadRequest());
        mockMvc.perform(as(jwt, multipart("/contracts/" + id + "/sign").file(new MockMultipartFile("file", "x.exe", "application/x-msdownload", new byte[]{1}))
                .param("expectedVersion", "3").param("signedOn", today))).andExpect(status().isBadRequest());
        assertThat(json(jwt, get("/contracts/" + id), 200).path("contract").path("status").asText()).isEqualTo("SENT");
        String signed = mockMvc.perform(as(jwt, multipart("/contracts/" + id + "/sign")
                        .file(new MockMultipartFile("file", "assinado.pdf", "application/pdf", "%PDF-1.4 assinado".getBytes()))
                        .param("expectedVersion", "3").param("signedOn", today).param("note", "Assinado em papel")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode signedDetail = http.parse(signed);
        assertThat(signedDetail.path("contract").path("status").asText()).isEqualTo("SIGNED");
        JsonNode evidence = signedDetail.path("attachments").get(0);
        assertThat(evidence.path("kind").asText()).isEqualTo("SIGNED_EVIDENCE");
        assertThat(evidence.path("documentVersion").asInt()).isEqualTo(1);
        json(jwt, delete("/contracts/" + id + "/attachments/" + evidence.path("id").asText()), 409);
        mockMvc.perform(as(jwt, get("/contracts/" + id + "/attachments/" + evidence.path("id").asText())))
                .andExpect(status().isOk()).andExpect(header().string("X-Content-Type-Options", "nosniff"));

        // Cancelamento preserva documento e evidência.
        json(jwt, jsonBody(post("/contracts/" + id + "/cancel"), "{\"expectedVersion\":4,\"reason\":\"\"}"), 400);
        JsonNode cancelled = json(jwt, jsonBody(post("/contracts/" + id + "/cancel"), "{\"expectedVersion\":4,\"reason\":\"Cliente desistiu\"}"), 200);
        assertThat(cancelled.path("contract").path("status").asText()).isEqualTo("CANCELLED");
        assertThat(cancelled.path("cancelReason").asText()).isEqualTo("Cliente desistiu");
        assertThat(cancelled.path("documents").size()).isEqualTo(1);
        assertThat(cancelled.path("attachments").size()).isEqualTo(1);
        assertThat(cancelled.path("history").toString()).contains("CONTRACT_CREATED", "CONTRACT_UPDATED", "DOCUMENT_GENERATED",
                "CONTRACT_SENT", "CONTRACT_SIGNED", "CONTRACT_CANCELLED");
        assertThat(cancelled.path("history").toString()).doesNotContain("password", "Bearer");

        assertThat(count("sales")).isEqualTo(sales);
        assertThat(count("receivables")).isEqualTo(receivables);
        assertThat(count("financial_entries")).isEqualTo(entries);
    }

    @Test
    void revisionReopensSentContractAndKeepsPreviousDocument() throws Exception {
        String jwt = http.loginOwner(mockMvc);
        String person = customer(jwt, "Revisão");
        String templateId = template(jwt, BODY).path("template").path("id").asText();
        String id = createDraft(jwt, person, templateId, COMPLETE).path("contract").path("id").asText();
        json(jwt, jsonBody(post("/contracts/" + id + "/documents"), "{\"expectedVersion\":1}"), 201);
        json(jwt, jsonBody(post("/contracts/" + id + "/send"), "{\"expectedVersion\":1,\"sentOn\":\"%s\"}".formatted(today)), 200);
        json(jwt, jsonBody(post("/contracts/" + id + "/revise"), "{\"expectedVersion\":2,\"reason\":\"Ajustar valor\"}"), 200);

        json(jwt, jsonBody(patch("/contracts/" + id), draft(person, templateId, COMPLETE.replace("1200.00", "1500.00") + ",\"expectedVersion\":3")), 200);
        JsonNode stale = json(jwt, jsonBody(post("/contracts/" + id + "/send"), "{\"expectedVersion\":4,\"sentOn\":\"%s\"}".formatted(today)), 409);
        assertThat(stale.path("code").asText()).isEqualTo("CONTRACT_CONFLICT");
        assertThat(json(jwt, jsonBody(post("/contracts/" + id + "/documents"), "{\"expectedVersion\":4}"), 201).path("version").asInt()).isEqualTo(2);
        JsonNode resent = json(jwt, jsonBody(post("/contracts/" + id + "/send"), "{\"expectedVersion\":4,\"sentOn\":\"%s\"}".formatted(today)), 200);
        assertThat(resent.path("documents").size()).isEqualTo(2);
        assertThat(json(jwt, get("/contracts/" + id + "/documents/1"), 200).path("renderedContent").asText()).contains("R$ 1.200,00");
        assertThat(json(jwt, get("/contracts/" + id + "/documents/2"), 200).path("renderedContent").asText()).contains("R$ 1.500,00");
        assertThat(resent.path("history").toString()).contains("REVISION_STARTED", "Ajustar valor");
    }

    @Test
    void draftsAreEditableAndDeletableWhileIssuedOnesRequireCancellation() throws Exception {
        String jwt = http.loginOwner(mockMvc);
        String person = customer(jwt, "Rascunho");
        String templateId = template(jwt, BODY).path("template").path("id").asText();
        UUID key = UUID.randomUUID();
        String payload = draft(person, templateId, "");
        json(jwt, jsonBody(post("/contracts").header("Idempotency-Key", key), payload), 201);
        json(jwt, jsonBody(patch("/contracts/" + key), draft(person, templateId, ",\"expectedVersion\":0")), 409);
        json(jwt, jsonBody(patch("/contracts/" + key), draft(customer(jwt, "Outra"), templateId, ",\"expectedVersion\":1")), 400);
        json(jwt, jsonBody(patch("/contracts/" + key), draft(person, templateId, ",\"endDate\":\"2026-01-01\",\"startDate\":\"2026-02-01\",\"expectedVersion\":1")), 400);
        json(jwt, jsonBody(patch("/contracts/" + key), draft(person, templateId, ",\"paymentMode\":\"INSTALLMENTS\",\"installments\":1,\"expectedVersion\":1")), 400);

        mockMvc.perform(as(jwt, multipart("/contracts/" + key + "/attachments")
                .file(new MockMultipartFile("file", "proposta.pdf", "application/pdf", "%PDF proposta".getBytes()))
                .param("description", "Proposta"))).andExpect(status().isCreated());
        json(jwt, delete("/contracts/" + key).param("expectedVersion", "1"), 204);
        json(jwt, get("/contracts/" + key), 404);
        json(jwt, jsonBody(post("/contracts").header("Idempotency-Key", key), payload), 409);
        assertThat(json(jwt, get("/contracts").param("customerId", person), 200).size()).isZero();

        String issued = createDraft(jwt, person, templateId, COMPLETE).path("contract").path("id").asText();
        json(jwt, jsonBody(post("/contracts/" + issued + "/documents"), "{\"expectedVersion\":1}"), 201);
        json(jwt, delete("/contracts/" + issued).param("expectedVersion", "1"), 409);
        assertThat(json(jwt, get("/contracts").param("customerId", person).param("status", "DRAFT"), 200).size()).isEqualTo(1);
    }

    @Test
    void concurrentGenerationCreatesASingleDocumentVersion() throws Exception {
        String jwt = http.loginOwner(mockMvc);
        String id = createDraft(jwt, customer(jwt, "Concorrência"), template(jwt, BODY).path("template").path("id").asText(), COMPLETE)
                .path("contract").path("id").asText();
        var executor = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Integer>> calls = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                calls.add(() -> mockMvc.perform(as(jwt, jsonBody(post("/contracts/" + id + "/documents"), "{\"expectedVersion\":1}")))
                        .andReturn().getResponse().getStatus());
            }
            var results = executor.invokeAll(calls);
            assertThat(List.of(results.get(0).get(), results.get(1).get())).containsExactly(201, 201);
        } finally {
            executor.shutdownNow();
        }
        assertThat(json(jwt, get("/contracts/" + id), 200).path("documents").size()).isEqualTo(1);
    }

    @Test
    void travelAndMilesContractsOfTheSamePersonAreIndependent() throws Exception {
        String jwt = http.loginOwner(mockMvc);
        String person = customer(jwt, "Dois produtos");
        String travelTemplate = template(jwt, BODY + " Produto: {{contrato.produto}}.", "TRAVEL").path("template").path("id").asText();
        JsonNode milesCreated = template(jwt, BODY + " Produto: {{contrato.produto}}.", "MILES");
        String milesTemplate = milesCreated.path("template").path("id").asText();
        assertThat(milesCreated.path("template").path("product").asText()).isEqualTo("MILES");

        // Produto do modelo é fixo; contrato não usa modelo de outro produto.
        String changeProduct = "{\"product\":\"TRAVEL\",\"name\":\"Milhas\",\"body\":%s,\"expectedVersion\":1}".formatted(quote(BODY));
        json(jwt, jsonBody(patch("/contract-templates/" + milesTemplate), changeProduct), 400);
        json(jwt, jsonBody(post("/contracts").header("Idempotency-Key", UUID.randomUUID()), draft("TRAVEL", person, milesTemplate, COMPLETE)), 400);
        json(jwt, jsonBody(post("/contracts").header("Idempotency-Key", UUID.randomUUID()), draft(person, travelTemplate, COMPLETE).replace("\"product\":\"TRAVEL\",", "")), 400);

        String travel = createDraft(jwt, person, travelTemplate, COMPLETE).path("contract").path("id").asText();
        JsonNode miles = json(jwt, jsonBody(post("/contracts").header("Idempotency-Key", UUID.randomUUID()), draft("MILES", person, milesTemplate, COMPLETE)), 201);
        String milesId = miles.path("contract").path("id").asText();
        assertThat(miles.path("contract").path("product").asText()).isEqualTo("MILES");
        json(jwt, jsonBody(patch("/contracts/" + milesId), draft("TRAVEL", person, milesTemplate, COMPLETE + ",\"expectedVersion\":1")), 400);

        assertThat(json(jwt, get("/contracts").param("customerId", person).param("product", "MILES"), 200).toString()).contains(milesId).doesNotContain(travel);
        assertThat(json(jwt, get("/contracts").param("customerId", person).param("product", "TRAVEL"), 200).toString()).contains(travel).doesNotContain(milesId);
        assertThat(json(jwt, get("/contract-templates").param("product", "MILES"), 200).toString()).contains(milesTemplate).doesNotContain(travelTemplate);

        JsonNode document = json(jwt, jsonBody(post("/contracts/" + milesId + "/documents"), "{\"expectedVersion\":1}"), 201);
        assertThat(document.path("product").asText()).isEqualTo("MILES");
        JsonNode issued = json(jwt, get("/contracts/" + milesId + "/documents/1"), 200);
        assertThat(issued.path("renderedContent").asText()).contains("Produto: Milhas.");
        assertThat(issued.path("dataSnapshot").path("product").asText()).isEqualTo("MILES");

        json(jwt, jsonBody(post("/contracts/" + milesId + "/cancel"), "{\"expectedVersion\":1,\"reason\":\"Encerrou acompanhamento de milhas\"}"), 200);
        assertThat(json(jwt, get("/contracts/" + travel), 200).path("contract").path("status").asText()).isEqualTo("DRAFT");
        json(jwt, get("/customers/" + person), 200);
    }

    // ------------------------------------------------------------------ recebíveis, perfis e agências

    @Test
    void ownerLinksOnlyExistingReceivablesOfTheSamePerson() throws Exception {
        String jwt = http.loginOwner(mockMvc);
        String person = customer(jwt, "Recebível");
        String sale = "{\"customerId\":\"%s\",\"totalAmount\":300.00,\"saleDate\":\"%s\",\"receivables\":[{\"number\":1,\"amount\":300.00,\"dueDate\":\"%s\"}]}";
        json(jwt, jsonBody(post("/sales"), sale.formatted(person, today, today)), 201);
        String other = customer(jwt, "Outra pessoa");
        json(jwt, jsonBody(post("/sales"), sale.formatted(other, today, today)), 201);
        String receivableId = jdbc.queryForObject("SELECT r.id::text FROM receivables r JOIN sales s ON s.id = r.sale_id WHERE s.customer_id = ?::uuid", String.class, person);
        String foreignReceivable = jdbc.queryForObject("SELECT r.id::text FROM receivables r JOIN sales s ON s.id = r.sale_id WHERE s.customer_id = ?::uuid", String.class, other);
        long receivables = count("receivables"), entries = count("financial_entries");

        String id = createDraft(jwt, person, template(jwt, BODY).path("template").path("id").asText(), COMPLETE).path("contract").path("id").asText();
        json(jwt, put("/contracts/" + id + "/receivables/" + foreignReceivable), 404);
        json(jwt, put("/contracts/" + id + "/receivables/" + receivableId), 200);
        JsonNode linked = json(jwt, put("/contracts/" + id + "/receivables/" + receivableId), 200);
        assertThat(linked.path("receivables").size()).isEqualTo(1);
        assertThat(linked.path("receivables").get(0).path("amount").decimalValue()).isEqualByComparingTo("300.00");
        assertThat(json(jwt, delete("/contracts/" + id + "/receivables/" + receivableId), 200).path("receivables").size()).isZero();
        assertThat(count("receivables")).isEqualTo(receivables);
        assertThat(count("financial_entries")).isEqualTo(entries);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM contract_audit WHERE resource_id = ?::uuid AND action IN ('RECEIVABLE_LINKED','RECEIVABLE_UNLINKED')", Long.class, id)).isEqualTo(2);
    }

    @Test
    void salesAgentUsesContractsButCannotManageTemplatesFinancialLinksOrCancelIssued() throws Exception {
        String owner = http.loginOwner(mockMvc);
        String agent = member(AccountKind.SALES_AGENT);
        String person = customer(owner, "Equipe");
        String templateId = template(owner, BODY).path("template").path("id").asText();

        json(agent, jsonBody(post("/contract-templates"), "{\"product\":\"TRAVEL\",\"name\":\"Agente\",\"body\":\"Texto\"}"), 403);
        json(agent, delete("/contract-templates/" + templateId).param("expectedVersion", "1"), 403);
        assertThat(json(agent, get("/contract-templates"), 200).toString()).contains(templateId);

        String id = createDraft(agent, person, templateId, COMPLETE).path("contract").path("id").asText();
        JsonNode detail = json(agent, get("/contracts/" + id), 200);
        assertThat(detail.path("receivables").isNull()).isTrue();
        assertThat(detail.path("history").get(0).path("author").asText()).contains("@test.local");
        json(agent, put("/contracts/" + id + "/receivables/" + UUID.randomUUID()), 403);
        json(agent, jsonBody(post("/contracts/" + id + "/documents"), "{\"expectedVersion\":1}"), 201);
        json(agent, jsonBody(post("/contracts/" + id + "/send"), "{\"expectedVersion\":1,\"sentOn\":\"%s\"}".formatted(today)), 200);
        JsonNode denied = json(agent, jsonBody(post("/contracts/" + id + "/cancel"), "{\"expectedVersion\":2,\"reason\":\"Teste\"}"), 403);
        assertThat(denied.path("code").asText()).isEqualTo("CONTRACT_ACCESS_DENIED");
        assertThat(json(owner, get("/contracts/" + id), 200).path("contract").path("status").asText()).isEqualTo("SENT");

        String platform = member(AccountKind.PLATFORM_ADMIN);
        json(platform, get("/contracts"), 403);
        mockMvc.perform(get("/contracts")).andExpect(IntegrationHttpSupport.unauthenticated());
    }

    @Test
    void agencyScopeProtectsContractsTemplatesAndPeople() throws Exception {
        String jwt = http.loginOwner(mockMvc);
        Agency other = agencies.save(Agency.builder().name("Agência isolada").status(AgencyStatus.ACTIVE)
                .subscriptionStatus(SubscriptionStatus.ACTIVE).build());
        CrmCustomer foreignPerson = customers.save(CrmCustomer.builder().agency(other).name("Pessoa isolada").status(CustomerStatus.ACTIVE).notes("").build());
        UUID foreignTemplate = UUID.randomUUID();
        jdbc.update("INSERT INTO contract_templates(id, agency_id, product, name, created_by) VALUES (?, ?, 'TRAVEL', 'Alheio', 'teste')", foreignTemplate, other.getId());
        jdbc.update("INSERT INTO contract_template_versions(id, template_id, agency_id, version, body, author) VALUES (?, ?, ?, 1, 'Texto', 'teste')",
                UUID.randomUUID(), foreignTemplate, other.getId());
        UUID foreignContract = UUID.randomUUID();
        jdbc.update("INSERT INTO contracts(id, agency_id, product, customer_id, template_id, template_version, title, created_by) VALUES (?, ?, 'TRAVEL', ?, ?, 1, 'Alheio', 'teste')",
                foreignContract, other.getId(), foreignPerson.getId(), foreignTemplate);

        String ownTemplate = template(jwt, BODY).path("template").path("id").asText();
        json(jwt, jsonBody(post("/contracts").header("Idempotency-Key", UUID.randomUUID()), draft(foreignPerson.getId().toString(), ownTemplate, "")), 404);
        json(jwt, jsonBody(post("/contracts").header("Idempotency-Key", UUID.randomUUID()), draft(customer(jwt, "Própria"), foreignTemplate.toString(), "")), 404);
        json(jwt, get("/contract-templates/" + foreignTemplate), 404);
        json(jwt, get("/contracts/" + foreignContract), 404);
        json(jwt, get("/contracts/" + foreignContract + "/documents/1"), 404);
        json(jwt, jsonBody(patch("/contracts/" + foreignContract), draft(foreignPerson.getId().toString(), foreignTemplate.toString(), ",\"expectedVersion\":1")), 404);
        json(jwt, delete("/contracts/" + foreignContract).param("expectedVersion", "1"), 404);
        json(jwt, jsonBody(post("/contracts/" + foreignContract + "/cancel"), "{\"expectedVersion\":1,\"reason\":\"x\"}"), 404);
        assertThat(json(jwt, get("/contracts"), 200).toString()).doesNotContain(foreignContract.toString());
        assertThat(jdbc.queryForObject("SELECT status FROM contracts WHERE id = ?", String.class, foreignContract)).isEqualTo("DRAFT");
    }
}
