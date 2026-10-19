package io.spring.application.user;

import java.time.Clock;
import java.time.Duration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Fixed-window limiter for login attempts, keyed both by account (the submitted email exactly as
 * the repository looks it up, whether or not it is registered) and by client address.
 *
 * <p>An attempt is counted when it starts, so concurrent requests cannot overshoot a limit. A
 * successful login resets the account window to the attempts still in flight and refunds the
 * client; an attempt aborted by a backend error is refunded on both.
 *
 * <p>State is per application instance and holds at most {@code maxTrackedKeys} windows. Windows
 * are kept in creation order, so expired ones are dropped from the head and, if a flood of distinct
 * keys fills the table, the oldest windows are forgotten rather than refusing everyone else.
 */
@Component
public class LoginAttemptLimiter {
  private final int maxAttemptsPerAccount;
  private final int maxAttemptsPerClient;
  private final long windowMillis;
  private final int maxTrackedKeys;
  private final Clock clock;
  private final LinkedHashMap<String, Window> windows = new LinkedHashMap<>();

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
   * Counts a login attempt for {@code email} from {@code clientAddress}. The caller must report the
   * outcome through exactly one of {@link #succeeded}, {@link #failed} or {@link #aborted}.
   *
   * @throws TooManyLoginAttemptsException if either key has used up its window
   */
  public synchronized Attempt acquire(String email, String clientAddress) {
    long now = clock.millis();
    dropExpired(now);
    String accountKey = "account:" + (email == null ? "" : email);
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
      throw new TooManyLoginAttemptsException(Math.max(1, (retryAfter + 999) / 1000));
    }

    makeRoom((account == null ? 1 : 0) + (client == null ? 1 : 0), account, client);
    if (account == null) {
      account = new Window(now + windowMillis);
      windows.put(accountKey, account);
    }
    if (client == null) {
      client = new Window(now + windowMillis);
      windows.put(clientKey, client);
    }
    account.start();
    client.start();
    return new Attempt(accountKey, account, clientKey, client);
  }

  public synchronized void succeeded(Attempt attempt) {
    attempt.account.pending--;
    if (windows.get(attempt.accountKey) == attempt.account) {
      attempt.account.count = attempt.account.pending;
      removeIfEmpty(attempt.accountKey, attempt.account);
    }
    refund(attempt.clientKey, attempt.client);
  }

  public synchronized void failed(Attempt attempt) {
    attempt.account.pending--;
    attempt.client.pending--;
  }

  public synchronized void aborted(Attempt attempt) {
    refund(attempt.accountKey, attempt.account);
    refund(attempt.clientKey, attempt.client);
  }

  synchronized int trackedKeys() {
    return windows.size();
  }

  private void dropExpired(long now) {
    Iterator<Window> it = windows.values().iterator();
    while (it.hasNext() && it.next().end <= now) {
      it.remove();
    }
  }

  private void makeRoom(int newKeys, Window keepAccount, Window keepClient) {
    Iterator<Window> it = windows.values().iterator();
    while (windows.size() + newKeys > maxTrackedKeys && it.hasNext()) {
      Window oldest = it.next();
      if (oldest != keepAccount && oldest != keepClient) {
        it.remove();
      }
    }
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
    window.pending--;
    if (windows.get(key) == window) {
      window.count--;
      removeIfEmpty(key, window);
    }
  }

  private void removeIfEmpty(String key, Window window) {
    if (window.count <= 0) {
      windows.remove(key);
    }
  }

  private static final class Window {
    private final long end;
    private int count;
    private int pending;

    private Window(long end) {
      this.end = end;
    }

    private void start() {
      count++;
      pending++;
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
