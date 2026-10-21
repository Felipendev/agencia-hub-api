package com.agenciahub.api.application.services.referrals;

import com.agenciahub.api.application.persistence.entity.PlatformAccount;
import com.agenciahub.api.domain.referrals.Referrals;
import com.agenciahub.api.domain.referrals.Referrals.*;
import com.agenciahub.api.exception.ReferralConflictException;
import com.agenciahub.api.exception.ResourceNotFoundException;
import com.agenciahub.api.security.SecurityContextUsers;
import com.agenciahub.api.security.TenantContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.agenciahub.api.domain.referrals.Referrals.invalid;

/**
 * Indicações indicador → indicado. Não cria crédito, desconto, cupom, comissão nem lançamento financeiro,
 * e não altera o cadastro de pessoas: vincular ou cadastrar o indicado são operações explícitas.
 */
@Service
@RequiredArgsConstructor
public class ReferralService {

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public record Member(UUID id, String name) {}

    public record ReferralView(UUID id, Product product, Status status, String statusReason, UUID referrerId, String referrerName,
                               UUID referredCustomerId, String referredCustomerName, String referredName, String referredPhone,
                               String referredEmail, LocalDate referredOn, Channel channel, String channelDetail, UUID responsibleId,
                               String responsibleName, String notes, LocalDate convertedOn, UUID saleId, boolean everConverted, int version,
                               OffsetDateTime createdAt, OffsetDateTime updatedAt) {}

    /** Aviso sem bloqueio: contato digitado coincide com pessoa do CRM ou outra indicação ativa do produto. */
    public record Warning(String kind, UUID id, String label) {}

    public record Event(UUID id, String action, Object detail, String author, OffsetDateTime createdAt) {}

    public record Detail(ReferralView referral, List<Warning> warnings, List<Event> history) {}

    public record Filters(Product product, Status status, UUID customerId, LocalDate from, LocalDate to, String q) {}

    public record TopReferrer(UUID customerId, String name, long total, long converted) {}

    public record Summary(Map<Status, Long> counts, List<TopReferrer> topReferrers) {}

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String SELECT = """
            SELECT r.*, rc.name AS referrer_name, dc.name AS referred_customer_name, u.name AS responsible_name
            FROM referrals r
            JOIN customers rc ON rc.id = r.referrer_customer_id AND rc.agency_id = r.agency_id
            LEFT JOIN customers dc ON dc.id = r.referred_customer_id AND dc.agency_id = r.agency_id
            LEFT JOIN users u ON u.id = r.responsible_id AND u.agency_id = r.agency_id
            WHERE r.agency_id = ?""";

    private static final RowMapper<ReferralView> ROW = (rs, n) -> new ReferralView(
            rs.getObject("id", UUID.class), Product.valueOf(rs.getString("product")), Status.valueOf(rs.getString("status")), rs.getString("status_reason"),
            rs.getObject("referrer_customer_id", UUID.class), rs.getString("referrer_name"), rs.getObject("referred_customer_id", UUID.class),
            rs.getString("referred_customer_name"), rs.getString("referred_name"), rs.getString("referred_phone"), rs.getString("referred_email"),
            rs.getObject("referred_on", LocalDate.class), Channel.valueOf(rs.getString("channel")), rs.getString("channel_detail"),
            rs.getObject("responsible_id", UUID.class), rs.getString("responsible_name"), rs.getString("notes"),
            rs.getObject("converted_on", LocalDate.class), rs.getObject("sale_id", UUID.class), rs.getBoolean("ever_converted"), rs.getInt("version"),
            rs.getObject("created_at", OffsetDateTime.class), rs.getObject("updated_at", OffsetDateTime.class));

    // ------------------------------------------------------------------ consultas

    @Transactional(readOnly = true)
    public List<Member> members() {
        return jdbc.query("""
                SELECT id, name FROM users WHERE agency_id = ? AND active = true AND account_kind IN ('AGENCY_OWNER','SALES_AGENT')
                ORDER BY lower(name), id""", (rs, n) -> new Member(rs.getObject(1, UUID.class), rs.getString(2)), agency());
    }

