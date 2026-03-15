package com.coffiness.calfit.storage.db.core.recruitment;

import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.RecruitmentStatus;
import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * 채용 엔티티
 * */
@Entity
@Table(name = "recruitments")
@NoArgsConstructor
@Getter
public class RecruitmentEntity extends TenantBaseEntity {

  // 채용 작성자 ID
  @Column(name = "creator_id", nullable = false)
  private Long creatorId;

  // 채용 제목
  @Column(name = "title", nullable = false)
  private String title;

  // 공고 상태(대기, 진행중, 닫힘)
  @Enumerated(EnumType.STRING)
  @Column(name = "recruitment_status", nullable = false)
  private RecruitmentStatus recruitmentStatus;

  // 채용 인원
  @Column(name = "target_count", nullable = false)
  private int targetCount;

  // 채용 시작 날짜
  @Column(name = "start_date")
  private LocalDateTime startDate;

  // 채용 마감 날짜
  @Column(name = "end_date")
  private LocalDateTime endDate;

  // 지원서 템플릿 ID
  @Column(name = "application_template_id", nullable = false)
  private Long applicationTemplateId;

  // 채용 상세 내용
  @Lob
  @Column(name = "contents")
  private String contents;

  // 경력 구분
  @Enumerated(EnumType.STRING)
  @Column(name = "career_type", nullable = false)
  private CareerType careerType;

  // 최소 경력 연차
  @Column(name = "min_experience_years")
  private Integer minExperienceYears;

  // 최대 경력 연차
  @Column(name = "max_experience_years")
  private Integer maxExperienceYears;

  // 담당 조직 ID
  @Column(name = "lead_group_id", nullable = false)
  private Long leadGroupId;

  @Builder
  public RecruitmentEntity(
      Long creatorId,
      String title,
      RecruitmentStatus recruitmentStatus,
      int targetCount,
      LocalDateTime startDate,
      LocalDateTime endDate,
      Long applicationTemplateId,
      String contents,
      CareerType careerType,
      Integer minExperienceYears,
      Integer maxExperienceYears,
      Long leadGroupId) {
    this.creatorId = creatorId;
    this.title = title;
    this.recruitmentStatus = recruitmentStatus;
    this.targetCount = targetCount;
    this.startDate = startDate;
    this.endDate = endDate;
    this.applicationTemplateId = applicationTemplateId;
    this.contents = contents;
    this.careerType = careerType;
    this.minExperienceYears = minExperienceYears;
    this.maxExperienceYears = maxExperienceYears;
    this.leadGroupId = leadGroupId;
  }

  public void update(
      String title,
      RecruitmentStatus recruitmentStatus,
      int targetCount,
      LocalDateTime startDate,
      LocalDateTime endDate,
      Long applicationTemplateId,
      String contents,
      CareerType careerType,
      Integer minExperienceYears,
      Integer maxExperienceYears,
      Long leadGroupId) {
    this.title = title;
    this.recruitmentStatus = recruitmentStatus;
    this.targetCount = targetCount;
    this.startDate = startDate;
    this.endDate = endDate;
    this.applicationTemplateId = applicationTemplateId;
    this.contents = contents;
    this.careerType = careerType;
    this.minExperienceYears = minExperienceYears;
    this.maxExperienceYears = maxExperienceYears;
    this.leadGroupId = leadGroupId;
  }
}
