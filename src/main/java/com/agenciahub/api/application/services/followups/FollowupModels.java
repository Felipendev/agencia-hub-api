package com.agenciahub.api.application.services.followups;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class FollowupModels {
    private FollowupModels() {}
    public enum Product { TRAVEL, MILES }
    public enum State { OPEN, DONE, CANCELLED }
    public enum Bucket { ALL, OVERDUE, TODAY, UPCOMING }
    public record Input(@NotNull Product product, @NotNull UUID customerId,
            @NotBlank @Size(max=180) String title, @Size(max=4000) String notes,
            @NotNull LocalDate dueOn, @NotNull UUID responsibleId, @Min(0) int expectedVersion) {}
    public record Transition(@NotNull State state, @Size(max=1000) String reason, @Min(1) int expectedVersion) {}
    public record Followup(UUID id, Product product, UUID customerId, String customerName,
            String title, String notes, LocalDate dueOn, UUID responsibleId, String responsibleName,
            State state, String stateReason, int version, boolean deleted) {}
    public record Counts(long overdue, long today, long upcoming) {}
    public record Board(LocalDate today, Counts counts, List<Followup> items) {}
    public record Event(UUID id, String action, Object snapshot, UUID authorId, String authorName, OffsetDateTime createdAt) {}
}
