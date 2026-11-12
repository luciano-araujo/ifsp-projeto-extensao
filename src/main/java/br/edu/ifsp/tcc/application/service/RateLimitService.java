package br.edu.ifsp.tcc.application.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.LongSupplier;

/**
 * In-memory fixed-window attempt counter (Caffeine). Single instance only: move to Redis if the app
 * ever runs on more than one instance.
 */
@Service
public class RateLimitService {

    private record Window(long startMillis, int count) { }

    private final long windowMillis;
    private final LongSupplier clock;
    private final Cache<String, Window> windows;

    @Autowired
    public RateLimitService(@Value("${app.rate-limit.window-minutes:10}") long windowMinutes) {
        this(windowMinutes, System::currentTimeMillis);
    }

    RateLimitService(long windowMinutes, LongSupplier clock) {
        this.windowMillis = Duration.ofMinutes(windowMinutes).toMillis();
        this.clock = clock;
        this.windows = Caffeine.newBuilder()
                .maximumSize(100_000)
                .expireAfterWrite(Duration.ofMillis(windowMillis))
                .build();
    }

    /**
     * Registers one attempt for the key; allowed while the window holds at most maxAttempts attempts.
     *
     * @return 0 when the attempt is allowed, otherwise the seconds left until the window ends
     */
    public long registerAttempt(String key, int maxAttempts) {
        long now = clock.getAsLong();
        long[] retryAfter = {0};
        windows.asMap().compute(key, (k, current) -> {
            Window window = current == null || now - current.startMillis() >= windowMillis
                    ? new Window(now, 0) : current;
            if (window.count() >= maxAttempts) {
                retryAfter[0] = Math.max(1, (window.startMillis() + windowMillis - now + 999) / 1000);
                return window;
            }
            return new Window(window.startMillis(), window.count() + 1);
        });
        return retryAfter[0];
    }
}
