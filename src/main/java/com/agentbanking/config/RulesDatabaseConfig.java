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
import org.springframework.context.annotation.Primary;
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
  basePackages = "com.agentbanking.rules.infrastructure.secondary",
  entityManagerFactoryRef = "rulesEntityManagerFactory",
  transactionManagerRef = "rulesTransactionManager"
)
public class RulesDatabaseConfig {

  private static final Logger log = LoggerFactory.getLogger(RulesDatabaseConfig.class);

  @Value("${spring.datasource.rules.url}")
  private String url;

  @Value("${spring.datasource.rules.username}")
  private String username;

  @Value("${spring.datasource.rules.password}")
  private String password;

  @Value("${spring.datasource.rules.driver-class-name:org.postgresql.Driver}")
  private String driverClassName;

  @Primary
  @Bean(name = "rulesDataSource")
  public DataSource rulesDataSource() {
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
    dataSource.setPoolName("rulesHikariPool");
    log.info("Configured rules DataSource: {}", url);
    return dataSource;
  }

  @Primary
  @Bean(name = "rulesEntityManagerFactory")
  public LocalContainerEntityManagerFactoryBean rulesEntityManagerFactory(
    @Qualifier("rulesDataSource") DataSource dataSource
  ) {
    var factory = new LocalContainerEntityManagerFactoryBean();
    factory.setDataSource(dataSource);
    factory.setPackagesToScan("com.agentbanking.rules.infrastructure.secondary");
    factory.setPersistenceUnitName("rules");
    factory.setJpaVendorAdapter(jpaVendorAdapter());
    factory.setJpaProperties(jpaProperties());
    return factory;
  }

  @Primary
  @Bean(name = "rulesTransactionManager")
  public PlatformTransactionManager rulesTransactionManager(
    @Qualifier("rulesEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory
  ) {
    return new JpaTransactionManager(Objects.requireNonNull(entityManagerFactory.getObject()));
  }

  @Primary
  @Bean(name = "rulesFlyway")
  public Flyway rulesFlyway(@Qualifier("rulesDataSource") DataSource dataSource) {
    log.info("Initializing Flyway for rules database");
    return Flyway.configure()
      .dataSource(dataSource)
      .locations("classpath:db/migration/rules")
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
