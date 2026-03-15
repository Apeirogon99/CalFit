package com.coffiness.calfit.domain.billing.cost;

import com.coffiness.calfit.core.enums.CostCategory;

public interface CostStore {

  Cost save(CostCategory category, String name, Long amount, int year, int month, String memo);

  Cost update(
      Long id, CostCategory category, String name, Long amount, int year, int month, String memo);

  void remove(Long id);
}
