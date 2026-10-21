package com.agenciahub.api.integration.http;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class ReferralsMigrationUpgradeTest {
    @Test void v62UpgradePreservesStagesAndDoesNotBackfillReferrals()throws Exception {
        try(EmbeddedPostgres pg=EmbeddedPostgres.builder().start()) {
            String url=pg.getJdbcUrl("postgres","postgres");Flyway.configure().dataSource(url,"postgres","").target("62").load().migrate();
            UUID person=UUID.randomUUID(),trip=UUID.randomUUID(),stage=UUID.randomUUID();
            try(var c=pg.getPostgresDatabase().getConnection()) {
                try(var p=c.prepareStatement("INSERT INTO customers(id,agency_id,name,status,notes,created_at) SELECT ?,id,'Pessoa anterior','ACTIVE','',now() FROM agencies LIMIT 1")) {p.setObject(1,person);assertThat(p.executeUpdate()).isEqualTo(1);}
                try(var p=c.prepareStatement("INSERT INTO trips(id,agency_id,customer_id,service_type,status) SELECT ?,agency_id,id,'AEREO','UPCOMING' FROM customers WHERE id=?")) {p.setObject(1,trip);p.setObject(2,person);assertThat(p.executeUpdate()).isEqualTo(1);}
                try(var p=c.prepareStatement("INSERT INTO trip_stages(id,agency_id,trip_id,kind,title,starts_at,created_by) SELECT ?,agency_id,id,'HOTEL','Hotel anterior',now(),'Teste' FROM trips WHERE id=?")) {p.setObject(1,stage);p.setObject(2,trip);assertThat(p.executeUpdate()).isEqualTo(1);}
            }
            assertThat(Flyway.configure().dataSource(url,"postgres","").load().migrate().migrationsExecuted).isEqualTo(1);
            try(var c=pg.getPostgresDatabase().getConnection();var p=c.prepareStatement("SELECT title FROM trip_stages WHERE id=?")) {
                p.setObject(1,stage);try(var r=p.executeQuery()) {assertThat(r.next()).isTrue();assertThat(r.getString(1)).isEqualTo("Hotel anterior");}
                try(var r=c.createStatement().executeQuery("SELECT count(*) FROM referrals")) {assertThat(r.next()).isTrue();assertThat(r.getLong(1)).isZero();}
            }
        }
    }
}
