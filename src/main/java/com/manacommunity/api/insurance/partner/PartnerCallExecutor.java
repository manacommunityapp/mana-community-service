package com.manacommunity.api.insurance.partner;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutionException;

/** Applies a timeout and bounded retries with exponential backoff to partner calls. Retries only retryable errors. */
public class PartnerCallExecutor {

    public interface Sleeper { void sleep(long millis) throws InterruptedException; }

    private final int maxAttempts;
    private final long baseBackoffMs;
    private final long timeoutMs;
    private final Sleeper sleeper;
    private final ExecutorService pool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "insurance-partner-call");
        t.setDaemon(true);
        return t;
    });

    public PartnerCallExecutor(int maxAttempts, long baseBackoffMs, long timeoutMs) {
        this(maxAttempts, baseBackoffMs, timeoutMs, Thread::sleep);
    }

    public PartnerCallExecutor(int maxAttempts, long baseBackoffMs, long timeoutMs, Sleeper sleeper) {
        this.maxAttempts = Math.max(1, maxAttempts);
        this.baseBackoffMs = baseBackoffMs;
        this.timeoutMs = timeoutMs;
        this.sleeper = sleeper;
    }

    public <T> T call(Callable<T> op) throws PartnerException {
        PartnerException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return callOnce(op);
            } catch (PartnerException e) {
                last = e;
                if (!e.isRetryable() || attempt == maxAttempts) {
                    throw e;
                }
                try {
                    sleeper.sleep(baseBackoffMs * (1L << (attempt - 1)));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new PartnerException("INTERRUPTED", "Interrupted", false, ie);
                }
            }
        }
        throw last;
    }

    private <T> T callOnce(Callable<T> op) throws PartnerException {
        Future<T> f = pool.submit(op);
        try {
            return f.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            f.cancel(true);
            throw new PartnerException("TIMEOUT", "Partner call timed out", true, e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof PartnerException pe) throw pe;
            throw new PartnerException("PARTNER_ERROR", "Partner call failed", false, e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PartnerException("INTERRUPTED", "Interrupted", false, e);
        }
    }
}
