package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.CostCategory;
import com.coffiness.calfit.domain.billing.cost.Cost;
import com.coffiness.calfit.domain.billing.cost.CostStore;
import com.coffiness.calfit.storage.db.core.billing.CostEntity;
import com.coffiness.calfit.storage.db.core.billing.CostRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CostStoreImpl implements CostStore {

  private final CostRepository costRepository;

  @Override
  public Cost save(
      CostCategory category, String name, Long amount, int year, int month, String memo) {
    CostEntity entity = CostEntity.create(category, name, amount, year, month, memo);
    return toCost(costRepository.save(entity));
  }

  @Override
  public Cost update(
      Long id, CostCategory category, String name, Long amount, int year, int month, String memo) {
    CostEntity entity =
        costRepository.findById(id).orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    entity.update(category, name, amount, year, month, memo);
    return toCost(costRepository.save(entity));
  }

  @Override
  public void remove(Long id) {
    CostEntity entity =
        costRepository
            .findById(id)
            .filter(CostEntity::isActive)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    entity.deleted();
    costRepository.save(entity);
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
