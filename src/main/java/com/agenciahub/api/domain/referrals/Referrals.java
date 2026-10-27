package com.agenciahub.api.domain.referrals;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Indicações indicador → indicado (F09). Regras puras de validação, ciclo e normalização de contato.
 * Não representa crédito, desconto, cupom ou comissão: essas regras ainda não foram definidas.
 */
public final class Referrals {

    private Referrals() {}

    public static final ZoneId AGENCY_ZONE = ZoneId.of("America/Fortaleza");

    public enum Product { TRAVEL, MILES }

    public enum Channel { CONVERSATION, WHATSAPP, SOCIAL_MEDIA, EVENT, OTHER }

    public enum Status { RECEIVED, CONTACTED, CONVERTED, NOT_CONVERTED, CANCELLED }

    public record ReferralInput(Product product, UUID referrerId, UUID referredCustomerId, String referredName, String referredPhone,
                                String referredEmail, LocalDate referredOn, Channel channel, String channelDetail, UUID responsibleId,
                                String notes, Integer expectedVersion) {}

    public record StatusInput(Status status, String reason, LocalDate convertedOn, UUID saleId, Integer expectedVersion) {}

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    /**
     * Valida e normaliza. Com indicado do CRM, os campos de contato digitados são descartados:
     * os dados da pessoa vêm do cadastro, que não é alterado aqui.
     */
    public static ReferralInput normalize(ReferralInput in, LocalDate today) {
        if (in == null) invalid("Informe os dados da indicação.");
        if (in.product() == null) invalid("Informe o produto da indicação: Viagens ou Milhas.");
        if (in.referrerId() == null) invalid("Informe quem fez a indicação.");
        if (in.referredOn() == null) invalid("Informe a data da indicação.");
        if (in.referredOn().isAfter(today)) invalid("A data da indicação não pode estar no futuro.");
        if (in.channel() == null) invalid("Informe a origem da indicação.");
        String channelDetail = text(in.channelDetail(), 200, "O detalhe da origem");
        if (in.channel() == Channel.OTHER && channelDetail == null) invalid("Descreva a origem quando escolher “Outro”.");
        String notes = text(in.notes(), 4000, "As observações");
        if (in.referredCustomerId() != null) {
            if (in.referredCustomerId().equals(in.referrerId())) invalid("A pessoa indicada não pode ser a mesma que indicou.");
            return new ReferralInput(in.product(), in.referrerId(), in.referredCustomerId(), null, null, null, in.referredOn(), in.channel(),
                    channelDetail, in.responsibleId(), notes, in.expectedVersion());
        }
        String name = text(in.referredName(), 200, "O nome da pessoa indicada");
        if (name == null) invalid("Informe a pessoa indicada: escolha alguém cadastrado ou digite o nome.");
        String phone = phone(in.referredPhone());
        String email = email(in.referredEmail());
        if (phone == null && email == null) invalid("Informe telefone ou e-mail da pessoa indicada.");
        return new ReferralInput(in.product(), in.referrerId(), null, name, phone, email, in.referredOn(), in.channel(), channelDetail,
                in.responsibleId(), notes, in.expectedVersion());
    }

    /** Telefone só com dígitos e "+" inicial; entre 8 e 20 dígitos. */
    public static String phone(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String trimmed = raw.strip();
        String digits = trimmed.replaceAll("\\D", "");
        if (digits.length() < 8 || digits.length() > 20) invalid("Telefone da pessoa indicada inválido.");
        return (trimmed.startsWith("+") ? "+" : "") + digits;
    }

    public static String email(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.strip().toLowerCase(Locale.ROOT);
        if (value.length() > 254 || !EMAIL.matcher(value).matches()) invalid("E-mail da pessoa indicada inválido.");
        return value;
    }

    /** Dígitos finais usados para comparar telefones com e sem DDI/DDD (últimos 8). */
    public static String phoneKey(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("\\D", "");
        return digits.length() <= 8 ? digits : digits.substring(digits.length() - 8);
    }

    /**
     * Valida a transição. Convertida exige indicado no CRM e data de conversão; venda só em Viagens.
     * Reabrir, cancelar, reativar e não convertida exigem motivo.
     */
    public static void requireTransition(Status from, StatusInput in, Product product, boolean referredInCrm, LocalDate today, LocalDate referredOn) {
        if (in == null || in.status() == null) invalid("Informe a nova situação.");
        Status to = in.status();
        if (from == to) return;
        boolean allowed = switch (to) {
            case CONTACTED -> from == Status.RECEIVED || from == Status.NOT_CONVERTED || from == Status.CONVERTED;
            case CONVERTED, NOT_CONVERTED -> from == Status.RECEIVED || from == Status.CONTACTED;
            case CANCELLED -> true;
            case RECEIVED -> from == Status.CANCELLED;
        };
        if (!allowed) invalid(switch (to) {
            case RECEIVED -> "Só uma indicação cancelada volta a recebida.";
            case CONVERTED, NOT_CONVERTED -> from == Status.CANCELLED ? "Reative a indicação antes de concluí-la." : "Reabra a indicação como em contato antes de alterar o resultado.";
            default -> "Transição não permitida.";
        });
        boolean needsReason = to == Status.NOT_CONVERTED || to == Status.CANCELLED || to == Status.RECEIVED
                || (to == Status.CONTACTED && from != Status.RECEIVED);
        String reason = in.reason() == null ? null : in.reason().strip();
        if (needsReason && (reason == null || reason.isEmpty())) invalid(switch (to) {
            case NOT_CONVERTED -> "Informe por que a indicação não foi convertida.";
            case CANCELLED -> "Informe o motivo do cancelamento.";
            case RECEIVED -> "Informe o motivo para reativar a indicação.";
            default -> "Informe o motivo para reabrir a indicação.";
        });
        if (reason != null && reason.length() > 1000) invalid("O motivo pode ter até 1.000 caracteres.");
        if (to == Status.CONVERTED) {
            if (!referredInCrm) invalid("Vincule a pessoa indicada a um cadastro antes de marcar como convertida.");
            if (in.convertedOn() == null) invalid("Informe a data da conversão.");
            if (in.convertedOn().isAfter(today)) invalid("A data da conversão não pode estar no futuro.");
            if (in.convertedOn().isBefore(referredOn)) invalid("A conversão não pode ser anterior à indicação.");
            if (in.saleId() != null && product != Product.TRAVEL) invalid("A venda só pode ser vinculada em indicações de Viagens.");
        } else if (in.saleId() != null || in.convertedOn() != null) {
            invalid("Data de conversão e venda só são informadas ao marcar como convertida.");
        }
    }

    private static String text(String value, int max, String name) {
        if (value == null || value.isBlank()) return null;
        String stripped = value.strip();
        if (stripped.length() > max) invalid(name + " pode ter até " + max + " caracteres.");
        return stripped;
    }

    public static void invalid(String message) { throw new IllegalArgumentException(message); }
}
