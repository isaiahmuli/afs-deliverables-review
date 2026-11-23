package com.example.payments.clients;

import com.example.payments.error.DownstreamFailedException;
import com.example.payments.error.DownstreamTimeoutException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

/**
 * Runs a call to another service and turns its failures into this service's own errors: an
 * answer later than the configured read timeout is a timeout (504), anything else that goes wrong
 * is a failed dependency (502). Callers handle the statuses they expect (404, 409) first.
 */
final class Downstream {

    private static final Logger log = LoggerFactory.getLogger(Downstream.class);

    private Downstream() {
    }

    static <T> T call(String service, Supplier<T> call) {
        try {
            return call.get();
        } catch (HttpStatusCodeException e) {
            log.warn("{} answered {}", service, e.getStatusCode());
            throw new DownstreamFailedException(service);
        } catch (ResourceAccessException e) {
            if (isTimeout(e)) {
                log.warn("{} did not answer in time: {}", service, e.getMessage());
                throw new DownstreamTimeoutException(service);
            }
            log.warn("{} could not be reached: {}", service, e.getMessage());
            throw new DownstreamFailedException(service);
        }
    }

    private static boolean isTimeout(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof HttpTimeoutException || t instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }
}
