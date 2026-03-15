package com.coffiness.calfit.domain.applicant;

import com.coffiness.calfit.storage.db.core.applicant.ApplicantEntity;
import com.coffiness.calfit.storage.db.core.applicant.ApplicantRepository;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import com.coffiness.calfit.support.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApplicantPortalService {

  private static final String APPLICANT_ROLE = "APPLICANT";
  private static final String DEFAULT_TENANT = "default";

  private final ApplicantRepository applicantRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;

  @Transactional
  public Applicant signUp(String email, String password, String name) {
    requireTenant();
    if (applicantRepository.existsByEmail(email)) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }

    ApplicantEntity saved =
        applicantRepository.save(
            ApplicantEntity.create(email, passwordEncoder.encode(password), name));
    return toApplicant(saved);
  }

  @Transactional(readOnly = true)
  public ApplicantLoginResult login(String email, String password) {
    requireTenant();
    ApplicantEntity applicant =
        applicantRepository
            .findByEmail(email)
            .orElseThrow(() -> new CoreException(ErrorType.UNAUTHORIZED));

    if (!passwordEncoder.matches(password, applicant.getPassword())) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }

    String token =
        jwtTokenProvider.createAccessToken(applicant.getId(), applicant.getEmail(), APPLICANT_ROLE);
    return new ApplicantLoginResult(token, toApplicant(applicant));
  }

  private void requireTenant() {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank() || DEFAULT_TENANT.equals(tenantId)) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
  }

  private Applicant toApplicant(ApplicantEntity entity) {
    return new Applicant(
        entity.getId(),
        entity.getTenantId(),
        entity.getEmail(),
        entity.getName(),
        entity.getCreatedAt());
  }

  public record ApplicantLoginResult(String accessToken, Applicant applicant) {}
}
