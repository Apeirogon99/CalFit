package com.coffiness.calfit.domain.billing.invoice;

import com.coffiness.calfit.core.enums.InvoiceStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface InvoiceReader {

  Optional<Invoice> findById(Long id);

  List<Invoice> getAll(InvoiceStatus status, Pageable pageable);

  InvoiceSummary getSummary(int year, int month);

  List<MonthlyRevenue> getMonthlyRevenue();
}
