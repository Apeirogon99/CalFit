package com.coffiness.calfit.api.v1.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApplicantSignUpRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 4, max = 20) String password,
    @NotBlank @Size(min = 2, max = 50) String name) {}
