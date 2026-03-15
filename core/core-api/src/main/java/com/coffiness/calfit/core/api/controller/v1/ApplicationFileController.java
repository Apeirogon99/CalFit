package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.api.v1.request.CompleteUploadRequest;
import com.coffiness.calfit.api.v1.request.PresignUploadRequest;
import com.coffiness.calfit.api.v1.response.CompleteUploadResponse;
import com.coffiness.calfit.api.v1.response.PresignDownloadResponse;
import com.coffiness.calfit.api.v1.response.PresignUploadResponse;
import com.coffiness.calfit.domain.applicationFile.ApplicationFileService;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/application-files")
public class ApplicationFileController {

  private final ApplicationFileService service;

  @PostMapping("/presign-upload")
  public PresignUploadResponse presignUpload(
      @AuthenticationPrincipal SecurityUser user, @RequestBody PresignUploadRequest req) {
    return service.presignUpload(req, requireUser(user));
  }

  @PostMapping("/complete")
  public CompleteUploadResponse complete(
      @AuthenticationPrincipal SecurityUser user, @RequestBody CompleteUploadRequest req) {
    return service.completeUpload(req, requireUser(user));
  }

  @GetMapping("/{fileId}/presign-download")
  public PresignDownloadResponse presignDownload(
      @AuthenticationPrincipal SecurityUser user, @PathVariable Long fileId) {
    return service.presignDownload(fileId, requireUser(user));
  }

  @GetMapping("/health")
  public String health() {
    return "ok";
  }

  private SecurityUser requireUser(SecurityUser user) {
    if (user == null) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    return user;
  }
}
