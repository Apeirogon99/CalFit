package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.CostCategory;
import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.domain.billing.cost.Cost;
import com.coffiness.calfit.domain.billing.cost.CostReader;
import com.coffiness.calfit.domain.billing.cost.MonthlyCostTotal;
import com.coffiness.calfit.storage.db.core.billing.CostEntity;
import com.coffiness.calfit.storage.db.core.billing.CostRepository;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CostReaderImpl implements CostReader {

  private final CostRepository costRepository;

  @Override
  public Optional<Cost> findById(Long id) {
    return costRepository.findById(id).map(this::toCost);
  }

  @Override
  public List<Cost> getAll(Integer year, Integer month, CostCategory category, Pageable pageable) {
    return costRepository
        .findAllWithFilter(EntityStatus.ACTIVE, year, month, category, pageable)
        .stream()
        .map(this::toCost)
        .toList();
  }

  @Override
  public Map<CostCategory, Long> getSummaryByCategory(int year, int month) {
    Map<CostCategory, Long> result = new EnumMap<>(CostCategory.class);
    Arrays.stream(CostCategory.values()).forEach(c -> result.put(c, 0L));
    costRepository
        .sumByCategoryAndMonth(EntityStatus.ACTIVE, year, month)
        .forEach(
            row -> {
              CostCategory category = (CostCategory) row[0];
              long amount = ((Number) row[1]).longValue();
              result.put(category, amount);
            });
    return result;
  }

  @Override
  public List<MonthlyCostTotal> getMonthlyTrend() {
    return costRepository.findMonthlyTrend(EntityStatus.ACTIVE).stream()
        .map(
            row ->
                new MonthlyCostTotal(
                    ((Number) row[0]).intValue(),
                    ((Number) row[1]).intValue(),
                    ((Number) row[2]).longValue()))
        .toList();
  }

  private Cost toCost(CostEntity entity) {
    return new Cost(
        entity.getId(),
        entity.getCategory(),
        entity.getName(),
        entity.getAmount(),
        entity.getCostYear(),
        entity.getCostMonth(),
        entity.getMemo());
  }
}
