package com.agenciahub.api.application.services.tasks;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class WorkModels {
    private WorkModels() {}
    public enum Product { TRAVEL, MILES }
    public enum Frequency { ONCE, WEEKLY, MONTHLY }
    public enum State { OPEN, DONE, CANCELLED }
    public record Step(@NotBlank @Size(max=180) String title, @Min(0) @Max(366) int dueOffsetDays) {}
    public record TemplateInput(@NotNull Product product, @NotBlank @Size(max=180) String name,
            @NotNull Frequency frequency, @NotEmpty @Size(max=50) List<@NotNull @Valid Step> steps,
            @Min(0) int expectedVersion) {}
    public record Template(UUID id, Product product, String name, Frequency frequency, List<Step> steps,
            int version, boolean deleted) {}
    public record TaskInput(@NotNull Product product, @NotNull UUID customerId,
            @NotBlank @Size(max=180) String title, @Size(max=4000) String notes, @NotNull LocalDate dueOn,
            UUID responsibleId, UUID tripId, UUID quotationId, @Min(0) int expectedVersion) {}
    public record Task(UUID id, Product product, UUID customerId, String customerName, String title, String notes,
            LocalDate dueOn, UUID responsibleId, String responsibleName, UUID tripId, UUID quotationId, UUID runId,
            State state, int version, UUID stateBy, OffsetDateTime stateAt, String stateReason, boolean deleted) {}
    public record Transition(@NotNull State state, @Size(max=1000) String reason, @Min(1) int expectedVersion) {}
    public record ApplyInput(@NotNull UUID customerId, @NotNull LocalDate date, UUID responsibleId) {}
    public record Run(UUID id, Product product, UUID customerId, String customerName, UUID templateId,
            String title, String notes, String periodKey, LocalDate periodStart, LocalDate periodEnd,
            Template snapshot, int version, OffsetDateTime cancelledAt, String cancelReason) {}
    public record RunInput(@NotBlank @Size(max=180) String title, @Size(max=4000) String notes, @Min(1) int expectedVersion) {}
    public record Version(@Min(1) int expectedVersion) {}
    public record CancelInput(@NotBlank @Size(max=1000) String reason, @Min(1) int expectedVersion) {}
    public record Event(UUID id, String action, Object snapshot, UUID authorId, OffsetDateTime createdAt) {}
    public record Member(UUID id, String name) {}
    public record References(List<Member> trips, List<Member> quotations) {}
}
