package io.spring.application.cache;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

final class TransactionalEviction {
  private TransactionalEviction() {}

  /**
   * Runs {@code eviction} immediately so the writing transaction reads its own writes, and again
   * once the surrounding transaction (if any) commits or rolls back so that values loaded by other
   * readers while the write was still uncommitted are discarded.
   */
  static void evict(Runnable eviction) {
    eviction.run();
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
              eviction.run();
            }
          });
    }
  }
}
