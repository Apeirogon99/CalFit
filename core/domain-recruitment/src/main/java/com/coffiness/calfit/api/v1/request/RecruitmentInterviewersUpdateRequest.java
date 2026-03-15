package com.coffiness.calfit.api.v1.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/*
 * 채용 공고의 면접관 목록만 별도로 수정하는 DTO
 * */
public record RecruitmentInterviewersUpdateRequest(
    @NotNull(message = "면접관 지정은 필수입니다.") @Size(min = 1, message = "면접관은 최소 1명 이상이어야 합니다.")
        List<@NotNull(message = "interviewerIds의 원소는 null일 수 없습니다.") Long> interviewerIds) {}
