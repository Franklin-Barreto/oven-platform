package br.com.f2e.ovenplatform.identity.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.DriverManager;
import java.util.Map;
import java.util.UUID;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class TenantMembershipRolesMigrationTest {

  @Container
  private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

  private JdbcTemplate jdbc;

  @BeforeEach
  void setUpLegacySchema() throws Exception {
    var dataSource =
        new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    jdbc = new JdbcTemplate(dataSource);
    jdbc.execute("DROP TABLE IF EXISTS databasechangeloglock");
    jdbc.execute("DROP TABLE IF EXISTS databasechangelog");
    jdbc.execute("DROP TABLE IF EXISTS tenant_membership_roles");
    jdbc.execute("DROP TABLE IF EXISTS tenant_memberships");
    jdbc.execute(
        """
        CREATE TABLE tenant_memberships (
          id UUID PRIMARY KEY,
          role VARCHAR(50) NOT NULL
        )
        """);
    insertLegacyMembership("10000000-0000-0000-0000-000000000001", "OWNER");
    insertLegacyMembership("10000000-0000-0000-0000-000000000002", "ADMIN");
    insertLegacyMembership("10000000-0000-0000-0000-000000000003", "MEMBER");

    var liquibase = new SpringLiquibase();
    liquibase.setDataSource(dataSource);
    liquibase.setChangeLog(
        "classpath:db/changelog/changes/db.changelog-019-add-tenant-membership-roles.xml");
    liquibase.afterPropertiesSet();
  }

  @Test
  void shouldBackfillLegacyRolesAndRemoveSingularColumn() throws Exception {
    var migratedRoles =
        jdbc.query(
            "SELECT membership_id, role FROM tenant_membership_roles",
            resultSet -> {
              var roles = new java.util.HashMap<UUID, String>();
              while (resultSet.next()) {
                roles.put(
                    resultSet.getObject("membership_id", UUID.class), resultSet.getString("role"));
              }
              return roles;
            });

    assertThat(migratedRoles)
        .containsExactlyInAnyOrderEntriesOf(
            Map.of(
                UUID.fromString("10000000-0000-0000-0000-000000000001"), "OWNER",
                UUID.fromString("10000000-0000-0000-0000-000000000002"), "MANAGER",
                UUID.fromString("10000000-0000-0000-0000-000000000003"), "ATTENDANT"));

    try (var connection =
            DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        var columns =
            connection.getMetaData().getColumns(null, null, "tenant_memberships", "role")) {
      assertThat(columns.next()).isFalse();
    }
  }

  @Test
  void shouldEnforceUniqueAssignmentsAndCascadeDeletion() {
    var membershipId = UUID.fromString("10000000-0000-0000-0000-000000000001");

    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO tenant_membership_roles (membership_id, role) VALUES (?, ?)",
                    membershipId,
                    "OWNER"))
        .isInstanceOf(DuplicateKeyException.class);

    jdbc.update("DELETE FROM tenant_memberships WHERE id = ?", membershipId);

    assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM tenant_membership_roles WHERE membership_id = ?",
                Long.class,
                membershipId))
        .isZero();
  }

  private void insertLegacyMembership(String id, String role) {
    jdbc.update(
        "INSERT INTO tenant_memberships (id, role) VALUES (?, ?)", UUID.fromString(id), role);
  }
}
