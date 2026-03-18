package com.coffiness.calfit.domain.recruitment;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * 채용 이력 변경 로그 표준 모델
 * */
public record RecruitmentHistoryChangeLog(
    String scope,
    Map<String, Object> before,
    Map<String, Object> after,
    List<String> changedFields) {

  // 채용 이력 JSON 변환
  public Map<String, Object> toMap() {
    Map<String, Object> changeLog = new LinkedHashMap<>();
    changeLog.put("scope", scope);
    changeLog.put("before", before);
    changeLog.put("after", after);
    changeLog.put("changedFields", changedFields == null ? List.of() : List.copyOf(changedFields));
    return changeLog;
  }
}
