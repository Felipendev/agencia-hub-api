package com.agenciahub.api.integration.http;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class SimulationCustomerMigrationTest {
    @Test void migrationPreservesLegacyIdsResultsAuthorsAndUnlinkedVersions()throws Exception{
        try(EmbeddedPostgres pg=EmbeddedPostgres.builder().start()){
            String url=pg.getJdbcUrl("postgres","postgres");UUID id=UUID.randomUUID();
            Flyway.configure().dataSource(url,"postgres","").target("60").load().migrate();
            try(var connection=pg.getPostgresDatabase().getConnection();var statement=connection.prepareStatement("INSERT INTO accumulation_simulations(id,agency_id,title,assumptions,created_by) SELECT ?,id,'Plano pessoal',?::jsonb,'autor-legado' FROM agencies LIMIT 1")){
                statement.setObject(1,id);statement.setString(2,SimulationCustomerIntegrationTest.INPUT);statement.executeUpdate();
                try(var version=connection.prepareStatement("INSERT INTO accumulation_simulation_versions(id,simulation_id,version,assumptions,author) VALUES(?,?,1,?::jsonb,'autor-legado')")){version.setObject(1,UUID.randomUUID());version.setObject(2,id);version.setString(3,SimulationCustomerIntegrationTest.INPUT);version.executeUpdate();}
            }
            Flyway.configure().dataSource(url,"postgres","").load().migrate();
            try(var connection=pg.getPostgresDatabase().getConnection();var query=connection.prepareStatement("SELECT s.id,s.customer_id,v.customer_id old_customer,v.author,s.assumptions->>'monthlySpending' spending FROM accumulation_simulations s JOIN accumulation_simulation_versions v ON v.simulation_id=s.id WHERE s.id=?")){
                query.setObject(1,id);try(var rows=query.executeQuery()){assertThat(rows.next()).isTrue();assertThat(rows.getObject("id",UUID.class)).isEqualTo(id);assertThat(rows.getObject("customer_id")).isNull();assertThat(rows.getObject("old_customer")).isNull();assertThat(rows.getString("author")).isEqualTo("autor-legado");assertThat(rows.getString("spending")).isEqualTo("10000");}
            }
        }
    }
}
