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
  basePackages = "com.agentbanking.commission.infrastructure.secondary",
  entityManagerFactoryRef = "commissionEntityManagerFactory",
  transactionManagerRef = "commissionTransactionManager"
)
public class CommissionDatabaseConfig {

  private static final Logger log = LoggerFactory.getLogger(CommissionDatabaseConfig.class);

  @Value("${spring.datasource.commission.url}")
  private String url;

  @Value("${spring.datasource.commission.username}")
  private String username;

  @Value("${spring.datasource.commission.password}")
  private String password;

  @Value("${spring.datasource.commission.driver-class-name:org.postgresql.Driver}")
  private String driverClassName;

  @Bean(name = "commissionDataSource")
  public DataSource commissionDataSource() {
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
    dataSource.setPoolName("commissionHikariPool");
    log.info("Configured commission DataSource: {}", url);
    return dataSource;
  }

  @Bean(name = "commissionEntityManagerFactory")
  public LocalContainerEntityManagerFactoryBean commissionEntityManagerFactory(
    @Qualifier("commissionDataSource") DataSource dataSource
  ) {
    var factory = new LocalContainerEntityManagerFactoryBean();
    factory.setDataSource(dataSource);
    factory.setPackagesToScan("com.agentbanking.commission.infrastructure.secondary");
    factory.setPersistenceUnitName("commission");
    factory.setJpaVendorAdapter(jpaVendorAdapter());
    factory.setJpaProperties(jpaProperties());
    return factory;
  }

  @Bean(name = "commissionTransactionManager")
  public PlatformTransactionManager commissionTransactionManager(
    @Qualifier("commissionEntityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory
  ) {
    return new JpaTransactionManager(Objects.requireNonNull(entityManagerFactory.getObject()));
  }

  @Bean(name = "commissionFlyway")
  public Flyway commissionFlyway(@Qualifier("commissionDataSource") DataSource dataSource) {
    log.info("Initializing Flyway for commission database");
    return Flyway.configure()
      .dataSource(dataSource)
      .locations("classpath:db/migration/commission")
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