    @Transactional(readOnly = true)
    public List<ReferralView> list(Filters f) {
        if (f.from() != null && f.to() != null && f.to().isBefore(f.from())) invalid("O fim do período não pode ser anterior ao início.");
        StringBuilder sql = new StringBuilder(SELECT);
        List<Object> args = new ArrayList<>(List.of(agency()));
        if (f.product() != null) { sql.append(" AND r.product = ?"); args.add(f.product().name()); }
        if (f.status() != null) { sql.append(" AND r.status = ?"); args.add(f.status().name()); }
        if (f.customerId() != null) { sql.append(" AND (r.referrer_customer_id = ? OR r.referred_customer_id = ?)"); args.add(f.customerId()); args.add(f.customerId()); }
        if (f.from() != null) { sql.append(" AND r.referred_on >= ?"); args.add(f.from()); }
        if (f.to() != null) { sql.append(" AND r.referred_on <= ?"); args.add(f.to()); }
        String q = f.q() == null ? "" : f.q().strip();
        if (q.length() > 200) invalid("A busca pode ter até 200 caracteres.");
        if (!q.isEmpty()) {
            String like = "%" + q.toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            sql.append(" AND (lower(rc.name) LIKE ? OR lower(coalesce(dc.name, '')) LIKE ? OR lower(coalesce(r.referred_name, '')) LIKE ?"
                    + " OR lower(coalesce(r.referred_email, '')) LIKE ? OR coalesce(r.referred_phone, '') LIKE ?)");
            String digits = q.replaceAll("\\D", "");
            args.addAll(List.of(like, like, like, like, digits.isEmpty() ? like : "%" + digits + "%"));
        }
        sql.append(" ORDER BY r.referred_on DESC, r.created_at DESC, r.id LIMIT 500");
        return jdbc.query(sql.toString(), ROW, args.toArray());
    }

    @Transactional(readOnly = true)
    public Summary summary(Product product, LocalDate from, LocalDate to) {
        StringBuilder where = new StringBuilder(" WHERE r.agency_id = ?");
        List<Object> args = new ArrayList<>(List.of(agency()));
        if (product != null) { where.append(" AND r.product = ?"); args.add(product.name()); }
        if (from != null) { where.append(" AND r.referred_on >= ?"); args.add(from); }
        if (to != null) { where.append(" AND r.referred_on <= ?"); args.add(to); }
        Map<Status, Long> counts = new EnumMap<>(Status.class);
        for (Status s : Status.values()) counts.put(s, 0L);
        jdbc.query("SELECT r.status, count(*) FROM referrals r" + where + " GROUP BY r.status",
                rs -> { counts.put(Status.valueOf(rs.getString(1)), rs.getLong(2)); }, args.toArray());
        List<TopReferrer> top = jdbc.query("""
                SELECT r.referrer_customer_id, c.name, count(*) AS total, count(*) FILTER (WHERE r.status = 'CONVERTED') AS converted
                FROM referrals r JOIN customers c ON c.id = r.referrer_customer_id AND c.agency_id = r.agency_id""" + where + """
                 AND r.status <> 'CANCELLED'
                GROUP BY r.referrer_customer_id, c.name ORDER BY total DESC, converted DESC, lower(c.name) LIMIT 5""",
                (rs, n) -> new TopReferrer(rs.getObject(1, UUID.class), rs.getString(2), rs.getLong(3), rs.getLong(4)), args.toArray());
        return new Summary(counts, top);
    }

    @Transactional(readOnly = true)
    public Detail detail(UUID id) {
        return detailOf(find(id, false));
    }

    // ------------------------------------------------------------------ escrita

