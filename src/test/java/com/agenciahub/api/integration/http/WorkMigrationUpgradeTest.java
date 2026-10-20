package com.agenciahub.api.integration.http;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/** Sequential upgrade from the integrated V59, preserving existing data. */
class WorkMigrationUpgradeTest {
    @Test void upgradingCurrentV59PreservesExistingDataAndCreatesTaskTables()throws Exception{
        try(EmbeddedPostgres pg=EmbeddedPostgres.builder().start()){
            String url=pg.getJdbcUrl("postgres","postgres");
            Flyway.configure().dataSource(url,"postgres","").target("59").load().migrate();
            try(var c=pg.getPostgresDatabase().getConnection();var sql=c.createStatement()){
                sql.execute("CREATE TABLE f01_upgrade_marker(value text)");sql.execute("INSERT INTO f01_upgrade_marker VALUES('preserved')");
            }
            Flyway.configure().dataSource(url,"postgres","").load().migrate();
            try(var c=pg.getPostgresDatabase().getConnection();var sql=c.createStatement()){
                try(var rows=sql.executeQuery("SELECT value FROM f01_upgrade_marker")){rows.next();assertThat(rows.getString(1)).isEqualTo("preserved");}
                try(var rows=sql.executeQuery("SELECT count(*) FROM flyway_schema_history WHERE version='60' AND success")){rows.next();assertThat(rows.getInt(1)).isEqualTo(1);}
                try(var rows=sql.executeQuery("SELECT count(*) FROM work_tasks")){rows.next();assertThat(rows.getInt(1)).isZero();}
            }
        }
    }
}
