package org.aburavov.urlshortener.redirect;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import org.aburavov.urlshortener.common.GoneException;
import org.aburavov.urlshortener.common.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedirectController {

    private final RedirectService redirectService;
    private final ApplicationEventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;

    public RedirectController(RedirectService redirectService, ApplicationEventPublisher eventPublisher,
                              MeterRegistry meterRegistry) {
        this.redirectService = redirectService;
        this.eventPublisher = eventPublisher;
        this.meterRegistry = meterRegistry;
    }

    @GetMapping("/{code:[A-Za-z0-9_-]{4,32}}")
    public ResponseEntity<Void> redirect(@PathVariable String code, HttpServletRequest request) {
        ResolvedLink link;
        try {
            link = redirectService.resolve(code);
        } catch (NotFoundException e) {
            countRedirect("not_found");
            throw e;
        }
        if (!link.isActive()) {
            countRedirect("gone");
            throw new GoneException("Link is no longer available");
        }
        countRedirect("found");
        eventPublisher.publishEvent(new LinkClickedEvent(link.id(), Instant.now(),
                request.getHeader(HttpHeaders.REFERER), request.getHeader(HttpHeaders.USER_AGENT),
                request.getRemoteAddr()));
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(link.originalUrl()))
                .cacheControl(CacheControl.noStore())
                .build();
    }

    private void countRedirect(String outcome) {
        meterRegistry.counter("shortener.redirects", "outcome", outcome).increment();
    }
}
