package com.coffiness.calfit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@ConfigurationPropertiesScan
@SpringBootApplication
@EnableFeignClients(basePackages = "com.coffiness.calfit.api")
@EnableScheduling
public class CoreApiApplication {

  public static void main(String[] args) {
    SpringApplication.run(CoreApiApplication.class, args);
  }
}
