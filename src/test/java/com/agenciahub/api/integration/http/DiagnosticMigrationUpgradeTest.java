package com.agenciahub.api.integration.http;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
class DiagnosticMigrationUpgradeTest {
 @Test void v65ToV66PreservesExistingWalletAndCreatesNoPublicLinkOrSubmission()throws Exception{
  try(var pg=EmbeddedPostgres.builder().start()){
   String url=pg.getJdbcUrl("postgres","postgres");Flyway.configure().dataSource(url,"postgres","").target("65").load().migrate();
   try(var c=pg.getPostgresDatabase().getConnection();var s=c.createStatement()){
    s.execute("INSERT INTO customers(id,agency_id,name,email,phone,interest_destination,status,notes,created_at) VALUES('b0660000-0000-0000-0000-000000000001','a0000000-0000-0000-0000-000000000000','Anterior D66','','','','ACTIVE','Preservar',now())");
    s.execute("INSERT INTO customer_miles_profiles(customer_id,agency_id,data,version) VALUES('b0660000-0000-0000-0000-000000000001','a0000000-0000-0000-0000-000000000000','{\"goals\":\"Meta antiga\"}',3)");
    s.execute("INSERT INTO customer_miles_items(id,customer_id,agency_id,kind,data) VALUES('b0660000-0000-0000-0000-000000000002','b0660000-0000-0000-0000-000000000001','a0000000-0000-0000-0000-000000000000','CARD','{\"issuer\":\"Banco antigo\",\"category\":\"Platinum\"}')");
   }
   assertThat(Flyway.configure().dataSource(url,"postgres","").target("66").load().migrate().migrationsExecuted).isEqualTo(1);
   try(var c=pg.getPostgresDatabase().getConnection();var s=c.createStatement()){
    try(var r=s.executeQuery("SELECT data->>'goals',version FROM customer_miles_profiles WHERE customer_id='b0660000-0000-0000-0000-000000000001'")){r.next();assertThat(r.getString(1)).isEqualTo("Meta antiga");assertThat(r.getInt(2)).isEqualTo(3);}
    try(var r=s.executeQuery("SELECT data->>'issuer' FROM customer_miles_items WHERE id='b0660000-0000-0000-0000-000000000002'")){r.next();assertThat(r.getString(1)).isEqualTo("Banco antigo");}
    for(String table:java.util.List.of("miles_diagnostic_configs","miles_diagnostics","miles_diagnostic_events"))try(var r=s.executeQuery("SELECT count(*) FROM "+table)){r.next();assertThat(r.getInt(1)).isZero();}
   }
  }
 }
}
