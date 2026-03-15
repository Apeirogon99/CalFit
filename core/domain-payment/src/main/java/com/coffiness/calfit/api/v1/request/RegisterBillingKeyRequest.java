package com.coffiness.calfit.api.v1.request;

import jakarta.validation.constraints.NotBlank;

public record RegisterBillingKeyRequest(@NotBlank String customerKey, @NotBlank String authKey) {}
