package io.spring.application.user;

import java.time.Clock;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Fixed-window limiter for login attempts, keyed both by account (normalised email, whether or not
 * it is registered) and by client address.
 *
 * <p>An attempt is counted when it starts, so concurrent requests cannot overshoot a limit. A
 * successful login refunds the client and clears the account window; an attempt aborted by a
 * backend error is refunded on both. State is per application instance and bounded by {@code
 * maxTrackedKeys}.
 */
@Component
public class LoginAttemptLimiter {
  private final int maxAttemptsPerAccount;
  private final int maxAttemptsPerClient;
  private final long windowMillis;
  private final int maxTrackedKeys;
  private final Clock clock;
  private final Map<String, Window> windows = new HashMap<>();
  private long nextPruneAt;

  @Autowired
  public LoginAttemptLimiter(
      @Value("${security.login.max-attempts-per-account:5}") int maxAttemptsPerAccount,
      @Value("${security.login.max-attempts-per-client:20}") int maxAttemptsPerClient,
      @Value("${security.login.window:PT15M}") Duration window,
      @Value("${security.login.max-tracked-keys:100000}") int maxTrackedKeys) {
    this(maxAttemptsPerAccount, maxAttemptsPerClient, window, maxTrackedKeys, Clock.systemUTC());
  }

  LoginAttemptLimiter(
      int maxAttemptsPerAccount,
      int maxAttemptsPerClient,
      Duration window,
      int maxTrackedKeys,
      Clock clock) {
    if (maxAttemptsPerAccount < 1 || maxAttemptsPerClient < 1 || maxTrackedKeys < 2) {
      throw new IllegalArgumentException("login attempt limits must be positive");
    }
    if (window.isNegative() || window.isZero()) {
      throw new IllegalArgumentException("login attempt window must be positive");
    }
    this.maxAttemptsPerAccount = maxAttemptsPerAccount;
    this.maxAttemptsPerClient = maxAttemptsPerClient;
    this.windowMillis = window.toMillis();
    this.maxTrackedKeys = maxTrackedKeys;
    this.clock = clock;
  }

  /**
   * Counts a login attempt for {@code email} from {@code clientAddress}.
   *
   * @throws TooManyLoginAttemptsException if either key has used up its window
   */
  public synchronized Attempt acquire(String email, String clientAddress) {
    long now = clock.millis();
    String accountKey = "account:" + normalise(email);
    String clientKey = "client:" + (clientAddress == null ? "unknown" : clientAddress);
    Window account = live(accountKey, now);
    Window client = live(clientKey, now);

    long retryAfter = 0;
    if (account != null && account.count >= maxAttemptsPerAccount) {
      retryAfter = account.end - now;
    }
    if (client != null && client.count >= maxAttemptsPerClient) {
      retryAfter = Math.max(retryAfter, client.end - now);
    }
    if (retryAfter > 0) {
      throw new TooManyLoginAttemptsException(toSeconds(retryAfter));
    }

    int newKeys = (account == null ? 1 : 0) + (client == null ? 1 : 0);
    if (windows.size() + newKeys > maxTrackedKeys) {
      prune(now);
      if (windows.size() + newKeys > maxTrackedKeys) {
        throw new TooManyLoginAttemptsException(toSeconds(nextPruneAt - now));
      }
    }
    if (account == null) {
      account = new Window(now + windowMillis);
      windows.put(accountKey, account);
    }
    if (client == null) {
      client = new Window(now + windowMillis);
      windows.put(clientKey, client);
    }
    account.count++;
    client.count++;
    return new Attempt(accountKey, account, clientKey, client);
  }

  public synchronized void succeeded(Attempt attempt) {
    windows.remove(attempt.accountKey, attempt.account);
    refund(attempt.clientKey, attempt.client);
  }

  public synchronized void aborted(Attempt attempt) {
    refund(attempt.accountKey, attempt.account);
    refund(attempt.clientKey, attempt.client);
  }

  synchronized int trackedKeys() {
    return windows.size();
  }

  private Window live(String key, long now) {
    Window window = windows.get(key);
    if (window != null && window.end <= now) {
      windows.remove(key);
      return null;
    }
    return window;
  }

  private void refund(String key, Window window) {
    if (windows.get(key) == window && --window.count <= 0) {
      windows.remove(key);
    }
  }

  private void prune(long now) {
    if (now < nextPruneAt) {
      return;
    }
    windows.values().removeIf(w -> w.end <= now);
    nextPruneAt = windows.values().stream().mapToLong(w -> w.end).min().orElse(now);
  }

  private static String normalise(String email) {
    return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
  }

  private static long toSeconds(long millis) {
    return Math.max(1, (millis + 999) / 1000);
  }

  private static final class Window {
    private final long end;
    private int count;

    private Window(long end) {
      this.end = end;
    }
  }

  public static final class Attempt {
    private final String accountKey;
    private final Window account;
    private final String clientKey;
    private final Window client;

    private Attempt(String accountKey, Window account, String clientKey, Window client) {
      this.accountKey = accountKey;
      this.account = account;
      this.clientKey = clientKey;
      this.client = client;
    }
  }
}
