package com.agentbanking.config;

import com.zaxxer.hikari.HikariDataSource;
import java.util.Objects;
import java.util.Properties;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
  basePackages = "com.agentbanking.onboarding.infrastructure.secondary",
  entityManagerFactoryRef = "onboardingEntityManagerFactory",
  transactionManagerRef = "onboardingTransactionManager"
)
public class OnboardingDatabaseConfig {

  private static final Logger log = LoggerFactory.getLogger(OnboardingDatabaseConfig.class);

  @Value("${spring.datasource.onboarding.url}")
  private String url;

  @Value("${spring.datasource.onboarding.username}")
  private String username;

  @Value("${spring.datasource.onboarding.password}")
  private String password;

  @Value("${spring.datasource.onboarding.driver-class-name:org.postgresql.Driver}")
  private String driverClassName;

  @Bean(name = "onboardingDataSource")
  public DataSource onboardingDataSource() {
    var dataSource = new HikariDataSource();
    dataSource.setJdbcUrl(url);
    dataSource.setUsername(username);
    dataSource.setPassword(password);
    dataSource.setDriverClassName(driverClassName);
    dataSource.setAutoCommit(false);
    dataSource.setMaximumPoolSize(10);
    dataSource.setMinimumIdle(5);
    dataSource.setIdleTimeout(300000);
    dataSource.setConnectionTimeout(20000);
    dataSource.setMaxLifetime(1200000);
    dataSource.setPoolName("onboardingHikariPool");
    log.info("Configured onboarding DataSource: {}", url);
    return dataSource;
  }

  @Bean(name = "onboardingEntityManagerFactory")
  public LocalContainerEntityManagerFactoryBean onboardingEntityManagerFactory(
    @Qualifier("onboardingDataSource") DataSource dataSource
  ) {
    var factory = new LocalContainerEntityManagerFactoryBean();
    factory.setDataSource(dataSource);
    factory.setPackagesToScan("com.agentbanking.onboarding.infrastructure.secondary");
    factory.setPersistenceUnitName("onboarding");
    factory.setJpaVendorAdapter(jpaVendorAdapter());
    factory.setJpaProperties(jpaProperties());
    return factory;
  }

  @Bean(name = "onboardingTransactionManager")
  public PlatformTransactionManager onboardingTransactionManager(
    @Qualifier("onboardingEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory
  ) {
    return new JpaTransactionManager(Objects.requireNonNull(entityManagerFactory.getObject()));
  }

  @Bean(name = "onboardingFlyway")
  public Flyway onboardingFlyway(@Qualifier("onboardingDataSource") DataSource dataSource) {
    log.info("Initializing Flyway for onboarding database");
    return Flyway.configure()
      .dataSource(dataSource)
      .locations("classpath:db/migration/onboarding")
      .baselineOnMigrate(true)
      .validateOnMigrate(true)
      .load();
  }

  private JpaVendorAdapter jpaVendorAdapter() {
    var adapter = new HibernateJpaVendorAdapter();
    adapter.setShowSql(false);
    adapter.setGenerateDdl(false);
    return adapter;
  }

  private Properties jpaProperties() {
    var props = new Properties();
    props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
    props.put("hibernate.hbm2ddl.auto", "validate");
    props.put("hibernate.show_sql", "false");
    props.put("hibernate.format_sql", "true");
    props.put("hibernate.jdbc.batch_size", "50");
    props.put("hibernate.order_inserts", "true");
    props.put("hibernate.order_updates", "true");
    return props;
  }
}
