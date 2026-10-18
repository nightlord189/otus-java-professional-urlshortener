package org.aburavov.urlshortener.redirect;

import java.net.URI;
import org.aburavov.urlshortener.common.GoneException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedirectController {

    private final RedirectService redirectService;

    public RedirectController(RedirectService redirectService) {
        this.redirectService = redirectService;
    }

    @GetMapping("/{code:[A-Za-z0-9_-]{4,32}}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        ResolvedLink link = redirectService.resolve(code);
        if (!link.isActive()) {
            throw new GoneException("Link is no longer available");
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(link.originalUrl()))
                .cacheControl(CacheControl.noStore())
                .build();
    }
}
