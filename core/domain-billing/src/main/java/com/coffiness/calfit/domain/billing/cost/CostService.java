package com.coffiness.calfit.domain.billing.cost;

import com.coffiness.calfit.core.enums.CostCategory;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CostService {

  private final CostReader costReader;
  private final CostStore costStore;

  public List<Cost> getAll(Integer year, Integer month, CostCategory category, int page, int size) {
    return costReader.getAll(year, month, category, PageRequest.of(page, size));
  }

  public Map<CostCategory, Long> getSummaryByCategory(int year, int month) {
    return costReader.getSummaryByCategory(year, month);
  }

  public List<MonthlyCostTotal> getMonthlyTrend() {
    return costReader.getMonthlyTrend();
  }

  @Transactional
  public Cost create(
      CostCategory category, String name, Long amount, int year, int month, String memo) {
    return costStore.save(category, name, amount, year, month, memo);
  }

  @Transactional
  public Cost update(
      Long id, CostCategory category, String name, Long amount, int year, int month, String memo) {
    costReader.findById(id).orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    return costStore.update(id, category, name, amount, year, month, memo);
  }

  @Transactional
  public void delete(Long id) {
    costReader.findById(id).orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    costStore.remove(id);
  }
}
