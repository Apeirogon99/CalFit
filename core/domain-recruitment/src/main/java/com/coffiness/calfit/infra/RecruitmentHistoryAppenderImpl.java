package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.RecruitmentActionType;
import com.coffiness.calfit.domain.recruitment.Recruitment;
import com.coffiness.calfit.domain.recruitment.RecruitmentHistoryAppender;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentHistoryEntity;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentHistoryRepository;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecruitmentHistoryAppenderImpl implements RecruitmentHistoryAppender {

  private final RecruitmentHistoryRepository recruitmentHistoryRepository;

  @Override
  public void append(
      Long recruitmentId,
      Long actorId,
      RecruitmentActionType actionType,
      String reason,
      Recruitment recruitmentSnapShot) {

    // 변경 로그 JSON
    Map<String, Object> changeLog = new HashMap<>();
    if (recruitmentSnapShot != null) {
      changeLog.put("title", recruitmentSnapShot.title());
      changeLog.put("targetCount", recruitmentSnapShot.targetCount());
      changeLog.put(
          "status",
          recruitmentSnapShot.recruitmentStatus() != null
              ? recruitmentSnapShot.recruitmentStatus().name()
              : null);
      changeLog.put(
          "careerType",
          recruitmentSnapShot.careerType() != null
              ? recruitmentSnapShot.careerType().name()
              : null);
      changeLog.put(
          "startDate",
          recruitmentSnapShot.startDate() != null
              ? recruitmentSnapShot.startDate().toString()
              : null);
      changeLog.put(
          "endDate",
          recruitmentSnapShot.endDate() != null ? recruitmentSnapShot.endDate().toString() : null);
    }

    RecruitmentHistoryEntity entity =
        RecruitmentHistoryEntity.builder()
            .actorId(actorId)
            .recruitmentId(recruitmentId)
            .reason(reason)
            .recruitmentActionType(actionType)
            .changeLog(changeLog)
            .stageId(null)
            .build();

    recruitmentHistoryRepository.save(entity);
  }
}
