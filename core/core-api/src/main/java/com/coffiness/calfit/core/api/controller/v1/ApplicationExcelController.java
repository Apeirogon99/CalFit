package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.InterviewStatus;
import com.coffiness.calfit.storage.db.core.application.ApplicationEntity;
import com.coffiness.calfit.storage.db.core.application.ApplicationRepository;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleApplicantEntity;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleApplicantRepository;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleEntity;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleRepository;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentEntity;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentRepository;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentStageEntity;
import com.coffiness.calfit.storage.db.core.recruitment.RecruitmentStageRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class ApplicationExcelController {

  private static final String EXCEL_CONTENT_TYPE =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy.MM.dd");
  private static final DateTimeFormatter DATE_TIME_FMT =
      DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
  private static final String EXCEL_SHEET_NAME = "지원자 목록";
  private static final String ALL_RECRUITMENTS_LABEL = "전체 공고";
  private static final String ALL_STAGES_LABEL = "전체 상태";
  private static final String EMPTY_CELL = "-";
  private static final String[] EXCEL_HEADERS = {"지원자명", "이메일", "공고", "진행 상태", "다음 일정", "지원일"};

  private final ApplicationRepository applicationRepository;
  private final RecruitmentRepository recruitmentRepository;
  private final RecruitmentStageRepository recruitmentStageRepository;
  private final InterviewScheduleRepository interviewScheduleRepository;
  private final InterviewScheduleApplicantRepository interviewScheduleApplicantRepository;

  @GetMapping({"/api/v1/applications/excel", "/api/v1/applications/export"})
  public ResponseEntity<byte[]> exportApplicantsExcel(
      @AuthenticationPrincipal SecurityUser user,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long stageId,
      @RequestParam(required = false) String job,
      @RequestParam(required = false) Long jobId,
      @RequestParam(required = false) Long recruitmentId) {
    if (user == null || "APPLICANT".equalsIgnoreCase(user.role())) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }

    ApplicantExportFilter filter =
        ApplicantExportFilter.of(keyword, search, status, stageId, job, jobId, recruitmentId);
    List<ApplicationEntity> entities = applicationRepository.findByStatus(EntityStatus.ACTIVE);

    Map<Long, RecruitmentEntity> recruitmentMap = buildRecruitmentMap(entities);
    Map<Long, RecruitmentStageEntity> stageMap = buildStageMap(entities);

    List<ApplicationEntity> filteredEntities =
        entities.stream()
            .filter(entity -> matchesKeyword(entity, filter.searchKeyword()))
            .filter(
                entity -> matchesRecruitment(entity, recruitmentMap, filter.recruitmentFilter()))
            .filter(entity -> matchesStage(entity, stageMap, filter.stageFilter()))
            .collect(Collectors.toList());

    Map<String, LocalDateTime> nextScheduleMap = buildNextScheduleMap(filteredEntities);

    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet(WorkbookUtil.createSafeSheetName(EXCEL_SHEET_NAME));
      CellStyle headerStyle = createHeaderStyle(workbook);

      Row headerRow = sheet.createRow(0);
      for (int i = 0; i < EXCEL_HEADERS.length; i++) {
        Cell cell = headerRow.createCell(i);
        cell.setCellValue(EXCEL_HEADERS[i]);
        cell.setCellStyle(headerStyle);
      }

      int rowIndex = 1;
      for (ApplicationEntity entity : filteredEntities) {
        RecruitmentEntity recruitment = recruitmentMap.get(entity.getRecruitmentId());
        RecruitmentStageEntity stage = stageMap.get(entity.getRecruitmentProcessId());

        Row row = sheet.createRow(rowIndex++);
        row.createCell(0).setCellValue(defaultCell(entity.getName()));
        row.createCell(1).setCellValue(defaultCell(entity.getEmail()));
        row.createCell(2)
            .setCellValue(recruitment != null ? defaultCell(recruitment.getTitle()) : EMPTY_CELL);
        row.createCell(3)
            .setCellValue(stage != null ? defaultCell(stage.getStageName()) : EMPTY_CELL);
        row.createCell(4)
            .setCellValue(
                formatDateTime(
                    nextScheduleMap.get(
                        buildScheduleKey(entity.getApplicantId(), entity.getRecruitmentId()))));
        row.createCell(5).setCellValue(formatDate(entity.getCreatedAt()));
      }

      for (int i = 0; i < EXCEL_HEADERS.length; i++) {
        sheet.autoSizeColumn(i);
      }

      workbook.write(out);

      String filename =
          "applicants-"
              + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
              + "-"
              + System.currentTimeMillis() % 1_000_000
              + ".xlsx";
      String encodedFilename =
          URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

      return ResponseEntity.ok()
          .header(
              HttpHeaders.CONTENT_DISPOSITION,
              "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFilename)
          .contentType(MediaType.parseMediaType(EXCEL_CONTENT_TYPE))
          .body(out.toByteArray());
    } catch (Exception e) {
      log.error("Failed to export applicants excel", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  private CellStyle createHeaderStyle(Workbook workbook) {
    CellStyle headerStyle = workbook.createCellStyle();
    Font headerFont = workbook.createFont();
    headerFont.setBold(true);
    headerStyle.setFont(headerFont);
    headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
    headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    return headerStyle;
  }

  private Map<Long, RecruitmentEntity> buildRecruitmentMap(List<ApplicationEntity> entities) {
    List<Long> recruitmentIds =
        entities.stream()
            .map(ApplicationEntity::getRecruitmentId)
            .distinct()
            .collect(Collectors.toList());
    if (recruitmentIds.isEmpty()) {
      return Map.of();
    }

    return recruitmentRepository.findAllById(recruitmentIds).stream()
        .collect(
            Collectors.toMap(RecruitmentEntity::getId, recruitment -> recruitment, (a, b) -> a));
  }

  private Map<Long, RecruitmentStageEntity> buildStageMap(List<ApplicationEntity> entities) {
    List<Long> stageIds =
        entities.stream()
            .map(ApplicationEntity::getRecruitmentProcessId)
            .distinct()
            .collect(Collectors.toList());
    if (stageIds.isEmpty()) {
      return Map.of();
    }

    return recruitmentStageRepository.findAllById(stageIds).stream()
        .collect(Collectors.toMap(RecruitmentStageEntity::getId, stage -> stage, (a, b) -> a));
  }

  private Map<String, LocalDateTime> buildNextScheduleMap(List<ApplicationEntity> entities) {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank() || entities.isEmpty()) {
      return Map.of();
    }

    List<Long> applicantIds =
        entities.stream()
            .map(ApplicationEntity::getApplicantId)
            .distinct()
            .collect(Collectors.toList());
    List<InterviewScheduleApplicantEntity> scheduleApplicants =
        interviewScheduleApplicantRepository.findAllByTenantIdAndApplicantIdIn(
            tenantId, applicantIds);
    if (scheduleApplicants.isEmpty()) {
      return Map.of();
    }

    List<Long> scheduleIds =
        scheduleApplicants.stream()
            .map(InterviewScheduleApplicantEntity::getInterviewScheduleId)
            .distinct()
            .collect(Collectors.toList());
    if (scheduleIds.isEmpty()) {
      return Map.of();
    }

    LocalDateTime now = LocalDateTime.now();
    Map<Long, InterviewScheduleEntity> scheduleMap =
        interviewScheduleRepository.findAllById(scheduleIds).stream()
            .filter(schedule -> schedule.getInterviewStatus() != InterviewStatus.CANCELLED)
            .filter(schedule -> !schedule.getScheduledAt().isBefore(now))
            .collect(
                Collectors.toMap(
                    InterviewScheduleEntity::getId, schedule -> schedule, (a, b) -> a));

    Map<Long, List<Long>> scheduleIdsByApplicantId =
        scheduleApplicants.stream()
            .collect(
                Collectors.groupingBy(
                    InterviewScheduleApplicantEntity::getApplicantId,
                    Collectors.mapping(
                        InterviewScheduleApplicantEntity::getInterviewScheduleId,
                        Collectors.toList())));

    Map<String, LocalDateTime> nextScheduleMap = new HashMap<>();
    for (ApplicationEntity entity : entities) {
      LocalDateTime nextSchedule =
          scheduleIdsByApplicantId.getOrDefault(entity.getApplicantId(), List.of()).stream()
              .map(scheduleMap::get)
              .filter(Objects::nonNull)
              .filter(
                  schedule ->
                      Objects.equals(schedule.getRecruitmentId(), entity.getRecruitmentId()))
              .map(InterviewScheduleEntity::getScheduledAt)
              .min(LocalDateTime::compareTo)
              .orElse(null);

      if (nextSchedule != null) {
        nextScheduleMap.put(
            buildScheduleKey(entity.getApplicantId(), entity.getRecruitmentId()), nextSchedule);
      }
    }

    return nextScheduleMap;
  }

  private boolean matchesKeyword(ApplicationEntity entity, String searchKeyword) {
    if (searchKeyword == null) {
      return true;
    }

    String lowerKeyword = searchKeyword.toLowerCase(Locale.ROOT);
    return (entity.getName() != null
            && entity.getName().toLowerCase(Locale.ROOT).contains(lowerKeyword))
        || (entity.getEmail() != null
            && entity.getEmail().toLowerCase(Locale.ROOT).contains(lowerKeyword));
  }

  private boolean matchesRecruitment(
      ApplicationEntity entity,
      Map<Long, RecruitmentEntity> recruitmentMap,
      RecruitmentFilter recruitmentFilter) {
    if (recruitmentFilter == null) {
      return true;
    }

    RecruitmentEntity recruitment = recruitmentMap.get(entity.getRecruitmentId());
    if (recruitment == null) {
      return false;
    }
    if (recruitmentFilter.recruitmentId() != null) {
      return Objects.equals(recruitment.getId(), recruitmentFilter.recruitmentId());
    }
    return recruitmentFilter.recruitmentTitle() == null
        || (recruitment.getTitle() != null
            && recruitment
                .getTitle()
                .trim()
                .equalsIgnoreCase(recruitmentFilter.recruitmentTitle()));
  }

  private boolean matchesStage(
      ApplicationEntity entity,
      Map<Long, RecruitmentStageEntity> stageMap,
      StageFilter stageFilter) {
    if (stageFilter == null) {
      return true;
    }

    RecruitmentStageEntity stage = stageMap.get(entity.getRecruitmentProcessId());
    if (stage == null) {
      return false;
    }
    if (stageFilter.stageId() != null) {
      return Objects.equals(stage.getId(), stageFilter.stageId());
    }

    String stageName = stage.getStageName();
    if (stageName != null && stageName.equalsIgnoreCase(stageFilter.stageName())) {
      return true;
    }
    return stage.getStageType() != null
        && stage.getStageType().name().equalsIgnoreCase(stageFilter.stageName());
  }

  private String defaultCell(String value) {
    return value == null || value.isBlank() ? EMPTY_CELL : value;
  }

  private String formatDate(LocalDateTime dateTime) {
    if (dateTime == null) {
      return EMPTY_CELL;
    }
    return dateTime.format(DATE_FMT);
  }

  private String formatDateTime(LocalDateTime dateTime) {
    if (dateTime == null) {
      return EMPTY_CELL;
    }
    return dateTime.format(DATE_TIME_FMT);
  }

  private String buildScheduleKey(Long applicantId, Long recruitmentId) {
    return applicantId + ":" + recruitmentId;
  }

  private record ApplicantExportFilter(
      String searchKeyword, RecruitmentFilter recruitmentFilter, StageFilter stageFilter) {

    private static ApplicantExportFilter of(
        String keyword,
        String search,
        String status,
        Long stageId,
        String job,
        Long jobId,
        Long recruitmentId) {
      return new ApplicantExportFilter(
          normalizeSearchKeyword(keyword, search),
          normalizeRecruitmentFilter(job, jobId, recruitmentId),
          normalizeStageFilter(status, stageId));
    }

    private static String normalizeSearchKeyword(String keyword, String search) {
      String raw = keyword != null ? keyword : search;
      if (raw == null || raw.isBlank()) {
        return null;
      }
      return raw.trim();
    }

    private static RecruitmentFilter normalizeRecruitmentFilter(
        String job, Long jobId, Long recruitmentId) {
      if (recruitmentId != null) {
        return new RecruitmentFilter(recruitmentId, null);
      }
      if (jobId != null) {
        return new RecruitmentFilter(jobId, null);
      }
      if (job == null || isAllFilter(job)) {
        return null;
      }

      String normalized = job.trim();
      try {
        return new RecruitmentFilter(Long.parseLong(normalized), null);
      } catch (NumberFormatException ignored) {
        return new RecruitmentFilter(null, normalized);
      }
    }

    private static StageFilter normalizeStageFilter(String status, Long stageId) {
      if (stageId != null) {
        return new StageFilter(stageId, null);
      }
      if (status == null || isAllFilter(status)) {
        return null;
      }
      return new StageFilter(null, status.trim());
    }

    private static boolean isAllFilter(String value) {
      String normalized = value.trim();
      return normalized.isBlank()
          || "all".equalsIgnoreCase(normalized)
          || ALL_RECRUITMENTS_LABEL.equals(normalized)
          || ALL_STAGES_LABEL.equals(normalized);
    }
  }

  private record RecruitmentFilter(Long recruitmentId, String recruitmentTitle) {}

  private record StageFilter(Long stageId, String stageName) {}
}
