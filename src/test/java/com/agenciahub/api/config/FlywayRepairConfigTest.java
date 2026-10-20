package com.agenciahub.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.assertThat;

class FlywayRepairConfigTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(FlywayRepairConfig.class);
    @Test void defaultStartupDoesNotInstallAutomaticRepair() {
        runner.run(context -> assertThat(context).doesNotHaveBean(FlywayMigrationStrategy.class));
    }
    @Test void disabledRepairLeavesNormalFlywayValidationInCharge() {
        runner.withPropertyValues("agenciahub.flyway.repair-before-migrate=false")
                .run(context -> assertThat(context).doesNotHaveBean(FlywayMigrationStrategy.class));
    }
}
