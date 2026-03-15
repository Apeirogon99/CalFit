package com.coffiness.calfit.api.v1.request;

import com.coffiness.calfit.core.enums.CareerType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public record RecruitmentUpdateRequest(
    @NotBlank(message = "제목은 필수입니다.") @Size(max = 100, message = "제목은 100자 이하여야 합니다.") String title,
    @Min(value = 1, message = "채용 인원은 최소 1명 이상이어야 합니다.") int targetCount,
    @NotNull(message = "채용 공고 템플릿은 필수입니다.") Long applicationTemplateId,
    @NotBlank(message = "내용은 필수입니다.") @Size(max = 2000, message = "내용은 2000자 이하여야 합니다.")
        String contents,
    @NotNull(message = "채용 시작 일자 지정은 필수입니다.") LocalDateTime startDate,
    @NotNull(message = "채용 마감 일자 지정은 필수입니다.") LocalDateTime endDate,
    @NotNull(message = "경력 구분 지정은 필수입니다.") CareerType careerType,
    @Min(value = 0, message = "최소 연차는 0 이상이어야 합니다.") Integer minExperienceYears,
    @Min(value = 0, message = "최대 연차는 0 이상이어야 합니다.") Integer maxExperienceYears,
    @NotNull(message = "채용 담당 그룹 지정은 필수입니다.") Long leadGroupId,
    List<Long> referenceGroupIds,
    @NotNull(message = "면접관 지정은 필수입니다.") @Size(min = 1, message = "면접관은 최소 1명 이상이어야 합니다.")
        List<@NotNull(message = "interviewerIds의 원소는 null일 수 없습니다.") Long> interviewerIds,
    @NotNull(message = "채용 단계는 필수입니다.") @Size(min = 1, message = "채용 단계는 최소 1가지 이상이어야 합니다.")
        List<@Valid @NotNull(message = "stages 값은 null일 수 없습니다.") RecruitmentStageRequest>
            stages) {}
