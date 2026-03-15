package com.coffiness.calfit.storage.db.core.billing;

import com.coffiness.calfit.core.enums.CostCategory;
import com.coffiness.calfit.storage.db.core.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "costs")
public class CostEntity extends BaseEntity {

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CostCategory category;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private Long amount;

  @Column(nullable = false)
  private int costYear;

  @Column(nullable = false)
  private int costMonth;

  @Column private String memo;

  protected CostEntity() {}

  private CostEntity(
      CostCategory category, String name, Long amount, int costYear, int costMonth, String memo) {
    this.category = category;
    this.name = name;
    this.amount = amount;
    this.costYear = costYear;
    this.costMonth = costMonth;
    this.memo = memo;
  }

  public static CostEntity create(
      CostCategory category, String name, Long amount, int costYear, int costMonth, String memo) {
    return new CostEntity(category, name, amount, costYear, costMonth, memo);
  }

  public CostCategory getCategory() {
    return category;
  }

  public String getName() {
    return name;
  }

  public Long getAmount() {
    return amount;
  }

  public int getCostYear() {
    return costYear;
  }

  public int getCostMonth() {
    return costMonth;
  }

  public String getMemo() {
    return memo;
  }

  public void update(
      CostCategory category, String name, Long amount, int costYear, int costMonth, String memo) {
    this.category = category;
    this.name = name;
    this.amount = amount;
    this.costYear = costYear;
    this.costMonth = costMonth;
    this.memo = memo;
  }
}
