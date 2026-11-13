package br.edu.ifsp.tcc.application.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** 429 raised by a use case; GlobalExceptionHandler copies the Retry-After header into the response. */
public class TooManyRequestsException extends ResponseStatusException {

    private final long retryAfterSeconds;

    public TooManyRequestsException(String reason, long retryAfterSeconds) {
        super(HttpStatus.TOO_MANY_REQUESTS, reason);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    @Override
    public HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        return headers;
    }
}
