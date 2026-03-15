package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.RecruitmentStatus;
import com.coffiness.calfit.domain.recruitment.Recruitment;
import com.coffiness.calfit.domain.recruitment.RecruitmentStore;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentEntity;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentInterviewerRepository;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentReferenceGroupRepository;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentRepository;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentStageRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecruitmentStoreImpl implements RecruitmentStore {

  private final RecruitmentRepository recruitmentRepository;
  private final RecruitmentStageRepository recruitmentStageRepository;
  private final RecruitmentReferenceGroupRepository recruitmentReferenceGroupRepository;
  private final RecruitmentInterviewerRepository recruitmentInterviewerRepository;

  private final JdbcTemplate jdbcTemplate;

  @Override
  public Recruitment store(Recruitment recruitment) {
    RecruitmentEntity entity =
        RecruitmentEntity.builder()
            .creatorId(recruitment.creatorId())
            .title(recruitment.title())
            .recruitmentStatus(recruitment.recruitmentStatus())
            .targetCount(recruitment.targetCount())
            .startDate(recruitment.startDate())
            .endDate(recruitment.endDate())
            .applicationTemplateId(recruitment.applicationTemplateId())
            .contents(recruitment.contents())
            .careerType(recruitment.careerType())
            .minExperienceYears(recruitment.minExperienceYears())
            .maxExperienceYears(recruitment.maxExperienceYears())
            .leadGroupId(recruitment.leadGroupId())
            .build();

    RecruitmentEntity savedEntity = recruitmentRepository.save(entity);
    Long newId = savedEntity.getId();

    insertChildren(newId, recruitment);

    return new Recruitment(
        newId,
        recruitment.creatorId(),
        recruitment.title(),
        recruitment.contents(),
        recruitment.recruitmentStatus(),
        recruitment.targetCount(),
        recruitment.startDate(),
        recruitment.endDate(),
        recruitment.applicationTemplateId(),
        recruitment.careerType(),
        recruitment.minExperienceYears(),
        recruitment.maxExperienceYears(),
        recruitment.leadGroupId(),
        recruitment.referenceGroupIds(),
        recruitment.interviewerIds(),
        recruitment.stages());
  }

  @Override
  public Recruitment update(Recruitment recruitment) {
    if (recruitment.id() == null) {
      throw new IllegalArgumentException("수정할 채용 공고 ID가 없습니다.");
    }

    RecruitmentEntity entity =
        recruitmentRepository
            .findByIdAndStatus(recruitment.id(), EntityStatus.ACTIVE)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채용 공고입니다."));

    recruitmentStageRepository.deleteAllByRecruitmentId(recruitment.id());
    recruitmentInterviewerRepository.deleteAllByRecruitmentId(recruitment.id());
    recruitmentReferenceGroupRepository.deleteAllByRecruitmentId(recruitment.id());

    entity.update(
        recruitment.title(),
        recruitment.recruitmentStatus(),
        recruitment.targetCount(),
        recruitment.startDate(),
        recruitment.endDate(),
        recruitment.applicationTemplateId(),
        recruitment.contents(),
        recruitment.careerType(),
        recruitment.minExperienceYears(),
        recruitment.maxExperienceYears(),
        recruitment.leadGroupId());

    // 부모 엔티티 변경을 먼저 flush해서 상태/일시 변경이 DB에 확정되도록 한다.
    recruitmentRepository.saveAndFlush(entity);

    insertChildren(recruitment.id(), recruitment);

    return recruitment;
  }

  @Override
  // 채용 공고 기본 정보는 건드리지 않고 면접관 연결만 갱신
  public Recruitment updateInterviewers(Recruitment recruitment) {
    if (recruitment.id() == null) {
      throw new IllegalArgumentException("수정할 채용 공고 ID가 없습니다.");
    }

    recruitmentRepository
        .findByIdAndStatus(recruitment.id(), EntityStatus.ACTIVE)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채용 공고입니다."));

    recruitmentInterviewerRepository.deleteAllByRecruitmentId(recruitment.id());
    batchInsertInterviewers(recruitment.id(), recruitment, resolveTenantId());

    return recruitment;
  }

  @Override
  public void delete(Long recruitmentId) {
    RecruitmentEntity entity =
        recruitmentRepository
            .findByIdAndStatus(recruitmentId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new IllegalArgumentException("이미 삭제되었거나 존재하지 않는 채용 공고입니다."));

    entity.deleted();
  }

  @Override
  // 시작 시각이 지난 DRAFT 공고를 OPEN으로 전환
  public int openScheduledRecruitments(LocalDateTime currentTime) {
    return recruitmentRepository.openScheduledRecruitments(
        currentTime, EntityStatus.ACTIVE, RecruitmentStatus.DRAFT, RecruitmentStatus.OPEN);
  }

  @Override
  // 종료 시각이 지난 DRAFT/OPEN 공고를 CLOSED로 전환
  public int closeEndedRecruitments(LocalDateTime currentTime) {
    return recruitmentRepository.closeEndedRecruitments(
        currentTime,
        EntityStatus.ACTIVE,
        List.of(RecruitmentStatus.DRAFT, RecruitmentStatus.OPEN),
        RecruitmentStatus.CLOSED);
  }

  private void insertChildren(Long recruitmentId, Recruitment recruitment) {
    String tenantId = resolveTenantId();

    batchInsertStages(recruitmentId, recruitment, tenantId);
    batchInsertInterviewers(recruitmentId, recruitment, tenantId);
    batchInsertReferenceGroups(recruitmentId, recruitment, tenantId);
  }

  private void batchInsertStages(Long recruitmentId, Recruitment recruitment, String tenantId) {
    if (recruitment.stages() == null || recruitment.stages().isEmpty()) {
      return;
    }

    String sql =
        "INSERT INTO recruitment_stages "
            + "(recruitment_id, stage_name, stage_step, stage_type, tenant_id, status, created_at, updated_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";

    jdbcTemplate.batchUpdate(
        sql,
        recruitment.stages(),
        recruitment.stages().size(),
        (ps, stage) -> {
          ps.setLong(1, recruitmentId);
          ps.setString(2, stage.stageName());
          ps.setInt(3, stage.stageStep());
          ps.setString(4, stage.stageType().name());
          ps.setString(5, tenantId);
          ps.setString(6, EntityStatus.ACTIVE.name());
        });
  }

  private void batchInsertInterviewers(
      Long recruitmentId, Recruitment recruitment, String tenantId) {
    if (recruitment.interviewerIds() == null || recruitment.interviewerIds().isEmpty()) {
      return;
    }

    String sql =
        "INSERT INTO recruitment_interviewers "
            + "(recruitment_id, user_id, tenant_id, status, created_at, updated_at) "
            + "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";

    jdbcTemplate.batchUpdate(
        sql,
        recruitment.interviewerIds(),
        recruitment.interviewerIds().size(),
        (ps, userId) -> {
          ps.setLong(1, recruitmentId);
          ps.setLong(2, userId);
          ps.setString(3, tenantId);
          ps.setString(4, EntityStatus.ACTIVE.name());
        });
  }

  private void batchInsertReferenceGroups(
      Long recruitmentId, Recruitment recruitment, String tenantId) {
    if (recruitment.referenceGroupIds() == null || recruitment.referenceGroupIds().isEmpty()) {
      return;
    }

    String sql =
        "INSERT INTO recruitment_reference_teams "
            + "(recruitment_id, group_id, tenant_id, status, created_at, updated_at) "
            + "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";

    jdbcTemplate.batchUpdate(
        sql,
        recruitment.referenceGroupIds(),
        recruitment.referenceGroupIds().size(),
        (ps, groupId) -> {
          ps.setLong(1, recruitmentId);
          ps.setLong(2, groupId);
          ps.setString(3, tenantId);
          ps.setString(4, EntityStatus.ACTIVE.name());
        });
  }

  private String resolveTenantId() {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) {
      return "default";
    }
    return tenantId;
  }
}
