package com.coffiness.calfit.storage.db.core.billing;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.InvoiceStatus;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<InvoiceEntity, Long> {

  List<InvoiceEntity> findByBillingYearAndBillingMonthAndStatus(
      int billingYear, int billingMonth, EntityStatus status);

  @Query(
      "SELECT COALESCE(SUM(i.amount), 0) FROM InvoiceEntity i WHERE i.status = :entityStatus"
          + " AND i.invoiceStatus = :invoiceStatus"
          + " AND i.billingYear = :year AND i.billingMonth = :month")
  long sumAmountByStatusAndMonth(
      @Param("entityStatus") EntityStatus entityStatus,
      @Param("invoiceStatus") InvoiceStatus invoiceStatus,
      @Param("year") int year,
      @Param("month") int month);

  @Query(
      "SELECT COUNT(i) FROM InvoiceEntity i WHERE i.status = :entityStatus"
          + " AND i.invoiceStatus = :invoiceStatus"
          + " AND i.billingYear = :year AND i.billingMonth = :month")
  long countByStatusAndMonth(
      @Param("entityStatus") EntityStatus entityStatus,
      @Param("invoiceStatus") InvoiceStatus invoiceStatus,
      @Param("year") int year,
      @Param("month") int month);

  @Query(
      "SELECT i FROM InvoiceEntity i WHERE i.status = :entityStatus"
          + " AND (:invoiceStatus IS NULL OR i.invoiceStatus = :invoiceStatus)"
          + " ORDER BY i.issuedAt DESC")
  List<InvoiceEntity> findAllWithFilter(
      @Param("entityStatus") EntityStatus entityStatus,
      @Param("invoiceStatus") InvoiceStatus invoiceStatus,
      Pageable pageable);

  @Query(
      "SELECT i.billingYear, i.billingMonth, COALESCE(SUM(i.amount), 0)"
          + " FROM InvoiceEntity i WHERE i.status = :entityStatus AND i.invoiceStatus = 'PAID'"
          + " GROUP BY i.billingYear, i.billingMonth"
          + " ORDER BY i.billingYear ASC, i.billingMonth ASC")
  List<Object[]> findMonthlyRevenue(@Param("entityStatus") EntityStatus entityStatus);

  List<InvoiceEntity> findByWorkspaceIdAndStatus(String workspaceId, EntityStatus status);
}
