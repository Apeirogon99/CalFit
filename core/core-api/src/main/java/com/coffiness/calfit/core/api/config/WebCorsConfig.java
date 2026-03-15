package com.coffiness.calfit.core.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@Configuration
public class WebCorsConfig extends WebMvcConfig {

  private final CorsProperties corsProperties;

  public WebCorsConfig(TenantInterceptor tenantInterceptor, CorsProperties corsProperties) {
    super(tenantInterceptor);
    this.corsProperties = corsProperties;
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    String[] origins = corsProperties.allowedOrigins().toArray(new String[0]);

    registry
        .addMapping("/api/**")
        .allowedOrigins(origins)
        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true);
  }
}
