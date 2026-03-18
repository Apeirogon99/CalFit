package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.api.v1.response.PnlReportResponse;
import com.coffiness.calfit.api.v1.response.PnlSummaryResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.billing.pnl.PnlReport;
import com.coffiness.calfit.domain.billing.pnl.PnlService;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/pnl")
@RequiredArgsConstructor
public class AdminPnlController {

  private final PnlService pnlService;

  @GetMapping("/summary")
  public ApiResponse<PnlSummaryResponse> getSummary(@RequestParam(required = false) String month) {
    LocalDate date = parseMonth(month);
    return ApiResponse.success(
        PnlSummaryResponse.from(pnlService.getSummary(date.getYear(), date.getMonthValue())));
  }

  @GetMapping("/report")
  public ApiResponse<PnlReportResponse> getReport(@RequestParam(defaultValue = "3") int months) {
    return ApiResponse.success(PnlReportResponse.from(pnlService.getReport(months)));
  }

  @GetMapping("/export")
  public ResponseEntity<byte[]> exportExcel(@RequestParam(defaultValue = "3") int months) {
    PnlReport report = pnlService.getReport(months);

    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {

      Sheet sheet = workbook.createSheet("손익 리포트");

      // 헤더 스타일
      CellStyle headerStyle = workbook.createCellStyle();
      Font headerFont = workbook.createFont();
      headerFont.setBold(true);
      headerStyle.setFont(headerFont);
      headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
      headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

      // 금액 스타일
      CellStyle amountStyle = workbook.createCellStyle();
      DataFormat format = workbook.createDataFormat();
      amountStyle.setDataFormat(format.getFormat("#,##0"));

      // 헤더 행
      Row headerRow = sheet.createRow(0);
      Cell cell0 = headerRow.createCell(0);
      cell0.setCellValue("항목");
      cell0.setCellStyle(headerStyle);
      for (int i = 0; i < report.columns().size(); i++) {
        Cell cell = headerRow.createCell(i + 1);
        cell.setCellValue(report.columns().get(i));
        cell.setCellStyle(headerStyle);
      }

      // 데이터 행
      String[] rowLabels = {"매출 합계", "인건비", "서버/인프라", "마케팅", "기타", "비용 합계", "영업이익"};
      long[][] rowData = {
        report.revenueTotal(),
        report.laborCost(),
        report.infraCost(),
        report.marketingCost(),
        report.otherCost(),
        report.costTotal(),
        report.operatingProfit()
      };

      for (int r = 0; r < rowLabels.length; r++) {
        Row row = sheet.createRow(r + 1);
        row.createCell(0).setCellValue(rowLabels[r]);
        for (int c = 0; c < report.columns().size(); c++) {
          Cell cell = row.createCell(c + 1);
          cell.setCellValue(rowData[r][c]);
          cell.setCellStyle(amountStyle);
        }
      }

      // 컬럼 너비 자동 조절
      for (int i = 0; i <= report.columns().size(); i++) {
        sheet.autoSizeColumn(i);
      }

      workbook.write(out);

      String filename = "PnL_report.xlsx";
      return ResponseEntity.ok()
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
          .contentType(
              MediaType.parseMediaType(
                  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
          .body(out.toByteArray());
    } catch (Exception e) {
      return ResponseEntity.internalServerError().build();
    }
  }

  private LocalDate parseMonth(String month) {
    if (month == null || month.isBlank()) {
      return LocalDate.now();
    }
    YearMonth ym = YearMonth.parse(month);
    return ym.atDay(1);
  }
}
