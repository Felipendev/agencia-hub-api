package com.agenciahub.api.integration.http;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class AgendaMigrationUpgradeTest {
    @Test void v58UpgradePreservesCustomersAndCreatesAgendaWithNoBackfill() throws Exception {
        try (EmbeddedPostgres pg=EmbeddedPostgres.builder().start()) {
            String url=pg.getJdbcUrl("postgres","postgres");
            Flyway.configure().dataSource(url,"postgres","").target("58").load().migrate();
            UUID id=UUID.randomUUID();
            try (var connection=pg.getPostgresDatabase().getConnection(); var insert=connection.prepareStatement("INSERT INTO customers(id,agency_id,name,status,notes,created_at) SELECT ?,id,'Pessoa anterior','ACTIVE','',now() FROM agencies LIMIT 1")) {
                insert.setObject(1,id); assertThat(insert.executeUpdate()).isEqualTo(1);
            }
            Flyway.configure().dataSource(url,"postgres","").target("59").load().migrate();
            try (var connection=pg.getPostgresDatabase().getConnection(); var query=connection.prepareStatement("SELECT name FROM customers WHERE id=?")) {
                query.setObject(1,id);
                try(var rows=query.executeQuery()) {assertThat(rows.next()).isTrue();assertThat(rows.getString(1)).isEqualTo("Pessoa anterior");}
                try(var rows=connection.createStatement().executeQuery("SELECT count(*) FROM appointments")) {assertThat(rows.next()).isTrue();assertThat(rows.getLong(1)).isZero();}
            }
        }
    }
}
