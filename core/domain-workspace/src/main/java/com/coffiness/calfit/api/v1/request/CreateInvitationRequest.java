package com.coffiness.calfit.api.v1.request;

import com.coffiness.calfit.core.enums.MemberType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateInvitationRequest(
    @NotBlank @Email String email, @NotNull MemberType memberType) {}
