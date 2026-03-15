package com.coffiness.calfit.domain.recruitment;

import com.coffiness.calfit.core.enums.RecruitmentActionType;
import org.springframework.stereotype.Repository;

@Repository
public interface RecruitmentHistoryAppender {
  void append(
      Long recruitmentId,
      Long actorId,
      RecruitmentActionType actionType,
      String reason,
      Recruitment recruitmentSnapShot);
}
