package com.coffiness.calfit.storage.db.core.config;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EntityScan(
    basePackages = {"com.coffiness.calfit.storage.db.core", "com.coffiness.calfit.support.email"})
@EnableJpaRepositories(
    basePackages = {"com.coffiness.calfit.storage.db.core", "com.coffiness.calfit.support.email"})
class CoreJpaConfig {

  @Bean
  public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(
      CurrentTenantIdentifierResolver tenantIdentifierResolver) {

    return hibernateProperties -> {
      hibernateProperties.put(
          AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantIdentifierResolver);
    };
  }
}
