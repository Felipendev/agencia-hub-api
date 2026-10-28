package com.agenciahub.api.application.services.diagnostics;

import com.agenciahub.api.domain.MilesWallet;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class DiagnosticDtos {
    private DiagnosticDtos() {}
    public static final String CONSENT="milhas-2026-10-28";
    public record Config(UUID token,String title,String introduction,boolean enabled,int version) {}
    public record ConfigInput(@NotBlank @Size(max=160) String title,@NotNull @Size(max=1000) String introduction,boolean enabled,@Min(0) int expectedVersion) {}
    public record PublicConfig(String agencyName,String title,String introduction,String consentVersion) {}
    public record Declared(@NotNull MilesWallet.Kind kind,@NotNull @Valid MilesWallet.Data data) {}
    public record Data(@NotBlank @Size(max=200) String name,@NotBlank @Email @Size(max=254) String email,
                       @NotNull @Size(max=30) @Pattern(regexp="[+0-9() .-]*") String phone,
                       @NotNull @Valid MilesWallet.Profile profile,@NotNull @Size(max=20) List<@NotNull @Valid Declared> items) {}
    public record Submission(@NotNull @Valid Data data,@AssertTrue boolean consent,@NotBlank @Pattern(regexp=CONSENT) String consentVersion) {}
    public record Edit(@NotNull @Valid Data data,@Min(1) int expectedVersion) {}
    public record State(@Min(1) int expectedVersion,@NotBlank @Size(max=500) String reason) {}
    public record Apply(@Min(1) int expectedVersion,@NotNull UUID customerId,boolean importProfile,@Min(0) int profileVersion,
                        @NotNull @Size(max=20) List<@NotNull @Min(0) @Max(19) Integer> items) {}
    public record Receipt(UUID customerId,UUID authorId,OffsetDateTime at,boolean importedProfile,List<UUID> itemIds,List<Integer> itemIndexes) {}
    public record Entry(UUID id,Data original,Data data,String source,String consentVersion,int version,boolean archived,UUID customerId,Receipt receipt,OffsetDateTime createdAt) {}
    public record Summary(UUID id,String name,String email,int version,boolean archived,boolean incorporated,OffsetDateTime createdAt) {}
    public record Event(String action,UUID authorId,String reason,Object snapshot,OffsetDateTime createdAt) {}
}
