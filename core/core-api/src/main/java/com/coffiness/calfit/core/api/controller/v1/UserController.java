package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.api.v1.request.LoginRequest;
import com.coffiness.calfit.api.v1.request.SignUpRequest;
import com.coffiness.calfit.api.v1.response.LoginResponse;
import com.coffiness.calfit.api.v1.response.UserResponse;
import com.coffiness.calfit.api.v1.response.UserWorkspaceResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.user.EmailVerificationService;
import com.coffiness.calfit.domain.user.User;
import com.coffiness.calfit.domain.user.UserService;
import com.coffiness.calfit.domain.workspace.Workspace;
import com.coffiness.calfit.domain.workspace.WorkspaceReader;
import com.coffiness.calfit.support.email.EmailPageService;
import com.coffiness.calfit.support.email.EmailProperties;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;
  private final WorkspaceReader workspaceReader;
  private final EmailVerificationService emailVerificationService;
  private final EmailProperties emailProperties;
  private final EmailPageService emailPageService;

  @PostMapping("/api/v1/users/signup")
  public ApiResponse<UserResponse> signUp(@Valid @RequestBody SignUpRequest request) {
    User user = userService.signUp(request.email(), request.password(), request.name());
    emailVerificationService.generateTokenAndSendEmail(
        user.email(), user.name(), emailProperties.getSignupVerificationUrl());
    return ApiResponse.success(UserResponse.from(user));
  }

  @PostMapping("/api/v1/users/signup/resend-verification")
  public ApiResponse<?> resendVerificationEmail(@RequestParam String email) {
    User user = userService.getUserByEmail(email);
    emailVerificationService.generateTokenAndSendEmail(
        user.email(), user.name(), emailProperties.getSignupVerificationUrl());
    return ApiResponse.success();
  }

  @GetMapping(value = "/api/v1/users/signup/verify", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> verifySignUpEmail(@RequestParam String token) {
    boolean verified = emailVerificationService.verifyEmailToken(token);
    String page =
        verified
            ? emailPageService.renderSignupVerificationSuccessPage(
                emailProperties.getWorkspaceCreateUrl(), emailProperties.getLoginUrl())
            : emailPageService.renderSignupVerificationFailurePage(emailProperties.getLoginUrl());
    return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(page);
  }

  @PostMapping("/api/v1/users/login")
  public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    UserService.LoginResult result = userService.login(request.email(), request.password());
    LoginResponse response =
        new LoginResponse(
            result.accessToken(),
            result.refreshToken(),
            UserResponse.from(result.user()),
            result.workspaceId());
    return ApiResponse.success(response);
  }

  @GetMapping("/api/v1/users/me")
  public ApiResponse<UserResponse> getMe(@AuthenticationPrincipal SecurityUser securityUser) {
    User user = userService.getUser(securityUser.userId());
    return ApiResponse.success(UserResponse.from(user));
  }

  @GetMapping("/api/v1/users/me/workspace")
  public ApiResponse<UserWorkspaceResponse> getMyWorkspace(
      @AuthenticationPrincipal SecurityUser securityUser) {
    String workspaceId = userService.getWorkspaceId(securityUser.userId());
    Workspace workspace = workspaceId != null ? workspaceReader.getWorkspace(workspaceId) : null;
    return ApiResponse.success(
        new UserWorkspaceResponse(workspaceId, workspace != null ? workspace.name() : null));
  }

  @DeleteMapping("/api/v1/users/me")
  public ApiResponse<?> deleteMe(@AuthenticationPrincipal SecurityUser securityUser) {
    userService.deleteUser(securityUser.userId());
    return ApiResponse.success();
  }
}
