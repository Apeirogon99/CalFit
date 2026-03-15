package com.coffiness.calfit.api.v1.request;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

class ApplicationCreateRequestJsonAliasTest {

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @Test
  void schemaKeyIsMappedToFormFields() throws Exception {
    String payload =
        """
        {
          "recruitmentId": 1,
          "name": "Applicant",
          "gender": "MALE",
          "birthDate": "1995-01-01",
          "phone": "01012345678",
          "email": "applicant@test.com",
          "schema": {
            "motivation": "I want to join"
          }
        }
        """;

    ApplicationCreateRequest request =
        objectMapper.readValue(payload, ApplicationCreateRequest.class);

    assertThat(request.formFields().get("motivation").asText()).isEqualTo("I want to join");
  }

  @Test
  void formFieldsKeyIsMappedToFormFields() throws Exception {
    String payload =
        """
        {
          "recruitmentId": 1,
          "name": "Applicant",
          "gender": "MALE",
          "birthDate": "1995-01-01",
          "phone": "01012345678",
          "email": "applicant@test.com",
          "formFields": {
            "motivation": "I want to join"
          }
        }
        """;

    ApplicationCreateRequest request =
        objectMapper.readValue(payload, ApplicationCreateRequest.class);

    assertThat(request.formFields().get("motivation").asText()).isEqualTo("I want to join");
  }
}
