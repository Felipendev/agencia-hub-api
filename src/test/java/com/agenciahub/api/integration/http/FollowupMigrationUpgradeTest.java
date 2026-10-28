package com.agenciahub.api.integration.http;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FollowupMigrationUpgradeTest {
    @Test void v63ToV64PreservesExistingRecordsWithoutBackfill()throws Exception {
        try(var pg=EmbeddedPostgres.builder().start()) {
            String url=pg.getJdbcUrl("postgres","postgres");
            Flyway.configure().dataSource(url,"postgres","").target("63").load().migrate();
            try(var c=pg.getPostgresDatabase().getConnection();var sql=c.createStatement()) {
                sql.execute("INSERT INTO customers(id,agency_id,name,email,phone,interest_destination,status,notes,created_at) VALUES('b0800000-0000-0000-0000-000000000001','a0000000-0000-0000-0000-000000000000','Contato anterior','legacy-f08@test.local','','','PROSPECT','Preservar observações',now())");
            }
            assertThat(Flyway.configure().dataSource(url,"postgres","").target("64").load().migrate().migrationsExecuted).isEqualTo(1);
            try(var c=pg.getPostgresDatabase().getConnection();var sql=c.createStatement()) {
                try(var r=sql.executeQuery("SELECT name,notes,status FROM customers WHERE id='b0800000-0000-0000-0000-000000000001'")){r.next();assertThat(r.getString(1)).isEqualTo("Contato anterior");assertThat(r.getString(2)).isEqualTo("Preservar observações");assertThat(r.getString(3)).isEqualTo("PROSPECT");}
                try(var r=sql.executeQuery("SELECT count(*) FROM commercial_followups")){r.next();assertThat(r.getInt(1)).isZero();}
                try(var r=sql.executeQuery("SELECT count(*) FROM commercial_followup_events")){r.next();assertThat(r.getInt(1)).isZero();}
            }
        }
    }
}
