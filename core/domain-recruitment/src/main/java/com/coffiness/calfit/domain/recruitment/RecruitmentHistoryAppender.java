package com.coffiness.calfit.domain.recruitment;

import com.coffiness.calfit.core.enums.RecruitmentActionType;
import org.springframework.stereotype.Repository;

@Repository
public interface RecruitmentHistoryAppender {

  // 채용 공고 이력 적재
  void append(
      Long recruitmentId,
      Long actorId,
      RecruitmentActionType actionType,
      String reason,
      RecruitmentHistoryChangeLog changeLog);

  // 채용 단계 이력 적재
  void appendStage(
      Long recruitmentId,
      Long stageId,
      Long actorId,
      RecruitmentActionType actionType,
      String reason,
      RecruitmentHistoryChangeLog changeLog);
}
