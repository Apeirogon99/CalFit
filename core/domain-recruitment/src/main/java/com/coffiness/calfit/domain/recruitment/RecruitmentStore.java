package com.coffiness.calfit.domain.recruitment;

import java.time.LocalDateTime;
import org.springframework.stereotype.Repository;

@Repository
public interface RecruitmentStore {

  Recruitment store(Recruitment recruitment);

  Recruitment update(Recruitment recruitment);

  // 채용 공고의 면접관 연결만 갱신
  Recruitment updateInterviewers(Recruitment recruitment);

  void delete(Long recruitmentId);

  // 시작 시각이 지난 DRAFT 공고를 OPEN으로 전환
  int openScheduledRecruitments(LocalDateTime currentTime);

  // 종료 시각이 지난 DRAFT/OPEN 공고를 CLOSED로 전환
  int closeEndedRecruitments(LocalDateTime currentTime);
}
