package com.coffiness.calfit.api.v1.request;

import com.coffiness.calfit.core.enums.Gender;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record ApplicationCreateRequest(
    Long applicantId,
    @NotNull Long recruitmentId,
    Long recruitmentProcessId,
    Long templateId,
    @NotBlank @Size(max = 50) String name,
    @NotNull Gender gender,
    @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthDate,
    @NotBlank @Size(max = 20) String phone,
    @NotBlank @Email @Size(max = 50) String email,
    @JsonProperty("formFields") @JsonAlias("schema") @NotNull JsonNode formFields) {}
