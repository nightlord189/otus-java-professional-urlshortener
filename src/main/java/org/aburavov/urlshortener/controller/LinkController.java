package org.aburavov.urlshortener.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aburavov.urlshortener.dto.CreateLinkRequest;
import org.aburavov.urlshortener.dto.LinkResponse;
import org.aburavov.urlshortener.dto.ResolvedLink;
import org.aburavov.urlshortener.dto.UpdateLinkRequest;
import org.aburavov.urlshortener.model.Click;
import org.aburavov.urlshortener.model.LinkStatus;
import org.aburavov.urlshortener.model.User;
import org.aburavov.urlshortener.repository.ClickRepository;
import org.aburavov.urlshortener.service.LinkService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;

@RestController
public class LinkController {

    private final LinkService linkService;
    private final ClickRepository clickRepository;

    @Value("${app.base-url}")
    private String baseUrl;

    public LinkController(LinkService linkService, ClickRepository clickRepository) {
        this.linkService = linkService;
        this.clickRepository = clickRepository;
    }

    @PostMapping("/api/v1/links")
    @ResponseStatus(HttpStatus.CREATED)
    public LinkResponse create(@AuthenticationPrincipal User user, @RequestBody CreateLinkRequest request) {
        return LinkResponse.from(linkService.create(user.getId(), request), baseUrl);
    }

    @GetMapping("/api/v1/links")
    public Page<LinkResponse> list(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) LinkStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return linkService.findByOwner(user.getId(), status, page, size).map(link -> LinkResponse.from(link, baseUrl));
    }

    @GetMapping("/api/v1/links/{id}")
    public LinkResponse get(@AuthenticationPrincipal User user, @PathVariable long id) {
        return LinkResponse.from(linkService.getOwned(user.getId(), id), baseUrl);
    }

    @PatchMapping("/api/v1/links/{id}")
    public LinkResponse update(@AuthenticationPrincipal User user, @PathVariable long id, @RequestBody UpdateLinkRequest request) {
        return LinkResponse.from(linkService.update(user.getId(), id, request), baseUrl);
    }

    @DeleteMapping("/api/v1/links/{id}/expiry")
    public LinkResponse clearExpiry(@AuthenticationPrincipal User user, @PathVariable long id) {
        return LinkResponse.from(linkService.clearExpiry(user.getId(), id), baseUrl);
    }

    @DeleteMapping("/api/v1/links/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal User user, @PathVariable long id) {
        linkService.delete(user.getId(), id);
    }

    @GetMapping("/{code}")
    public void redirect(@PathVariable String code, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        ResolvedLink link = linkService.resolve(code.toLowerCase());
        if (!link.isActive()) {
            throw new ResponseStatusException(HttpStatus.GONE, "Link is disabled or expired");
        }
        clickRepository.save(new Click(link.id(), Instant.now(), request.getHeader("Referer"),
                request.getHeader("User-Agent"), request.getRemoteAddr()));
        response.sendRedirect(link.originalUrl());
    }
}
