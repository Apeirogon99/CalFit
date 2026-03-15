package com.coffiness.calfit.api.v1.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ApplicantLoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
