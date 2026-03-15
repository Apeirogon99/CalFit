package com.coffiness.calfit.domain.billing.subscription;

import com.coffiness.calfit.core.enums.SubscriptionStatus;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

  private final SubscriptionReader subscriptionReader;
  private final SubscriptionStore subscriptionStore;

  public Subscription getById(Long id) {
    return subscriptionReader
        .findById(id)
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
  }

  public List<Subscription> getAll(SubscriptionStatus status, String search, int page, int size) {
    return subscriptionReader.getAll(status, search, PageRequest.of(page, size));
  }

  public long count(SubscriptionStatus status, String search) {
    return subscriptionReader.countAll(status, search);
  }

  public long countByStatus(SubscriptionStatus status) {
    return subscriptionReader.countByStatus(status);
  }

  public long getMrr() {
    return subscriptionReader.getMrr();
  }

  @Transactional
  public Subscription activate(Long subscriptionId) {
    return subscriptionStore.updateStatus(subscriptionId, SubscriptionStatus.ACTIVE);
  }

  @Transactional
  public void cancel(Long subscriptionId) {
    subscriptionStore.cancel(subscriptionId);
  }
}
