package com.coffiness.calfit.storage.db.core.billing;

import com.coffiness.calfit.core.enums.CostCategory;
import com.coffiness.calfit.core.enums.EntityStatus;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CostRepository extends JpaRepository<CostEntity, Long> {

  @Query(
      "SELECT c FROM CostEntity c WHERE c.status = :entityStatus"
          + " AND (:year IS NULL OR c.costYear = :year)"
          + " AND (:month IS NULL OR c.costMonth = :month)"
          + " AND (:category IS NULL OR c.category = :category)"
          + " ORDER BY c.costYear DESC, c.costMonth DESC, c.createdAt DESC")
  List<CostEntity> findAllWithFilter(
      @Param("entityStatus") EntityStatus entityStatus,
      @Param("year") Integer year,
      @Param("month") Integer month,
      @Param("category") CostCategory category,
      Pageable pageable);

  @Query(
      "SELECT c.category, COALESCE(SUM(c.amount), 0)"
          + " FROM CostEntity c WHERE c.status = :entityStatus"
          + " AND c.costYear = :year AND c.costMonth = :month"
          + " GROUP BY c.category")
  List<Object[]> sumByCategoryAndMonth(
      @Param("entityStatus") EntityStatus entityStatus,
      @Param("year") int year,
      @Param("month") int month);

  @Query(
      "SELECT c.costYear, c.costMonth, COALESCE(SUM(c.amount), 0)"
          + " FROM CostEntity c WHERE c.status = :entityStatus"
          + " GROUP BY c.costYear, c.costMonth"
          + " ORDER BY c.costYear ASC, c.costMonth ASC")
  List<Object[]> findMonthlyTrend(@Param("entityStatus") EntityStatus entityStatus);

  @Query(
      "SELECT c.category, COALESCE(SUM(c.amount), 0)"
          + " FROM CostEntity c WHERE c.status = :entityStatus"
          + " AND c.costYear = :year AND c.costMonth = :month AND c.category = :category")
  Object[] sumBySpecificCategory(
      @Param("entityStatus") EntityStatus entityStatus,
      @Param("year") int year,
      @Param("month") int month,
      @Param("category") CostCategory category);
}
