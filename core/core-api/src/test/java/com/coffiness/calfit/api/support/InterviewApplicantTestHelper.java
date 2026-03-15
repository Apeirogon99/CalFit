package com.coffiness.calfit.api.support;

import com.coffiness.calfit.core.enums.Gender;
import com.coffiness.calfit.storage.db.core.application.ApplicationEntity;
import com.coffiness.calfit.storage.db.core.application.ApplicationRepository;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import java.time.LocalDateTime;

/*
 * 면접 테스트에서 사용할 단계별 지원서 더미를 생성하는 헬퍼 클래스
 * */
public final class InterviewApplicantTestHelper {

  private InterviewApplicantTestHelper() {}

  // 현재 tenant의 특정 채용 단계에 대기 지원서를 저장
  public static Long createPendingApplication(
      String tenantId,
      ApplicationRepository applicationRepository,
      Long recruitmentId,
      Long recruitmentStageId,
      Long templateId,
      Long applicantId,
      String name,
      String email) {
    TenantContext.setTenantId(tenantId);
    try {
      ApplicationEntity application =
          applicationRepository.save(
              ApplicationEntity.create(
                  applicantId,
                  recruitmentId,
                  recruitmentStageId,
                  templateId,
                  name,
                  Gender.MALE,
                  LocalDateTime.of(1995, 1, 1, 0, 0),
                  "01012345678",
                  email,
                  "{}"));
      return application.getId();
    } finally {
      TenantContext.clear();
    }
  }
}
