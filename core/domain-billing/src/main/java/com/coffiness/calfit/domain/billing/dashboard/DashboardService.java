package com.coffiness.calfit.domain.billing.dashboard;

import com.coffiness.calfit.core.enums.SubscriptionStatus;
import com.coffiness.calfit.domain.billing.cost.CostReader;
import com.coffiness.calfit.domain.billing.cost.MonthlyCostTotal;
import com.coffiness.calfit.domain.billing.invoice.InvoiceReader;
import com.coffiness.calfit.domain.billing.invoice.MonthlyRevenue;
import com.coffiness.calfit.domain.billing.subscription.SubscriptionReader;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

  private final SubscriptionReader subscriptionReader;
  private final InvoiceReader invoiceReader;
  private final CostReader costReader;

  public DashboardSummary getSummary(int year, int month) {
    YearMonth ym = YearMonth.of(year, month);
    LocalDate monthStart = ym.atDay(1);
    LocalDate monthEnd = ym.atEndOfMonth();

    long mrr = subscriptionReader.getMrrInMonth(monthStart, monthEnd);
    long activeCount =
        subscriptionReader.countByStatusInMonth(SubscriptionStatus.ACTIVE, monthStart, monthEnd);
    long trialCount =
        subscriptionReader.countByStatusInMonth(SubscriptionStatus.TRIAL, monthStart, monthEnd);

    List<MonthlyRevenue> revenueHistory = invoiceReader.getMonthlyRevenue();
    double mrrGrowth = computeGrowth(revenueHistory, year, month);

    Map<com.coffiness.calfit.core.enums.CostCategory, Long> costSummary =
        costReader.getSummaryByCategory(year, month);
    long totalCost = costSummary.values().stream().mapToLong(Long::longValue).sum();

    List<MonthlyCostTotal> costHistory = costReader.getMonthlyTrend();
    double costGrowth = computeCostGrowth(costHistory, year, month);

    long subscribers = activeCount + trialCount;
    long newThisMonth = subscriptionReader.countNewInMonth(monthStart, monthEnd);

    long cancelledThisMonth = subscriptionReader.countCancelledInMonth(year, month);
    YearMonth prevYm = ym.minusMonths(1);
    LocalDate prevStart = prevYm.atDay(1);
    LocalDate prevEnd = prevYm.atEndOfMonth();
    long prevActive =
        subscriptionReader.countByStatusInMonth(SubscriptionStatus.ACTIVE, prevStart, prevEnd)
            + subscriptionReader.countByStatusInMonth(SubscriptionStatus.TRIAL, prevStart, prevEnd);
    double churnRate =
        prevActive > 0 ? Math.round((double) cancelledThisMonth / prevActive * 1000.0) / 10.0 : 0.0;

    YearMonth prev2Ym = ym.minusMonths(2);
    long cancelledPrevMonth =
        subscriptionReader.countCancelledInMonth(prevYm.getYear(), prevYm.getMonthValue());
    long prev2Active =
        subscriptionReader.countByStatusInMonth(
                SubscriptionStatus.ACTIVE, prev2Ym.atDay(1), prev2Ym.atEndOfMonth())
            + subscriptionReader.countByStatusInMonth(
                SubscriptionStatus.TRIAL, prev2Ym.atDay(1), prev2Ym.atEndOfMonth());
    double prevChurnRate =
        prev2Active > 0
            ? Math.round((double) cancelledPrevMonth / prev2Active * 1000.0) / 10.0
            : 0.0;
    double churnImprove = Math.round((prevChurnRate - churnRate) * 10.0) / 10.0;

    return new DashboardSummary(
        mrr, mrrGrowth, subscribers, newThisMonth, churnRate, churnImprove, totalCost, costGrowth);
  }

  public List<RevenueCostTrend> getRevenueCostTrend(String period) {
    List<MonthlyRevenue> revenues = invoiceReader.getMonthlyRevenue();
    List<MonthlyCostTotal> costs = costReader.getMonthlyTrend();

    Map<String, Long> revenueMap =
        revenues.stream()
            .collect(
                Collectors.toMap(
                    r -> r.year() + "-" + r.month(), MonthlyRevenue::amount, Long::sum));
    Map<String, Long> costMap =
        costs.stream()
            .collect(
                Collectors.toMap(
                    c -> c.year() + "-" + c.month(), MonthlyCostTotal::amount, Long::sum));

    if ("quarterly".equals(period)) {
      return buildQuarterlyTrend(revenueMap, costMap);
    }

    // 최근 8개월 추이 생성
    List<RevenueCostTrend> trend = new ArrayList<>();
    LocalDate now = LocalDate.now();
    for (int i = 7; i >= 0; i--) {
      LocalDate d = now.minusMonths(i);
      String key = d.getYear() + "-" + d.getMonthValue();
      trend.add(
          new RevenueCostTrend(
              d.getYear(),
              d.getMonthValue(),
              revenueMap.getOrDefault(key, 0L),
              costMap.getOrDefault(key, 0L)));
    }
    return trend;
  }

  private List<RevenueCostTrend> buildQuarterlyTrend(
      Map<String, Long> revenueMap, Map<String, Long> costMap) {
    List<RevenueCostTrend> trend = new ArrayList<>();
    LocalDate now = LocalDate.now();
    int currentQuarter = (now.getMonthValue() - 1) / 3 + 1;
    int currentYear = now.getYear();

    // 최근 8분기
    for (int i = 7; i >= 0; i--) {
      int q = currentQuarter - i;
      int y = currentYear;
      while (q <= 0) {
        q += 4;
        y--;
      }

      long revenue = 0;
      long cost = 0;
      int startMonth = (q - 1) * 3 + 1;
      for (int m = startMonth; m < startMonth + 3; m++) {
        String key = y + "-" + m;
        revenue += revenueMap.getOrDefault(key, 0L);
        cost += costMap.getOrDefault(key, 0L);
      }
      // month 필드에 분기 번호 저장 (1~4)
      trend.add(new RevenueCostTrend(y, q, revenue, cost));
    }
    return trend;
  }

  public List<PlanDistribution> getPlanDistribution() {
    List<PlanDistribution> result = new ArrayList<>();
    for (SubscriptionStatus status : SubscriptionStatus.values()) {
      long count = subscriptionReader.countByStatus(status);
      if (count > 0) {
        result.add(new PlanDistribution(status.name(), count));
      }
    }
    return result;
  }

  private double computeGrowth(List<MonthlyRevenue> history, int year, int month) {
    if (history.size() < 2) return 0.0;
    long current =
        history.stream()
            .filter(r -> r.year() == year && r.month() == month)
            .mapToLong(MonthlyRevenue::amount)
            .sum();
    LocalDate prevDate = LocalDate.of(year, month, 1).minusMonths(1);
    long previous =
        history.stream()
            .filter(r -> r.year() == prevDate.getYear() && r.month() == prevDate.getMonthValue())
            .mapToLong(MonthlyRevenue::amount)
            .sum();
    if (previous == 0) return 0.0;
    return Math.round(((double) (current - previous) / previous * 100) * 10.0) / 10.0;
  }

  private double computeCostGrowth(List<MonthlyCostTotal> history, int year, int month) {
    if (history.size() < 2) return 0.0;
    long current =
        history.stream()
            .filter(c -> c.year() == year && c.month() == month)
            .mapToLong(MonthlyCostTotal::amount)
            .sum();
    LocalDate prevDate = LocalDate.of(year, month, 1).minusMonths(1);
    long previous =
        history.stream()
            .filter(c -> c.year() == prevDate.getYear() && c.month() == prevDate.getMonthValue())
            .mapToLong(MonthlyCostTotal::amount)
            .sum();
    if (previous == 0) return 0.0;
    return Math.round(((double) (current - previous) / previous * 100) * 10.0) / 10.0;
  }
}
