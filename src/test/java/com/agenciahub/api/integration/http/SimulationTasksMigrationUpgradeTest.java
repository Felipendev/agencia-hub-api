package com.agenciahub.api.integration.http;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
class SimulationTasksMigrationUpgradeTest {
    @Test void v64ToV65PreservesExistingCrmAndDoesNotGenerateAnything()throws Exception{
        try(var pg=EmbeddedPostgres.builder().start()){
            String url=pg.getJdbcUrl("postgres","postgres");Flyway.configure().dataSource(url,"postgres","").target("64").load().migrate();
            try(var c=pg.getPostgresDatabase().getConnection();var s=c.createStatement()){
                s.execute("INSERT INTO customers(id,agency_id,name,email,phone,interest_destination,status,notes,created_at) VALUES('b0650000-0000-0000-0000-000000000001','a0000000-0000-0000-0000-000000000000','Pessoa anterior P65','','','','ACTIVE','Preservar',now())");
                s.execute("INSERT INTO accumulation_simulations(id,agency_id,customer_id,title,assumptions,created_by) VALUES('b0650000-0000-0000-0000-000000000002','a0000000-0000-0000-0000-000000000000','b0650000-0000-0000-0000-000000000001','Plano anterior','"+SimulationCustomerIntegrationTest.INPUT.replace("Plano pessoal","Plano anterior")+"','legado')");
                s.execute("INSERT INTO work_tasks(id,agency_id,customer_id,product,title,due_on,creation_key,creation_payload) VALUES('b0650000-0000-0000-0000-000000000003','a0000000-0000-0000-0000-000000000000','b0650000-0000-0000-0000-000000000001','MILES','Tarefa anterior','2026-11-01','b0650000-0000-0000-0000-000000000004','{}')");
            }
            assertThat(Flyway.configure().dataSource(url,"postgres","").target("65").load().migrate().migrationsExecuted).isEqualTo(1);
            try(var c=pg.getPostgresDatabase().getConnection();var s=c.createStatement()){
                try(var r=s.executeQuery("SELECT name,notes FROM customers WHERE id='b0650000-0000-0000-0000-000000000001'")){r.next();assertThat(r.getString(1)).isEqualTo("Pessoa anterior P65");assertThat(r.getString(2)).isEqualTo("Preservar");}
                try(var r=s.executeQuery("SELECT title,version,assumptions->>'monthlySpending' FROM accumulation_simulations WHERE id='b0650000-0000-0000-0000-000000000002'")){r.next();assertThat(r.getString(1)).isEqualTo("Plano anterior");assertThat(r.getInt(2)).isEqualTo(1);assertThat(r.getString(3)).isEqualTo("10000");}
                try(var r=s.executeQuery("SELECT title,state,version FROM work_tasks WHERE id='b0650000-0000-0000-0000-000000000003'")){r.next();assertThat(r.getString(1)).isEqualTo("Tarefa anterior");assertThat(r.getString(2)).isEqualTo("OPEN");assertThat(r.getInt(3)).isEqualTo(1);}
                for(String table:java.util.List.of("simulation_task_generations","simulation_task_links"))try(var r=s.executeQuery("SELECT count(*) FROM "+table)){r.next();assertThat(r.getInt(1)).isZero();}
            }
        }
    }
}
