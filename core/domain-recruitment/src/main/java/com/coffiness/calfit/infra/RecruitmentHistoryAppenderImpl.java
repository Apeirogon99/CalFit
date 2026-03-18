package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.RecruitmentActionType;
import com.coffiness.calfit.domain.recruitment.RecruitmentHistoryAppender;
import com.coffiness.calfit.domain.recruitment.RecruitmentHistoryChangeLog;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentHistoryEntity;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentHistoryRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecruitmentHistoryAppenderImpl implements RecruitmentHistoryAppender {

  private final RecruitmentHistoryRepository recruitmentHistoryRepository;

  // 채용 공고 이력 적재
  @Override
  public void append(
      Long recruitmentId,
      Long actorId,
      RecruitmentActionType actionType,
      String reason,
      RecruitmentHistoryChangeLog changeLog) {
    saveHistory(recruitmentId, null, actorId, actionType, reason, changeLog.toMap());
  }

  // 채용 단계 이력 적재
  @Override
  public void appendStage(
      Long recruitmentId,
      Long stageId,
      Long actorId,
      RecruitmentActionType actionType,
      String reason,
      RecruitmentHistoryChangeLog changeLog) {
    saveHistory(recruitmentId, stageId, actorId, actionType, reason, changeLog.toMap());
  }

  // 채용 이력 저장
  private void saveHistory(
      Long recruitmentId,
      Long stageId,
      Long actorId,
      RecruitmentActionType actionType,
      String reason,
      Map<String, Object> changeLog) {
    recruitmentHistoryRepository.save(
        RecruitmentHistoryEntity.builder()
            .actorId(actorId)
            .recruitmentId(recruitmentId)
            .reason(reason)
            .recruitmentActionType(actionType)
            .changeLog(changeLog)
            .stageId(stageId)
            .build());
  }
}
