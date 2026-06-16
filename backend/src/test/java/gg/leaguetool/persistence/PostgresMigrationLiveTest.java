package gg.leaguetool.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Applies the Flyway baseline to a real Postgres and then boots Hibernate with
 * {@code hbm2ddl.auto=validate}: if the migration and the JPA entities disagree, the validator
 * throws and this test fails. Skips automatically when no Postgres is reachable (e.g. in CI), so it
 * never blocks the build; run a local Postgres (see infra/docker-compose) to execute it.
 */
class PostgresMigrationLiveTest {

    private static final String URL = "jdbc:postgresql://localhost:5432/leaguetool";
    private static final String USER = "leaguetool";
    private static final String PASS = "leaguetool";

    private DataSource dataSource;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = new DriverManagerDataSource(URL, USER, PASS);
        assumeTrue(reachable(dataSource), "Postgres not reachable on localhost:5432 - skipping");
        // Start from a clean slate so migrate() runs the baseline fresh.
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            s.execute("DROP TABLE IF EXISTS match_participant, matches, champion_role_stat, "
                    + "champion_pair_stat, champion_matchup_stat, flyway_schema_history CASCADE");
        }
    }

    @Test
    void baselineMigrationMatchesTheEntities() {
        Flyway flyway = Flyway.configure()
                .dataSource(URL, USER, PASS)
                .locations("classpath:db/migration")
                .load();
        int applied = flyway.migrate().migrationsExecuted;
        assertThat(applied).isGreaterThanOrEqualTo(1);

        // Hibernate validate against the migrated schema; afterPropertiesSet() throws on any mismatch.
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPackagesToScan("gg.leaguetool.persistence");
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        Properties props = new Properties();
        props.put("hibernate.hbm2ddl.auto", "validate");
        props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        // Mirror Spring Boot's default camelCase -> snake_case mapping so validation sees the same
        // physical column names the application (and the migration) use.
        props.put("hibernate.physical_naming_strategy",
                "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        emf.setJpaProperties(props);
        try {
            emf.afterPropertiesSet();
        } finally {
            emf.destroy();
        }
    }

    private static boolean reachable(DataSource ds) {
        try (Connection c = ds.getConnection()) {
            return c.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }
}
