package com.agenciahub.api.domain.contract;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Substitui {@code {{chave}}} pelos valores do catálogo; não interpreta nem gera cláusulas. */
public final class ContractTemplateRenderer {

    public static final int MAX_BODY_LENGTH = 100_000;
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([A-Za-z][A-Za-z0-9.]*)\\s*}}");

    private ContractTemplateRenderer() {}

    public record Rendered(String content, List<ContractField> missing) {}

    /** Valida o conteúdo de um modelo e devolve os campos usados, na ordem em que aparecem. */
    public static Set<ContractField> validate(String body) {
        if (body == null || body.isBlank()) throw new IllegalArgumentException("Informe o conteúdo do modelo.");
        if (body.length() > MAX_BODY_LENGTH) throw new IllegalArgumentException("O modelo pode ter até 100.000 caracteres.");
        Set<ContractField> used = new LinkedHashSet<>();
        List<String> unknown = new ArrayList<>();
        Matcher matcher = PLACEHOLDER.matcher(body);
        while (matcher.find()) {
            String key = matcher.group(1);
            ContractField.byKey(key).ifPresentOrElse(used::add, () -> unknown.add("{{" + key + "}}"));
        }
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException("Campos desconhecidos no modelo: " + String.join(", ", new LinkedHashSet<>(unknown))
                    + ". Use apenas os campos da lista disponível.");
        }
        return used;
    }

    public static Rendered render(String body, Map<ContractField, String> values) {
        Set<ContractField> missing = new LinkedHashSet<>();
        Matcher matcher = PLACEHOLDER.matcher(body);
        StringBuilder out = new StringBuilder(body.length());
        while (matcher.find()) {
            ContractField field = ContractField.byKey(matcher.group(1))
                    .orElseThrow(() -> new IllegalStateException("Campo desconhecido no modelo."));
            String value = values.get(field);
            if (value == null || value.isBlank()) {
                missing.add(field);
                matcher.appendReplacement(out, Matcher.quoteReplacement("[" + field.label().toUpperCase(java.util.Locale.ROOT) + " PENDENTE]"));
            } else {
                matcher.appendReplacement(out, Matcher.quoteReplacement(value));
            }
        }
        matcher.appendTail(out);
        return new Rendered(out.toString(), List.copyOf(missing));
    }
}
