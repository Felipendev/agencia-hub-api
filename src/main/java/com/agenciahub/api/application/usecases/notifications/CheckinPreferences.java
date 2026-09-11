package com.agenciahub.api.application.usecases.notifications;

import jakarta.validation.constraints.*;

/** Agency-owned IN_APP settings; dates are calendar days in America/Sao_Paulo. */
public record CheckinPreferences(@NotNull Boolean startEnabled, @NotNull @Min(1) @Max(30) Integer startDays,
                                 @NotNull Boolean endEnabled, @NotNull @Min(1) @Max(30) Integer endDays) {
    public static CheckinPreferences defaults() { return new CheckinPreferences(false, 2, false, 2); }
}