    @Transactional
    public Detail create(UUID requestKey, ReferralInput raw) {
        ReferralInput in = Referrals.normalize(raw, today());
        requireReferences(in);
        Optional<ReferralView> existing = jdbc.query(SELECT + " AND r.id = ?", ROW, agency(), requestKey).stream().findFirst();
        if (existing.isPresent()) {
            ReferralView r = existing.get();
            boolean same = r.product() == in.product() && r.referrerId().equals(in.referrerId()) && r.referredOn().equals(in.referredOn())
                    && Objects.equals(r.referredCustomerId(), in.referredCustomerId())
                    && (in.referredCustomerId() != null || Objects.equals(r.referredName(), in.referredName()));
            if (!same) throw new ReferralConflictException("A confirmação já foi utilizada para outra indicação.");
            return detailOf(r);
        }
        Boolean used = jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM referral_events WHERE agency_id = ? AND referral_id = ?) OR EXISTS(SELECT 1 FROM referrals WHERE id = ?)",
                Boolean.class, agency(), requestKey, requestKey);
        if (Boolean.TRUE.equals(used)) throw new ReferralConflictException("Esta confirmação já foi usada. Recarregue a página antes de registrar outra indicação.");
        if (in.referredCustomerId() != null) requireNoActiveDuplicate(in.product(), in.referredCustomerId(), null);
        jdbc.update("""
                INSERT INTO referrals(id, agency_id, product, referrer_customer_id, referred_customer_id, referred_name, referred_phone, referred_phone_key,
                    referred_email, referred_on, channel, channel_detail, responsible_id, notes, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                requestKey, agency(), in.product().name(), in.referrerId(), in.referredCustomerId(), in.referredName(), in.referredPhone(),
                Referrals.phoneKey(in.referredPhone()), in.referredEmail(), in.referredOn(), in.channel().name(), in.channelDetail(),
                in.responsibleId(), in.notes(), author());
        ReferralView created = find(requestKey, false);
        event(created.id(), "CREATED", snapshot(created));
        return detailOf(created);
    }

    /** Edita pelo mesmo ID. Produto e estado não mudam aqui; a pessoa indicada já vinculada também não. */
    @Transactional
    public Detail update(UUID id, ReferralInput raw) {
        ReferralView before = find(id, true);
        expectVersion(before.version(), raw == null ? null : raw.expectedVersion());
        if (before.status() == Status.CANCELLED) throw new ReferralConflictException("Reative a indicação antes de editá-la.");
        if (raw.product() != null && raw.product() != before.product()) invalid("O produto não muda depois de registrado. Cancele esta indicação e registre outra.");
        if (before.referredCustomerId() != null && !before.referredCustomerId().equals(raw.referredCustomerId())) {
            invalid("A pessoa indicada já está vinculada ao cadastro. Para trocar, cancele esta indicação e registre outra.");
        }
        if (before.referredCustomerId() == null && raw.referredCustomerId() != null) invalid("Use “Vincular” para ligar a pessoa indicada ao cadastro.");
        ReferralInput in = Referrals.normalize(new ReferralInput(before.product(), raw.referrerId(), raw.referredCustomerId(), raw.referredName(),
                raw.referredPhone(), raw.referredEmail(), raw.referredOn(), raw.channel(), raw.channelDetail(), raw.responsibleId(), raw.notes(),
                raw.expectedVersion()), today());
        if (before.convertedOn() != null && in.referredOn().isAfter(before.convertedOn())) invalid("A data da indicação não pode ser posterior à conversão.");
        requireReferences(in);
        Map<String, Object> changes = new LinkedHashMap<>();
        diff(changes, "referrerId", before.referrerId(), in.referrerId());
        diff(changes, "referredName", before.referredName(), in.referredName());
        diff(changes, "referredPhone", before.referredPhone(), in.referredPhone());
        diff(changes, "referredEmail", before.referredEmail(), in.referredEmail());
        diff(changes, "referredOn", before.referredOn(), in.referredOn());
        diff(changes, "channel", before.channel(), in.channel());
        diff(changes, "channelDetail", before.channelDetail(), in.channelDetail());
        diff(changes, "responsibleId", before.responsibleId(), in.responsibleId());
        // Observações não são copiadas para a auditoria; registra-se só que mudaram.
        if (!Objects.equals(before.notes(), in.notes())) changes.put("notesChanged", true);
        if (changes.isEmpty()) return detailOf(before);
        jdbc.update("""
                UPDATE referrals SET referrer_customer_id = ?, referred_name = ?, referred_phone = ?, referred_phone_key = ?, referred_email = ?,
                    referred_on = ?, channel = ?, channel_detail = ?, responsible_id = ?, notes = ?, version = version + 1, updated_at = now()
                WHERE id = ? AND agency_id = ?""",
                in.referrerId(), in.referredName(), in.referredPhone(), Referrals.phoneKey(in.referredPhone()), in.referredEmail(), in.referredOn(),
                in.channel().name(), in.channelDetail(), in.responsibleId(), in.notes(), id, agency());
        event(id, "UPDATED", changes);
        return detailOf(find(id, false));
    }

    @Transactional
    public Detail changeStatus(UUID id, StatusInput input) {
        ReferralView before = find(id, true);
        if (input == null) invalid("Informe a nova situação.");
        if (before.status() == input.status()) return detailOf(before);
        expectVersion(before.version(), input.expectedVersion());
        Referrals.requireTransition(before.status(), input, before.product(), before.referredCustomerId() != null, today(), before.referredOn());
        Status to = input.status();
        if (to == Status.CONVERTED && input.saleId() != null) {
            Boolean belongs = jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM sales WHERE id = ? AND agency_id = ? AND customer_id = ?)",
                    Boolean.class, input.saleId(), agency(), before.referredCustomerId());
            if (!Boolean.TRUE.equals(belongs)) throw new ResourceNotFoundException("Venda não encontrada para a pessoa indicada.");
        }
        if (before.status() == Status.CANCELLED && before.referredCustomerId() != null) requireNoActiveDuplicate(before.product(), before.referredCustomerId(), id);
        String reason = input.reason() == null || input.reason().isBlank() ? null : input.reason().strip();
        boolean converted = to == Status.CONVERTED;
        jdbc.update("""
                UPDATE referrals SET status = ?, status_reason = ?, converted_on = ?, sale_id = ?, ever_converted = ever_converted OR ?,
                    version = version + 1, updated_at = now()
                WHERE id = ? AND agency_id = ?""",
                to.name(), reason, converted ? input.convertedOn() : null, converted ? input.saleId() : null, converted, id, agency());
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("from", before.status().name());
        detail.put("to", to.name());
        detail.put("reason", reason);
        if (converted) {
            detail.put("convertedOn", input.convertedOn().toString());
            detail.put("saleId", input.saleId());
        } else if (before.status() == Status.CONVERTED) {
            detail.put("previousConvertedOn", before.convertedOn() == null ? null : before.convertedOn().toString());
            detail.put("previousSaleId", before.saleId());
        }
        event(id, "STATUS_" + to.name(), detail);
        return detailOf(find(id, false));
    }

    /** Vincula o indicado a uma pessoa do CRM da agência. O cadastro da pessoa não é alterado. */
    @Transactional
    public Detail linkReferred(UUID id, UUID customerId, Integer expectedVersion) {
        ReferralView before = find(id, true);
        if (customerId.equals(before.referredCustomerId())) return detailOf(before);
        expectVersion(before.version(), expectedVersion);
        if (before.status() == Status.CANCELLED) throw new ReferralConflictException("Reative a indicação antes de vincular a pessoa indicada.");
        if (before.referredCustomerId() != null) invalid("A indicação já está vinculada a outra pessoa.");
        if (customerId.equals(before.referrerId())) invalid("A pessoa indicada não pode ser a mesma que indicou.");
        requireCustomer(customerId, "Pessoa indicada não encontrada.");
        requireNoActiveDuplicate(before.product(), customerId, id);
        jdbc.update("""
                UPDATE referrals SET referred_customer_id = ?, referred_name = NULL, referred_phone = NULL, referred_phone_key = NULL, referred_email = NULL,
                    version = version + 1, updated_at = now()
                WHERE id = ? AND agency_id = ?""", customerId, id, agency());
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("customerId", customerId);
        detail.put("previousName", before.referredName());
        detail.put("previousPhone", before.referredPhone());
        detail.put("previousEmail", before.referredEmail());
        event(id, "REFERRED_LINKED", detail);
        return detailOf(find(id, false));
    }

    /** Exclusão só para registro feito por engano que nunca foi convertido; o restante é cancelado. */
    @Transactional
    public void delete(UUID id, Integer expectedVersion) {
        ReferralView before = find(id, true);
        expectVersion(before.version(), expectedVersion);
        if (before.everConverted()) throw new ReferralConflictException("Indicações que já foram convertidas não são excluídas. Cancele para preservar o histórico.");
        jdbc.update("DELETE FROM referrals WHERE id = ? AND agency_id = ?", id, agency());
        event(id, "DELETED", snapshot(before));
    }

    // ------------------------------------------------------------------ apoio

    private ReferralView find(UUID id, boolean lock) {
        if (lock) {
            List<UUID> locked = jdbc.query("SELECT id FROM referrals WHERE id = ? AND agency_id = ? FOR UPDATE", (rs, n) -> rs.getObject(1, UUID.class), id, agency());
            if (locked.isEmpty()) throw notFound();
        }
        return jdbc.query(SELECT + " AND r.id = ?", ROW, agency(), id).stream().findFirst().orElseThrow(ReferralService::notFound);
    }

    private Detail detailOf(ReferralView r) {
        return new Detail(r, warnings(r), events(r.id()));
    }

    private List<Warning> warnings(ReferralView r) {
        if (r.referredCustomerId() != null || r.status() == Status.CANCELLED) return List.of();
        String key = Referrals.phoneKey(r.referredPhone());
        String email = r.referredEmail();
        List<Warning> out = new ArrayList<>(jdbc.query("""
                SELECT id, name FROM customers
                WHERE agency_id = ? AND id <> ?
                  AND ((CAST(? AS text) IS NOT NULL AND length(regexp_replace(coalesce(phone, ''), '\\D', '', 'g')) >= 8
                        AND right(regexp_replace(coalesce(phone, ''), '\\D', '', 'g'), 8) = ?)
                    OR (CAST(? AS text) IS NOT NULL AND lower(email) = ?))
                ORDER BY lower(name) LIMIT 5""",
                (rs, n) -> new Warning("CUSTOMER", rs.getObject(1, UUID.class), rs.getString(2)),
                agency(), r.referrerId(), key, key, email, email));
        out.addAll(jdbc.query("""
                SELECT r.id, rc.name, coalesce(dc.name, r.referred_name), r.referred_on
                FROM referrals r JOIN customers rc ON rc.id = r.referrer_customer_id AND rc.agency_id = r.agency_id
                LEFT JOIN customers dc ON dc.id = r.referred_customer_id AND dc.agency_id = r.agency_id
                WHERE r.agency_id = ? AND r.id <> ? AND r.product = ? AND r.status <> 'CANCELLED'
                  AND ((CAST(? AS text) IS NOT NULL AND r.referred_phone_key = ?) OR (CAST(? AS text) IS NOT NULL AND r.referred_email = ?))
                ORDER BY r.referred_on DESC LIMIT 5""",
                (rs, n) -> new Warning("REFERRAL", rs.getObject(1, UUID.class),
                        rs.getString(2) + " → " + rs.getString(3) + " (" + rs.getObject(4, LocalDate.class).format(BR) + ")"),
                agency(), r.id(), r.product().name(), key, key, email, email));
        return out;
    }

    private void requireReferences(ReferralInput in) {
        requireCustomer(in.referrerId(), "Pessoa que indicou não encontrada.");
        if (in.referredCustomerId() != null) requireCustomer(in.referredCustomerId(), "Pessoa indicada não encontrada.");
        if (in.responsibleId() != null) {
            Boolean member = jdbc.queryForObject("""
                    SELECT EXISTS(SELECT 1 FROM users WHERE id = ? AND agency_id = ? AND active = true AND account_kind IN ('AGENCY_OWNER','SALES_AGENT'))""",
                    Boolean.class, in.responsibleId(), agency());
            if (!Boolean.TRUE.equals(member)) throw new ResourceNotFoundException("Responsável não encontrado na equipe da agência.");
        }
    }

    private void requireCustomer(UUID id, String message) {
        Boolean exists = jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM customers WHERE id = ? AND agency_id = ?)", Boolean.class, id, agency());
        if (!Boolean.TRUE.equals(exists)) throw new ResourceNotFoundException(message);
    }

    private void requireNoActiveDuplicate(Product product, UUID referredCustomerId, UUID except) {
        jdbc.query("""
                SELECT rc.name, r.referred_on FROM referrals r JOIN customers rc ON rc.id = r.referrer_customer_id AND rc.agency_id = r.agency_id
                WHERE r.agency_id = ? AND r.product = ? AND r.referred_customer_id = ? AND r.status <> 'CANCELLED' AND r.id <> ?
                LIMIT 1""",
                (rs, n) -> new Object[]{rs.getString(1), rs.getObject(2, LocalDate.class)},
                agency(), product.name(), referredCustomerId, except == null ? new UUID(0, 0) : except)
                .stream().findFirst().ifPresent(row -> {
                    throw new ReferralConflictException("Esta pessoa já tem uma indicação ativa em " + (product == Product.TRAVEL ? "Viagens" : "Milhas")
                            + ", feita por " + row[0] + " em " + ((LocalDate) row[1]).format(BR) + ".");
                });
    }

    private List<Event> events(UUID referralId) {
        return jdbc.query("""
                SELECT id, action, detail, author, created_at FROM referral_events WHERE agency_id = ? AND referral_id = ?
                ORDER BY created_at DESC, id LIMIT 200""",
                (rs, n) -> new Event(rs.getObject(1, UUID.class), rs.getString(2), read(rs.getString(3)), rs.getString(4), rs.getObject(5, OffsetDateTime.class)),
                agency(), referralId);
    }

    private static Map<String, Object> snapshot(ReferralView r) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("product", r.product().name());
        detail.put("status", r.status().name());
        detail.put("referrerId", r.referrerId());
        detail.put("referredCustomerId", r.referredCustomerId());
        detail.put("referredName", r.referredName());
        detail.put("referredOn", r.referredOn().toString());
        detail.put("channel", r.channel().name());
        return detail;
    }

    private static void diff(Map<String, Object> changes, String field, Object before, Object after) {
        if (Objects.equals(before, after)) return;
        Map<String, Object> change = new LinkedHashMap<>();
        change.put("from", before == null ? null : before.toString());
        change.put("to", after == null ? null : after.toString());
        changes.put(field, change);
    }

    private void event(UUID referralId, String action, Object detail) {
        jdbc.update("INSERT INTO referral_events(id, agency_id, referral_id, action, detail, author) VALUES (?, ?, ?, ?, ?::jsonb, ?)",
                UUID.randomUUID(), agency(), referralId, action, json(detail), author());
    }

    private void expectVersion(int actual, Integer expected) {
        if (expected == null || actual != expected) throw new ReferralConflictException("A indicação foi alterada por outra pessoa. Recarregue antes de continuar.");
    }

    private static LocalDate today() { return LocalDate.now(Referrals.AGENCY_ZONE); }

    private static UUID agency() { return TenantContext.requireAgencyId(); }

    private static String author() {
        PlatformAccount user = SecurityContextUsers.requireUser();
        return user.getName() + " <" + user.getEmail() + ">";
    }

    private String json(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Não foi possível registrar a indicação.", e); }
    }

    private Object read(String value) {
        try { return value == null ? null : mapper.readValue(value, Object.class); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Não foi possível consultar a indicação.", e); }
    }

    private static ResourceNotFoundException notFound() { return new ResourceNotFoundException("Indicação não encontrada."); }
}
