package com.manacommunity.api.config.tenant;

import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

/**
 * Task decorator that explicitly captures the active {@link TenantContext} from the calling
 * thread at submission time and propagates it to worker threads in thread pools.
 * <p>
 * Crucially, it guarantees that {@link TenantContext#clear()} is called in a {@code finally}
 * block once the task completes, preventing any stale tenant identity from leaking across
 * pooled threads.
 */
@Component
public class TenantAwareTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        TenantContext captured = TenantContext.getRawContext();
        TenantContext contextCopy = captured != null ? captured.copy() : null;

        return () -> {
            try {
                if (contextCopy != null) {
                    TenantContext.setContext(contextCopy);
                } else {
                    TenantContext.clear();
                }
                runnable.run();
            } finally {
                TenantContext.clear();
            }
        };
    }
}
