package org.aburavov.urlshortener.link;

import org.aburavov.urlshortener.common.PageResponse;
import org.aburavov.urlshortener.link.dto.CreateLinkRequest;
import org.aburavov.urlshortener.link.dto.LinkResponse;
import org.aburavov.urlshortener.link.dto.UpdateLinkRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/links")
public class LinkController {

    private final LinkService linkService;
    private final LinkMapper linkMapper;

    public LinkController(LinkService linkService, LinkMapper linkMapper) {
        this.linkService = linkService;
        this.linkMapper = linkMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LinkResponse create(@AuthenticationPrincipal Jwt jwt, @RequestBody CreateLinkRequest request) {
        return linkMapper.toResponse(linkService.create(userId(jwt), request));
    }

    @GetMapping
    public PageResponse<LinkResponse> list(@AuthenticationPrincipal Jwt jwt,
                                           @RequestParam(required = false) LinkStatus status,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(linkService.listByOwner(userId(jwt), status, page, size)
                .map(linkMapper::toResponse));
    }

    @GetMapping("/{id}")
    public LinkResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return linkMapper.toResponse(linkService.getOwned(userId(jwt), id));
    }

    @PatchMapping("/{id}")
    public LinkResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                               @RequestBody UpdateLinkRequest request) {
        return linkMapper.toResponse(linkService.update(userId(jwt), id, request));
    }

    @DeleteMapping("/{id}/expiry")
    public LinkResponse clearExpiry(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return linkMapper.toResponse(linkService.clearExpiry(userId(jwt), id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        linkService.delete(userId(jwt), id);
    }

    private static long userId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
