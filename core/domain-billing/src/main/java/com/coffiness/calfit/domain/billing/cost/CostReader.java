package com.coffiness.calfit.domain.billing.cost;

import com.coffiness.calfit.core.enums.CostCategory;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface CostReader {

  Optional<Cost> findById(Long id);

  List<Cost> getAll(Integer year, Integer month, CostCategory category, Pageable pageable);

  Map<CostCategory, Long> getSummaryByCategory(int year, int month);

  List<MonthlyCostTotal> getMonthlyTrend();
}
