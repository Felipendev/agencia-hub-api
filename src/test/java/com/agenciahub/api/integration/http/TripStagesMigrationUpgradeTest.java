package com.agenciahub.api.integration.http;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class TripStagesMigrationUpgradeTest {
    @Test void v61UpgradePreservesTripAndDoesNotCreateStages() throws Exception {
        try(EmbeddedPostgres pg=EmbeddedPostgres.builder().start()) {
            String url=pg.getJdbcUrl("postgres","postgres");
            Flyway.configure().dataSource(url,"postgres","").target("61").load().migrate();
            UUID person=UUID.randomUUID(),trip=UUID.randomUUID();
            try(var c=pg.getPostgresDatabase().getConnection()) {
                try(var p=c.prepareStatement("INSERT INTO customers(id,agency_id,name,status,notes,created_at) SELECT ?,id,'Pessoa anterior','ACTIVE','',now() FROM agencies LIMIT 1")) {p.setObject(1,person);assertThat(p.executeUpdate()).isEqualTo(1);}
                try(var p=c.prepareStatement("INSERT INTO trips(id,agency_id,customer_id,service_type,status,booking_locator) SELECT ?,agency_id,id,'AEREO','UPCOMING','ANT123' FROM customers WHERE id=?")) {p.setObject(1,trip);p.setObject(2,person);assertThat(p.executeUpdate()).isEqualTo(1);}
            }
            assertThat(Flyway.configure().dataSource(url,"postgres","").target("62").load().migrate().migrationsExecuted).isEqualTo(1);
            try(var c=pg.getPostgresDatabase().getConnection();var p=c.prepareStatement("SELECT customer_id,booking_locator FROM trips WHERE id=?")) {
                p.setObject(1,trip);try(var r=p.executeQuery()) {assertThat(r.next()).isTrue();assertThat(r.getObject(1,UUID.class)).isEqualTo(person);assertThat(r.getString(2)).isEqualTo("ANT123");}
                for(String table:java.util.List.of("trip_stages","trip_stage_links","trip_stage_events")) {
                    try(var r=c.createStatement().executeQuery("SELECT count(*) FROM "+table)) {assertThat(r.next()).isTrue();assertThat(r.getLong(1)).isZero();}
                }
            }
        }
    }
}
