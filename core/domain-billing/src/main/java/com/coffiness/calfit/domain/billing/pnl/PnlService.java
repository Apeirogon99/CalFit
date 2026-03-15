package com.coffiness.calfit.domain.billing.pnl;

import com.coffiness.calfit.core.enums.CostCategory;
import com.coffiness.calfit.domain.billing.cost.CostReader;
import com.coffiness.calfit.domain.billing.cost.MonthlyCostTotal;
import com.coffiness.calfit.domain.billing.invoice.InvoiceReader;
import com.coffiness.calfit.domain.billing.invoice.MonthlyRevenue;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PnlService {

  private final InvoiceReader invoiceReader;
  private final CostReader costReader;

  public PnlSummary getSummary(int year, int month) {
    List<MonthlyRevenue> revenues = invoiceReader.getMonthlyRevenue();
    List<MonthlyCostTotal> costs = costReader.getMonthlyTrend();

    long totalRevenue =
        revenues.stream()
            .filter(r -> r.year() == year && r.month() == month)
            .mapToLong(MonthlyRevenue::amount)
            .sum();
    long totalCost =
        costs.stream()
            .filter(c -> c.year() == year && c.month() == month)
            .mapToLong(MonthlyCostTotal::amount)
            .sum();

    LocalDate prevDate = LocalDate.of(year, month, 1).minusMonths(1);
    long prevRevenue =
        revenues.stream()
            .filter(r -> r.year() == prevDate.getYear() && r.month() == prevDate.getMonthValue())
            .mapToLong(MonthlyRevenue::amount)
            .sum();
    long prevCost =
        costs.stream()
            .filter(c -> c.year() == prevDate.getYear() && c.month() == prevDate.getMonthValue())
            .mapToLong(MonthlyCostTotal::amount)
            .sum();

    double revenueGrowth =
        prevRevenue > 0
            ? Math.round((double) (totalRevenue - prevRevenue) / prevRevenue * 1000.0) / 10.0
            : 0.0;
    double costGrowth =
        prevCost > 0 ? Math.round((double) (totalCost - prevCost) / prevCost * 1000.0) / 10.0 : 0.0;
    long operatingProfit = totalRevenue - totalCost;
    double operatingMargin =
        totalRevenue > 0
            ? Math.round((double) operatingProfit / totalRevenue * 1000.0) / 10.0
            : 0.0;

    return new PnlSummary(
        totalRevenue, revenueGrowth, totalCost, costGrowth, operatingProfit, operatingMargin);
  }

  public PnlReport getReport(int months) {
    List<MonthlyRevenue> revenues = invoiceReader.getMonthlyRevenue();
    List<MonthlyCostTotal> allCosts = costReader.getMonthlyTrend();

    // 최근 N개월 컬럼 생성
    List<String> columns = new ArrayList<>();
    LocalDate now = LocalDate.now();
    for (int i = months - 1; i >= 0; i--) {
      LocalDate d = now.minusMonths(i);
      columns.add(String.format("%d.%02d", d.getYear(), d.getMonthValue()));
    }

    Map<String, Long> revenueMap =
        revenues.stream()
            .collect(
                Collectors.toMap(
                    r -> r.year() + "-" + r.month(), MonthlyRevenue::amount, Long::sum));

    // 매출 행
    long[] revenueTotal =
        buildAmountArray(
            columns,
            (col) -> {
              String[] parts = col.split("\\.");
              int y = Integer.parseInt(parts[0]);
              int m = Integer.parseInt(parts[1]);
              return revenueMap.getOrDefault(y + "-" + m, 0L);
            });

    // 카테고리별 비용
    Map<String, Map<CostCategory, Long>> costByMonthCategory =
        costReader.getMonthlyTrend().stream()
            .collect(
                Collectors.groupingBy(
                    c -> c.year() + "-" + c.month(),
                    Collectors.toMap(c -> CostCategory.LABOR, c -> 0L)));

    // 카테고리별 비용 배열 생성
    Map<CostCategory, long[]> costArrays =
        Arrays.stream(CostCategory.values())
            .collect(Collectors.toMap(cat -> cat, cat -> new long[months]));

    for (int i = 0; i < months; i++) {
      LocalDate d = now.minusMonths(months - 1 - i);
      Map<CostCategory, Long> summary =
          costReader.getSummaryByCategory(d.getYear(), d.getMonthValue());
      for (CostCategory cat : CostCategory.values()) {
        costArrays.get(cat)[i] = summary.getOrDefault(cat, 0L);
      }
    }

    long[] costTotal = new long[months];
    for (int i = 0; i < months; i++) {
      final int idx = i;
      costTotal[i] =
          Arrays.stream(CostCategory.values()).mapToLong(cat -> costArrays.get(cat)[idx]).sum();
    }

    long[] operatingProfit = new long[months];
    for (int i = 0; i < months; i++) {
      operatingProfit[i] = revenueTotal[i] - costTotal[i];
    }

    return new PnlReport(
        columns,
        revenueTotal,
        costArrays.get(CostCategory.LABOR),
        costArrays.get(CostCategory.INFRASTRUCTURE),
        costArrays.get(CostCategory.MARKETING),
        costArrays.get(CostCategory.OTHER),
        costTotal,
        operatingProfit);
  }

  @FunctionalInterface
  interface LongFunction<T> {
    long apply(T t);
  }

  private long[] buildAmountArray(List<String> columns, LongFunction<String> fn) {
    long[] arr = new long[columns.size()];
    for (int i = 0; i < columns.size(); i++) {
      arr[i] = fn.apply(columns.get(i));
    }
    return arr;
  }
}
